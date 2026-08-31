package net.baublase.bansystem.bootstrap;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.application.AltCheckService;
import net.baublase.bansystem.application.BanService;
import net.baublase.bansystem.application.ImmunityService;
import net.baublase.bansystem.application.LocationTracker;
import net.baublase.bansystem.application.PunishExecutor;
import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.application.SessionTracker;
import net.baublase.bansystem.application.TemplateService;
import net.baublase.bansystem.bukkit.KickBanScreen;
import net.baublase.bansystem.command.BansCommand;
import net.baublase.bansystem.command.InfoCommands;
import net.baublase.bansystem.command.PunishmentCommands;
import net.baublase.bansystem.command.TemplateCommand;
import net.baublase.bansystem.config.BanTemplateYamlStore;
import net.baublase.bansystem.config.DatabaseSettings;
import net.baublase.bansystem.config.PluginConfiguration;
import net.baublase.bansystem.gui.SkullFactory;
import net.baublase.bansystem.gui.input.PendingPunishActions;
import net.baublase.bansystem.i18n.LocaleResolver;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.i18n.MessageService;
import net.baublase.bansystem.i18n.YamlMessageCatalog;
import net.baublase.bansystem.listener.ChatInputListener;
import net.baublase.bansystem.listener.GuiListener;
import net.baublase.bansystem.listener.LoginBanListener;
import net.baublase.bansystem.listener.SessionListener;
import net.baublase.bansystem.logging.PluginLogger;
import net.baublase.bansystem.storage.Storage;
import net.baublase.bansystem.util.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Verdrahtet Config, Storage, Commands und Listener beim Plugin-Start.
 */
public final class PluginBootstrap {

    private final BanSystemPlugin plugin;

    public PluginBootstrap(BanSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean start() {
        saveDefaults();
        plugin.saveDefaultConfig();
        PluginConfiguration configuration = new PluginConfiguration(plugin.getConfig());
        plugin.configuration(configuration);
        plugin.pluginLogger(new PluginLogger(plugin, () -> plugin.configuration().isDebug()));
        plugin.scheduler(new TaskScheduler(plugin));

        File langFolder = new File(plugin.getDataFolder(), "lang");
        MessageService messages = new MessageService(new LocaleResolver(), new YamlMessageCatalog(langFolder));
        plugin.messages(messages);

        DatabaseSettings databaseSettings = new DatabaseSettings(new File(plugin.getDataFolder(), "database.yml"));
        plugin.databaseSettings(databaseSettings);

        Storage storage;
        if (!databaseSettings.isConnectionAllowed()) {
            plugin.pluginLogger().info(messages.plain(java.util.Locale.GERMAN, Message.INFO_DATABASE_DISABLED));
            storage = Storage.disabled();
        } else {
            try {
                storage = Storage.connect(databaseSettings, plugin.pluginLogger());
            } catch (Exception exception) {
                plugin.pluginLogger().error("PostgreSQL-Verbindung fehlgeschlagen. Plugin wird nicht gestartet.", exception);
                return false;
            }
        }
        plugin.storage(storage);

        BanTemplateYamlStore templateStore = new BanTemplateYamlStore(new File(plugin.getDataFolder(), "banTemplate.yml"));
        plugin.templateService(new TemplateService(templateStore));
        plugin.banService(new BanService(storage, plugin.pluginLogger()));
        plugin.sessionTracker(new SessionTracker(storage, plugin.pluginLogger()));
        plugin.locationTracker(new LocationTracker(storage));
        plugin.altCheckService(new AltCheckService(plugin, storage));
        KickBanScreen kickBanScreen = new KickBanScreen(messages);
        plugin.punishExecutor(new PunishExecutor(
                plugin,
                storage,
                plugin.banService(),
                new ImmunityService(),
                kickBanScreen,
                messages
        ));
        plugin.pendingActions(new PendingPunishActions());
        plugin.skullFactory(new SkullFactory(plugin));

        registerCommands();
        plugin.getServer().getPluginManager().registerEvents(new LoginBanListener(plugin, kickBanScreen), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SessionListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuiListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ChatInputListener(plugin), plugin);

        if (storage.isEnabled()) {
            plugin.scheduler().runAsync(() -> {
                plugin.banService().refreshNameCache();
                plugin.banService().deactivateExpired();
            });
            plugin.scheduler().runAsyncTimer(plugin.banService()::deactivateExpired, 20L * 60 * 5, 20L * 60 * 5);
            // Alle 2 Minuten aktuelle Chunks mitschreiben, damit der Score über Tage wächst.
            plugin.scheduler().runSyncTimer(this::sampleOnlineLocations, 20L * 60 * 2, 20L * 60 * 2);
        }

        plugin.pluginLogger().info("Ban-System geladen. Datenbank aktiv: " + storage.isEnabled());
        return true;
    }

    private void sampleOnlineLocations() {
        if (!plugin.storage().isEnabled()) {
            return;
        }
        List<KnownPlayer> snapshots = new ArrayList<>();
        Instant now = Instant.now();
        for (Player player : Bukkit.getOnlinePlayers()) {
            snapshots.add(SessionTracker.fromJoin(
                    player.getUniqueId(),
                    player.getName(),
                    player.locale().toString(),
                    player.getClientBrandName() == null || player.getClientBrandName().isBlank() ? "unknown" : player.getClientBrandName(),
                    player.getWorld().getName(),
                    player.getLocation().getBlockX() >> 4,
                    player.getLocation().getBlockZ() >> 4
            ).toBuilder().lastSeen(now).build());
        }
        if (snapshots.isEmpty()) {
            return;
        }
        plugin.scheduler().runAsync(() -> {
            for (KnownPlayer snapshot : snapshots) {
                plugin.storage().getPlayers().upsert(snapshot);
                plugin.locationTracker().record(
                        snapshot.getUuid(),
                        snapshot.getLastWorld(),
                        snapshot.getLastChunkX(),
                        snapshot.getLastChunkZ()
                );
            }
        });
    }

    /**
     * Lädt Config, Sprache und Templates neu. Die Datenbankverbindung bleibt bestehen.
     */
    public void reload() {
        plugin.reloadConfig();
        plugin.configuration(new PluginConfiguration(plugin.getConfig()));
        plugin.messages().reload();
        plugin.templateService().reload();
        plugin.pluginLogger().info("Reload abgeschlossen (ohne Datenbank-Reconnect).");
    }

    public void shutdown() {
        if (plugin.storage() != null) {
            plugin.storage().close();
        }
    }

    private void saveDefaults() {
        plugin.saveResource("database.yml", false);
        plugin.saveResource("banTemplate.yml", false);
        plugin.saveResource("lang/de.yml", false);
        plugin.saveResource("lang/en.yml", false);
    }

    private void registerCommands() {
        set("bans", new BansCommand(plugin));
        set("ban", new PunishmentCommands(plugin, PunishmentCommands.Type.BAN));
        set("tempban", new PunishmentCommands(plugin, PunishmentCommands.Type.TEMPBAN));
        set("unban", new PunishmentCommands(plugin, PunishmentCommands.Type.UNBAN));
        set("banhistory", new InfoCommands(plugin, InfoCommands.Type.HISTORY));
        set("altcheck", new InfoCommands(plugin, InfoCommands.Type.ALTCHECK));
        set("bantemplate", new TemplateCommand(plugin));
    }

    private void set(String name, org.bukkit.command.TabExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            plugin.pluginLogger().warn("Command nicht in plugin.yml: " + name);
        }
    }
}
