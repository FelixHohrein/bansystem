package net.baublase.bansystem.gui.input;

import net.baublase.bansystem.domain.player.PlayerRef;

import java.time.Duration;

/**
 * Zwischenschritt vor dem Bestätigungsmenü: Ziel, Dauer und Grund.
 */
public record PunishDraft(
        PlayerRef target,
        boolean permanent,
        Duration duration,
        String reason,
        String templateId
) {
}
