package net.baublase.bansystem.bukkit;

import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.i18n.MessageService;
import net.baublase.bansystem.util.DurationFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class KickBanScreen {

    private final MessageService messages;

    public KickBanScreen(MessageService messages) {
        this.messages = messages;
    }

    public void kick(Player player, Ban ban) {
        player.kick(screen(messages.resolveLocale(player), ban));
    }

    public Component screen(Locale locale, Ban ban) {
        if (ban.permanent()) {
            return messages.component(locale, Message.BAN_SCREEN_PERMANENT,
                    "reason", ban.getReason(),
                    "date", DurationFormatter.date(ban.getCreatedAt()));
        }
        return messages.component(locale, Message.BAN_SCREEN_TEMPORARY,
                "reason", ban.getReason(),
                "remaining", DurationFormatter.remaining(ban.getExpiresAt(), messages, locale),
                "date", DurationFormatter.date(ban.getCreatedAt()));
    }
}
