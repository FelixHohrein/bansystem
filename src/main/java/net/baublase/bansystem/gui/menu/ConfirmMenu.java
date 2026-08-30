package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.gui.GuiItems;
import net.baublase.bansystem.gui.GuiKeys;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.GuiSounds;
import net.baublase.bansystem.gui.input.PunishDraft;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.util.DurationFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Letzter Schritt vor Ban/Unban: Zusammenfassung und Bestätigen/Abbrechen.
 */
public final class ConfirmMenu extends GuiMenu {

    private final PunishDraft draft;
    private final boolean unban;

    public ConfirmMenu(BanSystemPlugin plugin, PunishDraft draft) {
        this(plugin, draft, false);
    }

    public ConfirmMenu(BanSystemPlugin plugin, PunishDraft draft, boolean unban) {
        super(plugin);
        this.draft = draft;
        this.unban = unban;
    }

    @Override
    public void open(Player player) {
        Inventory inventory = create(player, 27, Message.GUI_CONFIRM_TITLE);
        Locale locale = locale(player);
        String duration = unban
                ? "-"
                : (draft.permanent()
                ? messages.plain(locale, Message.DURATION_PERMANENT)
                : DurationFormatter.format(draft.duration(), messages, locale));
        Message action = unban
                ? Message.GUI_ACTION_UNBAN
                : (draft.permanent() ? Message.GUI_ACTION_PERM : Message.GUI_ACTION_TEMP);
        List<Component> lore = new ArrayList<>();
        lore.add(messages.component(locale, action));
        lore.add(Component.text(duration, NamedTextColor.AQUA));
        lore.add(Component.text(draft.reason() == null ? "-" : draft.reason(), NamedTextColor.WHITE));
        if (!unban) {
            plugin.scheduler().supplyAsync(() -> plugin.banService().activeBan(draft.target().getUuid()).isPresent())
                    .thenAccept(already -> plugin.scheduler().runSync(() -> {
                        if (already && player.isOnline() && inventory.equals(player.getOpenInventory().getTopInventory())) {
                            List<Component> withWarn = new ArrayList<>(lore);
                            withWarn.add(messages.component(locale, Message.GUI_ALREADY_BANNED));
                            inventory.setItem(4, head(player, withWarn));
                        }
                    }));
        }
        inventory.setItem(4, head(player, lore));
        inventory.setItem(11, GuiItems.glow(button(Material.LIME_CONCRETE, player, GuiKeys.CONFIRM, Message.GUI_CONFIRM, Message.GUI_CONFIRM_LORE)));
        inventory.setItem(15, button(Material.RED_CONCRETE, player, GuiKeys.CANCEL, Message.GUI_CANCEL));
        GuiSounds.open(player);
        player.openInventory(inventory);
    }

    private ItemStack head(Player player, List<Component> lore) {
        return plugin.skullFactory().simpleHead(
                draft.target().getUuid(),
                draft.target().getName(),
                Component.text(draft.target().getName(), NamedTextColor.YELLOW),
                lore
        );
    }

    @Override
    public void onAction(Player player, String action, String payload, InventoryClickEvent event) {
        if (GuiKeys.CANCEL.equals(action)) {
            new PunishMenu(plugin, draft.target()).open(player);
            return;
        }
        if (!GuiKeys.CONFIRM.equals(action)) {
            return;
        }
        player.closeInventory();
        GuiSounds.success(player);
        if (unban) {
            plugin.punishExecutor().unban(player, draft.target());
            return;
        }
        if (draft.permanent()) {
            plugin.punishExecutor().banPermanent(player, draft.target(), draft.reason(), draft.templateId());
        } else {
            plugin.punishExecutor().banTemporary(player, draft.target(), draft.reason(), draft.duration(), draft.templateId());
        }
    }
}
