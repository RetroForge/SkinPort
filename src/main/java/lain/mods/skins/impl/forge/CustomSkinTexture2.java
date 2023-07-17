package lain.mods.skins.impl.forge;

import lain.mods.skins.api.interfaces.ISkinTexture;
import lain.mods.skins.impl.SkinData;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class CustomSkinTexture2 extends AbstractTexture
{

    private final ResourceLocation _location;
    private final BufferedImage image;

    public CustomSkinTexture2(ResourceLocation location, BufferedImage image)
    {
        _location = location;
        this.image = image;
    }

    public ResourceLocation getLocation()
    {
        return _location;
    }

    @Override
    public void loadTexture(IResourceManager resMan) throws IOException
    {
        deleteGlTexture();

        TextureUtil.uploadTextureImageAllocate(getGlTextureId(), image, false, false);
    }

}
