package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.domain.template.BanTemplate;
import net.baublase.bansystem.gui.GuiKeys;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.GuiSounds;
import net.baublase.bansystem.gui.input.PendingPunishActions;
import net.baublase.bansystem.gui.input.PunishDraft;
import net.baublase.bansystem.i18n.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;

/**
 * Grund aus Template oder frei im Chat — immer Pflicht, danach Bestätigung.
 */
public final class ReasonMenu extends GuiMenu {

    private final PlayerRef target;
    private final boolean permanent;
    private final Duration duration;

    public ReasonMenu(BanSystemPlugin plugin, PlayerRef target, boolean permanent, Duration duration) {
        super(plugin);
        this.target = target;
        this.permanent = permanent;
        this.duration = duration;
    }

    @Override
    public void open(Player player) {
        Inventory inventory = create(player, 54, Message.GUI_REASON_TITLE);
        inventory.setItem(10, button(Material.NAME_TAG, player, GuiKeys.CUSTOM_REASON, Message.GUI_CUSTOM_REASON, Message.PROMPT_REASON));
        int slot = 19;
        for (BanTemplate template : plugin.templateService().list()) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 8) {
                slot += 2;
            }
            ItemStack paper = named(Material.PAPER,
                    Component.text(template.getName(), NamedTextColor.GOLD),
                    List.of(Component.text(template.getReason(), NamedTextColor.WHITE)));
            GuiKeys.setAction(plugin, paper, "template-reason", template.getId());
            inventory.setItem(slot, paper);
            slot++;
        }
        inventory.setItem(45, button(Material.ARROW, player, GuiKeys.BACK, Message.GUI_BACK));
        GuiSounds.open(player);
        player.openInventory(inventory);
    }

    @Override
    public void onAction(Player player, String action, String payload, InventoryClickEvent event) {
        if (GuiKeys.BACK.equals(action)) {
            new PunishMenu(plugin, target).open(player);
            return;
        }
        if (GuiKeys.CUSTOM_REASON.equals(action)) {
            player.closeInventory();
            PendingPunishActions.Step step = permanent
                    ? PendingPunishActions.Step.REASON_PERMANENT
                    : PendingPunishActions.Step.REASON_TEMPORARY;
            plugin.pendingActions().put(player, new PendingPunishActions.Pending(target, step, duration, null));
            messages.send(player, Message.PROMPT_REASON);
            return;
        }
        if ("template-reason".equals(action) && payload != null) {
            plugin.templateService().find(payload).ifPresent(template ->
                    new ConfirmMenu(plugin, new PunishDraft(target, permanent, duration, template.getReason(), template.getId())).open(player));
        }
    }
}
