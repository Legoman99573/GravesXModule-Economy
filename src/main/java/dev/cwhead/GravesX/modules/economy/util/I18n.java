package dev.cwhead.GravesX.modules.economy.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internationalization manager for GravesX modules.
 */
public final class I18n {

    private final JavaPlugin plugin;
    private final String defaultLanguage;
    private final Map<String, Map<String, String>> translations = new HashMap<>();

    public I18n(JavaPlugin plugin, String defaultLanguage) {
        this.plugin = plugin;
        this.defaultLanguage = (defaultLanguage == null ? "en_us" : defaultLanguage).toLowerCase();
        loadLanguages();
    }

    public void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "languages");
        if (!langFolder.exists()) langFolder.mkdirs();

        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        translations.clear();

        for (File f : files) {
            String localeKey = f.getName().replace(".yml", "").toLowerCase();
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);

            Map<String, String> flat = new HashMap<>();
            flattenSection("", cfg, flat);
            translations.put(localeKey, flat);
        }
    }

    /**
     * Recursively flattens a nested config into key->value strings.
     * Example: "graves.economy.teleport.charged"
     */
    private void flattenSection(String prefix, ConfigurationSection section, Map<String, String> out) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (section.isConfigurationSection(key)) {
                ConfigurationSection sub = section.getConfigurationSection(key);
                if (sub != null) flattenSection(fullKey, sub, out);
                continue;
            }

            Object val = section.get(key);
            if (val instanceof List<?> list) {
                // optional: join lists with newlines
                out.put(fullKey, String.join("\n", list.stream().map(String::valueOf).toList()));
            } else if (val != null) {
                out.put(fullKey, String.valueOf(val));
            }
        }
    }

    public String translate(String key, Map<String, String> placeholders, String locale) {
        String lc = (locale == null || locale.isEmpty()) ? defaultLanguage : locale.toLowerCase();

        String msg = getTranslation(lc, key);
        if (msg == null) msg = getTranslation(defaultLanguage, key);

        if (msg == null) return key; // final fallback (your existing behavior)

        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                msg = msg.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return msg;
    }

    private String getTranslation(String locale, String key) {
        Map<String, String> map = translations.get(locale);
        if (map == null) return null;
        return map.get(key);
    }

    public void saveDefaultLanguage(String locale, String resource) {
        File langFile = new File(plugin.getDataFolder(), "languages/" + locale + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("languages/" + resource, false);
        }
    }
}
