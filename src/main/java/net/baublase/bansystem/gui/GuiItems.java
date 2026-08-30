package net.baublase.bansystem.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Visuelle Defaults: keine kursive Vanilla-Lore, versteckte Attribute, optionales Glow.
 */
public final class GuiItems {

    private GuiItems() {
    }

    public static ItemStack pane(Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(plain(Component.empty()));
        hide(meta);
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack named(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(plain(name));
        if (lore != null && !lore.isEmpty()) {
            List<Component> styled = new ArrayList<>();
            for (Component line : lore) {
                styled.add(plain(line));
            }
            meta.lore(styled);
        }
        hide(meta);
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack glow(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        hide(meta);
        stack.setItemMeta(meta);
        return stack;
    }

    public static Component plain(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private static void hide(ItemMeta meta) {
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        );
    }
}
