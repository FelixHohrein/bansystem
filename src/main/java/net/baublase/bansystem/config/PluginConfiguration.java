package net.baublase.bansystem.config;

import lombok.Getter;
import lombok.Value;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Debug, Spawn-Ausschluss und Alt-Score-Gewichte aus config.yml.
 */
@Getter
public final class PluginConfiguration {

    private final boolean debug;
    private final int spawnChunkRadius;
    private final List<String> worldsUsingWorldSpawn;
    private final List<ExcludedChunk> extraChunks;
    private final AltScoreWeights altScoreWeights;

    public PluginConfiguration(FileConfiguration config) {
        this.debug = config.getBoolean("debug", false);
        this.spawnChunkRadius = config.getInt("excluded-areas.spawn-chunk-radius", 8);
        this.worldsUsingWorldSpawn = config.getStringList("excluded-areas.worlds-using-world-spawn");
        this.extraChunks = new ArrayList<>();
        List<java.util.Map<?, ?>> rawChunks = config.getMapList("excluded-areas.extra-chunks");
        for (java.util.Map<?, ?> raw : rawChunks) {
            Object world = raw.get("world");
            Object x = raw.get("x");
            Object z = raw.get("z");
            if (world != null && x instanceof Number xNumber && z instanceof Number zNumber) {
                extraChunks.add(new ExcludedChunk(String.valueOf(world), xNumber.intValue(), zNumber.intValue()));
            }
        }
        ConfigurationSection score = config.getConfigurationSection("alt-score");
        this.altScoreWeights = new AltScoreWeights(
                score == null ? 40 : score.getInt("same-ip-current", 40),
                score == null ? 25 : score.getInt("same-ip-history", 25),
                score == null ? 15 : score.getInt("never-online-together", 15),
                score == null ? 10 : score.getInt("similar-login-hours", 10),
                score == null ? 10 : score.getInt("same-chunk", 10),
                score == null ? 5 : score.getInt("same-locale-and-brand", 5)
        );
    }

    @Value
    public static class ExcludedChunk {
        String world;
        int x;
        int z;
    }

    @Value
    public static class AltScoreWeights {
        int sameIpCurrent;
        int sameIpHistory;
        int neverOnlineTogether;
        int similarLoginHours;
        int sameChunk;
        int sameLocaleAndBrand;
    }
}
