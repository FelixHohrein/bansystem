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

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

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
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, Message.ERROR_PLAYER_ONLY);
            return true;
        }
        if (!player.hasPermission("bansystem.gui") && !player.hasPermission("bansystem.admin")) {
            plugin.messages().send(player, Message.ERROR_NO_PERMISSION);
            return true;
        }
        if (!plugin.punishExecutor().ensureDatabase(player)) {
            return true;
        }
        if (args.length == 0) {
            new MainMenu(plugin).open(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("templates")) {
            new TemplateMenu(plugin).open(player);
            return true;
        }
        plugin.scheduler().supplyAsync(() -> plugin.banService().known(args[0])).thenAccept(known ->
                plugin.scheduler().runSync(() -> {
                    if (known.isEmpty()) {
                        plugin.messages().send(player, Message.ERROR_UNKNOWN_PLAYER, "player", args[0]);
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
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Stream.concat(Stream.of("help", "templates"), plugin.storage().isEnabled()
                            ? plugin.banService().allKnown().stream().map(KnownPlayer::getName)
                            : Stream.empty())
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .limit(20)
                    .toList();
        }
        return List.of();
    }
}
