package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.template.BanTemplate;
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

public final class TemplateMenu extends GuiMenu {

    public TemplateMenu(BanSystemPlugin plugin) {
        super(plugin);
    }

    @Override
    public void open(Player player) {
        Inventory inventory = create(player, 54, Message.GUI_TEMPLATES_TITLE);
        Locale locale = locale(player);
        int slot = 10;
        for (BanTemplate template : plugin.templateService().list()) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 8) {
                slot += 2;
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
        inventory.setItem(45, button(Material.ARROW, player, GuiKeys.BACK, Message.GUI_BACK));
        GuiSounds.open(player);
        player.openInventory(inventory);
    }

    @Override
    public void onAction(Player player, String action, String payload, InventoryClickEvent event) {
        if (GuiKeys.BACK.equals(action)) {
            new MainMenu(plugin).open(player);
        }
    }
}
