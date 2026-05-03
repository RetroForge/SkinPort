package lain.mods.skinport.impl.forge.network;

import java.util.EnumMap;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;

public class NetworkManager {

    private static final int MIN_DISCRIMINATOR = 0;
    private static final int MAX_DISCRIMINATOR = 255;

    private final NetworkPacketCodec codec = new NetworkPacketCodec();
    private final NetworkPacketHandler handler = new NetworkPacketHandler();

    private EnumMap<Side, FMLEmbeddedChannel> channels;

    public NetworkManager(String channelName) {
        channels = NetworkRegistry.INSTANCE.newChannel(channelName, codec);
        for (FMLEmbeddedChannel channel : channels.values()) {
            channel.pipeline()
                .addAfter(channel.findChannelHandlerNameForType(codec.getClass()), "NetworkPacketHandler", handler);
        }
    }

    public void registerPacket(int discriminator, Class<? extends NetworkPacket> packetClass) {
        if (discriminator < MIN_DISCRIMINATOR || discriminator > MAX_DISCRIMINATOR) {
            throw new IllegalArgumentException(
                String.format(
                    "Invalid discriminator %d, valid range: %d-%d",
                    discriminator,
                    MIN_DISCRIMINATOR,
                    MAX_DISCRIMINATOR));
        }
        codec.addDiscriminator(discriminator, packetClass);
    }

    public void sendTo(NetworkPacket packet, EntityPlayerMP player) {
        if (packet == null || player == null) {
            return;
        }

        FMLEmbeddedChannel serverChannel = channels.get(Side.SERVER);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET)
            .set(FMLOutboundHandler.OutboundTarget.PLAYER);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS)
            .set(player);
        serverChannel.writeAndFlush(packet);
    }

    public void sendToAll(NetworkPacket packet) {
        if (packet == null) {
            return;
        }

        FMLEmbeddedChannel serverChannel = channels.get(Side.SERVER);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET)
            .set(FMLOutboundHandler.OutboundTarget.ALL);
        serverChannel.writeAndFlush(packet);
    }

    public void sendToAllAround(NetworkPacket packet, TargetPoint point) {
        if (packet == null || point == null) {
            return;
        }

        FMLEmbeddedChannel serverChannel = channels.get(Side.SERVER);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET)
            .set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS)
            .set(point);
        serverChannel.writeAndFlush(packet);
    }

    public void sendToDimension(NetworkPacket packet, int dimensionId) {
        if (packet == null) {
            return;
        }

        FMLEmbeddedChannel serverChannel = channels.get(Side.SERVER);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET)
            .set(FMLOutboundHandler.OutboundTarget.DIMENSION);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS)
            .set(dimensionId);
        serverChannel.writeAndFlush(packet);
    }

    public void sendToServer(NetworkPacket packet) {
        if (packet == null) {
            return;
        }

        FMLEmbeddedChannel clientChannel = channels.get(Side.CLIENT);
        clientChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET)
            .set(FMLOutboundHandler.OutboundTarget.TOSERVER);
        clientChannel.writeAndFlush(packet);
    }

}
