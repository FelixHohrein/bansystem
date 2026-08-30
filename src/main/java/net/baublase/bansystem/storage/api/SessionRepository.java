package net.baublase.bansystem.storage.api;

import net.baublase.bansystem.domain.player.PlayerSession;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SessionRepository {

    long insertJoin(PlayerSession session);

    void updateQuit(long sessionId, PlayerSession session);

    List<PlayerSession> findByUuid(UUID uuid);

    List<PlayerSession> findByIps(Set<String> ips);
}
