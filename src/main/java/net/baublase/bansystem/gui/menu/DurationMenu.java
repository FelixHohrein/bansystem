package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.gui.GuiKeys;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.GuiSounds;
import net.baublase.bansystem.gui.input.PendingPunishActions;
import net.baublase.bansystem.i18n.Message;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.time.Duration;

/**
 * Schnelle Dauer-Buttons statt zuerst in den Chat zu müssen.
 */
public final class DurationMenu extends GuiMenu {

    private static final String[] PRESETS = {"1h", "6h", "12h", "1d", "7d", "30d"};

    private final PlayerRef target;

    public DurationMenu(BanSystemPlugin plugin, PlayerRef target) {
        super(plugin);
        this.target = target;
    }

    @Override
    public void open(Player player) {
        Inventory inventory = create(player, 27, Message.GUI_DURATION_TITLE);
        int[] slots = {10, 11, 12, 14, 15, 16};
        for (int i = 0; i < PRESETS.length; i++) {
            inventory.setItem(slots[i], button(
                    Material.CLOCK,
                    player,
                    "duration",
                    PRESETS[i],
                    Message.GUI_DURATION_PRESET,
                    "duration", PRESETS[i]
            ));
        }
        inventory.setItem(22, button(Material.NAME_TAG, player, GuiKeys.CUSTOM_DURATION, Message.GUI_CUSTOM_DURATION, Message.PROMPT_DURATION));
        inventory.setItem(18, button(Material.ARROW, player, GuiKeys.BACK, Message.GUI_BACK));
        GuiSounds.open(player);
        player.openInventory(inventory);
    }

    @Override
    public void onAction(Player player, String action, String payload, InventoryClickEvent event) {
        if (GuiKeys.BACK.equals(action)) {
            new PunishMenu(plugin, target).open(player);
            return;
        }
        if (GuiKeys.CUSTOM_DURATION.equals(action)) {
            player.closeInventory();
            plugin.pendingActions().put(player, new PendingPunishActions.Pending(
                    target, PendingPunishActions.Step.DURATION_TEMPORARY, null, null));
            messages.send(player, Message.PROMPT_DURATION);
            return;
        }
        if ("duration".equals(action) && payload != null) {
            Duration duration = net.baublase.bansystem.util.DurationParser.parse(payload).orElse(Duration.ofDays(1));
            new ReasonMenu(plugin, target, false, duration).open(player);
        }
    }
}
