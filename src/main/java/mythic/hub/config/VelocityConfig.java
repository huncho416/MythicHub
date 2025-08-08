package mythic.hub.config;

public class VelocityConfig {
    private final boolean enableModernForwarding;
    private final String forwardingSecret;
    private final boolean enableProxyProtocol;
    private final String serverName;
    private final boolean registerWithProxy;

    public VelocityConfig() {
        // Enable modern forwarding for Velocity
        this.enableModernForwarding = true;
        this.forwardingSecret = "radium-velocity-secret"; // Should match your Velocity config
        this.enableProxyProtocol = false; // Usually false for modern forwarding
        this.serverName = "hub"; // Name of this server in the proxy
        this.registerWithProxy = true; // Auto-register with Radium proxy
        
        System.out.println("Velocity Config: Accepting all client versions through proxy");
    }

    // Getters
    public boolean isModernForwardingEnabled() { return enableModernForwarding; }
    public String getForwardingSecret() { return forwardingSecret; }
    public boolean isProxyProtocolEnabled() { return enableProxyProtocol; }
    public String getServerName() { return serverName; }
    public boolean shouldRegisterWithProxy() { return registerWithProxy; }
}
