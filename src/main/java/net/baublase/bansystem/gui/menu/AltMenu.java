package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.alt.AltMatch;
import net.baublase.bansystem.domain.alt.AltScore;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.gui.GuiKeys;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.GuiSounds;
import net.baublase.bansystem.i18n.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

public final class AltMenu extends GuiMenu {

    private final PlayerRef target;

    public AltMenu(BanSystemPlugin plugin, PlayerRef target) {
        super(plugin);
        this.target = target;
    }

    @Override
    public void open(Player player) {
        plugin.scheduler().supplyAsync(() -> plugin.altCheckService().score(target.getUuid())).thenAccept(score ->
                plugin.scheduler().runSync(() -> render(player, score))).exceptionally(throwable -> {
            plugin.pluginLogger().error("Alt-GUI fehlgeschlagen", throwable);
            return null;
        });
    }

    private void render(Player player, AltScore score) {
        if (!player.isOnline()) {
            return;
        }
        Inventory inventory = create(player, 54, Message.GUI_ALT_TITLE, "player", target.getName());
        Locale locale = locale(player);
        String main = score.getLikelyMain() == null ? "-" : score.getLikelyMain().getName();
        inventory.setItem(4, named(Material.ENDER_EYE,
                messages.component(locale, Message.ALT_HEADER, "player", target.getName(), "score", String.valueOf(score.getValue())),
                List.of(
                        messages.component(locale, Message.ALT_MAIN, "main", main),
                        messages.component(locale, Message.GUI_MATCHES, "count", String.valueOf(score.getMatches().size()))
                )));
        int slot = 10;
        if (score.getMatches().isEmpty()) {
            inventory.setItem(22, button(Material.GRAY_STAINED_GLASS_PANE, player, GuiKeys.IGNORE, Message.ALT_NONE));
        } else {
            for (AltMatch match : score.getMatches()) {
                if (slot >= 44) {
                    break;
                }
                if (slot % 9 == 8) {
                    slot += 2;
                }
                ItemStack head = plugin.skullFactory().simpleHead(
                        match.getPlayer().getUuid(),
                        match.getPlayer().getName(),
                        Component.text(match.getPlayer().getName(), NamedTextColor.YELLOW),
                        List.of(
                                Component.text("Score: " + match.getScore() + "/100", NamedTextColor.GOLD),
                                Component.text("IPs: " + match.getSharedIps(), NamedTextColor.GRAY)
                        )
                );
                GuiKeys.setAction(plugin, head, "player", match.getPlayer().getUuid().toString() + ":" + match.getPlayer().getName());
                inventory.setItem(slot, head);
                slot++;
            }
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
        if ("player".equals(action) && payload != null) {
            String[] parts = payload.split(":", 2);
            java.util.UUID uuid = java.util.UUID.fromString(parts[0]);
            String name = parts.length > 1 ? parts[1] : "?";
            new PunishMenu(plugin, PlayerRef.builder().uuid(uuid).name(name).build()).open(player);
        }
    }
}
