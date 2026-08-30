package net.baublase.bansystem.listener;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.bukkit.KickBanScreen;
import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.player.KnownPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Locale;
import java.util.Optional;

/**
 * Blockiert gebannte Accounts noch vor dem Join und zeigt den Ban-Screen.
 */
public final class LoginBanListener implements Listener {

    private final BanSystemPlugin plugin;
    private final KickBanScreen kickBanScreen;

    public LoginBanListener(BanSystemPlugin plugin, KickBanScreen kickBanScreen) {
        this.plugin = plugin;
        this.kickBanScreen = kickBanScreen;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.storage().isEnabled()) {
            return;
        }
        plugin.banService().deactivateExpired();
        Optional<Ban> ban = plugin.banService().activeBan(event.getUniqueId());
        if (ban.isEmpty()) {
            return;
        }
        Locale locale = localeOf(plugin.banService().known(event.getUniqueId()));
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickBanScreen.screen(locale, ban.get()));
    }

    private Locale localeOf(Optional<KnownPlayer> known) {
        if (known.isEmpty() || known.get().getLocale() == null) {
            return Locale.GERMAN;
        }
        String raw = known.get().getLocale().replace('_', '-');
        Locale parsed = Locale.forLanguageTag(raw);
        if (parsed.getLanguage().isEmpty()) {
            return Locale.GERMAN;
        }
        return "en".equalsIgnoreCase(parsed.getLanguage()) ? Locale.ENGLISH : Locale.GERMAN;
    }
}
