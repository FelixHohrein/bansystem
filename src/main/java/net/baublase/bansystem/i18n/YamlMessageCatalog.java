package net.baublase.bansystem.i18n;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lädt MiniMessage-Texte aus lang/de.yml und lang/en.yml. Fallback ist immer Deutsch.
 */
public final class YamlMessageCatalog {

    private final File langFolder;
    private final Map<Locale, Map<Message, String>> lines = new HashMap<>();

    public YamlMessageCatalog(File langFolder) {
        this.langFolder = langFolder;
        reload();
    }

    public void reload() {
        lines.clear();
        load(Locale.GERMAN, new File(langFolder, "de.yml"));
        load(Locale.ENGLISH, new File(langFolder, "en.yml"));
    }

    public String line(Locale locale, Message message) {
        Map<Message, String> localized = lines.getOrDefault(normalize(locale), lines.get(Locale.GERMAN));
        if (localized == null) {
            return message.yamlKey();
        }
        String value = localized.get(message);
        if (value != null) {
            return value;
        }
        Map<Message, String> fallback = lines.get(Locale.GERMAN);
        if (fallback != null && fallback.get(message) != null) {
            return fallback.get(message);
        }
        return message.yamlKey();
    }

    private void load(Locale locale, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<Message, String> map = new EnumMap<>(Message.class);
        for (Message message : Message.values()) {
            String value = yaml.getString(message.yamlKey());
            if (value != null) {
                map.put(message, value);
            }
        }
        lines.put(locale, map);
    }

    private Locale normalize(Locale locale) {
        if (locale != null && "en".equalsIgnoreCase(locale.getLanguage())) {
            return Locale.ENGLISH;
        }
        return Locale.GERMAN;
    }
}
