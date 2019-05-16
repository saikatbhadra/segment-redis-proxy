package co.saikat;

import com.google.common.base.Ticker;
import redis.clients.jedis.JedisPool;

import static spark.Spark.*;

public class RedisProxy {

    /***
     * RedisProxy webservice which returns value of specified key through
     * GET /key
     *
     * Returns value from local LRU cache when available and unexpired
     * Otherwise returns value from backing Redis instance
     * If key is missing from the backing Redis instance service returns 404
     */
    public static void main(String[] args) {
        Configuration config = new Configuration(System.getenv());
        port(config.getProxyPort());

        JedisPool pool = new JedisPool(config.getRedisHost(), config.getRedisPort());
        LruCache cache = new LruCache(
                config.getCacheCapacity(),
                config.getCacheExpirySeconds(),
                pool,
                Ticker.systemTicker()
        );

        get("/cache/:key", (request, response) -> {
            String key = request.params(":key");
            String cacheValue = cache.get(key);
            response.type("text/plain");
            if (cacheValue != null) {
                return cacheValue;
            } else {
                // returns 404 with empty string if key is not found
                response.status(404);
                return "";
            }
        });

        delete("/cache", (request, response) -> {
            cache.clearCache();
            response.type("text/plain");
            response.status(204);
            return "";
        });
    }
}