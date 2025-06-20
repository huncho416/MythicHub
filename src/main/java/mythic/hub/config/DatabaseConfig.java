package mythic.hub.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class DatabaseConfig {
    private final String mongoHost;
    private final int mongoPort;
    private final String redisHost;
    private final int redisPort;
    private final String redisUsername;
    private final String redisPassword;

    public DatabaseConfig() {
        // Load configuration from resources or use default values
        this.mongoHost = "34.174.89.110";
        this.mongoPort = 27017;
        this.redisHost = "34.174.89.110";
        this.redisPort = 6379;
        this.redisUsername = "default";
        this.redisPassword = "mysecretpassword";
    }

    // Getters
    public String getMongoHost() { return mongoHost; }
    public int getMongoPort() { return mongoPort; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisUsername() { return redisUsername; }
    public String getRedisPassword() { return redisPassword; }

    public String getMongoConnectionString() {
        return String.format("mongodb://%s:%d", mongoHost, mongoPort);
    }

    public String getRedisConnectionString() {
        return String.format("redis://%s:%s@%s:%d", redisUsername, redisPassword, redisHost, redisPort);
    }
}