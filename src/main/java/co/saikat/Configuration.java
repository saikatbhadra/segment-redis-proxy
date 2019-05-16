package co.saikat;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {
    private final int proxyPort;
    private final String redisHost;
    private final int redisPort;
    private final int cacheExpirySeconds;
    private final int cacheCapacity;
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
    // TODO should check if the numbers are > 0 and raise a IllegalArgumentException if they are not

  /***
   * Defines configuration variable from a Map<String, String>
   *
   * In normal instances the constructor should take a map should be from the System environmental variables (System.getenv())
   * If the environmental variables are missing, sets the following defaults:
   *  - proxyPort - 4567
   *  - redisHost - localhost
   *  - redisPort - 4567
   *  - cacheExpirySeconds - 500
   *  - cacheCapacity - 10
   */
  public Configuration(Map<String, String> envValuesByName) {
        String proxyPort = envValuesByName.getOrDefault("PROXY_PORT", "4567");
        this.proxyPort = Integer.parseInt(proxyPort);
        LOGGER.info("Proxy port set to {}", this.proxyPort);

        String redisAddress = envValuesByName.getOrDefault("REDIS_ADDRESS", "localhost:6379");
        String[] redisAddressParts = redisAddress.split(":");
        if (redisAddressParts.length != 2) {
            throw new IllegalArgumentException(
                    String.format("REDIS_ADDRESS is %s but should be in format host:port", redisAddress)
            );
        }
        redisHost = redisAddressParts[0];
        LOGGER.info("Redis host set to {}", this.redisHost);
        redisPort = Integer.parseInt(redisAddressParts[1]);
        LOGGER.info("Redis port set to {}", this.redisPort);

        String cacheExpirySeconds = envValuesByName.getOrDefault("CACHE_EXPIRY_SECONDS", "500");
        this.cacheExpirySeconds = Integer.parseInt(cacheExpirySeconds);
        LOGGER.info("Cache expiry set to {}", this.cacheExpirySeconds);

        String cacheCapacity = envValuesByName.getOrDefault("CACHE_CAPACITY", "10");
        this.cacheCapacity = Integer.parseInt(cacheCapacity);
        LOGGER.info("Cache capacity set to {}", this.cacheCapacity);
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public int getCacheExpirySeconds() {
        return cacheExpirySeconds;
    }

    public int getCacheCapacity() {
        return cacheCapacity;
    }
}
