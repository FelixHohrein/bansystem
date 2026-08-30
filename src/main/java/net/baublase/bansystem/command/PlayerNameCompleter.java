package net.baublase.bansystem.command;

import net.baublase.bansystem.BanSystemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Tab-Complete für Spielernamen: zuerst der In-Memory-Cache, sonst Online-Spieler.
 * Kein Datenbankzugriff auf dem Hauptthread.
 */
public final class PlayerNameCompleter {

    private PlayerNameCompleter() {
    }

    public static List<String> complete(BanSystemPlugin plugin, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0];
        List<String> cached = plugin.banService().suggestNames(prefix, 20);
        if (!cached.isEmpty()) {
            return cached;
        }
        String lower = prefix.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lower))
                .limit(20)
                .toList();
    }
}
