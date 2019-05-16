package co.saikat;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.LoadingCache;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class LruCache {
    private LoadingCache<String, Optional<String>> cache;

    public LruCache(int capacity, int expirySeconds, JedisPool jedisPool, Ticker ticker) {
        cache = CacheBuilder.newBuilder()
                .maximumSize(capacity)
                .ticker(ticker)
                .expireAfterWrite(expirySeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Optional<String>>() {
                    @Override
                    public Optional<String> load(String key) {
                        try (Jedis jedis = jedisPool.getResource()) {
                            return Optional.ofNullable(jedis.get(key));
                        }
                    }
                });
    }

    /**
     * Return value from cache which has a TTL & capacity defined.
     *
     * If the cache is missing the key or the key is expired, goes to the underlying Redis instance to return the value
     * If the redis instance is missing the key, returns a null value
     */
    public String get(String key) {
        return cache.getUnchecked(key).orElse(null);
    }

    /**
     * Clear out the cache
     */
    @VisibleForTesting
    public void clearCache() {
        cache.invalidateAll();
    }
}
