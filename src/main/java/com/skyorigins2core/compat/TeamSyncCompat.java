package com.skyorigins2core.compat;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import de.melanx.skyblockbuilder.data.SkyblockSavedData;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Skyblock Builder and FTB Teams keep separate teams, so island teammates would have separate FTB Quests
// progress. This mirrors every Skyblock Builder island team into an FTB Teams party, with Skyblock Builder
// as the source of truth: joining, leaving or being moved between island teams moves the player between
// the matching parties. Parties made by this class are tagged with their island team's id.
//
// It reconciles once a second and on login rather than reacting to Skyblock Builder's events, because those
// fire before the change is applied (and can still be cancelled). Online players are moved immediately;
// players removed while offline are dropped from their party at the next pass.
//
// Only loaded when both Skyblock Builder and FTB Teams are installed. PartyTeam.join/leave/transferOwnership
// are FTB Teams internals (its public API can create parties but not move players), so recheck this class
// when updating FTB Teams.
public final class TeamSyncCompat
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "skyorigins2core:island_team";
    private static final int INTERVAL = 20;

    private static int ticks;

    private TeamSyncCompat() {}

    public static void register()
    {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && ++ticks >= INTERVAL)
            {
                ticks = 0;
                reconcile(event.getServer());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player)
                reconcile(player.server);
        });
    }

    static void reconcile(MinecraftServer server)
    {
        if (!FTBTeamsAPI.api().isManagerLoaded())
            return;
        TeamManager manager = FTBTeamsAPI.api().getManager();
        SkyblockSavedData islands = SkyblockSavedData.get(server.overworld());

        // Island team id -> its party, for parties this class made
        Map<UUID, PartyTeam> parties = new HashMap<>();
        for (Team team : manager.getTeams())
            if (team instanceof PartyTeam party && party.getExtraData().hasUUID(TAG))
                parties.put(party.getExtraData().getUUID(TAG), party);

        // Drop members (online or not) who are no longer on the party's island team
        for (Map.Entry<UUID, PartyTeam> entry : parties.entrySet())
        {
            de.melanx.skyblockbuilder.data.Team island = islandTeam(islands, entry.getKey());
            PartyTeam party = entry.getValue();
            for (UUID member : new ArrayList<>(party.getMembers()))
                if (island == null || !island.hasPlayer(member))
                    leave(server, party, member, island);
        }

        // Put every online player in the party matching their island team (or no synced party)
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            de.melanx.skyblockbuilder.data.Team island = islands.hasPlayerTeam(player) ? islands.getTeamFromPlayer(player) : null;
            if (island != null && island.isSpawn())
                island = null;
            Team current = manager.getTeamForPlayer(player).orElse(null);
            PartyTeam want = island == null ? null : parties.get(island.getId());
            if (current == null || (want != null && current.getId().equals(want.getId())))
                continue;
            if (island == null)
                continue; // not on an island team; leave synced parties to the loop above, manual parties alone
            if (current instanceof PartyTeam currentParty && !leave(server, currentParty, player.getUUID(), null))
                continue;
            try
            {
                if (want != null)
                    want.join(player);
                else
                    parties.put(island.getId(), createParty(manager, player, island));
            }
            catch (CommandSyntaxException e)
            {
                LOGGER.warn("Couldn't put {} in the FTB Teams party for island team {}: {}", player.getGameProfile().getName(), island.getName(), e.getMessage());
            }
        }
    }

    private static de.melanx.skyblockbuilder.data.Team islandTeam(SkyblockSavedData islands, UUID id)
    {
        for (de.melanx.skyblockbuilder.data.Team team : islands.getTeams())
            if (team.getId().equals(id))
                return team;
        return null;
    }

    private static PartyTeam createParty(TeamManager manager, ServerPlayer owner, de.melanx.skyblockbuilder.data.Team island) throws CommandSyntaxException
    {
        String name = island.getName();
        if (manager.getTeamByName(name).isPresent())
            name = name + " " + island.getId().toString().substring(0, 4);
        PartyTeam party = (PartyTeam) manager.createPartyTeam(owner, name, "Island team (synced from Skyblock Builder)", Color4I.rgb(0x6FC0FF));
        party.getExtraData().putUUID(TAG, island.getId());
        party.markDirty();
        return party;
    }

    // Removes a member, handing the party to another member first if they own it. Returns false if it can't.
    private static boolean leave(MinecraftServer server, PartyTeam party, UUID member, de.melanx.skyblockbuilder.data.Team island)
    {
        try
        {
            Set<UUID> members = party.getMembers();
            if (party.isOwner(member) && members.size() > 1)
            {
                Optional<UUID> heir = members.stream()
                        .filter(id -> !id.equals(member))
                        .filter(id -> island == null || island.hasPlayer(id))
                        .findFirst();
                if (heir.isEmpty())
                    heir = members.stream().filter(id -> !id.equals(member)).findFirst();
                GameProfile profile = server.getProfileCache() == null ? null : server.getProfileCache().get(heir.get()).orElse(null);
                party.transferOwnership(server.createCommandSourceStack().withSuppressedOutput(),
                        profile != null ? profile : new GameProfile(heir.get(), null));
            }
            party.leave(member);
            return true;
        }
        catch (CommandSyntaxException e)
        {
            LOGGER.warn("Couldn't remove {} from FTB Teams party {}: {}", member, party.getShortName(), e.getMessage());
            return false;
        }
    }
}
