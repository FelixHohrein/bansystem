package net.baublase.bansystem.storage.sql;

import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.ban.BanType;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.storage.api.BanRepository;

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
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-Zugriff auf {@code bans}. Abgelaufene Temp-Bans zählen nicht als aktiv.
 */
public final class JdbcBanRepository implements BanRepository {

    private final DataSource dataSource;

    public JdbcBanRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Ban insert(Ban ban) {
        String sql = """
                INSERT INTO bans (target_uuid, target_name, staff_uuid, staff_name, type, reason, template_id, created_at, expires_at, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setObject(1, ban.getTarget().getUuid());
            statement.setString(2, ban.getTarget().getName());
            if (ban.getStaff() == null || ban.getStaff().getUuid() == null) {
                statement.setObject(3, null);
            } else {
                statement.setObject(3, ban.getStaff().getUuid());
            }
            statement.setString(4, ban.getStaff() == null ? "Console" : ban.getStaff().getName());
            statement.setString(5, ban.getType().name());
            statement.setString(6, ban.getReason());
            statement.setString(7, ban.getTemplateId());
            statement.setTimestamp(8, Timestamp.from(ban.getCreatedAt()));
            statement.setTimestamp(9, ban.getExpiresAt() == null ? null : Timestamp.from(ban.getExpiresAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                long id = keys.next() ? keys.getLong(1) : 0L;
                return Ban.builder()
                        .id(id)
                        .target(ban.getTarget())
                        .staff(ban.getStaff())
                        .type(ban.getType())
                        .reason(ban.getReason())
                        .templateId(ban.getTemplateId())
                        .createdAt(ban.getCreatedAt())
                        .expiresAt(ban.getExpiresAt())
                        .active(true)
                        .build();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to insert ban", exception);
        }
    }

    @Override
    public void deactivateActive(UUID targetUuid) {
        String sql = "UPDATE bans SET active = FALSE WHERE target_uuid = ? AND active = TRUE";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, targetUuid);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to deactivate bans for " + targetUuid, exception);
        }
    }

    @Override
    public Optional<Ban> findActive(UUID targetUuid) {
        String sql = """
                SELECT * FROM bans
                WHERE target_uuid = ? AND active = TRUE
                  AND (expires_at IS NULL OR expires_at > NOW())
                ORDER BY created_at DESC
                LIMIT 1
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, targetUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find active ban for " + targetUuid, exception);
        }
    }

    @Override
    public List<Ban> findHistory(UUID targetUuid) {
        String sql = "SELECT * FROM bans WHERE target_uuid = ? ORDER BY created_at DESC";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, targetUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Ban> bans = new ArrayList<>();
                while (resultSet.next()) {
                    bans.add(map(resultSet));
                }
                return bans;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load history for " + targetUuid, exception);
        }
    }

    @Override
    public List<PlayerRef> findCurrentlyBannedPlayers() {
        String sql = """
                SELECT DISTINCT ON (target_uuid) target_uuid, target_name
                FROM bans
                WHERE active = TRUE AND (expires_at IS NULL OR expires_at > NOW())
                ORDER BY target_uuid, created_at DESC
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<PlayerRef> players = new ArrayList<>();
            while (resultSet.next()) {
                players.add(PlayerRef.builder()
                        .uuid(resultSet.getObject("target_uuid", UUID.class))
                        .name(resultSet.getString("target_name"))
                        .build());
            }
            return players;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list banned players", exception);
        }
    }

    @Override
    public int countCurrentlyBanned() {
        String sql = """
                SELECT COUNT(DISTINCT target_uuid)
                FROM bans
                WHERE active = TRUE AND (expires_at IS NULL OR expires_at > NOW())
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count banned players", exception);
        }
    }

    @Override
    public int deactivateExpired() {
        String sql = """
                UPDATE bans
                SET active = FALSE
                WHERE active = TRUE AND expires_at IS NOT NULL AND expires_at <= NOW()
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to deactivate expired bans", exception);
        }
    }

    private Ban map(ResultSet resultSet) throws SQLException {
        UUID staffUuid = resultSet.getObject("staff_uuid", UUID.class);
        return Ban.builder()
                .id(resultSet.getLong("id"))
                .target(PlayerRef.builder()
                        .uuid(resultSet.getObject("target_uuid", UUID.class))
                        .name(resultSet.getString("target_name"))
                        .build())
                .staff(PlayerRef.builder()
                        .uuid(staffUuid)
                        .name(resultSet.getString("staff_name"))
                        .build())
                .type(BanType.valueOf(resultSet.getString("type")))
                .reason(resultSet.getString("reason"))
                .templateId(resultSet.getString("template_id"))
                .createdAt(resultSet.getTimestamp("created_at").toInstant())
                .expiresAt(resultSet.getTimestamp("expires_at") == null ? null : resultSet.getTimestamp("expires_at").toInstant())
                .active(resultSet.getBoolean("active"))
                .build();
    }
}
