package net.baublase.bansystem.storage.sql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * CREATE TABLE IF NOT EXISTS — kein Flyway, Schema ist bewusst klein.
 */
public final class SchemaMigrator {

    private SchemaMigrator() {
    }

    public static void createTablesIfNotExists(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid UUID PRIMARY KEY,
                        name VARCHAR(16) NOT NULL,
                        first_seen TIMESTAMPTZ NOT NULL,
                        last_seen TIMESTAMPTZ NOT NULL,
                        locale VARCHAR(16),
                        client_brand VARCHAR(64),
                        last_world VARCHAR(64),
                        last_chunk_x INT,
                        last_chunk_z INT
                    )
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_players_name_lower ON players (LOWER(name))
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_sessions (
                        id BIGSERIAL PRIMARY KEY,
                        uuid UUID NOT NULL REFERENCES players(uuid),
                        ip VARCHAR(45) NOT NULL,
                        joined_at TIMESTAMPTZ NOT NULL,
                        quit_at TIMESTAMPTZ,
                        world VARCHAR(64),
                        chunk_x INT,
                        chunk_z INT,
                        locale VARCHAR(16),
                        client_brand VARCHAR(64)
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_sessions_uuid ON player_sessions (uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_sessions_ip ON player_sessions (ip)");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS bans (
                        id BIGSERIAL PRIMARY KEY,
                        target_uuid UUID NOT NULL,
                        target_name VARCHAR(16) NOT NULL,
                        staff_uuid UUID,
                        staff_name VARCHAR(16) NOT NULL,
                        type VARCHAR(16) NOT NULL,
                        reason TEXT NOT NULL,
                        template_id VARCHAR(64),
                        created_at TIMESTAMPTZ NOT NULL,
                        expires_at TIMESTAMPTZ,
                        active BOOLEAN NOT NULL DEFAULT TRUE
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bans_target ON bans (target_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bans_active ON bans (target_uuid, active)");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_locations (
                        uuid UUID NOT NULL REFERENCES players(uuid),
                        world VARCHAR(64) NOT NULL,
                        chunk_x INT NOT NULL,
                        chunk_z INT NOT NULL,
                        seen_on DATE NOT NULL,
                        PRIMARY KEY (uuid, world, chunk_x, chunk_z, seen_on)
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_locations_uuid ON player_locations (uuid)");
        }
    }
}
