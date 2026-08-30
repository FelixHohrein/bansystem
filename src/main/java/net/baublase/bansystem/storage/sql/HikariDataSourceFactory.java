package net.baublase.bansystem.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.baublase.bansystem.config.DatabaseSettings;

/**
 * HikariCP-Pool. Hikari wird geshadet, der Postgres-Treiber nicht.
 */
public final class HikariDataSourceFactory {

    private HikariDataSourceFactory() {
    }

    public static HikariDataSource create(DatabaseSettings settings) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.getUsername());
        config.setPassword(settings.getPassword());
        config.setMaximumPoolSize(settings.getMaximumPoolSize());
        config.setMinimumIdle(settings.getMinimumIdle());
        config.setConnectionTimeout(settings.getConnectionTimeoutMs());
        config.setPoolName("BaublaseBanSystem");
        config.setInitializationFailTimeout(settings.getConnectionTimeoutMs());
        return new HikariDataSource(config);
    }
}
