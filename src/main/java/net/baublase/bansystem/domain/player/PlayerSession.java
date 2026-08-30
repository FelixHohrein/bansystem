package net.baublase.bansystem.domain.player;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

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
