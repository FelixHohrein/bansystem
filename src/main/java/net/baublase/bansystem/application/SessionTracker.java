package net.baublase.bansystem.application;

import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.domain.player.PlayerSession;
import net.baublase.bansystem.logging.PluginLogger;
import net.baublase.bansystem.storage.Storage;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionTracker {

    private final Storage storage;
    private final PluginLogger logger;
    private final Map<UUID, Long> openSessions = new ConcurrentHashMap<>();

    public SessionTracker(Storage storage, PluginLogger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public void onJoin(KnownPlayer player, PlayerSession session) {
        storage.getPlayers().upsert(player);
        long id = storage.getSessions().insertJoin(session);
        openSessions.put(player.getUuid(), id);
        logger.debug("Session start " + player.getName() + " ip=" + session.getIp());
    }

    public void onQuit(KnownPlayer player, PlayerSession session) {
        storage.getPlayers().upsert(player);
        Long id = openSessions.remove(player.getUuid());
        if (id != null) {
            storage.getSessions().updateQuit(id, session);
        }
        logger.debug("Session end " + player.getName());
    }

    public static KnownPlayer fromJoin(UUID uuid, String name, String locale, String brand, String world, Integer chunkX, Integer chunkZ) {
        Instant now = Instant.now();
        return KnownPlayer.builder()
                .uuid(uuid)
                .name(name)
                .firstSeen(now)
                .lastSeen(now)
                .locale(locale)
                .clientBrand(brand)
                .lastWorld(world)
                .lastChunkX(chunkX)
                .lastChunkZ(chunkZ)
                .build();
    }
}
