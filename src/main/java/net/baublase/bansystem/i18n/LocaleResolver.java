package net.baublase.bansystem.i18n;

import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * EN wenn die Client-Sprache mit en beginnt, sonst DE.
 */
public final class LocaleResolver {

    public Locale resolve(Player player) {
        Locale locale = player.locale();
        if (locale != null && "en".equalsIgnoreCase(locale.getLanguage())) {
            return Locale.ENGLISH;
        }
        return Locale.GERMAN;
    }
}
