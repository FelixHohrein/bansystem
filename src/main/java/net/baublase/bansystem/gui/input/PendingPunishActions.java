package net.baublase.bansystem.gui.input;

import net.baublase.bansystem.BanSystemPlugin;
import net.baublase.bansystem.domain.player.PlayerRef;
import net.baublase.bansystem.domain.template.BanTemplate;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingPunishActions {

    public enum Step {
        REASON_PERMANENT,
        DURATION_TEMPORARY,
        REASON_TEMPORARY,
        TEMPLATE_CONFIRM
    }

    public record Pending(PlayerRef target, Step step, Duration duration, BanTemplate template) {
    }

    private final BanSystemPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public PendingPunishActions(BanSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public void put(Player staff, Pending value) {
        pending.put(staff.getUniqueId(), value);
    }

    public Pending get(UUID staff) {
        return pending.get(staff);
    }

    public Pending remove(UUID staff) {
        return pending.remove(staff);
    }

    public boolean has(UUID staff) {
        return pending.containsKey(staff);
    }
}
