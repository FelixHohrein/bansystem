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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;

public abstract class GuiMenu {

    protected final BanSystemPlugin plugin;
    protected final MessageService messages;

    protected GuiMenu(BanSystemPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.messages();
    }

    public abstract void open(Player player);

    public abstract void onClick(InventoryClickEvent event);

    protected Inventory create(Player player, int size, Message title, String... placeholders) {
        MenuHolder holder = new MenuHolder(this);
        Inventory inventory = Bukkit.createInventory(holder, size, messages.component(locale(player), title, placeholders));
        holder.inventory(inventory);
        return inventory;
    }

    protected ItemStack item(Material material, Player player, Message name, Message lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        Locale locale = locale(player);
        meta.displayName(messages.component(locale, name));
        if (lore != null) {
            meta.lore(List.of(messages.component(locale, lore)));
        }
        stack.setItemMeta(meta);
        return stack;
    }

    protected ItemStack item(Material material, Player player, Message name, String... placeholders) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(messages.component(locale(player), name, placeholders));
        stack.setItemMeta(meta);
        return stack;
    }

    protected ItemStack named(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    protected Locale locale(Player player) {
        return messages.resolveLocale(player);
    }
}
