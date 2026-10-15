package com.skyorigins2core.network;

import com.skyorigins2core.SkyOrigins2Core;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork
{
    // Bump when packets change, so mismatched client and server versions refuse to connect
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SkyOrigins2Core.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {}

    public static void register()
    {
        CHANNEL.messageBuilder(ManaSyncPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ManaSyncPacket::encode)
                .decoder(ManaSyncPacket::decode)
                .consumerMainThread(ManaSyncPacket::handle)
                .add();
    }

    public static void sendTo(ServerPlayer player, Object message)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
