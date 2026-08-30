package net.baublase.bansystem.domain.alt;

import lombok.Builder;
import lombok.Value;
import net.baublase.bansystem.domain.player.PlayerRef;

@Value
@Builder
public class AltMatch {
    PlayerRef player;
    int score;
    String sharedIps;
}
