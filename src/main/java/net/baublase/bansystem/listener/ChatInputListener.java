package net.baublase.bansystem.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.gui.input.PendingPunishActions;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.util.DurationParser;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.Optional;

public final class ChatInputListener implements Listener {

    private final BanSystemPlugin plugin;

    public ChatInputListener(BanSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.pendingActions().has(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        plugin.scheduler().runSync(() -> handle(player, text));
    }

    private void handle(Player player, String text) {
        PendingPunishActions.Pending pending = plugin.pendingActions().remove(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (text.equalsIgnoreCase("abort") || text.equalsIgnoreCase("cancel")) {
            plugin.messages().send(player, Message.ERROR_ABORT);
            return;
        }
        switch (pending.step()) {
            case REASON_PERMANENT -> plugin.punishExecutor().banPermanent(player, pending.target(), text, null);
            case DURATION_TEMPORARY -> {
                Optional<Duration> duration = DurationParser.parse(text);
                if (duration.isEmpty() || duration.get().isZero()) {
                    plugin.messages().send(player, Message.ERROR_INVALID_DURATION);
                    return;
                }
                plugin.pendingActions().put(player, new PendingPunishActions.Pending(
                        pending.target(), PendingPunishActions.Step.REASON_TEMPORARY, duration.get(), null));
                plugin.messages().send(player, Message.PROMPT_REASON);
            }
            case REASON_TEMPORARY -> plugin.punishExecutor().banTemporary(player, pending.target(), text, pending.duration(), null);
            case TEMPLATE_CONFIRM -> {
            }
        }
    }
}
