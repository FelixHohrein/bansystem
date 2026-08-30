package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.gui.GuiKeys;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.GuiSounds;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.util.DurationFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Locale;

public final class HistoryMenu extends GuiMenu {

    private final PlayerRef target;

    public HistoryMenu(BanSystemPlugin plugin, PlayerRef target) {
        super(plugin);
        this.target = target;
    }

    @Override
    public void open(Player player) {
        plugin.scheduler().supplyAsync(() -> plugin.banService().history(target.getUuid())).thenAccept(history ->
                plugin.scheduler().runSync(() -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    Inventory inventory = create(player, 54, Message.GUI_HISTORY_TITLE, "player", target.getName());
                    Locale locale = locale(player);
                    int slot = 10;
                    for (Ban ban : history) {
                        if (slot >= 44) {
                            break;
                        }
                        if (slot % 9 == 8) {
                            slot += 2;
                        }
                        String duration = ban.permanent()
                                ? messages.plain(locale, Message.HISTORY_DURATION_PERMANENT)
                                : DurationFormatter.remaining(ban.getExpiresAt(), messages, locale);
                        inventory.setItem(slot, named(Material.PAPER,
                                Component.text("#" + ban.getId() + " " + ban.getType().name(), NamedTextColor.GOLD),
                                List.of(
                                        Component.text(ban.getReason(), NamedTextColor.WHITE),
                                        Component.text(duration, NamedTextColor.AQUA),
                                        Component.text(DurationFormatter.date(ban.getCreatedAt()), NamedTextColor.GRAY),
                                        Component.text(ban.getStaff() == null ? "Console" : ban.getStaff().getName(), NamedTextColor.YELLOW)
                                )));
                        slot++;
                    }
                    if (history.isEmpty()) {
                        inventory.setItem(22, button(Material.GRAY_STAINED_GLASS_PANE, player, GuiKeys.IGNORE, Message.GUI_EMPTY));
                    }
                    inventory.setItem(45, button(Material.ARROW, player, GuiKeys.BACK, Message.GUI_BACK));
                    GuiSounds.open(player);
                    player.openInventory(inventory);
                })).exceptionally(throwable -> {
            plugin.pluginLogger().error("Historie-GUI fehlgeschlagen", throwable);
            return null;
        });
    }

    @Override
    public void onAction(Player player, String action, String payload, InventoryClickEvent event) {
        if (GuiKeys.BACK.equals(action)) {
            new PunishMenu(plugin, target).open(player);
        }
    }
}
