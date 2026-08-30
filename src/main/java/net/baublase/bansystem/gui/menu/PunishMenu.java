package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.domain.template.BanTemplate;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.input.PendingPunishActions;
import net.baublase.bansystem.i18n.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PunishMenu extends GuiMenu {

    private final PlayerRef target;
    private final Map<Integer, BanTemplate> templateSlots = new HashMap<>();

    public PunishMenu(BanSystemPlugin plugin, PlayerRef target) {
        super(plugin);
        this.target = target;
    }

    @Override
    public void open(Player player) {
        Inventory inventory = create(player, 54, Message.GUI_PUNISH_TITLE, "player", target.getName());
        inventory.setItem(4, plugin.skullFactory().simpleHead(
                target.getUuid(),
                target.getName(),
                Component.text(target.getName(), NamedTextColor.YELLOW),
                List.of()
        ));
        inventory.setItem(19, item(Material.RED_CONCRETE, player, Message.GUI_BAN_PERMANENT, null));
        inventory.setItem(21, item(Material.ORANGE_CONCRETE, player, Message.GUI_BAN_TEMPORARY, null));
        inventory.setItem(23, item(Material.LIME_CONCRETE, player, Message.GUI_UNBAN, null));
        inventory.setItem(25, item(Material.BOOK, player, Message.GUI_HISTORY, null));
        inventory.setItem(31, item(Material.ENDER_EYE, player, Message.GUI_ALTCHECK, null));
        List<BanTemplate> templates = plugin.templateService().list();
        int slot = 36;
        for (BanTemplate template : templates) {
            if (slot >= 45) {
                break;
            }
            ItemStack stack = named(Material.PAPER,
                    messages.component(locale(player), Message.GUI_APPLY_TEMPLATE, "name", template.getName()),
                    new ArrayList<>());
            inventory.setItem(slot, stack);
            templateSlots.put(slot, template);
            slot++;
        }
        inventory.setItem(49, item(Material.BARRIER, player, Message.GUI_BACK, null));
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 49) {
            new MainMenu(plugin).open(player);
            return;
        }
        if (slot == 19) {
            player.closeInventory();
            plugin.pendingActions().put(player, new PendingPunishActions.Pending(target, PendingPunishActions.Step.REASON_PERMANENT, null, null));
            messages.send(player, Message.PROMPT_REASON);
            return;
        }
        if (slot == 21) {
            player.closeInventory();
            plugin.pendingActions().put(player, new PendingPunishActions.Pending(target, PendingPunishActions.Step.DURATION_TEMPORARY, null, null));
            messages.send(player, Message.PROMPT_DURATION);
            return;
        }
        if (slot == 23) {
            plugin.punishExecutor().unban(player, target);
            player.closeInventory();
            return;
        }
        if (slot == 25) {
            new HistoryMenu(plugin, target).open(player);
            return;
        }
        if (slot == 31) {
            new AltMenu(plugin, target).open(player);
            return;
        }
        BanTemplate template = templateSlots.get(slot);
        if (template != null) {
            plugin.punishExecutor().applyTemplate(player, target, template);
            player.closeInventory();
        }
    }
}
