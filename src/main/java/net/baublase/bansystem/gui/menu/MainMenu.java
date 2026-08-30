package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.i18n.Message;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class MainMenu extends GuiMenu {

    public MainMenu(BanSystemPlugin plugin) {
        super(plugin);
    }

    @Override
    public void open(Player player) {
        Inventory inventory = create(player, 27, Message.GUI_MAIN_TITLE);
        inventory.setItem(11, item(Material.PLAYER_HEAD, player, Message.GUI_ALL_PLAYERS, Message.GUI_ALL_PLAYERS_LORE));
        inventory.setItem(13, item(Material.WITHER_SKELETON_SKULL, player, Message.GUI_BANNED_PLAYERS, Message.GUI_BANNED_PLAYERS_LORE));
        inventory.setItem(15, item(Material.WRITABLE_BOOK, player, Message.GUI_TEMPLATES, Message.GUI_TEMPLATES_LORE));
        inventory.setItem(22, item(Material.BARRIER, player, Message.GUI_CLOSE, null));
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 11) {
            new PlayerBrowserMenu(plugin, PlayerBrowserMenu.Mode.ALL).open(player);
        } else if (slot == 13) {
            new PlayerBrowserMenu(plugin, PlayerBrowserMenu.Mode.BANNED).open(player);
        } else if (slot == 15) {
            new TemplateMenu(plugin).open(player);
        } else if (slot == 22) {
            player.closeInventory();
        }
    }
}
