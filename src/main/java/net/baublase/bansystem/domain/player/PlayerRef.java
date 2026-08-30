package net.baublase.bansystem.domain.player;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class PlayerRef {
    UUID uuid;
    String name;
}
