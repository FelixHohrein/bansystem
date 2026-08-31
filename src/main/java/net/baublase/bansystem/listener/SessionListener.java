package net.baublase.bansystem.listener;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.application.SessionTracker;
import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.domain.player.PlayerSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Instant;

/**
 * Speichert Join/Quit für Alt-Check (IP, Chunk, Locale, Client-Brand).
 */
public final class SessionListener implements Listener {

    private final BanSystemPlugin plugin;

    public SessionListener(BanSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.storage().isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        KnownPlayer known = snapshot(player, Instant.now());
        PlayerSession session = session(player, Instant.now(), null);
        plugin.scheduler().runAsync(() -> {
            plugin.sessionTracker().onJoin(known, session);
            plugin.banService().remember(known.getUuid(), known.getName());
            plugin.locationTracker().record(
                    known.getUuid(),
                    known.getLastWorld(),
                    known.getLastChunkX(),
                    known.getLastChunkZ()
            );
        });
        // Client-Brand kommt oft erst nach dem Join — einmal nachziehen.
        plugin.scheduler().runLater(() -> {
            if (!player.isOnline() || !plugin.storage().isEnabled()) {
                return;
            }
            KnownPlayer updated = snapshot(player, Instant.now());
            plugin.scheduler().runAsync(() -> plugin.storage().getPlayers().upsert(updated));
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.pendingActions().remove(event.getPlayer().getUniqueId());
        if (!plugin.storage().isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        Instant now = Instant.now();
        KnownPlayer known = snapshot(player, now);
        PlayerSession session = session(player, now, now);
        plugin.scheduler().runAsync(() -> {
            plugin.sessionTracker().onQuit(known, session);
            plugin.locationTracker().record(
                    known.getUuid(),
                    known.getLastWorld(),
                    known.getLastChunkX(),
                    known.getLastChunkZ()
            );
        });
    }

    private KnownPlayer snapshot(Player player, Instant now) {
        return SessionTracker.fromJoin(
                player.getUniqueId(),
                player.getName(),
                player.locale().toString(),
                brandOf(player),
                player.getWorld().getName(),
                player.getLocation().getBlockX() >> 4,
                player.getLocation().getBlockZ() >> 4
        ).toBuilder()
                .lastSeen(now)
                .build();
    }

    private PlayerSession session(Player player, Instant joinedOrNow, Instant quitAt) {
        String ip = player.getAddress() == null ? "unknown" : player.getAddress().getAddress().getHostAddress();
        return PlayerSession.builder()
                .uuid(player.getUniqueId())
                .ip(ip)
                .joinedAt(joinedOrNow)
                .quitAt(quitAt)
                .world(player.getWorld().getName())
                .chunkX(player.getLocation().getBlockX() >> 4)
                .chunkZ(player.getLocation().getBlockZ() >> 4)
                .locale(player.locale().toString())
                .clientBrand(brandOf(player))
                .build();
    }

    private String brandOf(Player player) {
        String brand = player.getClientBrandName();
        return brand == null || brand.isBlank() ? "unknown" : brand;
    }
}
