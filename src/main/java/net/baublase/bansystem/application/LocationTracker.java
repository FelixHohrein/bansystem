package net.baublase.bansystem.application;

import net.baublase.bansystem.domain.player.PlayerLocation;
import net.baublase.bansystem.storage.Storage;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Sammelt Chunk-Sichtungen über Tage. Öffentliche Gebiete filtert der Alt-Score später.
 */
public final class LocationTracker {

    private final Storage storage;

    public LocationTracker(Storage storage) {
        this.storage = storage;
    }

    public void record(UUID uuid, String world, int chunkX, int chunkZ) {
        if (!storage.isEnabled() || world == null) {
            return;
        }
        storage.getLocations().upsert(PlayerLocation.builder()
                .uuid(uuid)
                .world(world)
                .chunkX(chunkX)
                .chunkZ(chunkZ)
                .seenOn(LocalDate.now(ZoneId.systemDefault()))
                .build());
    }

    public List<PlayerLocation> of(UUID uuid) {
        if (!storage.isEnabled()) {
            return List.of();
        }
        return storage.getLocations().findByUuid(uuid);
    }
}
