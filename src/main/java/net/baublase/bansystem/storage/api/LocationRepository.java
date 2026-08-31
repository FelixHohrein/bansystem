package net.baublase.bansystem.storage.api;

import net.baublase.bansystem.domain.player.PlayerLocation;

import java.util.List;
import java.util.UUID;

public interface LocationRepository {

    void upsert(PlayerLocation location);

    List<PlayerLocation> findByUuid(UUID uuid);
}
