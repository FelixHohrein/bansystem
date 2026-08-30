package net.baublase.bansystem.application;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.bukkit.PublicAreaRegistry;
import net.baublase.bansystem.domain.alt.AltMatch;
import net.baublase.bansystem.domain.alt.AltScore;
import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.domain.player.PlayerSession;
import net.baublase.bansystem.storage.Storage;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Alt-Score 0–100 aus Vanilla-Signalen (IP, Sessions, Login-Stunden, private Chunks, Locale+Brand).
 */
public final class AltCheckService {

    private final BanSystemPlugin plugin;
    private final Storage storage;
    private final PublicAreaRegistry publicAreas;

    public AltCheckService(BanSystemPlugin plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.publicAreas = new PublicAreaRegistry(plugin);
    }

    public AltScore score(UUID targetUuid) {
        KnownPlayer target = storage.getPlayers().findByUuid(targetUuid).orElse(null);
        if (target == null) {
            return AltScore.builder()
                    .target(PlayerRef.builder().uuid(targetUuid).name("unknown").build())
                    .value(0)
                    .matches(List.of())
                    .build();
        }
        List<PlayerSession> ownSessions = storage.getSessions().findByUuid(targetUuid);
        Set<String> ips = ownSessions.stream().map(PlayerSession::getIp).filter(Objects::nonNull).collect(Collectors.toSet());
        List<PlayerSession> related = storage.getSessions().findByIps(ips);
        Map<UUID, List<PlayerSession>> byPlayer = new HashMap<>();
        for (PlayerSession session : related) {
            if (session.getUuid().equals(targetUuid)) {
                continue;
            }
            byPlayer.computeIfAbsent(session.getUuid(), ignored -> new ArrayList<>()).add(session);
        }
        List<AltMatch> matches = new ArrayList<>();
        int selfScore = 0;
        for (Map.Entry<UUID, List<PlayerSession>> entry : byPlayer.entrySet()) {
            KnownPlayer other = storage.getPlayers().findByUuid(entry.getKey()).orElse(null);
            if (other == null) {
                continue;
            }
            int value = compute(target, ownSessions, other, entry.getValue());
            if (value <= 0) {
                continue;
            }
            selfScore = Math.max(selfScore, value);
            Set<String> shared = new HashSet<>(ips);
            shared.retainAll(entry.getValue().stream().map(PlayerSession::getIp).collect(Collectors.toSet()));
            matches.add(AltMatch.builder()
                    .player(PlayerRef.builder().uuid(other.getUuid()).name(other.getName()).build())
                    .score(Math.min(100, value))
                    .sharedIps(String.join(", ", shared))
                    .build());
        }
        matches.sort(Comparator.comparingInt(AltMatch::getScore).reversed());
        PlayerRef likelyMain = likelyMain(target, matches);
        return AltScore.builder()
                .target(PlayerRef.builder().uuid(target.getUuid()).name(target.getName()).build())
                .value(Math.min(100, selfScore))
                .likelyMain(likelyMain)
                .matches(matches)
                .build();
    }

    private PlayerRef likelyMain(KnownPlayer target, List<AltMatch> matches) {
        KnownPlayer oldest = target;
        for (AltMatch match : matches) {
            if (match.getScore() < 20) {
                continue;
            }
            KnownPlayer other = storage.getPlayers().findByUuid(match.getPlayer().getUuid()).orElse(null);
            if (other != null && other.getFirstSeen().isBefore(oldest.getFirstSeen())) {
                oldest = other;
            }
        }
        return PlayerRef.builder().uuid(oldest.getUuid()).name(oldest.getName()).build();
    }

    private int compute(KnownPlayer target, List<PlayerSession> own, KnownPlayer other, List<PlayerSession> theirs) {
        int score = 0;
        String ownLastIp = lastIp(own);
        String otherLastIp = lastIp(theirs);
        boolean currentIp = ownLastIp != null && ownLastIp.equals(otherLastIp);
        if (currentIp) {
            score += plugin.configuration().getAltScoreWeights().getSameIpCurrent();
        } else if (sharesIp(own, theirs)) {
            score += plugin.configuration().getAltScoreWeights().getSameIpHistory();
        }
        if (!overlaps(own, theirs)) {
            score += plugin.configuration().getAltScoreWeights().getNeverOnlineTogether();
        }
        if (similarHours(own, theirs)) {
            score += plugin.configuration().getAltScoreWeights().getSimilarLoginHours();
        }
        if (samePrivateChunk(target, other)) {
            score += plugin.configuration().getAltScoreWeights().getSameChunk();
        }
        if (target.getLocale() != null && target.getLocale().equalsIgnoreCase(other.getLocale())
                && target.getClientBrand() != null && target.getClientBrand().equalsIgnoreCase(other.getClientBrand())) {
            score += plugin.configuration().getAltScoreWeights().getSameLocaleAndBrand();
        }
        return score;
    }

    private boolean samePrivateChunk(KnownPlayer target, KnownPlayer other) {
        if (target.getLastWorld() == null || other.getLastWorld() == null) {
            return false;
        }
        if (!target.getLastWorld().equals(other.getLastWorld())) {
            return false;
        }
        if (!Objects.equals(target.getLastChunkX(), other.getLastChunkX()) || !Objects.equals(target.getLastChunkZ(), other.getLastChunkZ())) {
            return false;
        }
        return !publicAreas.isExcluded(target.getLastWorld(), target.getLastChunkX(), target.getLastChunkZ());
    }

    private String lastIp(List<PlayerSession> sessions) {
        return sessions.stream()
                .max(Comparator.comparing(PlayerSession::getJoinedAt))
                .map(PlayerSession::getIp)
                .orElse(null);
    }

    private boolean sharesIp(List<PlayerSession> own, List<PlayerSession> theirs) {
        Set<String> ips = own.stream().map(PlayerSession::getIp).collect(Collectors.toSet());
        return theirs.stream().map(PlayerSession::getIp).anyMatch(ips::contains);
    }

    private boolean overlaps(List<PlayerSession> own, List<PlayerSession> theirs) {
        for (PlayerSession a : own) {
            Instant aEnd = a.getQuitAt() == null ? Instant.now() : a.getQuitAt();
            for (PlayerSession b : theirs) {
                Instant bEnd = b.getQuitAt() == null ? Instant.now() : b.getQuitAt();
                boolean separate = aEnd.isBefore(b.getJoinedAt()) || bEnd.isBefore(a.getJoinedAt());
                if (!separate) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean similarHours(List<PlayerSession> own, List<PlayerSession> theirs) {
        int ownPeak = peakHour(own);
        int otherPeak = peakHour(theirs);
        if (ownPeak < 0 || otherPeak < 0) {
            return false;
        }
        int diff = Math.abs(ownPeak - otherPeak);
        return Math.min(diff, 24 - diff) <= 1;
    }

    private int peakHour(List<PlayerSession> sessions) {
        if (sessions.isEmpty()) {
            return -1;
        }
        int[] hours = new int[24];
        for (PlayerSession session : sessions) {
            hours[session.getJoinedAt().atZone(ZoneId.systemDefault()).getHour()]++;
        }
        int peak = 0;
        for (int i = 1; i < 24; i++) {
            if (hours[i] > hours[peak]) {
                peak = i;
            }
        }
        return hours[peak] == 0 ? -1 : peak;
    }
}
