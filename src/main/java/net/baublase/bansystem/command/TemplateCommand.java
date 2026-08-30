package net.baublase.bansystem.command;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.template.BanTemplate;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.util.DurationFormatter;
import net.baublase.bansystem.util.DurationParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public final class TemplateCommand implements TabExecutor {

    private final BanSystemPlugin plugin;

    public TemplateCommand(BanSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bansystem.template") && !sender.hasPermission("bansystem.admin")) {
            plugin.messages().send(sender, Message.ERROR_NO_PERMISSION);
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            plugin.messages().sendRaw(sender, Message.TEMPLATE_LIST_HEADER);
            Locale locale = plugin.messages().resolveLocale(sender);
            for (BanTemplate template : plugin.templateService().list()) {
                plugin.messages().sendRaw(sender, Message.TEMPLATE_LIST_ENTRY,
                        "id", template.getId(),
                        "name", template.getName(),
                        "duration", DurationFormatter.format(template.getDuration(), plugin.messages(), locale),
                        "reason", template.getReason());
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("delete") && args.length >= 2) {
            boolean deleted = plugin.templateService().delete(args[1]);
            if (!deleted) {
                plugin.messages().send(sender, Message.ERROR_TEMPLATE_NOT_FOUND, "id", args[1]);
                return true;
            }
            plugin.messages().send(sender, Message.TEMPLATE_DELETED, "id", args[1].toLowerCase(Locale.ROOT));
            return true;
        }
        if (args[0].equalsIgnoreCase("set") && args.length >= 4) {
            String id = args[1].toLowerCase(Locale.ROOT);
            Optional<Duration> duration = DurationParser.parse(args[2]);
            if (duration.isEmpty()) {
                plugin.messages().send(sender, Message.ERROR_INVALID_DURATION);
                return true;
            }
            String reason = Arrays.stream(args).skip(3).collect(Collectors.joining(" "));
            BanTemplate template = BanTemplate.builder()
                    .id(id)
                    .name(id)
                    .duration(duration.get())
                    .reason(reason)
                    .build();
            plugin.templateService().upsert(template);
            plugin.messages().send(sender, Message.TEMPLATE_SAVED, "id", id);
            return true;
        }
        plugin.messages().send(sender, Message.ERROR_USAGE, "usage", "/bantemplate <list|set|delete> [id] [dauer] [grund]");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("list", "set", "delete");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            return plugin.templateService().list().stream().map(BanTemplate::getId).toList();
        }
        return List.of();
    }
}
