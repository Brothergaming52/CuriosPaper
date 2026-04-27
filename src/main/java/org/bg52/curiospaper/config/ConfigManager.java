package org.bg52.curiospaper.config;

import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
  private final CuriosPaper plugin;
  private final Map<String, SlotConfiguration> slotConfigurations;

  /**
   * Slot activity registry — true when at least one item has been tagged for
   * that slot type. Checked O(1) by AccessoryGUI to decide which slot buttons
   * to show in the main menu.
   * ConcurrentHashMap so external plugins registering items asynchronously are safe.
   */
  private final Map<String, Boolean> slotActivityRegistry = new ConcurrentHashMap<>();

  // Hotkey settings
  private boolean hotkeyEnabled;
  private int hotkeySlot;
  private SneakType hotkeySneakType;
  private int sneakHoldDuration;
  private KeepInventoryType keepInventoryType;

  public enum SneakType {
    SINGLE, DOUBLE, HOLD
  }

  public enum KeepInventoryType {
    ALWAYS, AUTO, NEVER
  }

  private static final int MIN_SLOT_AMOUNT = 1;
  private static final int MAX_SLOT_AMOUNT = 54;
  private static final Material DEFAULT_ICON = Material.BARRIER;

  public ConfigManager(CuriosPaper plugin) {
    this.plugin = plugin;
    this.slotConfigurations = new LinkedHashMap<>();
    loadConfigurations();
  }

  private void loadConfigurations() {
    ConfigurationSection slotsSection = plugin.getConfig().getConfigurationSection("slots");
    if (slotsSection == null) {
      plugin.getLogger().warning("No slots configured in config.yml!");
      plugin.getLogger().warning("Please add slot configurations or the plugin will not function properly.");
      return;
    }

    int loadedCount = 0;
    int errorCount = 0;

    for (String key : slotsSection.getKeys(false)) {
      try {
        SlotConfiguration config = loadSlotConfiguration(key, slotsSection.getConfigurationSection(key));
        if (config != null) {
          slotConfigurations.put(key.toLowerCase(), config);
          loadedCount++;
          plugin.getLogger().info("✓ Loaded slot: '" + key + "' (" + config.getAmount() + " slots)");
        } else {
          errorCount++;
        }
      } catch (Exception e) {
        plugin.getLogger().severe("✗ Failed to load slot '" + key + "': " + e.getMessage());
        errorCount++;
      }
    }

    loadHotkeySettings();
    loadKeepInventorySettings();

    plugin.getLogger().info("Slot configuration loading complete:");
    plugin.getLogger().info(" Successfully loaded: " + loadedCount);
    if (errorCount > 0) {
      plugin.getLogger().warning(" Failed to load: " + errorCount);
    }
  }

  private SlotConfiguration loadSlotConfiguration(String key, ConfigurationSection section) {
    if (section == null) {
      plugin.getLogger().warning("✗ Slot '" + key + "' has no configuration section. Skipping.");
      return null;
    }

    // Validate key
    if (key.trim().isEmpty()) {
      plugin.getLogger().warning("✗ Empty slot key found. Skipping.");
      return null;
    }

    // Load and validate name
    String name = section.getString("name");
    if (name == null || name.trim().isEmpty()) {
      plugin.getLogger().warning("✗ Slot '" + key + "' has no name. Using default.");
      name = "&7" + key;
    }

    // Load and validate amount
    int amount = section.getInt("amount", 1);
    if (amount < MIN_SLOT_AMOUNT) {
      plugin.getLogger().warning("✗ Slot '" + key + "' has invalid amount (" + amount + "). Must be at least "
          + MIN_SLOT_AMOUNT + ". Using minimum.");
      amount = MIN_SLOT_AMOUNT;
    } else if (amount > MAX_SLOT_AMOUNT) {
      plugin.getLogger().warning("⚠ Slot '" + key + "' has amount (" + amount
          + ") exceeding recommended maximum (" + MAX_SLOT_AMOUNT + "). This may cause performance issues.");
    }

    // Load and validate icon
    String iconStr = section.getString("icon", "STONE");
    Material icon;
    try {
      icon = Material.valueOf(iconStr.toUpperCase().replace(" ", "_"));

      // Check if material is a valid item
      if (!icon.isItem()) {
        plugin.getLogger()
            .warning("⚠ Slot '" + key + "' uses non-item material '" + iconStr + "'. Using default.");
        icon = DEFAULT_ICON;
      }
    } catch (IllegalArgumentException e) {
      plugin.getLogger().warning(
          "⚠ Slot '" + key + "' has invalid material '" + iconStr + "'. Using default barrier icon.");
      icon = DEFAULT_ICON;
    }

    String modelStr = section.getString("item-model", null);
    NamespacedKey ItemModel = null;
    Integer customModelData = null;

    if (modelStr != null && !modelStr.isEmpty()) {
      // Check if it's an integer (CustomModelData)
      try {
        customModelData = Integer.parseInt(modelStr);
      } catch (NumberFormatException e) {
        // Not an integer, treat as NamespacedKey
        ItemModel = org.bg52.curiospaper.util.VersionUtil.parseNamespacedKey(modelStr);
      }
    }

    // Explicit custom-model-data override
    if (section.contains("custom-model-data")) {
      customModelData = section.getInt("custom-model-data");
    }

    // Load lore (optional)
    List<String> lore = section.getStringList("lore");
    if (lore.isEmpty()) {
      plugin.getLogger().info(" Note: Slot '" + key + "' has no lore defined.");
    }

    return new SlotConfiguration(key, name, icon, ItemModel, customModelData, amount, lore);
  }

  public Map<String, SlotConfiguration> getSlotConfigurations() {
    return new LinkedHashMap<>(slotConfigurations);
  }

  public SlotConfiguration getSlotConfiguration(String key) {
    if (key == null) {
      return null;
    }
    return slotConfigurations.get(key.toLowerCase());
  }

  public boolean hasSlotType(String key) {
    if (key == null) {
      return false;
    }
    return slotConfigurations.containsKey(key.toLowerCase());
  }

  public void reload() {
    plugin.reloadConfig();
    slotConfigurations.clear();
    slotActivityRegistry.clear();
    plugin.getLogger().info("Reloading slot configurations...");
    loadConfigurations();
  }

  /**
   * Adds a slot configuration at runtime (does not persist to config.yml)
   */
  public boolean addSlotConfiguration(String key, SlotConfiguration config) {
    if (key == null || key.trim().isEmpty() || config == null) {
      return false;
    }

    String normalizedKey = key.toLowerCase();
    if (slotConfigurations.containsKey(normalizedKey)) {
      plugin.getLogger().warning("Slot type '" + key + "' already exists!");
      return false;
    }

    slotConfigurations.put(normalizedKey, config);
    // New dynamic slots start inactive — they become active when an item is tagged
    slotActivityRegistry.putIfAbsent(normalizedKey, false);
    plugin.getLogger().info("✓ Registered dynamic slot: '" + key + "' (" + config.getAmount() + " slots)");
    return true;
  }

  /**
   * Removes a slot configuration at runtime
   */
  public boolean removeSlotConfiguration(String key) {
    if (key == null) {
      return false;
    }

    String normalizedKey = key.toLowerCase();
    SlotConfiguration removed = slotConfigurations.remove(normalizedKey);

    if (removed != null) {
      slotActivityRegistry.remove(normalizedKey);
      plugin.getLogger().info("✓ Unregistered slot: '" + key + "'");
      return true;
    }

    return false;
  }

  /**
   * Validates the entire configuration and returns a report
   */
  public ConfigValidationReport validate() {
    ConfigValidationReport report = new ConfigValidationReport();

    if (slotConfigurations.isEmpty()) {
      report.addError("No slot configurations loaded!");
      return report;
    }

    for (Map.Entry<String, SlotConfiguration> entry : slotConfigurations.entrySet()) {
      String key = entry.getKey();
      SlotConfiguration config = entry.getValue();

      if (config.getAmount() < MIN_SLOT_AMOUNT) {
        report.addWarning("Slot '" + key + "' has invalid amount");
      }

      if (config.getIcon() == DEFAULT_ICON) {
        report.addWarning("Slot '" + key + "' is using default barrier icon");
      }
    }

    return report;
  }

  // ========== SLOT ACTIVITY REGISTRY ==========

  /**
   * Marks a slot type as having at least one item registered to it.
   * Called automatically by the API whenever an item is tagged for a slot.
   * Safe to call from any thread.
   *
   * @param slotType the slot key (case-insensitive)
   */
  public void markSlotActive(String slotType) {
    if (slotType == null) return;
    slotActivityRegistry.put(slotType.toLowerCase(), true);
  }

  /**
   * Returns true if at least one item has been tagged for this slot type.
   *
   * @param slotType the slot key (case-insensitive)
   */
  public boolean isSlotActive(String slotType) {
    if (slotType == null) return false;
    return Boolean.TRUE.equals(slotActivityRegistry.get(slotType.toLowerCase()));
  }

  /**
   * Returns an ordered list of slot keys that are currently active
   * (have at least one item registered).
   * Preserves the insertion order of slotConfigurations.
   */
  public List<String> getActiveSlotKeys() {
    List<String> active = new ArrayList<>();
    // Iterate in config insertion order so the GUI order is predictable
    for (String key : slotConfigurations.keySet()) {
      if (Boolean.TRUE.equals(slotActivityRegistry.get(key))) {
        active.add(key);
      }
    }
    return active;
  }

  /**
   * Scans all loaded ItemData entries and marks slots active for any item
   * that has a slotType set. Called once after ItemDataManager finishes loading.
   */
  public void initSlotActivityFromItems() {
    org.bg52.curiospaper.manager.ItemDataManager idm = plugin.getItemDataManager();
    if (idm == null) return;
    for (org.bg52.curiospaper.data.ItemData data : idm.getAllItems().values()) {
      String slotType = data.getSlotType();
      if (slotType != null && !slotType.isEmpty()) {
        // Items may declare multiple slots via comma-separation
        for (String s : slotType.split(",\\s*")) {
          String normalized = s.trim().toLowerCase();
          if (!normalized.isEmpty()) {
            markSlotActive(normalized);
          }
        }
      }
    }
  }

  /**
   * Clears the current slot activity registry and recalculates it fully from all
   * currently loaded ItemData. This ensures that when items are dynamically
   * created or deleted, unused slots disappear and new slots appear.
   */
  public void recalculateSlotActivityFromItems() {
    slotActivityRegistry.clear();
    initSlotActivityFromItems();
  }

  private void loadHotkeySettings() {
    hotkeyEnabled = plugin.getConfig().getBoolean("features.hotkey.enabled", true);
    hotkeySlot = plugin.getConfig().getInt("features.hotkey.slot", 8);
    if (hotkeySlot < 1 || hotkeySlot > 9) {
      hotkeySlot = 8;
      plugin.getLogger().warning("Invalid hotkey slot in config, using 8.");
    }

    String sneakTypeStr = plugin.getConfig().getString("features.hotkey.sneak-type", "double");
    try {
      hotkeySneakType = SneakType.valueOf(sneakTypeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      hotkeySneakType = SneakType.DOUBLE;
      plugin.getLogger().warning("Invalid sneak-type in config, using DOUBLE.");
    }

    sneakHoldDuration = plugin.getConfig().getInt("features.hotkey.sneak-hold-duration", 3);
    if (sneakHoldDuration < 1) {
      sneakHoldDuration = 3;
      plugin.getLogger().warning("Invalid sneak-hold-duration in config, using 3 seconds.");
    }
  }

  public boolean isHotkeyEnabled() {
    return hotkeyEnabled;
  }

  public int getHotkeySlot() {
    return hotkeySlot;
  }

  public SneakType getHotkeySneakType() {
    return hotkeySneakType;
  }

  public int getSneakHoldDuration() {
    return sneakHoldDuration;
  }

  private void loadKeepInventorySettings() {
    String typeStr = plugin.getConfig().getString("features.keep-curio-inventory.type", "Auto");
    try {
      keepInventoryType = KeepInventoryType.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      keepInventoryType = KeepInventoryType.AUTO;
      plugin.getLogger().warning("Invalid keep-curio-inventory type in config, using AUTO.");
    }
  }

  public KeepInventoryType getKeepInventoryType() {
    return keepInventoryType;
  }

  public static class ConfigValidationReport {
    private final List<String> errors = new java.util.ArrayList<>();
    private final List<String> warnings = new java.util.ArrayList<>();

    public void addError(String error) {
      errors.add(error);
    }

    public void addWarning(String warning) {
      warnings.add(warning);
    }

    public boolean hasErrors() {
      return !errors.isEmpty();
    }

    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }

    public List<String> getErrors() {
      return new java.util.ArrayList<>(errors);
    }

    public List<String> getWarnings() {
      return new java.util.ArrayList<>(warnings);
    }
  }
}