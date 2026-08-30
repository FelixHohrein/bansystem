package net.baublase.bansystem.storage.sql;

import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.storage.api.PlayerRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcPlayerRepository implements PlayerRepository {

    private final DataSource dataSource;

    public JdbcPlayerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void upsert(KnownPlayer player) {
        String sql = """
                INSERT INTO players (uuid, name, first_seen, last_seen, locale, client_brand, last_world, last_chunk_x, last_chunk_z)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (uuid) DO UPDATE SET
                    name = EXCLUDED.name,
                    last_seen = EXCLUDED.last_seen,
                    locale = EXCLUDED.locale,
                    client_brand = EXCLUDED.client_brand,
                    last_world = EXCLUDED.last_world,
                    last_chunk_x = EXCLUDED.last_chunk_x,
                    last_chunk_z = EXCLUDED.last_chunk_z
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, player.getUuid());
            statement.setString(2, player.getName());
            statement.setTimestamp(3, Timestamp.from(player.getFirstSeen()));
            statement.setTimestamp(4, Timestamp.from(player.getLastSeen()));
            statement.setString(5, player.getLocale());
            statement.setString(6, player.getClientBrand());
            statement.setString(7, player.getLastWorld());
            statement.setObject(8, player.getLastChunkX());
            statement.setObject(9, player.getLastChunkZ());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to upsert player " + player.getUuid(), exception);
        }
    }

    @Override
    public Optional<KnownPlayer> findByUuid(UUID uuid) {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, uuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find player " + uuid, exception);
        }
    }

    @Override
    public Optional<KnownPlayer> findByName(String name) {
        String sql = "SELECT * FROM players WHERE LOWER(name) = LOWER(?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find player " + name, exception);
        }
    }

    @Override
    public List<KnownPlayer> findAll() {
        String sql = "SELECT * FROM players ORDER BY last_seen DESC";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<KnownPlayer> players = new ArrayList<>();
            while (resultSet.next()) {
                players.add(map(resultSet));
            }
            return players;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list players", exception);
        }
    }

    private KnownPlayer map(ResultSet resultSet) throws SQLException {
        return KnownPlayer.builder()
                .uuid(resultSet.getObject("uuid", UUID.class))
                .name(resultSet.getString("name"))
                .firstSeen(toInstant(resultSet.getTimestamp("first_seen")))
                .lastSeen(toInstant(resultSet.getTimestamp("last_seen")))
                .locale(resultSet.getString("locale"))
                .clientBrand(resultSet.getString("client_brand"))
                .lastWorld(resultSet.getString("last_world"))
                .lastChunkX((Integer) resultSet.getObject("last_chunk_x"))
                .lastChunkZ((Integer) resultSet.getObject("last_chunk_z"))
                .build();
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
