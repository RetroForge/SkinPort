package lain.mods.skinport.init.forge;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.UUID;

import net.minecraftforge.common.MinecraftForge;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

@Mod(modid = "skinport", useMetadata = true)
public class ForgeSkinPort {

    private static final Logger LOGGER = LogManager.getLogger("SkinPort");
    private static final String OPTIONS_FILE = "options_skinport.txt";
    private static final String CLIENT_FLAGS_KEY = "clientFlags";

    private static class DefaultSkinProvider implements ISkinProvider {

        private static final int HASH_MASK = 0x1;

        private final byte[] defaultSteve;
        private final byte[] defaultAlex;

        DefaultSkinProvider() {
            byte[] steve = null;
            byte[] alex = null;

            try {
                LOGGER.debug("Loading default Steve skin");
                steve = IOUtils.toByteArray(DefaultSkinProvider.class.getResource("/DefaultSteve.png"));
                LOGGER.debug("Default Steve skin loaded successfully");

                LOGGER.debug("Loading default Alex skin");
                alex = IOUtils.toByteArray(DefaultSkinProvider.class.getResource("/DefaultAlex.png"));
                LOGGER.debug("Default Alex skin loaded successfully");
            } catch (IOException e) {
                LOGGER.error("Failed to load default skins", e);
            }

            this.defaultSteve = steve;
            this.defaultAlex = alex;
        }

        @Override
        public ISkin getSkin(IPlayerProfile profile) {
            UUID uuid = profile.getPlayerID();
            if (uuid != null && (uuid.hashCode() & HASH_MASK) == 1) {
                LOGGER.debug("Using default Alex skin for player: {} (UUID: {})", profile.getPlayerName(), uuid);
                return createSkin(defaultAlex, "slim");
            }
            LOGGER.debug("Using default Steve skin for player: {} (UUID: {})", profile.getPlayerName(), uuid);
            return createSkin(defaultSteve, "default");
        }

        private ISkin createSkin(byte[] data, String type) {
            if (data == null) return null;

            SkinData skin = new SkinData();
            skin.put(data, type);
            return skin;
        }

    }

    @SidedProxy(
        clientSide = "lain.mods.skinport.init.forge.ClientProxy",
        serverSide = "lain.mods.skinport.init.forge.CommonProxy")
    public static CommonProxy proxy = new CommonProxy();
    public static NetworkManager network = new NetworkManager("skinport");

    public static void loadOptions() {
        try {
            File optionsFile = Paths.get(".", OPTIONS_FILE)
                .toFile();
            LOGGER.debug("Loading options from: {}", optionsFile.getAbsolutePath());

            if (!optionsFile.exists()) {
                LOGGER.info("Options file does not exist, creating default options");
                saveOptions();
                return;
            }

            for (String line : FileUtils.readLines(optionsFile, StandardCharsets.UTF_8)) {
                String[] parts = line.split(":", 2);
                if (parts.length != 2 || parts[0].startsWith("#")) {
                    continue;
                }
                if (CLIENT_FLAGS_KEY.equals(parts[0])) {
                    SkinCustomization.ClientFlags = Integer.parseInt(parts[1]);
                    LOGGER.debug("Loaded client flags: {}", SkinCustomization.ClientFlags);
                }
            }
            LOGGER.info("Options loaded successfully");
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid number format in options file, using defaults: {}", e.getMessage());
            saveOptions();
        } catch (IOException e) {
            LOGGER.error("Error loading options from {}: {}", OPTIONS_FILE, e.getMessage(), e);
            LOGGER.info("Creating default options file");
            saveOptions();
        }
    }

    public static void saveOptions() {
        try {
            File optionsFile = Paths.get(".", OPTIONS_FILE)
                .toFile();
            LOGGER.debug("Saving options to: {}", optionsFile.getAbsolutePath());

            FileUtils.write(
                optionsFile,
                String.format("%s:%d", CLIENT_FLAGS_KEY, SkinCustomization.ClientFlags),
                StandardCharsets.UTF_8);
            LOGGER.info("Options saved successfully");
        } catch (IOException e) {
            LOGGER.error("Error saving options to {}: {}", OPTIONS_FILE, e.getMessage(), e);
        }
    }

    @Mod.EventHandler
    public void init(FMLPreInitializationEvent event) {
        LOGGER.info("Initializing SkinPort mod");

        if (event.getSide()
            .isClient()) {
            LOGGER.info("Client side initialization");
            loadOptions();

            LOGGER.debug("Clearing existing skin providers");
            SkinProviderAPI.SKIN.clearProviders();

            LOGGER.info("Registering MojangSkinProvider with legacy conversion filter");
            SkinProviderAPI.SKIN.registerProvider(new MojangSkinProvider().withFilter(LegacyConversion.createFilter()));

            LOGGER.info("Registering DefaultSkinProvider");
            SkinProviderAPI.SKIN.registerProvider(new DefaultSkinProvider());

            LOGGER.debug("Clearing existing cape providers");
            SkinProviderAPI.CAPE.clearProviders();

            LOGGER.info("Registering MojangCapeProvider");
            SkinProviderAPI.CAPE.registerProvider(new MojangCapeProvider());
        }

        LOGGER.debug("Registering network packets");
        network.registerPacket(1, PacketGet0.class);
        network.registerPacket(2, PacketPut0.class);
        network.registerPacket(3, PacketGet1.class);
        network.registerPacket(4, PacketPut1.class);
        LOGGER.debug("Network packets registered");

        LOGGER.debug("Registering event handlers");
        MinecraftForge.EVENT_BUS.register(proxy);
        FMLCommonHandler.instance()
            .bus()
            .register(proxy);

        LOGGER.info("SkinPort mod initialization complete");
    }

}
