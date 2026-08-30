package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.gui.GuiItems;
import net.baublase.bansystem.gui.GuiKeys;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.GuiSounds;
import net.baublase.bansystem.gui.input.PendingPunishActions;
import net.baublase.bansystem.i18n.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Zentraler Einstieg: Listen, Suche, Templates, Hilfe und Kurzstatistik.
 */
public final class MainMenu extends GuiMenu {

    public MainMenu(BanSystemPlugin plugin) {
        super(plugin);
    }

    @Override
    public void open(Player player) {
        Inventory inventory = create(player, 54, Message.GUI_MAIN_TITLE);
        inventory.setItem(4, plugin.skullFactory().simpleHead(
                player.getUniqueId(),
                player.getName(),
                Component.text(player.getName(), NamedTextColor.GOLD),
                List.of(messages.component(locale(player), Message.GUI_HELP_ITEM_LORE))
        ));

        if (!plugin.storage().isEnabled()) {
            inventory.setItem(22, button(Material.BARRIER, player, GuiKeys.IGNORE, Message.GUI_DATABASE_DISABLED));
            inventory.setItem(31, button(Material.BOOK, player, GuiKeys.HELP, Message.GUI_HELP_ITEM, Message.GUI_HELP_ITEM_LORE));
            inventory.setItem(49, button(Material.BARRIER, player, GuiKeys.CLOSE, Message.GUI_CLOSE));
            GuiSounds.open(player);
            player.openInventory(inventory);
            return;
        }

        ItemStack all = button(Material.PLAYER_HEAD, player, GuiKeys.ALL_PLAYERS, Message.GUI_ALL_PLAYERS, Message.GUI_ALL_PLAYERS_LORE);
        ItemStack banned = GuiItems.glow(button(Material.WITHER_SKELETON_SKULL, player, GuiKeys.BANNED_PLAYERS, Message.GUI_BANNED_PLAYERS, Message.GUI_BANNED_PLAYERS_LORE));
        ItemStack templates = button(Material.WRITABLE_BOOK, player, GuiKeys.TEMPLATES, Message.GUI_TEMPLATES, Message.GUI_TEMPLATES_LORE);
        inventory.setItem(20, all);
        inventory.setItem(22, banned);
        inventory.setItem(24, templates);

        inventory.setItem(29, button(Material.COMPASS, player, GuiKeys.SEARCH, Message.GUI_SEARCH, Message.GUI_SEARCH_LORE));
        inventory.setItem(31, button(Material.BOOK, player, GuiKeys.HELP, Message.GUI_HELP_ITEM, Message.GUI_HELP_ITEM_LORE));
        inventory.setItem(33, statsButton(player));
        inventory.setItem(49, button(Material.BARRIER, player, GuiKeys.CLOSE, Message.GUI_CLOSE));

        GuiSounds.open(player);
        player.openInventory(inventory);
        refreshStats(player, inventory);
    }

    private ItemStack statsButton(Player player) {
        return button(Material.MAP, player, GuiKeys.STATS, Message.GUI_STATS, List.of(
                messages.component(locale(player), Message.GUI_LOADING)
        ));
    }

    private void refreshStats(Player player, Inventory inventory) {
        plugin.scheduler().supplyAsync(() -> new int[]{
                plugin.banService().countKnown(),
                plugin.banService().countBanned()
        }).thenAccept(counts -> plugin.scheduler().runSync(() -> {
            if (!player.isOnline()) {
                return;
            }
            ItemStack stats = button(Material.MAP, player, GuiKeys.STATS, Message.GUI_STATS, List.of(
                    messages.component(locale(player), Message.GUI_STATS_PLAYERS, "count", String.valueOf(counts[0])),
                    messages.component(locale(player), Message.GUI_STATS_BANNED, "count", String.valueOf(counts[1]))
            ));
            inventory.setItem(33, stats);
        }));
    }

    @Override
    public void onAction(Player player, String action, String payload, InventoryClickEvent event) {
        switch (action) {
            case GuiKeys.ALL_PLAYERS -> new PlayerBrowserMenu(plugin, PlayerBrowserMenu.Mode.ALL).open(player);
            case GuiKeys.BANNED_PLAYERS -> new PlayerBrowserMenu(plugin, PlayerBrowserMenu.Mode.BANNED).open(player);
            case GuiKeys.TEMPLATES -> new TemplateMenu(plugin).open(player);
            case GuiKeys.SEARCH -> {
                player.closeInventory();
                plugin.pendingActions().put(player, new PendingPunishActions.Pending(
                        null, PendingPunishActions.Step.SEARCH, null, null));
                messages.send(player, Message.PROMPT_SEARCH);
            }
            case GuiKeys.HELP -> {
                player.closeInventory();
                player.performCommand("bans help");
            }
            case GuiKeys.CLOSE -> player.closeInventory();
            default -> {
            }
        }
    }
}
