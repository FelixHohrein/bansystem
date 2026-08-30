package net.baublase.bansystem.listener;

import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bricht Item-Bewegung in unseren GUIs ab und entprellt Doppelklicks.
 */
public final class GuiListener implements Listener {

    private static final long DEBOUNCE_MS = 180L;
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof MenuHolder menuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = lastClick.put(player.getUniqueId(), now);
        if (previous != null && now - previous < DEBOUNCE_MS) {
            return;
        }
        GuiMenu menu = menuHolder.getMenu();
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            menu.onClick(event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }
}
