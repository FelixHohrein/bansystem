package net.baublase.bansystem.config;

import net.baublase.bansystem.domain.template.BanTemplate;
import net.baublase.bansystem.util.DurationParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ban-Vorlagen in banTemplate.yml, ohne Server-Neustart per Reload aktualisierbar.
 */
public final class BanTemplateYamlStore {

    private final File file;
    private YamlConfiguration yaml;
    private final Map<String, BanTemplate> templates = new LinkedHashMap<>();

    public BanTemplateYamlStore(File file) {
        this.file = file;
        reload();
    }

    public void reload() {
        this.yaml = YamlConfiguration.loadConfiguration(file);
        templates.clear();
        ConfigurationSection section = yaml.getConfigurationSection("templates");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) {
                continue;
            }
            String durationRaw = entry.getString("duration", "permanent");
            Duration duration = DurationParser.parse(durationRaw).orElse(Duration.ZERO);
            templates.put(id.toLowerCase(), BanTemplate.builder()
                    .id(id.toLowerCase())
                    .name(entry.getString("name", id))
                    .duration(duration)
                    .reason(entry.getString("reason", id))
                    .build());
        }
    }

    public List<BanTemplate> all() {
        return new ArrayList<>(templates.values());
    }

    public Optional<BanTemplate> find(String id) {
        return Optional.ofNullable(templates.get(id.toLowerCase()));
    }

    public void upsert(BanTemplate template) throws IOException {
        templates.put(template.getId().toLowerCase(), template);
        yaml.set("templates." + template.getId() + ".name", template.getName());
        yaml.set("templates." + template.getId() + ".duration", template.permanent() ? "permanent" : toRaw(template.getDuration()));
        yaml.set("templates." + template.getId() + ".reason", template.getReason());
        yaml.save(file);
    }

    public boolean delete(String id) throws IOException {
        BanTemplate removed = templates.remove(id.toLowerCase());
        if (removed == null) {
            return false;
        }
        yaml.set("templates." + id.toLowerCase(), null);
        yaml.save(file);
        return true;
    }

    private String toRaw(Duration duration) {
        if (duration.toDays() > 0 && duration.minusDays(duration.toDays()).isZero()) {
            return duration.toDays() + "d";
        }
        if (duration.toHours() > 0 && duration.minusHours(duration.toHours()).isZero()) {
            return duration.toHours() + "h";
        }
        return duration.toMinutes() + "m";
    }
}
