package lain.mods.skinport;

import java.awt.image.BufferedImage;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lain.mods.skinport.api.ISkin;
import lain.mods.skinport.api.ISkinProvider;
import net.minecraft.client.entity.AbstractClientPlayer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class MojangSkinProviderService
{

    public static ISkinProvider createSkinProvider()
    {
        return new ISkinProvider()
        {

            @Override
            public ISkin getSkin(AbstractClientPlayer player)
            {
                return skinCache.getUnchecked(player.getUniqueID());
            }

        };
    }

    private static final ExecutorService pool = new ThreadPoolExecutor(0, 2, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
    private static final LoadingCache<UUID, SkinData> skinCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).removalListener(new RemovalListener<UUID, SkinData>()
    {

        @Override
        public void onRemoval(RemovalNotification<UUID, SkinData> notification)
        {
            SkinData data = notification.getValue();
            if (data != null)
                data.put(null);
        }

    }).build(new CacheLoader<UUID, SkinData>()
    {

        @Override
        public SkinData load(UUID key) throws Exception
        {
            final SkinData data = new SkinData(key);
            pool.execute(new Runnable()
            {

                @Override
                public void run()
                {
                    // TODO
                    BufferedImage image = null;
                    String type = "";
                    data.put(new LegacyConversion().convert(image), type);
                }

            });
            return data;
        }

    });

}