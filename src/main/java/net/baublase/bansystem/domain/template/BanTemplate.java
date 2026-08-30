package net.baublase.bansystem.domain.template;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@Value
@Builder
public class BanTemplate {
    String id;
    String name;
    Duration duration;
    String reason;

    public boolean permanent() {
        return duration == null || duration.isZero() || duration.isNegative();
    }
}
