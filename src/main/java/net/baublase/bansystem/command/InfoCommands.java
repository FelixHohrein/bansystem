package net.baublase.bansystem.command;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.alt.AltMatch;
import net.baublase.bansystem.domain.alt.AltScore;
import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.util.DurationFormatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public final class InfoCommands implements TabExecutor {

    public enum Type {
        HISTORY, ALTCHECK
    }

    private final BanSystemPlugin plugin;
    private final Type type;

    public InfoCommands(BanSystemPlugin plugin, Type type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String permission = type == Type.HISTORY ? "bansystem.history" : "bansystem.altcheck";
        if (!sender.hasPermission(permission) && !sender.hasPermission("bansystem.admin")) {
            plugin.messages().send(sender, Message.ERROR_NO_PERMISSION);
            return true;
        }
        if (args.length < 1) {
            plugin.messages().send(sender, Message.ERROR_USAGE, "usage", "/" + label + " <spieler>");
            return true;
        }
        plugin.scheduler().supplyAsync(() -> plugin.punishExecutor().requireKnown(sender, args[0]))
                .thenAccept(optional -> optional.ifPresent(target -> {
                    if (type == Type.HISTORY) {
                        sendHistory(sender, target);
                    } else {
                        sendAlt(sender, target);
                    }
                }));
        return true;
    }

    private void sendHistory(CommandSender sender, PlayerRef target) {
        List<Ban> history = plugin.banService().history(target.getUuid());
        plugin.scheduler().runSync(() -> {
            Locale locale = plugin.messages().resolveLocale(sender);
            if (history.isEmpty()) {
                plugin.messages().send(sender, Message.HISTORY_EMPTY, "player", target.getName());
                return;
            }
            plugin.messages().sendRaw(sender, Message.HISTORY_HEADER, "player", target.getName());
            AtomicInteger index = new AtomicInteger(1);
            for (Ban ban : history) {
                String typeName = ban.permanent()
                        ? plugin.messages().plain(locale, Message.HISTORY_TYPE_PERMANENT)
                        : plugin.messages().plain(locale, Message.HISTORY_TYPE_TEMPORARY);
                String duration = ban.permanent()
                        ? plugin.messages().plain(locale, Message.HISTORY_DURATION_PERMANENT)
                        : DurationFormatter.format(java.time.Duration.between(ban.getCreatedAt(), ban.getExpiresAt() == null ? ban.getCreatedAt() : ban.getExpiresAt()), plugin.messages(), locale);
                plugin.messages().sendRaw(sender, Message.HISTORY_ENTRY,
                        "index", String.valueOf(index.getAndIncrement()),
                        "type", typeName,
                        "duration", duration,
                        "reason", ban.getReason(),
                        "date", DurationFormatter.date(ban.getCreatedAt()),
                        "staff", ban.getStaff() == null ? "Console" : ban.getStaff().getName());
            }
        });
    }

    private void sendAlt(CommandSender sender, PlayerRef target) {
        AltScore score = plugin.altCheckService().score(target.getUuid());
        plugin.scheduler().runSync(() -> {
            plugin.messages().sendRaw(sender, Message.ALT_HEADER, "player", target.getName(), "score", String.valueOf(score.getValue()));
            if (score.getLikelyMain() != null) {
                plugin.messages().sendRaw(sender, Message.ALT_MAIN, "main", score.getLikelyMain().getName());
            }
            if (score.getMatches().isEmpty()) {
                plugin.messages().send(sender, Message.ALT_NONE);
                return;
            }
            for (AltMatch match : score.getMatches()) {
                plugin.messages().sendRaw(sender, Message.ALT_ENTRY,
                        "player", match.getPlayer().getName(),
                        "score", String.valueOf(match.getScore()),
                        "ips", match.getSharedIps());
            }
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
