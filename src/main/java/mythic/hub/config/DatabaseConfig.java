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
        this.mongoUsername = null; // No authentication required
        this.mongoPassword = null; // No authentication required
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
        if (mongoUsername != null && mongoPassword != null) {
            return String.format("mongodb://%s:%s@%s:%d/%s", mongoUsername, mongoPassword, mongoHost, mongoPort, mongoDatabase);
        } else {
            return String.format("mongodb://%s:%d/%s", mongoHost, mongoPort, mongoDatabase);
        }
    }

    public String getRedisConnectionString() {
        return String.format("redis://%s:%s@%s:%d", redisUsername, redisPassword, redisHost, redisPort);
    }
}