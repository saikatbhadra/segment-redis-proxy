package co.saikat;

import com.google.common.testing.FakeTicker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;


public class LruCacheTest {
    private LruCache lruCache;
    private JedisPool jedisPool;
    private Jedis jedis;
    private FakeTicker ticker;
    private static final int CAPACITY = 3;
    private static final int EXPIRY_TIME = 4;

    @Before
    public void setUp() {
        jedisPool = mock(JedisPool.class);
        jedis = mock(Jedis.class);
        ticker = new FakeTicker();
        lruCache = new LruCache(CAPACITY, EXPIRY_TIME, jedisPool, ticker);

        when(jedisPool.getResource()).thenReturn(jedis);
    }

    @Test
    public void get_callsJedisWhenCacheIsNotBuilt_andReturnsNullForMissingValues() {
        String value = lruCache.get("missingKey");

        assertNull(value);
        verify(jedis).get("missingKey");
    }

    @Test
    public void get_callsJedisWhenCacheIsNotBuilt_andReturnsJedisValue() {
        when(jedis.get("key")).thenReturn("value");
        String value = lruCache.get("key");

        assertEquals(value, "value");
        verify(jedis).get("key");
    }

    @Test
    public void get_returnsJedisValues() {
        when(jedis.get("a")).thenReturn("1");
        when(jedis.get("b")).thenReturn("2");

        Assert.assertEquals("1",lruCache.get("a"));
        Assert.assertEquals("2",lruCache.get("b"));
        assertNull(lruCache.get("c"));
    }

    @Test
    public void get_callsJedisOnce_afterTwoGets_ifKeyDoesntExpire() {
        when(jedis.get("key")).thenReturn("value");
        lruCache.get("key");
        ticker.advance(EXPIRY_TIME - 1, TimeUnit.SECONDS);
        lruCache.get("key");

        verify(jedis).get("key");
    }

    @Test
    public void get_callsJedisTwice_afterTwoGets_ifKeyExpires() {
        when(jedis.get("key")).thenReturn("value");
        lruCache.get("key");
        ticker.advance(EXPIRY_TIME + 1, TimeUnit.SECONDS);
        lruCache.get("key");

        verify(jedis, times(2)).get("key");
    }

    @Test
    public void get_evictsOldestItem_ifCacheHitsCapacity_andCallsJedis() {
        when(jedis.get("a")).thenReturn("1");
        when(jedis.get("b")).thenReturn("2");
        when(jedis.get("c")).thenReturn("3");
        when(jedis.get("d")).thenReturn("4");

        lruCache.get("a"); // calls jedis
        lruCache.get("b"); // calls jedis, cache = b,a
        lruCache.get("c"); // calls jedis, cache = c,b,a
        lruCache.get("d"); // calls jedis, cache is d,c,b. a is evicted
        lruCache.get("b"); // cache is b,d,c
        lruCache.get("c"); // cache is c,b,d,
        lruCache.get("d"); // cache is d,c,b
        lruCache.get("a"); // calls jedis, cache is a,d,c. b is evicted


        verify(jedis, times(2)).get("a"); // evicted from cache
        verify(jedis, times(1)).get("b");
        verify(jedis, times(1)).get("c");
        verify(jedis, times(1)).get("d");
    }
}