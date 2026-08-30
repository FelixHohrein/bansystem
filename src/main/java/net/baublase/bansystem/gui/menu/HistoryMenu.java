package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.gui.GuiItems;
import net.baublase.bansystem.gui.GuiKeys;
import net.baublase.bansystem.gui.GuiLayouts;
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
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ban-Historie als Karten: aktive Einträge leuchten, abgelaufene bleiben sichtbar.
 */
public final class HistoryMenu extends GuiMenu {

    private static final int PAGE_SIZE = 28;

    private final PlayerRef target;
    private final int page;
    private int totalPages = 1;

    public HistoryMenu(BanSystemPlugin plugin, PlayerRef target) {
        this(plugin, target, 0);
    }

    public HistoryMenu(BanSystemPlugin plugin, PlayerRef target, int page) {
        super(plugin);
        this.target = target;
        this.page = Math.max(0, page);
    }

    @Override
    public void open(Player player) {
        plugin.scheduler().supplyAsync(() -> plugin.banService().history(target.getUuid())).thenAccept(history ->
                plugin.scheduler().runSync(() -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    totalPages = Math.max(1, (int) Math.ceil(history.size() / (double) PAGE_SIZE));
                    Inventory inventory = create(player, 54, Message.GUI_HISTORY_TITLE, "player", target.getName());
                    Locale locale = locale(player);
                    int from = Math.min(page * PAGE_SIZE, history.size());
                    int to = Math.min(history.size(), from + PAGE_SIZE);
                    int[] slots = GuiLayouts.inner28();
                    int index = 0;
                    for (int i = from; i < to; i++) {
                        inventory.setItem(slots[index++], card(history.get(i), locale));
                    }
                    if (history.isEmpty()) {
                        inventory.setItem(22, button(Material.GRAY_STAINED_GLASS_PANE, player, GuiKeys.IGNORE, Message.GUI_EMPTY));
                    }
                    boolean hasPrev = page > 0;
                    boolean hasNext = page + 1 < totalPages;
                    inventory.setItem(45, hasPrev
                            ? button(Material.ARROW, player, GuiKeys.PREV, Message.GUI_PREVIOUS)
                            : disabledNav(player, Message.GUI_PREVIOUS));
                    inventory.setItem(49, button(Material.PAPER, player, GuiKeys.IGNORE, Message.GUI_PAGE,
                            "page", String.valueOf(page + 1),
                            "pages", String.valueOf(totalPages)));
                    inventory.setItem(53, hasNext
                            ? button(Material.ARROW, player, GuiKeys.NEXT, Message.GUI_NEXT)
                            : disabledNav(player, Message.GUI_NEXT));
                    inventory.setItem(48, button(Material.ARROW, player, GuiKeys.BACK, Message.GUI_BACK));
                    GuiSounds.open(player);
                    player.openInventory(inventory);
                })).exceptionally(throwable -> {
            plugin.pluginLogger().error("Historie-GUI fehlgeschlagen", throwable);
            return null;
        });
    }

    private ItemStack card(Ban ban, Locale locale) {
        boolean live = ban.currentlyActive(Instant.now());
        String typeName = ban.permanent()
                ? messages.plain(locale, Message.HISTORY_TYPE_PERMANENT)
                : messages.plain(locale, Message.HISTORY_TYPE_TEMPORARY);
        String duration = ban.permanent()
                ? messages.plain(locale, Message.HISTORY_DURATION_PERMANENT)
                : DurationFormatter.remaining(ban.getExpiresAt(), messages, locale);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(ban.getReason(), NamedTextColor.WHITE));
        lore.add(Component.text(duration, NamedTextColor.AQUA));
        lore.add(messages.component(locale, Message.GUI_STAFF, "staff", ban.getStaff() == null ? "Console" : ban.getStaff().getName()));
        lore.add(Component.text(DurationFormatter.date(ban.getCreatedAt()), NamedTextColor.GRAY));
        lore.add(messages.component(locale, live ? Message.GUI_HISTORY_ACTIVE : Message.GUI_HISTORY_INACTIVE));
        ItemStack paper = named(live ? Material.FILLED_MAP : Material.MAP,
                Component.text("#" + ban.getId() + " " + typeName, live ? NamedTextColor.RED : NamedTextColor.GOLD),
                lore);
        if (live) {
            GuiItems.glow(paper);
        }
        GuiKeys.setAction(plugin, paper, GuiKeys.IGNORE);
        return paper;
    }

    @Override
    public void onAction(Player player, String action, String payload, InventoryClickEvent event) {
        switch (action) {
            case GuiKeys.BACK -> new PunishMenu(plugin, target).open(player);
            case GuiKeys.PREV -> {
                if (page > 0) {
                    GuiSounds.page(player);
                    new HistoryMenu(plugin, target, page - 1).open(player);
                } else {
                    GuiSounds.deny(player);
                }
            }
            case GuiKeys.NEXT -> {
                if (page + 1 < totalPages) {
                    GuiSounds.page(player);
                    new HistoryMenu(plugin, target, page + 1).open(player);
                } else {
                    GuiSounds.deny(player);
                }
            }
            default -> {
            }
        }
    }
}
