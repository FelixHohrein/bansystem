package net.baublase.bansystem.listener;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.bukkit.KickBanScreen;
import net.baublase.bansystem.domain.ban.Ban;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Locale;
import java.util.Optional;

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
        Optional<Ban> ban = plugin.banService().activeBan(event.getUniqueId());
        if (ban.isEmpty()) {
            return;
        }
        Locale locale = Locale.GERMAN;
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickBanScreen.screen(locale, ban.get()));
    }
}
