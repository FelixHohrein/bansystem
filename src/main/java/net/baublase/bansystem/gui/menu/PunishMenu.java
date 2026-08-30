package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.domain.template.BanTemplate;
import net.baublase.bansystem.gui.GuiKeys;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.GuiSounds;
import net.baublase.bansystem.gui.input.PunishDraft;
import net.baublase.bansystem.i18n.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Aktionszentrale für einen Spieler: Ban, Tempban, Unban, Historie, Alt-Check, Templates.
 */
public final class PunishMenu extends GuiMenu {

    private final PlayerRef target;

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
                List.of(messages.component(locale(player), Message.GUI_CLICK_OPEN))
        ));
        inventory.setItem(19, button(Material.RED_CONCRETE, player, GuiKeys.BAN_PERM, Message.GUI_BAN_PERMANENT, Message.GUI_BAN_PERMANENT_LORE));
        inventory.setItem(21, button(Material.ORANGE_CONCRETE, player, GuiKeys.BAN_TEMP, Message.GUI_BAN_TEMPORARY, Message.GUI_BAN_TEMPORARY_LORE));
        inventory.setItem(23, button(Material.LIME_CONCRETE, player, GuiKeys.UNBAN, Message.GUI_UNBAN, Message.GUI_UNBAN_LORE));
        inventory.setItem(25, button(Material.BOOK, player, GuiKeys.HISTORY, Message.GUI_HISTORY, Message.GUI_HISTORY_LORE));
        inventory.setItem(31, button(Material.ENDER_EYE, player, GuiKeys.ALT, Message.GUI_ALTCHECK, Message.GUI_ALTCHECK_LORE));

        int slot = 37;
        for (BanTemplate template : plugin.templateService().list()) {
            if (slot >= 44) {
                break;
            }
            ItemStack paper = named(Material.PAPER,
                    messages.component(locale(player), Message.GUI_APPLY_TEMPLATE, "name", template.getName()),
                    List.of(Component.text(template.getReason(), NamedTextColor.GRAY)));
            GuiKeys.setAction(plugin, paper, "apply-template", template.getId());
            inventory.setItem(slot, paper);
            slot++;
        }
        inventory.setItem(45, button(Material.ARROW, player, GuiKeys.BACK, Message.GUI_BACK));
        GuiSounds.open(player);
        player.openInventory(inventory);
    }

    @Override
    public void onAction(Player player, String action, String payload, InventoryClickEvent event) {
        switch (action) {
            case GuiKeys.BACK -> new MainMenu(plugin).open(player);
            case GuiKeys.BAN_PERM -> new ReasonMenu(plugin, target, true, null).open(player);
            case GuiKeys.BAN_TEMP -> new DurationMenu(plugin, target).open(player);
            case GuiKeys.UNBAN -> new ConfirmMenu(plugin, new PunishDraft(target, true, null, "-", null), true).open(player);
            case GuiKeys.HISTORY -> new HistoryMenu(plugin, target).open(player);
            case GuiKeys.ALT -> new AltMenu(plugin, target).open(player);
            case "apply-template" -> plugin.templateService().find(payload).ifPresent(template ->
                    new ConfirmMenu(plugin, new PunishDraft(
                            target,
                            template.permanent(),
                            template.getDuration(),
                            template.getReason(),
                            template.getId()
                    )).open(player));
            default -> {
            }
        }
    }
}
