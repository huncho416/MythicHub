
package mythic.hub.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

public class HubWorld {
    private static final Pos SPAWN_LOCATION = new Pos(0, 65, 0, 0, 0);

    // Cache frequently used blocks
    private static final Block QUARTZ_BLOCK = Block.QUARTZ_BLOCK;
    private static final Block PINK_STAINED_GLASS = Block.PINK_STAINED_GLASS;
    private static final Block PINK_CONCRETE = Block.PINK_CONCRETE;
    private static final Block PINK_GLAZED_TERRACOTTA = Block.PINK_GLAZED_TERRACOTTA;
    private static final Block BEACON = Block.BEACON;
    private static final Block DIAMOND_BLOCK = Block.DIAMOND_BLOCK;
    private static final Block SEA_LANTERN = Block.SEA_LANTERN;

    public static void generateHub(InstanceContainer instance) {
        // Create a simple platform for spawn
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                // Base layer
                instance.setBlock(x, 64, z, QUARTZ_BLOCK);

                // Border (more efficient condition)
                if (Math.abs(x) == 10 || Math.abs(z) == 10) {
                    instance.setBlock(x, 65, z, PINK_STAINED_GLASS);
                }

                // Center decoration (more efficient condition)
                if (Math.abs(x) <= 2 && Math.abs(z) <= 2) {
                    instance.setBlock(x, 64, z, PINK_CONCRETE);
                }
            }
        }

        // Center spawn point
        instance.setBlock(0, 64, 0, PINK_GLAZED_TERRACOTTA);

        // Some decoration blocks
        instance.setBlock(0, 65, 0, BEACON);
        instance.setBlock(0, 63, 0, DIAMOND_BLOCK);

        // Add some lights (optimized loop)
        for (int i = -8; i <= 8; i += 4) {
            for (int j = -8; j <= 8; j += 4) {
                if (i != 0 || j != 0) {
                    instance.setBlock(i, 66, j, SEA_LANTERN);
                }
            }
        }
    }

    public static Pos getSpawnLocation() {
        return SPAWN_LOCATION;
    }
}