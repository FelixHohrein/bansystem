package net.baublase.bansystem.i18n;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MessageService {

    private final LocaleResolver localeResolver;
    private final YamlMessageCatalog catalog;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageService(LocaleResolver localeResolver, YamlMessageCatalog catalog) {
        this.localeResolver = localeResolver;
        this.catalog = catalog;
    }

    public void send(Audience audience, Message message, String... placeholders) {
        audience.sendMessage(prefixed(resolveLocale(audience), message, placeholders));
    }

    public void sendRaw(Audience audience, Message message, String... placeholders) {
        audience.sendMessage(component(resolveLocale(audience), message, placeholders));
    }

    public Component component(Locale locale, Message message, String... placeholders) {
        String raw = catalog.line(locale, message);
        raw = apply(raw, placeholders);
        raw = raw.replace("\\n", "\n");
        return miniMessage.deserialize(raw);
    }

    public Component prefixed(Locale locale, Message message, String... placeholders) {
        Component prefix = component(locale, Message.PREFIX);
        return prefix.append(component(locale, message, placeholders));
    }

    public String plain(Locale locale, Message message, String... placeholders) {
        return apply(catalog.line(locale, message), placeholders).replace("\\n", "\n");
    }

    public Locale resolveLocale(Audience audience) {
        if (audience instanceof Player player) {
            return localeResolver.resolve(player);
        }
        return Locale.GERMAN;
    }

    private String apply(String template, String... placeholders) {
        if (placeholders == null || placeholders.length == 0) {
            return template;
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            map.put(placeholders[i], placeholders[i + 1]);
        }
        String result = template;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }
}
