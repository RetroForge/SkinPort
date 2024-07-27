package lain.mods.skinport.impl.forge.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import io.netty.buffer.ByteBuf;
import lain.mods.skinport.impl.forge.SkinCustomization;
import lain.mods.skinport.impl.forge.network.NetworkPacket;
import lain.mods.skinport.init.forge.ForgeSkinPort;

public class PacketGet0 extends NetworkPacket {

    public PacketGet0() {}

    @Override
    public void handlePacketClient() {
        ForgeSkinPort.network.sendToServer(new PacketPut0(SkinCustomization.ClientFlags));
    }

    @Override
    public void handlePacketServer(EntityPlayerMP player) {}

    @Override
    public void readFromBuffer(ByteBuf buf) {}

    @Override
    public void writeToBuffer(ByteBuf buf) {}

}
