package lain.mods.skins.providers;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import lain.lib.SharedPool;
import lain.mods.skins.api.interfaces.IPlayerProfile;
import lain.mods.skins.api.interfaces.ISkin;
import lain.mods.skins.api.interfaces.ISkinProvider;
import lain.mods.skins.impl.Shared;
import lain.mods.skins.impl.SkinData;
import lain.mods.skins.impl.forge.MinecraftUtils;

public class MojangSkinProvider implements ISkinProvider
{

    private Function<ByteBuffer, ByteBuffer> _filter;

    @Override
    public ISkin getSkin(IPlayerProfile profile)
    {
        SkinData skin = new SkinData();
        if (_filter != null)
            skin.setSkinFilter(_filter);
        SharedPool.execute(() -> {
            //if (!Shared.isOfflinePlayer(profile.getPlayerID(), profile.getPlayerName())) {
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = MinecraftUtils.getSessionService().getTextures((GameProfile) profile.getOriginal(), false);
                if (textures != null && textures.containsKey(MinecraftProfileTexture.Type.SKIN))
                {
                    MinecraftProfileTexture tex = textures.get(MinecraftProfileTexture.Type.SKIN);
                    Shared.downloadSkin(tex.getUrl(), Runnable::run).thenApply(Optional::get).thenAccept(data -> {
                        if (SkinData.validateData(data))
                            skin.put(data, SkinData.getSkinType(tex));
                    });
                }
            //}
        });
        return skin;
    }

    public MojangSkinProvider withFilter(Function<ByteBuffer, ByteBuffer> filter)
    {
        _filter = filter;
        return this;
    }

}
