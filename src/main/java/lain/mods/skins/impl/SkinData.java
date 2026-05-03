package lain.mods.skins.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import lain.mods.skins.api.interfaces.ISkin;

public class SkinData implements ISkin {

    private static final Logger LOGGER = LogManager.getLogger("SkinPort/SkinData");

    private static final String SKIN_TYPE_DEFAULT = "default";
    private static final String SKIN_TYPE_SLIM = "slim";
    private static final String SKIN_TYPE_UNKNOWN = "unknown";
    private static final int ALPHA_SHIFT = 24;
    private static final int ALPHA_MASK = 0xFF000000;
    private static final int SLIM_CHECK_X = 55;
    private static final int SLIM_CHECK_Y = 20;
    private static final int TEXTURE_SIZE_64 = 64;

    public static String getSkinType(MinecraftProfileTexture tex) {
        String model = tex.getMetadata("model");
        String skinType = model != null ? model : SKIN_TYPE_DEFAULT;
        LOGGER.debug("Skin type from authlib metadata: {}", skinType);
        return skinType;
    }

    public static String judgeSkinType(byte[] data) {
        LOGGER.debug("Judging skin type from byte array (length: {})", data != null ? data.length : 0);
        try (InputStream input = new ByteArrayInputStream(data)) {
            String type = judgeSkinTypeFromImage(ImageIO.read(input));
            LOGGER.debug("Judged skin type: {}", type);
            return type;
        } catch (Throwable t) {
            LOGGER.warn("Failed to judge skin type from byte array", t);
            return SKIN_TYPE_UNKNOWN;
        }
    }

    public static String judgeSkinType(ByteBuffer data) {
        LOGGER.debug("Judging skin type from ByteBuffer (capacity: {})", data != null ? data.capacity() : 0);
        try (InputStream input = wrapByteBufferAsInputStream(data)) {
            String type = judgeSkinTypeFromImage(ImageIO.read(input));
            LOGGER.debug("Judged skin type: {}", type);
            return type;
        } catch (Throwable t) {
            LOGGER.warn("Failed to judge skin type from ByteBuffer", t);
            return SKIN_TYPE_UNKNOWN;
        }
    }

    private static String judgeSkinTypeFromImage(BufferedImage image) {
        if (image == null) {
            LOGGER.warn("Cannot judge skin type: image is null");
            return SKIN_TYPE_UNKNOWN;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        LOGGER.debug("Analyzing skin image: {}x{}", width, height);

        // Legacy format (64x32) - will be converted to default by filter
        if (width == height * 2) {
            LOGGER.debug("Detected legacy skin format (64x32)");
            return SKIN_TYPE_DEFAULT;
        }

        // Modern format (64x64)
        if (width == height) {
            int scale = Math.max(width / TEXTURE_SIZE_64, 1);
            int checkX = SLIM_CHECK_X * scale;
            int checkY = SLIM_CHECK_Y * scale;

            // Check if the pixel at the slim arm position is transparent
            int alpha = (image.getRGB(checkX, checkY) & ALPHA_MASK) >>> ALPHA_SHIFT;
            String type = alpha == 0 ? SKIN_TYPE_SLIM : SKIN_TYPE_DEFAULT;
            LOGGER.debug("Detected modern skin format: {} (alpha at [{},{}] = {})", type, checkX, checkY, alpha);
            return type;
        }

        LOGGER.warn("Unknown skin format: {}x{}", width, height);
        return SKIN_TYPE_UNKNOWN;
    }

    public static ByteBuffer toBuffer(byte[] data) {
        ByteBuffer buf = ByteBuffer.allocateDirect(data.length)
            .order(ByteOrder.nativeOrder());
        buf.put(data);
        buf.rewind();
        return buf;
    }

    public static boolean validateData(byte[] data) {
        LOGGER.debug("Validating skin data (length: {})", data != null ? data.length : 0);
        try (InputStream input = new ByteArrayInputStream(data)) {
            boolean valid = ImageIO.read(input) != null;
            LOGGER.debug("Skin data validation result: {}", valid);
            return valid;
        } catch (Throwable t) {
            LOGGER.warn("Skin data validation failed", t);
            return false;
        }
    }

    public static InputStream wrapByteBufferAsInputStream(ByteBuffer original) {
        ByteBuffer buf = original.duplicate();
        return new InputStream() {

            @Override
            public int read() throws IOException {
                if (!buf.hasRemaining()) return -1;
                return buf.get() & 0xFF;
            }

            @Override
            public int read(byte[] bytes, int off, int len) throws IOException {
                if (!buf.hasRemaining()) return -1;
                len = Math.min(len, buf.remaining());
                buf.get(bytes, off, len);
                return len;
            }

        };
    }

    private ByteBuffer data;
    private String type;
    private final Collection<Consumer<ISkin>> listeners = new CopyOnWriteArrayList<>();
    private final Collection<Function<ByteBuffer, ByteBuffer>> filters = new CopyOnWriteArrayList<>();

    @Override
    public ByteBuffer getData() {
        return data;
    }

    @Override
    public String getSkinType() {
        return type;
    }

    @Override
    public boolean isDataReady() {
        return data != null;
    }

    @Override
    public synchronized void onRemoval() {
        for (Consumer<ISkin> listener : listeners) listener.accept(this);

        data = null;
        type = null;
    }

    public synchronized void put(byte[] data, String type) {
        LOGGER.debug("Putting skin data: type={}, dataLength={}", type, data != null ? data.length : 0);

        ByteBuffer buf = null;
        if (data != null) {
            buf = toBuffer(data);
            LOGGER.debug("Converted to ByteBuffer, applying {} filters", filters.size());

            for (Function<ByteBuffer, ByteBuffer> filter : filters) {
                buf = filter.apply(buf);
                if (buf == null) {
                    LOGGER.warn("Filter returned null, stopping filter chain");
                    break;
                }
            }
        }

        this.data = buf;
        this.type = type;
        LOGGER.debug("Skin data stored successfully: type={}, hasData={}", type, buf != null);
    }

    @Override
    public boolean setRemovalListener(Consumer<ISkin> listener) {
        if (listener == null || listeners.contains(listener)) {
            return false;
        }
        boolean added = listeners.add(listener);
        LOGGER.debug("Removal listener added: {}", added);
        return added;
    }

    @Override
    public boolean setSkinFilter(Function<ByteBuffer, ByteBuffer> filter) {
        if (filter == null || filters.contains(filter)) {
            return false;
        }
        boolean added = filters.add(filter);
        LOGGER.debug("Skin filter added: {}", added);
        return added;
    }

}
