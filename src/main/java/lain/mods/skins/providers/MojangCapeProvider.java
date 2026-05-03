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

public class MojangCapeProvider implements ISkinProvider {

    private static final Logger LOGGER = LogManager.getLogger("SkinPort/MojangCapeProvider");

    private Function<ByteBuffer, ByteBuffer> _filter;

    @Override
    public ISkin getSkin(IPlayerProfile profile) {
        SkinData skin = new SkinData();
        if (_filter != null) skin.setSkinFilter(_filter);

        LOGGER.debug("Getting cape for player: {} (UUID: {})", profile.getPlayerName(), profile.getPlayerID());

        SharedPool.execute(() -> {
            if (!Shared.isOfflinePlayer(profile.getPlayerID(), profile.getPlayerName())) {
                LOGGER.debug("Player {} is online, fetching cape from Mojang", profile.getPlayerName());

                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = MinecraftUtils.getSessionService()
                    .getTextures((GameProfile) profile.getOriginal(), false);

                if (textures != null && textures.containsKey(MinecraftProfileTexture.Type.CAPE)) {
                    MinecraftProfileTexture tex = textures.get(MinecraftProfileTexture.Type.CAPE);

                    LOGGER.debug("Found cape texture for {}: URL={}", profile.getPlayerName(), tex.getUrl());

                    Shared.downloadSkin(tex.getUrl(), Runnable::run)
                        .thenApply(Optional::get)
                        .thenAccept(data -> {
                            if (SkinData.validateData(data)) {
                                LOGGER.debug(
                                    "Successfully downloaded and validated cape for {}",
                                    profile.getPlayerName());
                                skin.put(data, "cape");
                            } else {
                                LOGGER.warn("Invalid cape data downloaded for {}", profile.getPlayerName());
                            }
                        })
                        .exceptionally(ex -> {
                            LOGGER.error(
                                "Failed to download cape for {}: {}",
                                profile.getPlayerName(),
                                ex.getMessage(),
                                ex);
                            return null;
                        });
                } else {
                    LOGGER.debug("No cape texture found for {}", profile.getPlayerName());
                }
            } else {
                LOGGER.debug("Player {} is offline, skipping Mojang cape fetch", profile.getPlayerName());
            }
        });
        return skin;
    }

    public MojangCapeProvider withFilter(Function<ByteBuffer, ByteBuffer> filter) {
        _filter = filter;
        LOGGER.debug("Cape filter applied to MojangCapeProvider");
        return this;
    }

}
