package mythic.hub.config;

public class DatabaseConfig {
    private final String mongoHost;
    private final int mongoPort;
    private final String mongoUsername;
    private final String mongoPassword;
    private final String mongoDatabase;
    private final String redisHost;
    private final int redisPort;
    private final String redisUsername;
    private final String redisPassword;

    public DatabaseConfig() {
        // Updated configuration to match Radium database settings
        this.mongoHost = "localhost";
        this.mongoPort = 27017;
        this.mongoUsername = "radium_user";
        this.mongoPassword = "HKtsu0ByCvIQ0UYZlEBmZ9o3";
        this.mongoDatabase = "radium";
        this.redisHost = "localhost";
        this.redisPort = 6379;
        this.redisUsername = "default";
        this.redisPassword = "P3XWLBKRFpEROPqaLfq7JKyn";
    }

    // Getters
    public String getMongoHost() { return mongoHost; }
    public int getMongoPort() { return mongoPort; }
    public String getMongoUsername() { return mongoUsername; }
    public String getMongoPassword() { return mongoPassword; }
    public String getMongoDatabase() { return mongoDatabase; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisUsername() { return redisUsername; }
    public String getRedisPassword() { return redisPassword; }

    public String getMongoConnectionString() {
        return String.format("mongodb://%s:%s@%s:%d/%s", mongoUsername, mongoPassword, mongoHost, mongoPort, mongoDatabase);
    }

    public String getRedisConnectionString() {
        return String.format("redis://%s:%s@%s:%d", redisUsername, redisPassword, redisHost, redisPort);
    }
}