package net.baublase.bansystem.storage.sql;

import net.baublase.bansystem.domain.player.PlayerSession;
import net.baublase.bansystem.storage.api.SessionRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class JdbcSessionRepository implements SessionRepository {

    private final DataSource dataSource;

    public JdbcSessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long insertJoin(PlayerSession session) {
        String sql = """
                INSERT INTO player_sessions (uuid, ip, joined_at, world, chunk_x, chunk_z, locale, client_brand)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setObject(1, session.getUuid());
            statement.setString(2, session.getIp());
            statement.setTimestamp(3, Timestamp.from(session.getJoinedAt()));
            statement.setString(4, session.getWorld());
            statement.setObject(5, session.getChunkX());
            statement.setObject(6, session.getChunkZ());
            statement.setString(7, session.getLocale());
            statement.setString(8, session.getClientBrand());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("No session id returned");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to insert session", exception);
        }
    }

    @Override
    public void updateQuit(long sessionId, PlayerSession session) {
        String sql = """
                UPDATE player_sessions
                SET quit_at = ?, world = ?, chunk_x = ?, chunk_z = ?, locale = ?, client_brand = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(session.getQuitAt()));
            statement.setString(2, session.getWorld());
            statement.setObject(3, session.getChunkX());
            statement.setObject(4, session.getChunkZ());
            statement.setString(5, session.getLocale());
            statement.setString(6, session.getClientBrand());
            statement.setLong(7, sessionId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update session " + sessionId, exception);
        }
    }

    @Override
    public List<PlayerSession> findByUuid(UUID uuid) {
        String sql = "SELECT * FROM player_sessions WHERE uuid = ? ORDER BY joined_at DESC";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, uuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load sessions for " + uuid, exception);
        }
    }

    @Override
    public List<PlayerSession> findByIps(Set<String> ips) {
        if (ips == null || ips.isEmpty()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder("SELECT * FROM player_sessions WHERE ip IN (");
        sql.append("?,".repeat(ips.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (String ip : ips) {
                statement.setString(index++, ip);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load sessions by IP", exception);
        }
    }

    private List<PlayerSession> mapAll(ResultSet resultSet) throws SQLException {
        List<PlayerSession> sessions = new ArrayList<>();
        while (resultSet.next()) {
            sessions.add(PlayerSession.builder()
                    .id(resultSet.getLong("id"))
                    .uuid(resultSet.getObject("uuid", UUID.class))
                    .ip(resultSet.getString("ip"))
                    .joinedAt(toInstant(resultSet.getTimestamp("joined_at")))
                    .quitAt(toInstant(resultSet.getTimestamp("quit_at")))
                    .world(resultSet.getString("world"))
                    .chunkX((Integer) resultSet.getObject("chunk_x"))
                    .chunkZ((Integer) resultSet.getObject("chunk_z"))
                    .locale(resultSet.getString("locale"))
                    .clientBrand(resultSet.getString("client_brand"))
                    .build());
        }
        return sessions;
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
