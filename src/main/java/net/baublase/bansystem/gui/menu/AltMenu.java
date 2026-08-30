package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.alt.AltMatch;
import net.baublase.bansystem.domain.alt.AltScore;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.i18n.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AltMenu extends GuiMenu {

    private final PlayerRef target;
    private final Map<Integer, PlayerRef> slotToPlayer = new HashMap<>();

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
                List.of(messages.component(locale, Message.ALT_MAIN, "main", main))));
        int slot = 9;
        slotToPlayer.clear();
        if (score.getMatches().isEmpty()) {
            inventory.setItem(22, item(Material.GRAY_STAINED_GLASS_PANE, player, Message.ALT_NONE, null));
        } else {
            for (AltMatch match : score.getMatches()) {
                if (slot >= 45) {
                    break;
                }
                inventory.setItem(slot, plugin.skullFactory().simpleHead(
                        match.getPlayer().getUuid(),
                        match.getPlayer().getName(),
                        Component.text(match.getPlayer().getName(), NamedTextColor.YELLOW),
                        List.of(
                                Component.text("Score: " + match.getScore(), NamedTextColor.GOLD),
                                Component.text("IPs: " + match.getSharedIps(), NamedTextColor.GRAY)
                        )
                ));
                slotToPlayer.put(slot, match.getPlayer());
                slot++;
            }
        }
        inventory.setItem(49, item(Material.BARRIER, player, Message.GUI_BACK, null));
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() == 49) {
            new PunishMenu(plugin, target).open(player);
            return;
        }
        PlayerRef other = slotToPlayer.get(event.getRawSlot());
        if (other != null) {
            new PunishMenu(plugin, other).open(player);
        }
    }
}
