package net.baublase.bansystem.gui;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.alt.AltScore;
import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.util.DurationFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SkullFactory {

    private final BanSystemPlugin plugin;

    public SkullFactory(BanSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack head(PlayerRef player, AltScore score, List<Ban> history, boolean banned, Locale locale) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(player.getUuid(), player.getName());
        meta.setOwnerProfile(profile);
        meta.displayName(Component.text(player.getName(), NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(plugin.messages().component(locale, banned ? Message.GUI_LORE_BANNED : Message.GUI_LORE_NOT_BANNED));
        lore.add(plugin.messages().component(locale, Message.GUI_LORE_SCORE, "score", String.valueOf(score.getValue())));
        if (score.getLikelyMain() != null) {
            lore.add(plugin.messages().component(locale, Message.GUI_LORE_MAIN, "main", score.getLikelyMain().getName()));
        } else {
            lore.add(plugin.messages().component(locale, Message.GUI_LORE_NO_MAIN));
        }
        lore.add(plugin.messages().component(locale, Message.GUI_LORE_HISTORY));
        if (history.isEmpty()) {
            lore.add(plugin.messages().component(locale, Message.GUI_LORE_HISTORY_EMPTY));
        } else {
            history.stream().limit(5).forEach(ban -> lore.add(Component.text(
                    "- " + ban.getType().name() + " | " + ban.getReason() + " | " + DurationFormatter.date(ban.getCreatedAt()),
                    NamedTextColor.GRAY
            )));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack simpleHead(UUID uuid, String name, Component display, List<Component> lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwnerProfile(Bukkit.createProfile(uuid, name));
        meta.displayName(display);
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
