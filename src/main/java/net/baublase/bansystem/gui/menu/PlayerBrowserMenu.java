package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.alt.AltScore;
import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.SkullFactory;
import net.baublase.bansystem.i18n.Message;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerBrowserMenu extends GuiMenu {

    public enum Mode {
        ALL,
        BANNED
    }

    private static final int PAGE_SIZE = 45;

    private final Mode mode;
    private final int page;
    private final Map<Integer, PlayerRef> slotToPlayer = new HashMap<>();
    private int totalPages = 1;

    public PlayerBrowserMenu(BanSystemPlugin plugin, Mode mode) {
        this(plugin, mode, 0);
    }

    public PlayerBrowserMenu(BanSystemPlugin plugin, Mode mode, int page) {
        super(plugin);
        this.mode = mode;
        this.page = Math.max(0, page);
    }

    @Override
    public void open(Player player) {
        plugin.scheduler().supplyAsync(() -> loadHeads(player)).thenAccept(items -> plugin.scheduler().runSync(() -> {
            if (!player.isOnline()) {
                return;
            }
            Inventory inventory = create(player, 54, mode == Mode.ALL ? Message.GUI_ALL_PLAYERS : Message.GUI_BANNED_PLAYERS);
            for (int i = 0; i < items.size(); i++) {
                inventory.setItem(i, items.get(i));
            }
            inventory.setItem(45, item(Material.ARROW, player, Message.GUI_PREVIOUS, null));
            inventory.setItem(49, item(Material.PAPER, player, Message.GUI_PAGE,
                    "page", String.valueOf(page + 1),
                    "pages", String.valueOf(Math.max(1, totalPages))));
            inventory.setItem(53, item(Material.ARROW, player, Message.GUI_NEXT, null));
            inventory.setItem(48, item(Material.BARRIER, player, Message.GUI_BACK, null));
            player.openInventory(inventory);
        })).exceptionally(throwable -> {
            plugin.pluginLogger().error("GUI Spielerliste fehlgeschlagen", throwable);
            return null;
        });
    }

    private List<ItemStack> loadHeads(Player viewer) {
        List<PlayerRef> players;
        if (mode == Mode.BANNED) {
            players = plugin.banService().bannedPlayers();
        } else {
            players = new ArrayList<>();
            for (KnownPlayer known : plugin.banService().allKnown()) {
                players.add(PlayerRef.builder().uuid(known.getUuid()).name(known.getName()).build());
            }
        }
        totalPages = Math.max(1, (int) Math.ceil(players.size() / (double) PAGE_SIZE));
        int from = page * PAGE_SIZE;
        int to = Math.min(players.size(), from + PAGE_SIZE);
        List<ItemStack> items = new ArrayList<>();
        SkullFactory skulls = plugin.skullFactory();
        slotToPlayer.clear();
        int slot = 0;
        for (int i = from; i < to; i++) {
            PlayerRef ref = players.get(i);
            AltScore score = plugin.altCheckService().score(ref.getUuid());
            List<Ban> history = plugin.banService().history(ref.getUuid());
            boolean banned = plugin.banService().activeBan(ref.getUuid()).isPresent();
            slotToPlayer.put(slot, ref);
            items.add(skulls.head(ref, score, history, banned, locale(viewer)));
            slot++;
        }
        if (items.isEmpty()) {
            items.add(item(Material.GRAY_STAINED_GLASS_PANE, viewer, Message.GUI_EMPTY, null));
        }
        return items;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 48) {
            new MainMenu(plugin).open(player);
            return;
        }
        if (slot == 45 && page > 0) {
            new PlayerBrowserMenu(plugin, mode, page - 1).open(player);
            return;
        }
        if (slot == 53) {
            new PlayerBrowserMenu(plugin, mode, page + 1).open(player);
            return;
        }
        PlayerRef target = slotToPlayer.get(slot);
        if (target != null) {
            new PunishMenu(plugin, target).open(player);
        }
    }
}
