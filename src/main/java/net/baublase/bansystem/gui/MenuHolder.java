package net.baublase.bansystem.gui;

import lombok.Getter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Inventar-Halter, damit der Click-Listener das {@link GuiMenu} wiederfindet.
 */
@Getter
public final class MenuHolder implements InventoryHolder {

    private final GuiMenu menu;
    private Inventory inventory;

    public MenuHolder(GuiMenu menu) {
        this.menu = menu;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
