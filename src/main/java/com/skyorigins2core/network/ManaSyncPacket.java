package com.skyorigins2core.network;

import com.skyorigins2core.mana.ClientManaData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Server -> client: the player's current and maximum mana, for the mana bar
public record ManaSyncPacket(int mana, int maxMana)
{
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarInt(mana);
        buf.writeVarInt(maxMana);
    }

    public static ManaSyncPacket decode(FriendlyByteBuf buf)
    {
        return new ManaSyncPacket(buf.readVarInt(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        ClientManaData.set(mana, maxMana);
        context.get().setPacketHandled(true);
    }
}
