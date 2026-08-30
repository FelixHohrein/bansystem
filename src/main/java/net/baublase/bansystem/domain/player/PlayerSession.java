package net.baublase.bansystem.domain.player;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/** Eine Login-Session für Alt-Signale (IP, Chunk, Zeiten). */
@Value
@Builder
public class PlayerSession {
    long id;
    UUID uuid;
    String ip;
    Instant joinedAt;
    Instant quitAt;
    String world;
    Integer chunkX;
    Integer chunkZ;
    String locale;
    String clientBrand;
}
