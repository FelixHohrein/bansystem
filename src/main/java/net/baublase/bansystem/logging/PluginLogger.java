package net.baublase.bansystem.logging;

import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.BooleanSupplier;
import java.util.logging.Level;

/**
 * Debug-Logs nur wenn config.yml debug: true.
 */
@RequiredArgsConstructor
public final class PluginLogger {

    private final JavaPlugin plugin;
    private final BooleanSupplier debugEnabled;

    public void info(String message) {
        plugin.getLogger().info(message);
    }

    public void warn(String message) {
        plugin.getLogger().warning(message);
    }

    public void error(String message) {
        plugin.getLogger().severe(message);
    }

    public void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, message, throwable);
    }

    public void debug(String message) {
        if (debugEnabled.getAsBoolean()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}
