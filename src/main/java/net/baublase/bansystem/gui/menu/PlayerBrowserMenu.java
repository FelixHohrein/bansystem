package net.baublase.bansystem.gui.menu;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.alt.AltScore;
import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.gui.GuiItems;
import net.baublase.bansystem.gui.GuiKeys;
import net.baublase.bansystem.gui.GuiMenu;
import net.baublase.bansystem.gui.GuiSounds;
import net.baublase.bansystem.gui.SkullFactory;
import net.baublase.bansystem.i18n.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Paginiertes Kopf-Menü: Online zuerst, Lore mit Score/Main/Historie.
 */
public final class PlayerBrowserMenu extends GuiMenu {

    public enum Mode {
        ALL,
        BANNED
    }

    private static final int PAGE_SIZE = 28;

    private final Mode mode;
    private final int page;
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
        player.sendActionBar(messages.component(locale(player), Message.GUI_LOADING));
        plugin.scheduler().supplyAsync(() -> loadHeads(player)).thenAccept(items -> plugin.scheduler().runSync(() -> {
            if (!player.isOnline()) {
                return;
            }
            Inventory inventory = create(player, 54, mode == Mode.ALL ? Message.GUI_ALL_PLAYERS : Message.GUI_BANNED_PLAYERS);
            int[] content = contentSlots();
            for (int i = 0; i < items.size() && i < content.length; i++) {
                inventory.setItem(content[i], items.get(i));
            }
            inventory.setItem(45, button(Material.ARROW, player, GuiKeys.PREV, Message.GUI_PREVIOUS));
            inventory.setItem(49, button(Material.PAPER, player, GuiKeys.IGNORE, Message.GUI_PAGE,
                    "page", String.valueOf(page + 1),
                    "pages", String.valueOf(Math.max(1, totalPages))));
            inventory.setItem(53, button(Material.ARROW, player, GuiKeys.NEXT, Message.GUI_NEXT));
            inventory.setItem(48, button(Material.ARROW, player, GuiKeys.BACK, Message.GUI_BACK));
            GuiSounds.open(player);
            player.openInventory(inventory);
        })).exceptionally(throwable -> {
            plugin.pluginLogger().error("GUI Spielerliste fehlgeschlagen", throwable);
            return null;
        });
    }

    /**
     * Innere Slots ohne Rahmen (7×4).
     */
    private int[] contentSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private List<ItemStack> loadHeads(Player viewer) {
        List<PlayerRef> players = resolvePlayers();
        players.sort(Comparator
                .comparing((PlayerRef ref) -> Bukkit.getPlayer(ref.getUuid()) == null)
                .thenComparing(ref -> ref.getName().toLowerCase()));
        totalPages = Math.max(1, (int) Math.ceil(players.size() / (double) PAGE_SIZE));
        int from = Math.min(page * PAGE_SIZE, players.size());
        int to = Math.min(players.size(), from + PAGE_SIZE);
        List<ItemStack> items = new ArrayList<>();
        SkullFactory skulls = plugin.skullFactory();
        for (int i = from; i < to; i++) {
            PlayerRef ref = players.get(i);
            AltScore score = plugin.altCheckService().score(ref.getUuid());
            List<Ban> history = plugin.banService().history(ref.getUuid());
            boolean banned = plugin.banService().activeBan(ref.getUuid()).isPresent();
            ItemStack head = skulls.head(ref, score, history, banned, locale(viewer));
            GuiKeys.setAction(plugin, head, "player", ref.getUuid().toString());
            if (banned) {
                GuiItems.glow(head);
            }
            items.add(head);
        }
        if (items.isEmpty()) {
            items.add(button(Material.GRAY_STAINED_GLASS_PANE, viewer, GuiKeys.IGNORE, Message.GUI_EMPTY));
        }
        return items;
    }

    private List<PlayerRef> resolvePlayers() {
        if (mode == Mode.BANNED) {
            return new ArrayList<>(plugin.banService().bannedPlayers());
        }
        List<PlayerRef> players = new ArrayList<>();
        for (KnownPlayer known : plugin.banService().allKnown()) {
            players.add(PlayerRef.builder().uuid(known.getUuid()).name(known.getName()).build());
        }
        return players;
    }

    @Override
    public void onAction(Player player, String action, String payload, InventoryClickEvent event) {
        switch (action) {
            case GuiKeys.BACK -> new MainMenu(plugin).open(player);
            case GuiKeys.PREV -> {
                if (page > 0) {
                    GuiSounds.page(player);
                    new PlayerBrowserMenu(plugin, mode, page - 1).open(player);
                } else {
                    GuiSounds.deny(player);
                }
            }
            case GuiKeys.NEXT -> {
                if (page + 1 < totalPages) {
                    GuiSounds.page(player);
                    new PlayerBrowserMenu(plugin, mode, page + 1).open(player);
                } else {
                    GuiSounds.deny(player);
                }
            }
            case "player" -> {
                if (payload == null) {
                    return;
                }
                UUID uuid = UUID.fromString(payload);
                Player online = Bukkit.getPlayer(uuid);
                String name = online != null ? online.getName() : plugin.banService().known(uuid).map(KnownPlayer::getName).orElse("?");
                new PunishMenu(plugin, PlayerRef.builder().uuid(uuid).name(name).build()).open(player);
            }
            default -> {
            }
        }
    }
}
