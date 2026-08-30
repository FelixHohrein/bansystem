package net.baublase.bansystem.storage.api;

import net.baublase.bansystem.domain.player.KnownPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository {

    void upsert(KnownPlayer player);

    Optional<KnownPlayer> findByUuid(UUID uuid);

    Optional<KnownPlayer> findByName(String name);

    /**
     * Teilstring-Suche, case-insensitive, neueste zuerst.
     */
    List<KnownPlayer> searchByName(String query, int limit);

    List<KnownPlayer> findAll();

    int countAll();
}
