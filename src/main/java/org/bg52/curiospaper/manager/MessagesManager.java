package org.bg52.curiospaper.manager;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Manages all user-facing messages for CuriosPaper.
 * Messages are loaded from messages.yml and support all color code formats
 * via {@link ColorUtil#translate(String)}.
 * 
 * Placeholder tokens use {key} syntax and are replaced at call time.
 */
public class MessagesManager {

  private final CuriosPaper plugin;
  private FileConfiguration messagesConfig;
  private File messagesFile;

  public MessagesManager(CuriosPaper plugin) {
    this.plugin = plugin;
    loadMessages();
  }

  /**
   * Loads or reloads messages.yml from disk.
   * Creates the default file if it doesn't exist.
   */
  public void loadMessages() {
    messagesFile = new File(plugin.getDataFolder(), "messages.yml");

    if (!messagesFile.exists()) {
      plugin.saveResource("messages.yml", false);
    }

    messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

    // Merge defaults so new keys added in updates are available
    InputStream defaultStream = plugin.getResource("messages.yml");
    if (defaultStream != null) {
      YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
      messagesConfig.setDefaults(defaults);
      messagesConfig.options().copyDefaults(true);
      try {
        messagesConfig.save(messagesFile);
      } catch (IOException e) {
        plugin.getLogger().warning("Could not save merged messages.yml: " + e.getMessage());
      }
    }
  }

  /**
   * Reloads messages from disk.
   */
  public void reload() {
    loadMessages();
    plugin.getLogger().info("Messages reloaded from messages.yml");
  }

  /**
   * Gets a translated message by its key path.
   * All color codes (legacy, hex, MiniMessage, gradients) are processed.
   * Returns the raw key path if the message is not found.
   *
   * @param key the YAML path (e.g. "commands.no-permission")
   * @return the colorized message string
   */
  public String get(String key) {
    String raw = messagesConfig.getString(key);
    if (raw == null) {
      plugin.getLogger().warning("Missing message key: " + key);
      return key;
    }
    return ColorUtil.translate(raw);
  }

  /**
   * Gets a translated message by its key path, with a fallback default.
   *
   * @param key          the YAML path
   * @param defaultValue value to return (and colorize) if key is missing
   * @return the colorized message or colorized default
   */
  public String get(String key, String defaultValue) {
    String raw = messagesConfig.getString(key);
    if (raw == null) {
      return ColorUtil.translate(defaultValue);
    }
    return ColorUtil.translate(raw);
  }

  /**
   * Gets a translated message with placeholder replacement.
   * Placeholders use {key} syntax.
   *
   * @param key          the YAML path
   * @param replacements pairs of placeholder-name, replacement-value
   * @return the colorized, placeholder-replaced message
   */
  public String get(String key, String... replacements) {
    String message = get(key);
    if (replacements.length % 2 != 0) {
      plugin.getLogger().warning("Odd number of replacement args for key: " + key);
    }
    for (int i = 0; i < replacements.length - 1; i += 2) {
      message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
    }
    return message;
  }

  /**
   * Gets a translated message with a map of placeholders.
   *
   * @param key          the YAML path
   * @param placeholders map of placeholder-name → replacement-value
   * @return the colorized, placeholder-replaced message
   */
  public String get(String key, Map<String, String> placeholders) {
    String message = get(key);
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      message = message.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return message;
  }

  /**
   * Gets the raw (untranslated) message string.
   * Useful for GUI titles that need raw processing.
   *
   * @param key the YAML path
   * @return the raw message or the key if not found
   */
  public String getRaw(String key) {
    String raw = messagesConfig.getString(key);
    return raw != null ? raw : key;
  }

  /**
   * Returns the underlying FileConfiguration for advanced use cases.
   */
  public FileConfiguration getConfig() {
    return messagesConfig;
  }
}
