package src;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/***
 *
 *  Requires the following configuration in the Proxy Service:
 *   - CACHE_EXPIRY_SECONDS = 4
 *   - CACHE_CAPACITY = 2
 *
 *  Also requires that following environmental variables are set correctly:
 *  - PROXY_PORT
 *  - PROXY_HOST
 *  - REDIS_HOST
 *  - REDIS_PORT
 *
 *  Otherwise integration tests will fail.
 */
public class RedisProxyIT {
    private static JedisPool JEDIS_POOL;
    private static String REDIS_HOST;
    private static int REDIS_PORT;
    private static String PROXY_HOST;
    private static int PROXY_PORT;

    private static void readEnvironment() {
        PROXY_HOST = System.getenv("PROXY_HOST");
        PROXY_PORT = Integer.parseInt(System.getenv("PROXY_PORT"));
        String redisAddress = System.getenv("REDIS_ADDRESS");
        String[] redisAddressParts = redisAddress.split(":");
        if (redisAddressParts.length != 2) {
            throw new IllegalArgumentException(
                    String.format("REDIS_ADDRESS is %s but should be in format host:port", redisAddress)
            );
        }
        REDIS_HOST = redisAddressParts[0];
        REDIS_PORT = Integer.parseInt(redisAddressParts[1]);
    }

    @BeforeClass
    public static void setUpOnce() {
        readEnvironment();
        JEDIS_POOL = new JedisPool(REDIS_HOST, REDIS_PORT);
    }

    @After
    public void breakDown() throws  Exception {
        try (Jedis jedis = JEDIS_POOL.getResource()) {
            jedis.flushDB();
        }

        // Clear cache
        HttpUriRequest request = new HttpDelete("http://"  + PROXY_HOST + ":" + PROXY_PORT + "/cache");
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
        assertEquals(httpResponse.getStatusLine().getStatusCode(), 204);
    }

    private HttpUriRequest getRequest(String key) {
        return new HttpGet( "http://"  + PROXY_HOST + ":" + PROXY_PORT + "/cache/"  + key);
    }

    @Test
    public void proxyReturns404_forMissingKeys() throws Exception {
        try (Jedis jedis = JEDIS_POOL.getResource()) {
            jedis.set("key", "value");
        }

        HttpUriRequest request = getRequest("newKey");
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
        assertEquals(httpResponse.getStatusLine().getStatusCode(), 404);
    }

    private void assertResponse(String key, String response) throws Exception {
        HttpUriRequest request = getRequest(key);
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
        assertEquals(response, EntityUtils.toString(httpResponse.getEntity()));
    }

    @Test
    public void proxyReturnsRedisValue_forSetKeys() throws Exception {
        try (Jedis jedis = JEDIS_POOL.getResource()) {
            jedis.set("key", "value");
        }

        assertResponse("key", "value");
    }

    @Test
    public void proxyReturnsCacheValueOverRedisValue_ifCacheIsUnexpired() throws Exception {
        try (Jedis jedis = JEDIS_POOL.getResource()) {
            jedis.set("key", "value");
        }
        assertResponse("key", "value");

        // overwrite jedis entries to differentiate between local cache and Redis
        try (Jedis jedis = JEDIS_POOL.getResource()) {
            jedis.set("key", "newValue");
        }

        assertResponse("key", "value");
    }

    @Test
    public void proxyReturnsRedisValue_ifCacheIsExpired() throws Exception {
        try (Jedis jedis = JEDIS_POOL.getResource()) {
            jedis.set("key", "value");
        }
        HttpUriRequest request = getRequest("key");
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
        assertEquals("value", EntityUtils.toString(httpResponse.getEntity()));

        // overwrite jedis entries to differentiate between local cache and Redis
        try (Jedis jedis = JEDIS_POOL.getResource()) {
            jedis.set("key", "newValue");
        }

        TimeUnit.SECONDS.sleep( 5);

        httpResponse = HttpClientBuilder.create().build().execute(request);
        assertEquals("newValue", EntityUtils.toString(httpResponse.getEntity()));
    }

    @Test
    public void proxyEvictsAfterCapacityReached() throws Exception {
        try (Jedis jedis = JEDIS_POOL.getResource()) {
            jedis.set("key", "value");
            jedis.set("key2", "value2");
            jedis.set("key3", "value3");
        }

        assertResponse("key", "value");
        assertResponse("key2", "value2");
        assertResponse("key3", "value3"); // key is evicted from cache

        // overwrite jedis entries to differentiate between local cache and Redis
        try (Jedis jedis = JEDIS_POOL.getResource()) {
            jedis.set("key", "newValue");
            jedis.set("key2", "newValue2");
            jedis.set("key3", "newValue3");
        }

        assertResponse("key3", "value3");
        assertResponse("key2", "value2");
        // since key is evicted cache goes to backing redis instance and fetches newest value
        assertResponse("key", "newValue");
    }
}
