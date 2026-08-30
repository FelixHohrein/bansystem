package net.baublase.bansystem.domain.player;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class KnownPlayer {
    UUID uuid;
    String name;
    Instant firstSeen;
    Instant lastSeen;
    String locale;
    String clientBrand;
    String lastWorld;
    Integer lastChunkX;
    Integer lastChunkZ;
}
