package net.baublase.bansystem.application;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.bukkit.KickBanScreen;
import net.baublase.bansystem.domain.ban.Ban;
import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.domain.template.BanTemplate;
import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.i18n.MessageService;
import net.baublase.bansystem.storage.Storage;
import net.baublase.bansystem.util.DurationFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Führt Ban/Unban aus, kickt Online-Spieler und prüft Immunität.
 */
public final class PunishExecutor {

    private final BanSystemPlugin plugin;
    private final Storage storage;
    private final BanService banService;
    private final ImmunityService immunityService;
    private final KickBanScreen kickBanScreen;
    private final MessageService messages;

    public PunishExecutor(
            BanSystemPlugin plugin,
            Storage storage,
            BanService banService,
            ImmunityService immunityService,
            KickBanScreen kickBanScreen,
            MessageService messages
    ) {
        this.plugin = plugin;
        this.storage = storage;
        this.banService = banService;
        this.immunityService = immunityService;
        this.kickBanScreen = kickBanScreen;
        this.messages = messages;
    }

    public boolean ensureDatabase(CommandSender sender) {
        if (storage.isEnabled()) {
            return true;
        }
        messages.send(sender, Message.ERROR_DATABASE_DISABLED);
        return false;
    }

    public Optional<PlayerRef> lookupKnown(String name) {
        Optional<KnownPlayer> known = banService.known(name);
        if (known.isEmpty()) {
            return Optional.empty();
        }
        KnownPlayer player = known.get();
        return Optional.of(PlayerRef.builder().uuid(player.getUuid()).name(player.getName()).build());
    }

    public Optional<PlayerRef> requireKnown(CommandSender sender, String name) {
        Optional<PlayerRef> found = lookupKnown(name);
        if (found.isEmpty()) {
            messages.send(sender, Message.ERROR_UNKNOWN_PLAYER, "player", name);
        }
        return found;
    }

    /**
     * Immunität gilt nur für online Spieler mit {@code bansystem.bypass}.
     * Offline-Rechte (LuckPerms) prüfen wir bewusst nicht.
     */
    public boolean isImmune(CommandSender staff, Player onlineTarget) {
        return immunityService.isImmune(onlineTarget, staff);
    }

    public void banPermanent(CommandSender staff, PlayerRef target, String reason, String templateId) {
        if (!ensureDatabase(staff) || !canPunish(staff, target)) {
            return;
        }
        if (reason == null || reason.isBlank()) {
            messages.send(staff, Message.ERROR_REASON_REQUIRED);
            return;
        }
        plugin.scheduler().supplyAsync(() -> {
            Ban ban = banService.banPermanent(target, staffRef(staff), reason.trim(), templateId);
            return ban;
        }).thenAccept(ban -> plugin.scheduler().runSync(() -> {
            kickIfOnline(target, ban);
            messages.send(staff, Message.BAN_PERMANENT, "player", target.getName(), "reason", reason.trim());
        })).exceptionally(throwable -> {
            plugin.pluginLogger().error("Permanent-Ban fehlgeschlagen", throwable);
            return null;
        });
    }

    public void banTemporary(CommandSender staff, PlayerRef target, String reason, Duration duration, String templateId) {
        if (!ensureDatabase(staff) || !canPunish(staff, target)) {
            return;
        }
        if (reason == null || reason.isBlank()) {
            messages.send(staff, Message.ERROR_REASON_REQUIRED);
            return;
        }
        plugin.scheduler().supplyAsync(() -> banService.banTemporary(target, staffRef(staff), reason.trim(), duration, templateId))
                .thenAccept(ban -> plugin.scheduler().runSync(() -> {
                    kickIfOnline(target, ban);
                    messages.send(staff, Message.BAN_TEMPORARY,
                            "player", target.getName(),
                            "duration", DurationFormatter.format(duration, messages, messages.resolveLocale(staff instanceof Player player ? player : null)),
                            "reason", reason.trim());
                })).exceptionally(throwable -> {
                    plugin.pluginLogger().error("Temp-Ban fehlgeschlagen", throwable);
                    return null;
                });
    }

    public void applyTemplate(CommandSender staff, PlayerRef target, BanTemplate template) {
        if (template.permanent()) {
            banPermanent(staff, target, template.getReason(), template.getId());
        } else {
            banTemporary(staff, target, template.getReason(), template.getDuration(), template.getId());
        }
    }

    public void unban(CommandSender staff, PlayerRef target) {
        if (!ensureDatabase(staff)) {
            return;
        }
        plugin.scheduler().supplyAsync(() -> {
            if (banService.activeBan(target.getUuid()).isEmpty()) {
                return false;
            }
            banService.unban(target.getUuid());
            return true;
        }).thenAccept(found -> plugin.scheduler().runSync(() -> {
            if (!found) {
                messages.send(staff, Message.ERROR_NOT_BANNED, "player", target.getName());
                return;
            }
            messages.send(staff, Message.UNBAN, "player", target.getName());
        })).exceptionally(throwable -> {
            plugin.pluginLogger().error("Unban fehlgeschlagen", throwable);
            return null;
        });
    }

    private boolean canPunish(CommandSender staff, PlayerRef target) {
        Player online = Bukkit.getPlayer(target.getUuid());
        if (immunityService.isImmune(online, staff)) {
            messages.send(staff, Message.ERROR_IMMUNE, "player", target.getName());
            return false;
        }
        return true;
    }

    private void kickIfOnline(PlayerRef target, Ban ban) {
        Player online = Bukkit.getPlayer(target.getUuid());
        if (online != null) {
            kickBanScreen.kick(online, ban);
        }
    }

    private PlayerRef staffRef(CommandSender staff) {
        if (staff instanceof Player player) {
            return PlayerRef.builder().uuid(player.getUniqueId()).name(player.getName()).build();
        }
        return PlayerRef.builder().uuid(new UUID(0, 0)).name("Console").build();
    }
}
