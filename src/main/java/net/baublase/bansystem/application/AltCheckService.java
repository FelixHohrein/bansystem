package net.baublase.bansystem.application;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.bukkit.PublicAreaRegistry;
import net.baublase.bansystem.config.PluginConfiguration;
import net.baublase.bansystem.domain.alt.AltMatch;
import net.baublase.bansystem.domain.alt.AltScore;
import net.baublase.bansystem.domain.player.KnownPlayer;
import net.baublase.bansystem.domain.player.PlayerLocation;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.domain.player.PlayerSession;
import net.baublase.bansystem.storage.Storage;

import java.time.Instant;
import java.time.LocalDate;
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
 * Alt-Score 0–100, berechnet immer live aus der Historie.
 * Punkte wachsen, wenn sich Signale über mehrere Tage wiederholen (gleiche IP, gleiche private Chunks, ähnliche Login-Stunden).
 */
public final class AltCheckService {

    private static final ZoneId ZONE = ZoneId.systemDefault();

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
        List<PlayerLocation> ownLocations = storage.getLocations().findByUuid(targetUuid);
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
            List<PlayerLocation> otherLocations = storage.getLocations().findByUuid(entry.getKey());
            int value = compute(target, ownSessions, ownLocations, other, entry.getValue(), otherLocations);
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

    private int compute(
            KnownPlayer target,
            List<PlayerSession> own,
            List<PlayerLocation> ownLocations,
            KnownPlayer other,
            List<PlayerSession> theirs,
            List<PlayerLocation> otherLocations
    ) {
        PluginConfiguration.AltScoreWeights weights = plugin.configuration().getAltScoreWeights();
        int score = 0;
        String ownLastIp = lastIp(own);
        String otherLastIp = lastIp(theirs);
        if (ownLastIp != null && ownLastIp.equals(otherLastIp)) {
            score += weights.getSameIpCurrent();
        }
        int sharedIpDays = sharedIpDays(own, theirs);
        int sharedIps = sharedIpCount(own, theirs);
        if (sharedIpDays > 0) {
            double dayFactor = Math.min(1.0, sharedIpDays / 5.0);
            double ipFactor = Math.min(1.0, sharedIps / 3.0);
            score += (int) Math.round(weights.getSameIpHistory() * Math.max(dayFactor, ipFactor * 0.6));
        }
        if (!overlaps(own, theirs)) {
            score += weights.getNeverOnlineTogether();
        }
        int similarHourDays = similarLoginDays(own, theirs);
        if (similarHourDays > 0) {
            score += (int) Math.round(weights.getSimilarLoginHours() * Math.min(1.0, similarHourDays / 4.0));
        }
        int locationDays = sharedPrivateLocationDays(ownLocations, otherLocations);
        int uniqueChunks = sharedPrivateChunks(ownLocations, otherLocations);
        if (locationDays > 0 || uniqueChunks > 0) {
            double dayFactor = Math.min(1.0, locationDays / 4.0);
            double chunkFactor = Math.min(1.0, uniqueChunks / 3.0);
            score += (int) Math.round(weights.getSameChunk() * Math.max(dayFactor, chunkFactor));
        } else if (samePrivateChunk(target, other)) {
            score += Math.max(1, weights.getSameChunk() / 3);
        }
        if (target.getLocale() != null && target.getLocale().equalsIgnoreCase(other.getLocale())
                && target.getClientBrand() != null && target.getClientBrand().equalsIgnoreCase(other.getClientBrand())) {
            score += weights.getSameLocaleAndBrand();
        }
        return Math.min(100, score);
    }

    private int sharedIpDays(List<PlayerSession> own, List<PlayerSession> theirs) {
        Map<LocalDate, Set<String>> ownDays = ipsByDay(own);
        Map<LocalDate, Set<String>> theirDays = ipsByDay(theirs);
        int days = 0;
        for (Map.Entry<LocalDate, Set<String>> entry : ownDays.entrySet()) {
            Set<String> otherIps = theirDays.get(entry.getKey());
            if (otherIps == null) {
                continue;
            }
            for (String ip : entry.getValue()) {
                if (otherIps.contains(ip)) {
                    days++;
                    break;
                }
            }
        }
        return days;
    }

    private int sharedIpCount(List<PlayerSession> own, List<PlayerSession> theirs) {
        Set<String> ownIps = own.stream().map(PlayerSession::getIp).filter(Objects::nonNull).collect(Collectors.toSet());
        ownIps.retainAll(theirs.stream().map(PlayerSession::getIp).filter(Objects::nonNull).collect(Collectors.toSet()));
        return ownIps.size();
    }

    private Map<LocalDate, Set<String>> ipsByDay(List<PlayerSession> sessions) {
        Map<LocalDate, Set<String>> byDay = new HashMap<>();
        for (PlayerSession session : sessions) {
            if (session.getIp() == null) {
                continue;
            }
            LocalDate start = session.getJoinedAt().atZone(ZONE).toLocalDate();
            LocalDate end = (session.getQuitAt() == null ? Instant.now() : session.getQuitAt()).atZone(ZONE).toLocalDate();
            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
                byDay.computeIfAbsent(day, ignored -> new HashSet<>()).add(session.getIp());
            }
        }
        return byDay;
    }

    private int similarLoginDays(List<PlayerSession> own, List<PlayerSession> theirs) {
        Map<LocalDate, Integer> ownPeaks = peakHourByDay(own);
        Map<LocalDate, Integer> theirPeaks = peakHourByDay(theirs);
        int days = 0;
        for (Map.Entry<LocalDate, Integer> entry : ownPeaks.entrySet()) {
            Integer other = theirPeaks.get(entry.getKey());
            if (other == null) {
                continue;
            }
            int diff = Math.abs(entry.getValue() - other);
            if (Math.min(diff, 24 - diff) <= 1) {
                days++;
            }
        }
        return days;
    }

    private Map<LocalDate, Integer> peakHourByDay(List<PlayerSession> sessions) {
        Map<LocalDate, int[]> hours = new HashMap<>();
        for (PlayerSession session : sessions) {
            LocalDate day = session.getJoinedAt().atZone(ZONE).toLocalDate();
            hours.computeIfAbsent(day, ignored -> new int[24])[session.getJoinedAt().atZone(ZONE).getHour()]++;
        }
        Map<LocalDate, Integer> peaks = new HashMap<>();
        for (Map.Entry<LocalDate, int[]> entry : hours.entrySet()) {
            int[] counts = entry.getValue();
            int peak = 0;
            for (int i = 1; i < 24; i++) {
                if (counts[i] > counts[peak]) {
                    peak = i;
                }
            }
            if (counts[peak] > 0) {
                peaks.put(entry.getKey(), peak);
            }
        }
        return peaks;
    }

    private int sharedPrivateLocationDays(List<PlayerLocation> own, List<PlayerLocation> theirs) {
        Set<String> theirKeys = new HashSet<>();
        for (PlayerLocation location : theirs) {
            if (!publicAreas.isExcluded(location.getWorld(), location.getChunkX(), location.getChunkZ())) {
                theirKeys.add(dayChunkKey(location));
            }
        }
        Set<LocalDate> days = new HashSet<>();
        for (PlayerLocation location : own) {
            if (publicAreas.isExcluded(location.getWorld(), location.getChunkX(), location.getChunkZ())) {
                continue;
            }
            if (theirKeys.contains(dayChunkKey(location))) {
                days.add(location.getSeenOn());
            }
        }
        return days.size();
    }

    private int sharedPrivateChunks(List<PlayerLocation> own, List<PlayerLocation> theirs) {
        Set<String> theirChunks = new HashSet<>();
        for (PlayerLocation location : theirs) {
            if (!publicAreas.isExcluded(location.getWorld(), location.getChunkX(), location.getChunkZ())) {
                theirChunks.add(chunkKey(location));
            }
        }
        Set<String> shared = new HashSet<>();
        for (PlayerLocation location : own) {
            if (publicAreas.isExcluded(location.getWorld(), location.getChunkX(), location.getChunkZ())) {
                continue;
            }
            String key = chunkKey(location);
            if (theirChunks.contains(key)) {
                shared.add(key);
            }
        }
        return shared.size();
    }

    private String dayChunkKey(PlayerLocation location) {
        return location.getSeenOn() + "|" + chunkKey(location);
    }

    private String chunkKey(PlayerLocation location) {
        return location.getWorld() + ":" + location.getChunkX() + ":" + location.getChunkZ();
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
}
