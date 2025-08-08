package mythic.hub.integrations.radium;

import java.util.List;
import java.util.Set;

/**
 * Represents a rank definition from Radium backend
 */
public class RadiumRank {
    
    private final String name;
    private final String prefix;
    private final int weight;
    private final String color;
    private final Set<String> permissions;
    private final List<String> inherits;
    
    public RadiumRank(String name, String prefix, int weight, String color, 
                      Set<String> permissions, List<String> inherits) {
        this.name = name;
        this.prefix = prefix;
        this.weight = weight;
        this.color = color;
        this.permissions = permissions;
        this.inherits = inherits;
    }
    
    public String getName() {
        return name;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public String getColor() {
        return color;
    }
    
    public Set<String> getPermissions() {
        return permissions;
    }
    
    public List<String> getInherits() {
        return inherits;
    }
    
    public boolean hasPermission(String permission) {
        // Check for wildcard permission
        if (permissions.contains("*")) {
            return true;
        }
        
        // Check exact permission
        if (permissions.contains(permission)) {
            return true;
        }
        
        // Check wildcard patterns (e.g., "mythic.hub.*" matches "mythic.hub.something")
        for (String perm : permissions) {
            if (perm.endsWith("*")) {
                String prefix = perm.substring(0, perm.length() - 1);
                if (permission.startsWith(prefix)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return "RadiumRank{" +
                "name='" + name + '\'' +
                ", prefix='" + prefix + '\'' +
                ", weight=" + weight +
                ", color='" + color + '\'' +
                ", permissions=" + permissions.size() +
                ", inherits=" + inherits +
                '}';
    }
}
