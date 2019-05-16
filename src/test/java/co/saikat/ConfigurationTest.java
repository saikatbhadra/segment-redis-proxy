package co.saikat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.fail;

public class ConfigurationTest {
    private Map<String, String> envByName;

    @Before
    public void setUp() {
        envByName = new HashMap<>();
    }

    @Test
    public void raisesNumberFormatException_ifProxyPortIsNotANumber() {
        envByName.put("PROXY_PORT", "asf123");
        try {
            Configuration configuration = new Configuration(envByName);
            fail("constructor should raise exception");
        } catch (NumberFormatException ex) {

        }
    }

    @Test
    public void setsProxyPortTo4567_ifEnvironmentalVariableMissing() {
        Configuration configuration = new Configuration(emptyMap());
        Assert.assertEquals(4567, configuration.getProxyPort());
    }

    @Test
    public void setsProxyPortToEnvironmentalVariable() {
        envByName.put("PROXY_PORT", "123");
        Configuration configuration = new Configuration(envByName);
        Assert.assertEquals(123, configuration.getProxyPort());
    }

    @Test
    public void raisesIllegalArgumentException_ifRedisAddressMissingColon() {
        envByName.put("REDIS_ADDRESS", "blaha123");
        try {
            Configuration configuration = new Configuration(envByName);
            fail("constructor should raise exception");
        } catch (IllegalArgumentException ex) {

        }
    }

    @Test
    public void setsRedisHostToLocalHost_whenEnvironmentalVariableIsMissing() {
        Configuration configuration = new Configuration(emptyMap());
        Assert.assertEquals("localhost", configuration.getRedisHost());
    }

    @Test
    public void setsRedisHostTo1stPartOfEnvironmentalVariable() {
        envByName.put("REDIS_ADDRESS", "blah:123");
        Configuration configuration = new Configuration(envByName);
        Assert.assertEquals("blah", configuration.getRedisHost());
    }

    @Test
    public void raisesNumberFormatException_whenRedisPortIsNotNumber() {
        envByName.put("REDIS_ADDRESS", "blah:a123");
        try {
            Configuration configuration = new Configuration(envByName);
            fail("constructor should raise exception");
        } catch (NumberFormatException ex) {

        }
    }

    @Test
    public void setsRedisPortTo6378__whenEnvironmentalVariableMissing() {
        Configuration configuration = new Configuration(emptyMap());
        Assert.assertEquals(6379, configuration.getRedisPort());
    }

    @Test
    public void setsRedisPortTo2ndPartOfEnvironmentalVariable() {
        envByName.put("REDIS_ADDRESS", "blah:123");
        Configuration configuration = new Configuration(envByName);
        Assert.assertEquals(123, configuration.getRedisPort());
    }

    @Test
    public void setsExpiryOf500Seconds_whenEnvironmentalVariableMissing() {
        Configuration configuration = new Configuration(envByName);
        Assert.assertEquals(500, configuration.getCacheExpirySeconds());
    }

    @Test
    public void setsExpiryToEnvironmentalVariable() {
        envByName.put("CACHE_EXPIRY_SECONDS", "200");
        Configuration configuration = new Configuration(envByName);
        Assert.assertEquals(200, configuration.getCacheExpirySeconds());
    }

    @Test
    public void raisesNumberFormattingException_whenExpiryIsNotNumber() {
        envByName.put("CACHE_EXPIRY_SECONDS", "a200");
        try {
            Configuration configuration = new Configuration(envByName);
            fail("constructor should raise exception");
        } catch (NumberFormatException ex) {

        }
    }

    @Test
    public void setsCacheCapacityTo10_whenEnvironmentalVariableMissing() {
        Configuration configuration = new Configuration(envByName);
        Assert.assertEquals(10, configuration.getCacheCapacity());
    }

    @Test
    public void setsCacheCapacityToEnvironmentalVariable() {
        envByName.put("CACHE_CAPACITY", "200");
        Configuration configuration = new Configuration(envByName);
        Assert.assertEquals(200, configuration.getCacheCapacity());
    }

    @Test
    public void raisesNumberFormattingException_whenCacheCapacityIsNotNumber() {
        envByName.put("CACHE_CAPACITY", "");
        try {
            Configuration configuration = new Configuration(envByName);
            fail("constructor should raise exception");
        } catch (NumberFormatException ex) {

        }
    }
}