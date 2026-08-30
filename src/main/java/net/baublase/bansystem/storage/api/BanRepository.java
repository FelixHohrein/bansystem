package net.baublase.bansystem.storage.api;

import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.player.PlayerRef;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BanRepository {

    Ban insert(Ban ban);

    void deactivateActive(UUID targetUuid);

    Optional<Ban> findActive(UUID targetUuid);

    List<Ban> findHistory(UUID targetUuid);

    List<PlayerRef> findCurrentlyBannedPlayers();

    int countCurrentlyBanned();

    /**
     * Setzt abgelaufene Temp-Bans auf inactive, damit die Tabelle nicht voller Altlasten bleibt.
     */
    int deactivateExpired();
}
