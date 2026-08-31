package net.baublase.bansystem.storage;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import net.baublase.bansystem.config.DatabaseSettings;
import net.baublase.bansystem.logging.PluginLogger;
import net.baublase.bansystem.storage.api.BanRepository;
import net.baublase.bansystem.storage.api.LocationRepository;
import net.baublase.bansystem.storage.api.PlayerRepository;
import net.baublase.bansystem.storage.api.SessionRepository;
import net.baublase.bansystem.storage.sql.HikariDataSourceFactory;
import net.baublase.bansystem.storage.sql.JdbcBanRepository;
import net.baublase.bansystem.storage.sql.JdbcLocationRepository;
import net.baublase.bansystem.storage.sql.JdbcPlayerRepository;
import net.baublase.bansystem.storage.sql.JdbcSessionRepository;
import net.baublase.bansystem.storage.sql.SchemaMigrator;

/**
 * JDBC-Fassade. Ohne connection-allowed bleiben die Repositories null.
 */
@Getter
public final class Storage {

    private final boolean enabled;
    private final HikariDataSource dataSource;
    private final PlayerRepository players;
    private final SessionRepository sessions;
    private final BanRepository bans;
    private final LocationRepository locations;

    private Storage(
            boolean enabled,
            HikariDataSource dataSource,
            PlayerRepository players,
            SessionRepository sessions,
            BanRepository bans,
            LocationRepository locations
    ) {
        this.enabled = enabled;
        this.dataSource = dataSource;
        this.players = players;
        this.sessions = sessions;
        this.bans = bans;
        this.locations = locations;
    }

    public static Storage disabled() {
        return new Storage(false, null, null, null, null, null);
    }

    public static Storage connect(DatabaseSettings settings, PluginLogger logger) {
        logger.info("Verbinde mit PostgreSQL " + settings.getHost() + ":" + settings.getPort() + "/" + settings.getDatabase());
        HikariDataSource dataSource = HikariDataSourceFactory.create(settings);
        try {
            SchemaMigrator.createTablesIfNotExists(dataSource);
        } catch (Exception exception) {
            dataSource.close();
            throw new IllegalStateException("Tabellen konnten nicht erstellt werden", exception);
        }
        logger.info("PostgreSQL verbunden, Tabellen geprüft.");
        Storage storage = new Storage(
                true,
                dataSource,
                new JdbcPlayerRepository(dataSource),
                new JdbcSessionRepository(dataSource),
                new JdbcBanRepository(dataSource),
                new JdbcLocationRepository(dataSource)
        );
        storage.getBans().deactivateExpired();
        return storage;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
