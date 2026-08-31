package net.baublase.bansystem.domain.player;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Ein privater Chunk, an dem ein Spieler an einem Kalendertag gesehen wurde.
 */
@Value
@Builder
public class PlayerLocation {
    UUID uuid;
    String world;
    int chunkX;
    int chunkZ;
    LocalDate seenOn;
}
