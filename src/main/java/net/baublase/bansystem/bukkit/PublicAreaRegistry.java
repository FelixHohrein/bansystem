package net.baublase.bansystem.bukkit;

import net.baublase.bansystem.config.PluginConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class PublicAreaRegistry {

    private final PluginConfiguration configuration;

    public PublicAreaRegistry(PluginConfiguration configuration) {
        this.configuration = configuration;
    }

    public boolean isExcluded(String worldName, Integer chunkX, Integer chunkZ) {
        if (worldName == null || chunkX == null || chunkZ == null) {
            return true;
        }
        for (PluginConfiguration.ExcludedChunk extra : configuration.getExtraChunks()) {
            if (worldName.equalsIgnoreCase(extra.getWorld()) && extra.getX() == chunkX && extra.getZ() == chunkZ) {
                return true;
            }
        }
        if (!configuration.getWorldsUsingWorldSpawn().contains(worldName)) {
            return false;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }
        Location spawn = world.getSpawnLocation();
        int spawnChunkX = spawn.getBlockX() >> 4;
        int spawnChunkZ = spawn.getBlockZ() >> 4;
        int radius = configuration.getSpawnChunkRadius();
        int dx = Math.abs(chunkX - spawnChunkX);
        int dz = Math.abs(chunkZ - spawnChunkZ);
        return Math.max(dx, dz) <= radius;
    }
}
