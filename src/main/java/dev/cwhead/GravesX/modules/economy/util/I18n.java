package dev.cwhead.GravesX.modules.economy.util;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Internationalization manager for GravesX modules.
 * <p>
 * Loads language files from the plugin's "languages" folder and resolves
 * messages for players based on their client locale, with fallback to
 * a default language.
 * </p>
 */
public final class I18n {

    /** Plugin instance to load files. */
    private final JavaPlugin plugin;

    /** Default language key (e.g., "en_us") */
    private final String defaultLanguage;

    /** Map of locale -> flattened translation key -> value */
    private final Map<String, Map<String, String>> translations = new HashMap<>();

    /**
     * Creates a new i18n manager and loads all language files.
     *
     * @param plugin          plugin instance
     * @param defaultLanguage default language key to fall back to
     */
    public I18n(JavaPlugin plugin, String defaultLanguage) {
        this.plugin = plugin;
        this.defaultLanguage = defaultLanguage.toLowerCase();
        loadLanguages();
    }

    /**
     * Loads or reloads all language files from "languages" folder.
     */
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
            flattenConfig("", cfg, flat);
            translations.put(localeKey, flat);
        }
    }

    /**
     * Recursively flattens a nested YamlConfiguration into key->value strings.
     *
     * Example: "graves.economy.teleport.charged"
     *
     * @param prefix current prefix
     * @param cfg    configuration section
     * @param map    destination map
     */
    private void flattenConfig(String prefix, FileConfiguration cfg, Map<String, String> map) {
        for (String key : cfg.getKeys(false)) {
            Object val = cfg.get(key);
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (val instanceof FileConfiguration) {
                flattenConfig(fullKey, (FileConfiguration) val, map);
            } else if (val instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sub = (Map<String, Object>) val;
                flattenMap(fullKey, sub, map);
            } else {
                map.put(fullKey, val.toString());
            }
        }
    }

    private void flattenMap(String prefix, Map<String,Object> mapIn, Map<String,String> mapOut) {
        for (Map.Entry<String,Object> e : mapIn.entrySet()) {
            String fullKey = prefix + "." + e.getKey();
            Object val = e.getValue();
            if (val instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String,Object> sub = (Map<String,Object>) val;
                flattenMap(fullKey, sub, mapOut);
            } else {
                mapOut.put(fullKey, val.toString());
            }
        }
    }

    /**
     * Translates a key for a player using their locale.
     * Falls back to default language if missing.
     *
     * @param key          translation key
     * @param placeholders map of placeholder -> replacement
     * @param locale       player locale (e.g., "en_us")
     * @return translated string with placeholders replaced
     */
    public String translate(String key, Map<String,String> placeholders, String locale) {
        String lc = (locale == null || locale.isEmpty()) ? defaultLanguage : locale.toLowerCase();

        String msg = getTranslation(lc, key);
        if (msg == null) {
            msg = getTranslation(defaultLanguage, key);
        }
        if (msg == null) return key; // final fallback

        if (placeholders != null) {
            for (Map.Entry<String,String> e : placeholders.entrySet()) {
                msg = msg.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return msg;
    }

    private String getTranslation(String locale, String key) {
        Map<String,String> map = translations.get(locale);
        if (map == null) return null;
        return map.get(key);
    }

    /**
     * Saves a new language file if missing.
     *
     * @param locale   language key
     * @param resource plugin resource path
     */
    public void saveDefaultLanguage(String locale, String resource) {
        File langFile = new File(plugin.getDataFolder(), "languages/" + locale + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("languages/" + resource, false);
        }
    }
}
