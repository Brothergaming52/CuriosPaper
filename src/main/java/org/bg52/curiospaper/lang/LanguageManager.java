package org.bg52.curiospaper.lang;

import net.kyori.adventure.text.Component;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.util.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Enterprise multi-language localization manager for CuriosPaper.
 * Supports hot-swapping language files from plugins/CuriosPaper/languages/<lang>.yml
 * with automatic fallback and auto-syncing of newly introduced keys from embedded en_US.yml defaults,
 * identical to Synthetix's LanguageManager.
 */
public class LanguageManager {

    private final CuriosPaper plugin;
    private final File langFolder;
    private FileConfiguration langConfig;
    private YamlConfiguration embeddedDefaults;
    private String activeLanguage;
    private String prefix = "";
    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(CuriosPaper plugin) {
        this.plugin = plugin;
        this.langFolder = new File(plugin.getDataFolder(), "languages");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        load();
    }

    /**
     * Loads or reloads the active language configuration based on config.yml's 'language' setting.
     */
    public void load() {
        messageCache.clear();
        this.activeLanguage = plugin.getConfig().getString("language", "en_US").trim();

        // 1. Load embedded defaults
        InputStream embeddedStream = plugin.getResource("languages/en_US.yml");
        if (embeddedStream != null) {
            this.embeddedDefaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(embeddedStream, StandardCharsets.UTF_8));
        }

        // 2. Ensure default en_US.yml exists in the languages folder
        File defaultFile = new File(langFolder, "en_US.yml");
        if (!defaultFile.exists()) {
            plugin.saveResource("languages/en_US.yml", false);
        }

        // 3. Resolve active language file
        File activeFile = new File(langFolder, activeLanguage + ".yml");
        if (!activeFile.exists()) {
            String alt = activeLanguage.replace("-", "_");
            File altFile = new File(langFolder, alt + ".yml");
            if (altFile.exists()) {
                activeFile = altFile;
                this.activeLanguage = alt;
            } else {
                plugin.getLogger().warning("Language file '" + activeLanguage + ".yml' not found in languages/ folder. Falling back to en_US.yml.");
                activeFile = defaultFile;
                this.activeLanguage = "en_US";
            }
        }

        this.langConfig = YamlConfiguration.loadConfiguration(activeFile);
        if (this.embeddedDefaults != null) {
            this.langConfig.setDefaults(this.embeddedDefaults);
            this.langConfig.options().copyDefaults(true);

            // Auto-sync missing keys to the active on-disk file
            boolean modified = false;
            for (String key : this.embeddedDefaults.getKeys(true)) {
                if (!this.langConfig.contains(key)) {
                    this.langConfig.set(key, this.embeddedDefaults.get(key));
                    modified = true;
                }
            }
            if (modified) {
                try {
                    this.langConfig.save(activeFile);
                } catch (Exception ignored) {}
            }
        }

        this.prefix = langConfig.getString("prefix", "<gradient:#FFB300:#FF5500><bold>[CuriosPaper]</bold></gradient> ");

        // Index all keys recursively so both dot-paths ("gui.main-title") and leaf names ("main-title") match
        indexSection("", langConfig);

        plugin.getLogger().info("Loaded CuriosPaper language: " + activeLanguage);
    }

    private void indexSection(String pathPrefix, ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            String fullPath = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;
            if (section.isConfigurationSection(key)) {
                indexSection(fullPath, section.getConfigurationSection(key));
            } else if (section.isString(key)) {
                String val = section.getString(key);
                messageCache.put(fullPath.toLowerCase(Locale.ROOT), val);
                // Also index leaf key if not collision
                messageCache.putIfAbsent(key.toLowerCase(Locale.ROOT), val);
            }
        }
    }

    /**
     * Resolves the raw string for a key.
     */
    public String getRaw(String key) {
        if (key == null) return "";
        String normalized = key.toLowerCase(Locale.ROOT);
        return messageCache.getOrDefault(normalized, key);
    }

    /**
     * Resolves a formatted string with placeholders replaced.
     */
    public String get(String key, String... placeholders) {
        String raw = getRaw(key);
        return applyPlaceholders(raw, placeholders);
    }

    /**
     * Generalized placeholder replacement supporting both {key} and %key% formats,
     * stripping delimiters so callers can pass "key", "%key%", or "{key}".
     */
    public static String applyPlaceholders(String text, String... placeholders) {
        if (text == null || placeholders == null || placeholders.length < 2) {
            return text;
        }
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String pKey = placeholders[i];
            String pVal = placeholders[i + 1] != null ? placeholders[i + 1] : "";
            if (pKey == null) continue;

            String clean = pKey;
            while (clean.startsWith("%") || clean.startsWith("{")) {
                clean = clean.substring(1);
            }
            while (clean.endsWith("%") || clean.endsWith("}")) {
                clean = clean.substring(0, clean.length() - 1);
            }

            text = text.replace("{" + clean + "}", pVal)
                       .replace("%" + clean + "%", pVal);
            if (!pKey.isEmpty()) {
                text = text.replace(pKey, pVal);
            }
        }
        return text;
    }

    /**
     * Gets a formatted Component message with prefix and placeholders replaced.
     */
    public Component getComponent(String key, String... placeholders) {
        return TextUtil.parse(prefix + get(key, placeholders));
    }

    /**
     * Gets a raw parsed Component without prefix (ideal for GUI titles, lore, and button names).
     */
    public Component getRawComponent(String key, String... placeholders) {
        return TextUtil.parse(get(key, placeholders));
    }

    /**
     * Gets a list of localized components for lore lines.
     */
    public List<Component> getComponentList(String key, String... placeholders) {
        List<Component> result = new ArrayList<>();
        List<String> list = langConfig != null ? langConfig.getStringList(key) : Collections.emptyList();
        if (list.isEmpty()) {
            // Check message cache for single line
            String single = get(key, placeholders);
            if (!single.equals(key)) {
                result.add(TextUtil.parse(single));
            }
            return result;
        }

        for (String line : list) {
            result.add(TextUtil.parse(applyPlaceholders(line, placeholders)));
        }
        return result;
    }

    /**
     * Sends a localized message to a CommandSender.
     */
    public void sendMessage(CommandSender sender, String key, String... placeholders) {
        if (sender != null) {
            sender.sendMessage(getComponent(key, placeholders));
        }
    }

    /**
     * Broadcasts a message to all online players.
     */
    public void broadcastMessage(String key, String... placeholders) {
        Component comp = getComponent(key, placeholders);
        for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendMessage(comp);
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public String getActiveLanguage() {
        return activeLanguage;
    }
}
