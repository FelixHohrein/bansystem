package net.baublase.bansystem.application;

import net.baublase.bansystem.domain.player.PlayerRef;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code bansystem.bypass} schützt nur Online-Spieler. Override: {@code bansystem.bypass.override}.
 */
public final class ImmunityService {

    public static final String BYPASS = "bansystem.bypass";
    public static final String OVERRIDE = "bansystem.bypass.override";

    public boolean isImmune(Player target, CommandSender staff) {
        if (target == null || !target.hasPermission(BYPASS)) {
            return false;
        }
        return staff == null || !staff.hasPermission(OVERRIDE);
    }

    public boolean isImmune(PlayerRef ignored, CommandSender staff, Player onlineTarget) {
        return isImmune(onlineTarget, staff);
    }
}
