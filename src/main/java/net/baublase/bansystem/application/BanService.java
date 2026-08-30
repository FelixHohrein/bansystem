package net.baublase.bansystem.application;

import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.ban.BanType;
import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.logging.PluginLogger;
import net.baublase.bansystem.storage.Storage;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BanService {

    private final Storage storage;
    private final PluginLogger logger;

    public BanService(Storage storage, PluginLogger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public Ban banPermanent(PlayerRef target, PlayerRef staff, String reason, String templateId) {
        return insert(target, staff, BanType.PERMANENT, reason, null, templateId);
    }

    public Ban banTemporary(PlayerRef target, PlayerRef staff, String reason, Duration duration, String templateId) {
        Instant expires = Instant.now().plus(duration);
        return insert(target, staff, BanType.TEMPORARY, reason, expires, templateId);
    }

    public void unban(UUID targetUuid) {
        storage.getBans().deactivateActive(targetUuid);
        logger.info("Unban für " + targetUuid);
    }

    public Optional<Ban> activeBan(UUID targetUuid) {
        return storage.getBans().findActive(targetUuid);
    }

    public List<Ban> history(UUID targetUuid) {
        return storage.getBans().findHistory(targetUuid);
    }

    public List<PlayerRef> bannedPlayers() {
        return storage.getBans().findCurrentlyBannedPlayers();
    }

    public Optional<KnownPlayer> known(String name) {
        return storage.getPlayers().findByName(name);
    }

    public Optional<KnownPlayer> known(UUID uuid) {
        return storage.getPlayers().findByUuid(uuid);
    }

    public List<KnownPlayer> allKnown() {
        return storage.getPlayers().findAll();
    }

    private Ban insert(PlayerRef target, PlayerRef staff, BanType type, String reason, Instant expiresAt, String templateId) {
        storage.getBans().deactivateActive(target.getUuid());
        Ban ban = Ban.builder()
                .target(target)
                .staff(staff)
                .type(type)
                .reason(reason)
                .templateId(templateId)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .active(true)
                .build();
        Ban stored = storage.getBans().insert(ban);
        logger.info(type + " Ban: " + target.getName() + " von " + (staff == null ? "Console" : staff.getName()) + " Grund: " + reason);
        return stored;
    }
}
