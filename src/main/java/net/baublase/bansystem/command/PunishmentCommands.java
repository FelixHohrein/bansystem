package net.baublase.bansystem.command;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.util.DurationParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class PunishmentCommands implements TabExecutor {

    public enum Type {
        BAN, TEMPBAN, UNBAN
    }

    private final BanSystemPlugin plugin;
    private final Type type;

    public PunishmentCommands(BanSystemPlugin plugin, Type type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String permission = switch (type) {
            case BAN -> "bansystem.ban";
            case TEMPBAN -> "bansystem.tempban";
            case UNBAN -> "bansystem.unban";
        };
        if (!sender.hasPermission(permission) && !sender.hasPermission("bansystem.admin")) {
            plugin.messages().send(sender, Message.ERROR_NO_PERMISSION);
            return true;
        }
        if (type == Type.BAN && args.length < 2) {
            plugin.messages().send(sender, Message.ERROR_USAGE, "usage", "/ban <spieler> <grund>");
            return true;
        }
        if (type == Type.TEMPBAN && args.length < 3) {
            plugin.messages().send(sender, Message.ERROR_USAGE, "usage", "/tempban <spieler> <dauer> <grund>");
            return true;
        }
        if (type == Type.UNBAN && args.length < 1) {
            plugin.messages().send(sender, Message.ERROR_USAGE, "usage", "/unban <spieler>");
            return true;
        }
        plugin.scheduler().supplyAsync(() -> plugin.punishExecutor().requireKnown(sender, args[0]))
                .thenAccept(optional -> plugin.scheduler().runSync(() -> optional.ifPresent(target -> execute(sender, target, args))));
        return true;
    }

    private void execute(CommandSender sender, PlayerRef target, String[] args) {
        switch (type) {
            case BAN -> plugin.punishExecutor().banPermanent(sender, target, join(args, 1), null);
            case TEMPBAN -> {
                Optional<Duration> duration = DurationParser.parse(args[1]);
                if (duration.isEmpty() || duration.get().isZero()) {
                    plugin.messages().send(sender, Message.ERROR_INVALID_DURATION);
                    return;
                }
                plugin.punishExecutor().banTemporary(sender, target, join(args, 2), duration.get(), null);
            }
            case UNBAN -> plugin.punishExecutor().unban(sender, target);
        }
    }

    private String join(String[] args, int from) {
        return Arrays.stream(args).skip(from).collect(Collectors.joining(" "));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
