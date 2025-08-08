package mythic.hub.config;

public class VersionConfig {
    
    // Supported Minecraft versions
    public static final String[] SUPPORTED_VERSIONS = {
        "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
        "1.21"
    };
    
    // Server version configuration
    private final String serverVersion;
    private final String protocolName;
    private final boolean enableVersionCheck;
    
    public VersionConfig() {
        // Configure for 1.21 compatibility
        this.serverVersion = "1.21";
        this.protocolName = "MythicHub";
        this.enableVersionCheck = false; // Allow all compatible versions
    }
    
    /**
     * Initialize version compatibility settings
     */
    public void initialize() {
        System.out.println("=== Version Configuration ===");
        System.out.println("Server Version: " + serverVersion);
        System.out.println("Protocol Name: " + protocolName);
        System.out.println("Version Check: " + (enableVersionCheck ? "Enabled" : "Disabled"));
        
        // Log supported versions
        System.out.println("Supported Minecraft Versions:");
        for (String version : SUPPORTED_VERSIONS) {
            System.out.println("  - " + version);
        }
        
        // Configure brand name (will be set in main server class)
        System.out.println("Brand Name: " + protocolName + " " + serverVersion);
        
        System.out.println("Version configuration completed!");
    }
    
    /**
     * Check if a client version is supported
     */
    public boolean isVersionSupported(String clientVersion) {
        if (!enableVersionCheck) {
            return true; // Accept all versions when check is disabled
        }
        
        for (String supportedVersion : SUPPORTED_VERSIONS) {
            if (supportedVersion.equals(clientVersion)) {
                return true;
            }
        }
        return false;
    }
    
    // Getters
    public String getServerVersion() { return serverVersion; }
    public String getProtocolName() { return protocolName; }
    public boolean isVersionCheckEnabled() { return enableVersionCheck; }
    public String[] getSupportedVersions() { return SUPPORTED_VERSIONS.clone(); }
}
