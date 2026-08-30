package net.baublase.bansystem.gui;

import net.baublase.bansystem.BanSystemPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Persistente GUI-Aktionen in Item-PDC.
 * So hängen Klicks nicht an magischen Slot-Nummern.
 */
public final class GuiKeys {

    public static final String ALL_PLAYERS = "all-players";
    public static final String BANNED_PLAYERS = "banned-players";
    public static final String TEMPLATES = "templates";
    public static final String CLOSE = "close";
    public static final String BACK = "back";
    public static final String NEXT = "next";
    public static final String PREV = "prev";
    public static final String SEARCH = "search";
    public static final String HELP = "help";
    public static final String STATS = "stats";
    public static final String BAN_PERM = "ban-perm";
    public static final String BAN_TEMP = "ban-temp";
    public static final String UNBAN = "unban";
    public static final String HISTORY = "history";
    public static final String ALT = "alt";
    public static final String CONFIRM = "confirm";
    public static final String CANCEL = "cancel";
    public static final String CUSTOM_REASON = "custom-reason";
    public static final String CUSTOM_DURATION = "custom-duration";
    public static final String IGNORE = "ignore";

    private GuiKeys() {
    }

    public static NamespacedKey action(BanSystemPlugin plugin) {
        return new NamespacedKey(plugin, "gui-action");
    }

    public static NamespacedKey payload(BanSystemPlugin plugin) {
        return new NamespacedKey(plugin, "gui-payload");
    }

    public static void setAction(BanSystemPlugin plugin, ItemStack stack, String action) {
        setAction(plugin, stack, action, null);
    }

    public static void setAction(BanSystemPlugin plugin, ItemStack stack, String action, String extra) {
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(action(plugin), PersistentDataType.STRING, action);
        if (extra != null) {
            meta.getPersistentDataContainer().set(payload(plugin), PersistentDataType.STRING, extra);
        }
        stack.setItemMeta(meta);
    }

    public static String actionOf(BanSystemPlugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer().get(action(plugin), PersistentDataType.STRING);
    }

    public static String payloadOf(BanSystemPlugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer().get(payload(plugin), PersistentDataType.STRING);
    }
}
