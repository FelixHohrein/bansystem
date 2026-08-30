package net.baublase.bansystem.gui;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.i18n.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Basis für alle Inventar-Menüs: Rahmen, PDC-Aktionen, einheitliche Buttons.
 */
public abstract class GuiMenu {

    protected final BanSystemPlugin plugin;
    protected final MessageService messages;

    protected GuiMenu(BanSystemPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.messages();
    }

    public abstract void open(Player player);

    /**
     * Klick auf ein Item mit {@link GuiKeys}-Aktion.
     */
    public abstract void onAction(Player player, String action, String payload, InventoryClickEvent event);

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        String action = GuiKeys.actionOf(plugin, current);
        if (action == null || GuiKeys.IGNORE.equals(action)) {
            return;
        }
        GuiSounds.click(player);
        onAction(player, action, GuiKeys.payloadOf(plugin, current), event);
    }

    protected Inventory create(Player player, int size, Message title, String... placeholders) {
        MenuHolder holder = new MenuHolder(this);
        Inventory inventory = Bukkit.createInventory(holder, size, messages.component(locale(player), title, placeholders));
        holder.inventory(inventory);
        frame(inventory);
        return inventory;
    }

    /**
     * Dunkler Rahmen, damit der Inhalt in der Mitte klarer wirkt.
     */
    protected void frame(Inventory inventory) {
        ItemStack pane = filler();
        int size = inventory.getSize();
        int rows = size / 9;
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, pane);
            inventory.setItem(size - 9 + i, pane);
        }
        for (int row = 1; row < rows - 1; row++) {
            inventory.setItem(row * 9, pane);
            inventory.setItem(row * 9 + 8, pane);
        }
    }

    protected ItemStack filler() {
        ItemStack pane = GuiItems.pane(Material.BLACK_STAINED_GLASS_PANE);
        GuiKeys.setAction(plugin, pane, GuiKeys.IGNORE);
        return pane;
    }

    protected ItemStack button(Material material, Player player, String action, Message name) {
        return button(material, player, action, name, List.of());
    }

    protected ItemStack button(Material material, Player player, String action, Message name, Message lore) {
        List<Component> loreLines = new ArrayList<>();
        if (lore != null) {
            loreLines.add(messages.component(locale(player), lore));
        }
        return button(material, player, action, name, loreLines);
    }

    protected ItemStack button(Material material, Player player, String action, Message name, String... placeholders) {
        ItemStack stack = GuiItems.named(material, messages.component(locale(player), name, placeholders), List.of());
        GuiKeys.setAction(plugin, stack, action);
        return stack;
    }

    protected ItemStack button(Material material, Player player, String action, String payload, Message name, String... placeholders) {
        ItemStack stack = GuiItems.named(material, messages.component(locale(player), name, placeholders), List.of());
        GuiKeys.setAction(plugin, stack, action, payload);
        return stack;
    }

    protected ItemStack button(Material material, Player player, String action, Message name, List<Component> extraLore) {
        List<Component> lore = extraLore == null ? List.of() : extraLore;
        ItemStack stack = GuiItems.named(material, messages.component(locale(player), name), lore);
        GuiKeys.setAction(plugin, stack, action);
        return stack;
    }

    protected ItemStack named(Material material, Component name, List<Component> lore) {
        return GuiItems.named(material, name, lore);
    }

    protected Locale locale(Player player) {
        return messages.resolveLocale(player);
    }
}
