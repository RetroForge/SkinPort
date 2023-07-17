package lain.mods.skinport.init.forge;

import com.mojang.authlib.GameProfile;
import cpw.mods.fml.client.FMLClientHandler;
import lain.mods.skins.impl.forge.CustomSkinTexture;
import lain.mods.skins.impl.forge.CustomSkinTexture2;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

public class DefaultSkins {
    private CustomSkinTexture2 steve;
    private CustomSkinTexture2 alex;
    private volatile boolean isInit;

    public static DefaultSkins INSTANCE;
    public void init() {
        if(!isInit) {
            steve = new CustomSkinTexture2(new ResourceLocation("skinport", "steve"), loadImage(DefaultSkins.class.getResource("/DefaultSteve.png")));
            FMLClientHandler.instance().getClient().getTextureManager().loadTexture(steve.getLocation(), steve);
            alex = new CustomSkinTexture2(new ResourceLocation("skinport", "alex"), loadImage(DefaultSkins.class.getResource("/DefaultAlex.png")));
            FMLClientHandler.instance().getClient().getTextureManager().loadTexture(alex.getLocation(), alex);
            isInit = true;
        }
    }

    public CustomSkinTexture2 get(GameProfile profile) {
        init();
        if(profile == null) {
            return steve;
        }
        UUID uuid;
        if ((uuid = profile.getId()) != null && (uuid.hashCode() & 0x1) == 1) {
            return alex;
        }
        return steve;
    }

    public String getModel(GameProfile profile) {
        UUID uuid;
        if ((uuid = profile.getId()) != null && (uuid.hashCode() & 0x1) == 1) {
            return "slim";
        }
        return "default";
    }

    public CustomSkinTexture2 getSteve() {
        return steve;
    }

    public CustomSkinTexture2 getAlex() {
        return alex;
    }

    private BufferedImage loadImage(URL url) {
        try {
            URLConnection c = url.openConnection();
            try(InputStream input = c.getInputStream()) {
                return ImageIO.read(input);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
