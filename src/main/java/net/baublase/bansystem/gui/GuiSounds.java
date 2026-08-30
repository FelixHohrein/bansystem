package net.baublase.bansystem.gui;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Einheitliche UI-Sounds, damit jedes Menü gleich bedienbar wirkt.
 */
public final class GuiSounds {

    private GuiSounds() {
    }

    public static void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.15f);
    }

    public static void open(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.35f, 1.35f);
    }

    public static void success(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
    }

    public static void deny(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
    }

    public static void page(Player player) {
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.1f);
    }
}
