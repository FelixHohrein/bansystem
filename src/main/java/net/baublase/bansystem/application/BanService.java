package net.baublase.bansystem.application;

import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.ban.BanType;
import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.logging.PluginLogger;
import net.baublase.bansystem.storage.Storage;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fachlogik für Bans und bekannte Spieler. Alle DB-Zugriffe laufen über {@link Storage}.
 */
public final class BanService {

    private final Storage storage;
    private final PluginLogger logger;
    /** Name-Cache für Tab-Complete, ohne den Hauptthread zu blockieren. */
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

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
        if (!storage.isEnabled()) {
            return;
        }
        storage.getBans().deactivateActive(targetUuid);
        logger.info("Unban für " + targetUuid);
    }

    public Optional<Ban> activeBan(UUID targetUuid) {
        if (!storage.isEnabled()) {
            return Optional.empty();
        }
        return storage.getBans().findActive(targetUuid);
    }

    public List<Ban> history(UUID targetUuid) {
        if (!storage.isEnabled()) {
            return List.of();
        }
        return storage.getBans().findHistory(targetUuid);
    }

    public List<PlayerRef> bannedPlayers() {
        if (!storage.isEnabled()) {
            return List.of();
        }
        return storage.getBans().findCurrentlyBannedPlayers();
    }

    public Optional<KnownPlayer> known(String name) {
        if (!storage.isEnabled() || name == null || name.isBlank()) {
            return Optional.empty();
        }
        return storage.getPlayers().findByName(name.trim());
    }

    public Optional<KnownPlayer> known(UUID uuid) {
        if (!storage.isEnabled()) {
            return Optional.empty();
        }
        return storage.getPlayers().findByUuid(uuid);
    }

    public List<KnownPlayer> allKnown() {
        if (!storage.isEnabled()) {
            return List.of();
        }
        return storage.getPlayers().findAll();
    }

    /**
     * Exakte Namenssuche zuerst, sonst Teilstring (max. 28 Treffer).
     */
    public List<KnownPlayer> search(String query) {
        if (!storage.isEnabled() || query == null || query.isBlank()) {
            return List.of();
        }
        String trimmed = query.trim();
        Optional<KnownPlayer> exact = storage.getPlayers().findByName(trimmed);
        if (exact.isPresent()) {
            return List.of(exact.get());
        }
        return storage.getPlayers().searchByName(trimmed, 28);
    }

    public int countKnown() {
        if (!storage.isEnabled()) {
            return 0;
        }
        return storage.getPlayers().countAll();
    }

    public int countBanned() {
        if (!storage.isEnabled()) {
            return 0;
        }
        return storage.getBans().countCurrentlyBanned();
    }

    public void deactivateExpired() {
        if (!storage.isEnabled()) {
            return;
        }
        int changed = storage.getBans().deactivateExpired();
        if (changed > 0) {
            logger.debug("Abgelaufene Temp-Bans deaktiviert: " + changed);
        }
    }

    public void remember(UUID uuid, String name) {
        if (uuid != null && name != null && !name.isBlank()) {
            names.put(uuid, name);
        }
    }

    public void refreshNameCache() {
        names.clear();
        for (KnownPlayer player : allKnown()) {
            names.put(player.getUuid(), player.getName());
        }
        logger.debug("Namens-Cache geladen: " + names.size());
    }

    public List<String> suggestNames(String prefix, int limit) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String name : names.values()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(name);
            }
        }
        matches.sort(String.CASE_INSENSITIVE_ORDER);
        if (matches.size() > limit) {
            return matches.subList(0, limit);
        }
        return matches;
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
        remember(target.getUuid(), target.getName());
        logger.info(type + " Ban: " + target.getName() + " von " + (staff == null ? "Console" : staff.getName()) + " Grund: " + reason);
        return stored;
    }
}
