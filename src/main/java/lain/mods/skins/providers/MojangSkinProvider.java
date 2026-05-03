package lain.mods.skins.providers;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import lain.lib.SharedPool;
import lain.mods.skins.api.interfaces.IPlayerProfile;
import lain.mods.skins.api.interfaces.ISkin;
import lain.mods.skins.api.interfaces.ISkinProvider;
import lain.mods.skins.impl.Shared;
import lain.mods.skins.impl.SkinData;
import lain.mods.skins.impl.forge.MinecraftUtils;

public class MojangSkinProvider implements ISkinProvider {

    private static final Logger LOGGER = LogManager.getLogger("SkinPort/MojangSkinProvider");

    private Function<ByteBuffer, ByteBuffer> _filter;

    @Override
    public ISkin getSkin(IPlayerProfile profile) {
        SkinData skin = new SkinData();
        if (_filter != null) skin.setSkinFilter(_filter);

        LOGGER.debug("Getting skin for player: {} (UUID: {})", profile.getPlayerName(), profile.getPlayerID());

        SharedPool.execute(() -> {
            if (!Shared.isOfflinePlayer(profile.getPlayerID(), profile.getPlayerName())) {
                LOGGER.debug("Player {} is online, fetching textures from Mojang", profile.getPlayerName());

                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = MinecraftUtils.getSessionService()
                    .getTextures((GameProfile) profile.getOriginal(), false);

                if (textures != null && textures.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                    MinecraftProfileTexture tex = textures.get(MinecraftProfileTexture.Type.SKIN);
                    String skinType = SkinData.getSkinType(tex);

                    LOGGER.debug(
                        "Found skin texture for {}: URL={}, Type={}",
                        profile.getPlayerName(),
                        tex.getUrl(),
                        skinType);

                    Shared.downloadSkin(tex.getUrl(), Runnable::run)
                        .thenApply(Optional::get)
                        .thenAccept(data -> {
                            if (SkinData.validateData(data)) {
                                LOGGER.debug(
                                    "Successfully downloaded and validated skin for {} (type: {})",
                                    profile.getPlayerName(),
                                    skinType);
                                skin.put(data, skinType);
                            } else {
                                LOGGER.warn("Invalid skin data downloaded for {}", profile.getPlayerName());
                            }
                        })
                        .exceptionally(ex -> {
                            LOGGER.error(
                                "Failed to download skin for {}: {}",
                                profile.getPlayerName(),
                                ex.getMessage(),
                                ex);
                            return null;
                        });
                } else {
                    LOGGER.debug("No skin texture found for {}", profile.getPlayerName());
                }
            } else {
                LOGGER.debug("Player {} is offline, skipping Mojang skin fetch", profile.getPlayerName());
            }
        });
        return skin;
    }

    public MojangSkinProvider withFilter(Function<ByteBuffer, ByteBuffer> filter) {
        _filter = filter;
        LOGGER.debug("Skin filter applied to MojangSkinProvider");
        return this;
    }

}
