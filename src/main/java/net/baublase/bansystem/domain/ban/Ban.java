package net.baublase.bansystem.domain.ban;

import lombok.Builder;
import lombok.Value;
import net.baublase.bansystem.domain.player.PlayerRef;

import java.time.Instant;

/**
 * Ein Ban-Eintrag. Historie bleibt nach Unban/Ablauf erhalten (active=false).
 */
@Value
@Builder
public class Ban {
    long id;
    PlayerRef target;
    PlayerRef staff;
    BanType type;
    String reason;
    String templateId;
    Instant createdAt;
    Instant expiresAt;
    boolean active;

    public boolean permanent() {
        return type == BanType.PERMANENT || expiresAt == null;
    }

    public boolean currentlyActive(Instant now) {
        if (!active) {
            return false;
        }
        return permanent() || expiresAt.isAfter(now);
    }
}
