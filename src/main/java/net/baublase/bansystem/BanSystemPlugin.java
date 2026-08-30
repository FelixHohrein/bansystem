package net.baublase.bansystem;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.baublase.bansystem.application.AltCheckService;
import net.baublase.bansystem.application.BanService;
import net.baublase.bansystem.application.PunishExecutor;
import net.baublase.bansystem.application.SessionTracker;
import net.baublase.bansystem.application.TemplateService;
import net.baublase.bansystem.bootstrap.PluginBootstrap;
import net.baublase.bansystem.config.DatabaseSettings;
import net.baublase.bansystem.config.PluginConfiguration;
import net.baublase.bansystem.gui.SkullFactory;
import net.baublase.bansystem.gui.input.PendingPunishActions;
import net.baublase.bansystem.i18n.MessageService;
import net.baublase.bansystem.logging.PluginLogger;
import net.baublase.bansystem.storage.Storage;
import net.baublase.bansystem.util.TaskScheduler;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Einstiegspunkt. Hält nur verdrahtete Services, keine Fachlogik.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class BanSystemPlugin extends JavaPlugin {

    private PluginLogger pluginLogger;
    private PluginConfiguration configuration;
    private DatabaseSettings databaseSettings;
    private MessageService messages;
    private TaskScheduler scheduler;
    private Storage storage;
    private BanService banService;
    private SessionTracker sessionTracker;
    private AltCheckService altCheckService;
    private TemplateService templateService;
    private PunishExecutor punishExecutor;
    private PendingPunishActions pendingActions;
    private SkullFactory skullFactory;
    private PluginBootstrap bootstrap;

    @Override
    public void onEnable() {
        this.bootstrap = new PluginBootstrap(this);
        if (!bootstrap.start()) {
            getLogger().severe("Ban-System wird deaktiviert, weil die Datenbankverbindung fehlgeschlagen ist.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.shutdown();
        }
        getLogger().info("Baublase Ban-System deaktiviert.");
    }
}
