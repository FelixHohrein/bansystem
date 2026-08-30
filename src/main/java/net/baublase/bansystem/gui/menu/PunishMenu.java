package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.domain.template.BanTemplate;
import net.baublase.bansystem.gui.GuiItems;
import net.baublase.bansystem.gui.GuiKeys;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.GuiSounds;
import net.baublase.bansystem.gui.input.PunishDraft;
import net.baublase.bansystem.i18n.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        player.sendActionBar(messages.component(locale(player), Message.GUI_LOADING));
        plugin.scheduler().supplyAsync(() -> plugin.banService().activeBan(target.getUuid()))
                .thenAccept(active -> plugin.scheduler().runSync(() -> render(player, active)))
                .exceptionally(throwable -> {
                    plugin.pluginLogger().error("Punish-GUI fehlgeschlagen", throwable);
                    return null;
                });
    }

    private void render(Player player, Optional<Ban> active) {
        if (!player.isOnline()) {
            return;
        }
        Inventory inventory = create(player, 54, Message.GUI_PUNISH_TITLE, "player", target.getName());
        Player online = Bukkit.getPlayer(target.getUuid());
        boolean banned = active.isPresent();
        boolean immune = plugin.punishExecutor().isImmune(player, online);

        List<Component> headLore = new ArrayList<>();
        headLore.add(messages.component(locale(player), online != null ? Message.GUI_ONLINE : Message.GUI_OFFLINE));
        headLore.add(messages.component(locale(player), banned ? Message.GUI_LORE_BANNED : Message.GUI_LORE_NOT_BANNED));
        if (immune) {
            headLore.add(messages.component(locale(player), Message.GUI_LORE_IMMUNE));
        }
        headLore.add(messages.component(locale(player), Message.GUI_CLICK_OPEN));
        ItemStack head = plugin.skullFactory().simpleHead(
                target.getUuid(),
                target.getName(),
                Component.text(target.getName(), NamedTextColor.YELLOW),
                headLore
        );
        GuiKeys.setAction(plugin, head, GuiKeys.HISTORY);
        inventory.setItem(4, head);

        inventory.setItem(19, button(Material.RED_CONCRETE, player, GuiKeys.BAN_PERM, Message.GUI_BAN_PERMANENT, Message.GUI_BAN_PERMANENT_LORE));
        inventory.setItem(21, button(Material.ORANGE_CONCRETE, player, GuiKeys.BAN_TEMP, Message.GUI_BAN_TEMPORARY, Message.GUI_BAN_TEMPORARY_LORE));
        if (banned) {
            inventory.setItem(23, GuiItems.glow(button(Material.LIME_CONCRETE, player, GuiKeys.UNBAN, Message.GUI_UNBAN, Message.GUI_UNBAN_LORE)));
        } else {
            inventory.setItem(23, button(Material.LIGHT_GRAY_CONCRETE, player, GuiKeys.IGNORE, Message.GUI_UNBAN, Message.GUI_NOT_BANNED_HINT));
        }
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
            GuiKeys.setAction(plugin, paper, GuiKeys.APPLY_TEMPLATE, template.getId());
            inventory.setItem(slot, paper);
            slot++;
        }
        inventory.setItem(45, button(Material.ARROW, player, GuiKeys.BACK, Message.GUI_BACK));
        GuiSounds.open(player);
        player.openInventory(inventory);
    }

    @Override
    public void onAction(Player player, String action, String payload, InventoryClickEvent event) {
        Player online = Bukkit.getPlayer(target.getUuid());
        boolean punishing = GuiKeys.BAN_PERM.equals(action)
                || GuiKeys.BAN_TEMP.equals(action)
                || GuiKeys.APPLY_TEMPLATE.equals(action);
        if (punishing && plugin.punishExecutor().isImmune(player, online)) {
            GuiSounds.deny(player);
            messages.send(player, Message.ERROR_IMMUNE, "player", target.getName());
            return;
        }
        switch (action) {
            case GuiKeys.BACK -> new MainMenu(plugin).open(player);
            case GuiKeys.BAN_PERM -> new ReasonMenu(plugin, target, true, null).open(player);
            case GuiKeys.BAN_TEMP -> new DurationMenu(plugin, target).open(player);
            case GuiKeys.UNBAN -> new ConfirmMenu(plugin, new PunishDraft(target, true, null, "-", null), true).open(player);
            case GuiKeys.HISTORY -> new HistoryMenu(plugin, target).open(player);
            case GuiKeys.ALT -> new AltMenu(plugin, target).open(player);
            case GuiKeys.APPLY_TEMPLATE -> plugin.templateService().find(payload).ifPresent(template ->
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
