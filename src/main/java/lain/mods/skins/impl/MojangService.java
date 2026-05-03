package lain.mods.skins.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;

import lain.mods.skins.impl.forge.MinecraftUtils;

public class MojangService {

    private static final int CACHE_EXPIRE_HOURS = 3;
    private static final int CACHE_REFRESH_MINUTES = 30;
    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_SUCCESS_CLASS = 2;
    private static final int HTTP_CLASS_DIVISOR = 100;
    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/%s";

    private static final LoadingCache<GameProfile, Optional<GameProfile>> filledProfiles = CacheBuilder.newBuilder()
        .expireAfterAccess(CACHE_EXPIRE_HOURS, TimeUnit.HOURS)
        .refreshAfterWrite(CACHE_REFRESH_MINUTES, TimeUnit.MINUTES)
        .build(new CacheLoader<GameProfile, Optional<GameProfile>>() {

            @Override
            public Optional<GameProfile> load(GameProfile key) throws Exception {
                // Bad profile - missing ID or properties
                if (key.getId() == null || key.getProperties() == null || key == Shared.DUMMY) {
                    return Optional.empty();
                }

                // Already filled
                if (key.isComplete() && !key.getProperties()
                    .isEmpty()) {
                    return Optional.of(key);
                }

                // Fill it
                GameProfile filled = Shared.blockyCall(
                    () -> {
                        return MinecraftUtils.getSessionService()
                            .fillProfileProperties(key, false);
                    },
                    key,
                    null);

                // Failed to fill
                if (filled == key) {
                    return Optional.empty();
                }

                // Partially filled (shouldn't happen in current implementation)
                if (!filled.isComplete() || filled.getProperties()
                    .isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(filled);
            }

            @Override
            public ListenableFuture<Optional<GameProfile>> reload(GameProfile key, Optional<GameProfile> oldValue)
                throws Exception {
                // Good result doesn't need refresh
                if (oldValue.isPresent()) {
                    return Futures.immediateFuture(oldValue);
                }
                return Shared.submitTask(() -> load(key));
            }

        });

    private static final LoadingCache<String, Optional<GameProfile>> resolvedProfiles = CacheBuilder.newBuilder()
        .expireAfterAccess(CACHE_EXPIRE_HOURS, TimeUnit.HOURS)
        .refreshAfterWrite(CACHE_REFRESH_MINUTES, TimeUnit.MINUTES)
        .build(new CacheLoader<String, Optional<GameProfile>>() {

            private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
                .create();

            @Override
            public Optional<GameProfile> load(String key) throws Exception {
                if (Shared.isBlank(key)) {
                    return Optional.of(Shared.DUMMY);
                }

                return Optional.ofNullable(
                    Shared.blockyCall(() -> { return makeRequest(String.format(MOJANG_API_URL, key)); }, null, null));
            }

            private GameProfile makeRequest(String request) throws IOException {
                HttpURLConnection conn = (HttpURLConnection) new URL(request).openConnection(MinecraftUtils.getProxy());
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setUseCaches(false);
                conn.connect();

                int code = conn.getResponseCode();

                // Not found
                if (code == HTTP_NO_CONTENT || code == HTTP_NOT_FOUND) {
                    return Shared.DUMMY;
                }

                // Success
                if (code / HTTP_CLASS_DIVISOR == HTTP_SUCCESS_CLASS) {
                    try (InputStream in = conn.getInputStream()) {
                        StringBuilder buf = new StringBuilder();
                        readLines(in, buf);

                        GameProfile constructed = gson.fromJson(buf.toString(), GameProfile.class);

                        // Server returned incomplete profile - treat as not found
                        if (!constructed.isComplete()) {
                            return Shared.DUMMY;
                        }

                        // Server returned offline profile - treat as not found
                        if (Shared.isOfflinePlayer(constructed.getId(), constructed.getName())) {
                            return Shared.DUMMY;
                        }

                        // Reconstruct because default JsonDeserializer doesn't construct properly
                        // Can't use GameProfileSerializer because it's a private class
                        return new GameProfile(constructed.getId(), constructed.getName());
                    }
                }

                return null;
            }

            private void readLines(InputStream in, StringBuilder buf) throws IOException {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    String newLine = System.getProperty("line.separator");
                    while ((line = reader.readLine()) != null) {
                        if (buf.length() > 0) {
                            buf.append(newLine);
                        }
                        buf.append(line);
                    }
                }
            }

            @Override
            public ListenableFuture<Optional<GameProfile>> reload(String key, Optional<GameProfile> oldValue)
                throws Exception {
                if (oldValue.isPresent()) {
                    // Effectively schedule a refresh in next reload
                    if (oldValue.get() == Shared.DUMMY) {
                        return Futures.immediateFuture(Optional.empty());
                    }
                    // Good result doesn't need refresh
                    return Futures.immediateFuture(oldValue);
                }
                return Shared.submitTask(() -> load(key));
            }

        });

    /**
     * @param profile the profile to fill, requires an ID to actually fill.
     * @return a ListenableFuture of a filled profile, otherwise previous profile.
     */
    public static ListenableFuture<GameProfile> fillProfile(GameProfile profile) {
        if (profile == null) {
            return Futures.immediateFailedFuture(new NullPointerException("profile must not be null"));
        }

        Optional<GameProfile> cachedResult = filledProfiles.getIfPresent(profile);
        if (cachedResult != null) {
            return Futures.immediateFuture(cachedResult.orElse(profile));
        }

        return Shared.submitTask(
            () -> filledProfiles.getUnchecked(profile)
                .orElse(profile));
    }

    /**
     * @param username the username to query about, requires non-blank to actually resolve.
     * @return a ListenableFuture of a resolved profile, otherwise {@link Shared#DUMMY DUMMY}.
     */
    public static ListenableFuture<GameProfile> getProfile(String username) {
        if (username == null) {
            return Futures.immediateFailedFuture(new NullPointerException("username must not be null"));
        }

        Optional<GameProfile> cachedResult = resolvedProfiles.getIfPresent(username);
        if (cachedResult != null) {
            return Futures.immediateFuture(cachedResult.orElse(Shared.DUMMY));
        }

        return Shared.submitTask(
            () -> resolvedProfiles.getUnchecked(username)
                .orElse(Shared.DUMMY));
    }

}
