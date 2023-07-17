package lain.mods.skinport.init.forge;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import lain.mods.skinport.impl.forge.SkinCustomization;
import lain.mods.skinport.impl.forge.network.NetworkManager;
import lain.mods.skinport.impl.forge.network.packet.PacketGet0;
import lain.mods.skinport.impl.forge.network.packet.PacketGet1;
import lain.mods.skinport.impl.forge.network.packet.PacketPut0;
import lain.mods.skinport.impl.forge.network.packet.PacketPut1;
import lain.mods.skins.api.SkinProviderAPI;
import lain.mods.skins.api.interfaces.IPlayerProfile;
import lain.mods.skins.api.interfaces.ISkin;
import lain.mods.skins.api.interfaces.ISkinProvider;
import lain.mods.skins.impl.LegacyConversion;
import lain.mods.skins.impl.SkinData;
import lain.mods.skins.providers.MojangCapeProvider;
import lain.mods.skins.providers.MojangSkinProvider;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = "skinport", useMetadata = true)
public class ForgeSkinPort
{

    private static class DefaultSkinProvider implements ISkinProvider
    {

        ISkin DefaultSteve;
        ISkin DefaultAlex;

        DefaultSkinProvider()
        {
            try
            {
                byte[] data;
                ((SkinData) (DefaultSteve = new SkinData())).put(data = IOUtils.toByteArray(DefaultSkinProvider.class.getResource("/DefaultSteve.png")), "default");
                ((SkinData) (DefaultAlex = new SkinData())).put(data = IOUtils.toByteArray(DefaultSkinProvider.class.getResource("/DefaultAlex.png")), "slim");
            }
            catch (IOException e)
            {
                DefaultSteve = null;
                DefaultAlex = null;
            }
        }

        @Override
        public ISkin getSkin(IPlayerProfile profile)
        {
            if(profile == null) {
                return DefaultSteve;
            }
            UUID uuid;
            if ((uuid = profile.getPlayerID()) != null && (uuid.hashCode() & 0x1) == 1) {
                return DefaultAlex;
            }
            return DefaultSteve;
        }

    }

    @SidedProxy(clientSide = "lain.mods.skinport.init.forge.ClientProxy", serverSide = "lain.mods.skinport.init.forge.CommonProxy")
    public static CommonProxy proxy = new CommonProxy();
    public static NetworkManager network = new NetworkManager("skinport");

    public static void loadOptions()
    {
        try
        {
            for (String line : FileUtils.readLines(Paths.get(".", "options_skinport.txt").toFile(), StandardCharsets.UTF_8))
            {
                String[] as = line.split(":", 2);
                if (as.length != 2 || as[0].startsWith("#"))
                    continue;
                if ("clientFlags".equals(as[0]))
                    SkinCustomization.ClientFlags = Integer.parseInt(as[1]);
            }
        }
        catch (FileNotFoundException | NumberFormatException e)
        {
            saveOptions();
        }
        catch (IOException e)
        {
            System.err.println(String.format("Error loading options: %s", e.getMessage()));
        }
    }

    public static void saveOptions()
    {
        try
        {
            FileUtils.write(Paths.get(".", "options_skinport.txt").toFile(), String.format("clientFlags:%d", SkinCustomization.ClientFlags), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            System.err.println(String.format("Error saving options: %s", e.getMessage()));
        }
    }

    @Mod.EventHandler
    public void init(FMLPreInitializationEvent event)
    {
        if (event.getSide().isClient())
        {
            DefaultSkins.INSTANCE = new DefaultSkins();
            loadOptions();
            SkinProviderAPI.SKIN.clearProviders();
            //SkinProviderAPI.SKIN.registerProvider(new MojangSkinProvider().withFilter(LegacyConversion.createFilter()));
            SkinProviderAPI.SKIN.registerProvider(new DefaultSkinProvider());
            SkinProviderAPI.CAPE.clearProviders();
            //SkinProviderAPI.CAPE.registerProvider(new MojangCapeProvider());
        }

        network.registerPacket(1, PacketGet0.class);
        network.registerPacket(2, PacketPut0.class);
        network.registerPacket(3, PacketGet1.class);
        network.registerPacket(4, PacketPut1.class);

        MinecraftForge.EVENT_BUS.register(proxy);
        FMLCommonHandler.instance().bus().register(proxy);
    }

}
