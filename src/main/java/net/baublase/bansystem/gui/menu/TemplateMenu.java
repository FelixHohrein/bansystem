package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.template.BanTemplate;
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

public final class TemplateMenu extends GuiMenu {

    public TemplateMenu(BanSystemPlugin plugin) {
        super(plugin);
    }

    @Override
    public void open(Player player) {
        Inventory inventory = create(player, 54, Message.GUI_TEMPLATES_TITLE);
        Locale locale = locale(player);
        int slot = 0;
        for (BanTemplate template : plugin.templateService().list()) {
            if (slot >= 45) {
                break;
            }
            String duration = DurationFormatter.format(template.getDuration(), messages, locale);
            inventory.setItem(slot, named(Material.PAPER,
                    Component.text(template.getName(), NamedTextColor.GOLD),
                    List.of(
                            Component.text(template.getId(), NamedTextColor.DARK_GRAY),
                            Component.text(duration, NamedTextColor.AQUA),
                            Component.text(template.getReason(), NamedTextColor.WHITE)
                    )));
            slot++;
        }
        inventory.setItem(49, item(Material.BARRIER, player, Message.GUI_BACK, null));
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getRawSlot() == 49 && event.getWhoClicked() instanceof Player player) {
            new MainMenu(plugin).open(player);
        }
    }
}
