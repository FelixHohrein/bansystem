package net.baublase.bansystem.command;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.gui.menu.MainMenu;
import net.baublase.bansystem.gui.menu.PunishMenu;
import net.baublase.bansystem.gui.menu.TemplateMenu;
import net.baublase.bansystem.i18n.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Hub-Befehl: GUI, Hilfe und Reload von Sprache/Config/Templates.
 */
public final class BansCommand implements TabExecutor {

    private final BanSystemPlugin plugin;

    public BansCommand(BanSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("hilfe"))) {
            sendHelp(sender);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bansystem.reload") && !sender.hasPermission("bansystem.admin")) {
                plugin.messages().send(sender, Message.ERROR_NO_PERMISSION);
                return true;
            }
            plugin.bootstrap().reload();
            plugin.messages().send(sender, Message.RELOAD_DONE);
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, Message.ERROR_PLAYER_ONLY);
            return true;
        }
        if (!player.hasPermission("bansystem.gui") && !player.hasPermission("bansystem.admin")) {
            plugin.messages().send(player, Message.ERROR_NO_PERMISSION);
            return true;
        }
        if (args.length == 0) {
            new MainMenu(plugin).open(player);
            return true;
        }
        if (!plugin.punishExecutor().ensureDatabase(player)) {
            return true;
        }
        if (args[0].equalsIgnoreCase("templates")) {
            new TemplateMenu(plugin).open(player);
            return true;
        }
        String query = args[0];
        plugin.scheduler().supplyAsync(() -> plugin.banService().known(query)).thenAccept(known ->
                plugin.scheduler().runSync(() -> {
                    if (known.isEmpty()) {
                        plugin.messages().send(player, Message.ERROR_UNKNOWN_PLAYER, "player", query);
                        return;
                    }
                    KnownPlayer target = known.get();
                    new PunishMenu(plugin, PlayerRef.builder().uuid(target.getUuid()).name(target.getName()).build()).open(player);
                }));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        plugin.messages().sendRaw(sender, Message.HELP_HEADER);
        Stream.of(
                Message.HELP_BANS,
                Message.HELP_BANS_PLAYER,
                Message.HELP_BANS_HELP,
                Message.HELP_RELOAD,
                Message.HELP_BAN,
                Message.HELP_TEMPBAN,
                Message.HELP_UNBAN,
                Message.HELP_HISTORY,
                Message.HELP_ALTCHECK,
                Message.HELP_TEMPLATE
        ).forEach(message -> plugin.messages().sendRaw(sender, message));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>(List.of("help", "templates", "reload"));
        options.addAll(PlayerNameCompleter.complete(plugin, args));
        return options.stream()
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .limit(20)
                .toList();
    }
}
