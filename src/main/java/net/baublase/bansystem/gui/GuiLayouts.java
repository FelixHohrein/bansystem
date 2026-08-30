package net.baublase.bansystem.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Feste Slot-Muster für 54er-Inventare, damit Listen den Rahmen nicht überschreiben.
 */
public final class GuiLayouts {

    private GuiLayouts() {
    }

    /**
     * Innere 7×4-Fläche (28 Slots) eines 54er-Menüs.
     */
    public static int[] inner28() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Nächster innerer Slot, überspringt linken und rechten Rahmen.
     */
    public static int nextInner(int slot) {
        int next = slot + 1;
        if (next % 9 == 8) {
            next += 2;
        }
        if (next % 9 == 0) {
            next += 1;
        }
        return next;
    }
}
