package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.gui.GuiMenu;
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
                    int slot = 0;
                    for (Ban ban : history) {
                        if (slot >= 45) {
                            break;
                        }
                        String duration = ban.permanent()
                                ? messages.plain(locale, Message.HISTORY_DURATION_PERMANENT)
                                : DurationFormatter.remaining(ban.getExpiresAt(), messages, locale);
                        inventory.setItem(slot, named(Material.PAPER,
                                Component.text("#" + (slot + 1) + " " + ban.getType().name(), NamedTextColor.GOLD),
                                List.of(
                                        Component.text(ban.getReason(), NamedTextColor.WHITE),
                                        Component.text(duration, NamedTextColor.AQUA),
                                        Component.text(DurationFormatter.date(ban.getCreatedAt()), NamedTextColor.GRAY),
                                        Component.text(ban.getStaff() == null ? "Console" : ban.getStaff().getName(), NamedTextColor.YELLOW)
                                )));
                        slot++;
                    }
                    if (slot == 0) {
                        inventory.setItem(22, item(Material.GRAY_STAINED_GLASS_PANE, player, Message.GUI_EMPTY, null));
                    }
                    inventory.setItem(49, item(Material.BARRIER, player, Message.GUI_BACK, null));
                    player.openInventory(inventory);
                })).exceptionally(throwable -> {
            plugin.pluginLogger().error("Historie-GUI fehlgeschlagen", throwable);
            return null;
        });
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player player) {
            new PunishMenu(plugin, target).open(player);
        }
    }
}
