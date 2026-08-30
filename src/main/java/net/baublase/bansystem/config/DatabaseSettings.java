package net.baublase.bansystem.config;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Liest database.yml. connection-allowed=false startet ohne Verbindung.
 */
@Getter
public final class DatabaseSettings {

    private final boolean connectionAllowed;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int maximumPoolSize;
    private final int minimumIdle;
    private final long connectionTimeoutMs;

    public DatabaseSettings(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        this.connectionAllowed = yaml.getBoolean("connection-allowed", false);
        this.host = yaml.getString("host", "localhost");
        this.port = yaml.getInt("port", 5432);
        this.database = yaml.getString("database", "bansystem");
        this.username = yaml.getString("username", "postgres");
        this.password = yaml.getString("password", "");
        this.maximumPoolSize = yaml.getInt("pool.maximum-pool-size", 10);
        this.minimumIdle = yaml.getInt("pool.minimum-idle", 2);
        this.connectionTimeoutMs = yaml.getLong("pool.connection-timeout-ms", 10_000L);
    }

    public String jdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }
}
