package net.baublase.bansystem.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.gui.GuiSounds;
import net.baublase.bansystem.gui.input.PendingPunishActions;
import net.baublase.bansystem.gui.input.PunishDraft;
import net.baublase.bansystem.gui.menu.ConfirmMenu;
import net.baublase.bansystem.gui.menu.PunishMenu;
import net.baublase.bansystem.gui.menu.ReasonMenu;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.util.DurationParser;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.Optional;

/**
 * Fängt Chat ab, solange Staff einen GUI-Schritt (Suche, Dauer, Grund) offen hat.
 */
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
            GuiSounds.deny(player);
            return;
        }
        switch (pending.step()) {
            case SEARCH -> plugin.scheduler().supplyAsync(() -> plugin.banService().known(text)).thenAccept(known ->
                    plugin.scheduler().runSync(() -> openSearch(player, text, known)));
            case REASON_PERMANENT -> new ConfirmMenu(plugin, new PunishDraft(pending.target(), true, null, text, null)).open(player);
            case DURATION_TEMPORARY -> {
                Optional<Duration> duration = DurationParser.parse(text);
                if (duration.isEmpty() || duration.get().isZero()) {
                    plugin.messages().send(player, Message.ERROR_INVALID_DURATION);
                    GuiSounds.deny(player);
                    return;
                }
                new ReasonMenu(plugin, pending.target(), false, duration.get()).open(player);
            }
            case REASON_TEMPORARY -> new ConfirmMenu(plugin, new PunishDraft(
                    pending.target(), false, pending.duration(), text, null)).open(player);
            case TEMPLATE_CONFIRM -> {
            }
        }
    }

    private void openSearch(Player player, String query, Optional<KnownPlayer> known) {
        if (known.isEmpty()) {
            plugin.messages().send(player, Message.ERROR_UNKNOWN_PLAYER, "player", query);
            GuiSounds.deny(player);
            return;
        }
        KnownPlayer target = known.get();
        GuiSounds.success(player);
        new PunishMenu(plugin, PlayerRef.builder().uuid(target.getUuid()).name(target.getName()).build()).open(player);
    }
}
