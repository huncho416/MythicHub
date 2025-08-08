package mythic.hub.config;

public class ProxyConfig {
    
    private final boolean enableVelocityForwarding;
    private final String velocitySecret;
    
    public ProxyConfig() {
        // Check if we should enable Velocity forwarding
        // Since Velocity is set to "none" forwarding mode, disable Velocity forwarding
        this.enableVelocityForwarding = Boolean.parseBoolean(
            System.getProperty("velocity.enabled", "false")  // Changed default to false
        );
        this.velocitySecret = System.getProperty("velocity.secret", "radium-velocity-secret");
    }
    
    public boolean isVelocityForwardingEnabled() {
        return enableVelocityForwarding;
    }
    
    public String getVelocitySecret() {
        return velocitySecret;
    }
    
    public void initialize() {
        System.out.println("=== Proxy Configuration ===");
        System.out.println("Velocity Forwarding: " + (enableVelocityForwarding ? "Enabled" : "Disabled"));
        if (enableVelocityForwarding) {
            System.out.println("Velocity Secret: " + velocitySecret);
            System.out.println("Connection Mode: Proxy Only (connect to port 25565)");
            System.out.println("Velocity forwarding mode must be 'modern' or 'legacy'");
        } else {
            System.out.println("Connection Mode: Direct (connect to port 25566)");
            System.out.println("Velocity forwarding mode should be 'none'");
        }
        System.out.println("Proxy configuration completed!");
    }
}
