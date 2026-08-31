package net.baublase.bansystem.storage.sql;

import net.baublase.bansystem.domain.player.PlayerLocation;
import net.baublase.bansystem.storage.api.LocationRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ein Eintrag pro Spieler, Chunk und Kalendertag — damit der Alt-Score über Tage wachsen kann.
 */
public final class JdbcLocationRepository implements LocationRepository {

    private final DataSource dataSource;

    public JdbcLocationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void upsert(PlayerLocation location) {
        String sql = """
                INSERT INTO player_locations (uuid, world, chunk_x, chunk_z, seen_on)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (uuid, world, chunk_x, chunk_z, seen_on) DO NOTHING
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, location.getUuid());
            statement.setString(2, location.getWorld());
            statement.setInt(3, location.getChunkX());
            statement.setInt(4, location.getChunkZ());
            statement.setDate(5, Date.valueOf(location.getSeenOn()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to store location for " + location.getUuid(), exception);
        }
    }

    @Override
    public List<PlayerLocation> findByUuid(UUID uuid) {
        String sql = "SELECT * FROM player_locations WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, uuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PlayerLocation> locations = new ArrayList<>();
                while (resultSet.next()) {
                    locations.add(PlayerLocation.builder()
                            .uuid(resultSet.getObject("uuid", UUID.class))
                            .world(resultSet.getString("world"))
                            .chunkX(resultSet.getInt("chunk_x"))
                            .chunkZ(resultSet.getInt("chunk_z"))
                            .seenOn(resultSet.getDate("seen_on").toLocalDate())
                            .build());
                }
                return locations;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load locations for " + uuid, exception);
        }
    }
}
