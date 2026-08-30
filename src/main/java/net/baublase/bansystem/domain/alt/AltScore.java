package net.baublase.bansystem.domain.alt;

import lombok.Builder;
import lombok.Value;
import net.baublase.bansystem.domain.player.PlayerRef;

import java.util.List;

/** Gesamtscore und Liste verdächtiger Accounts. */
@Value
@Builder
public class AltScore {
    PlayerRef target;
    int value;
    PlayerRef likelyMain;
    List<AltMatch> matches;
}
