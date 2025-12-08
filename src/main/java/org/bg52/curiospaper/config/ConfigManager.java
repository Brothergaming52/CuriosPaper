package org.bg52.curiospaper.config;

import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final CuriosPaper plugin;
    private final Map<String, SlotConfiguration> slotConfigurations;

    private static final int MIN_SLOT_AMOUNT = 1;
    private static final int MAX_SLOT_AMOUNT = 54;
    private static final Material DEFAULT_ICON = Material.BARRIER;

    public ConfigManager(CuriosPaper plugin) {
        this.plugin = plugin;
        this.slotConfigurations = new HashMap<>();
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

        plugin.getLogger().info("Slot configuration loading complete:");
        plugin.getLogger().info("  Successfully loaded: " + loadedCount);
        if (errorCount > 0) {
            plugin.getLogger().warning("  Failed to load: " + errorCount);
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

        if (modelStr != null && !modelStr.isEmpty()) {
            // Accept both "curiospaper:head_slot" and "head_slot"
            if (modelStr.contains(":")) {
                ItemModel = NamespacedKey.fromString(modelStr);
            } else {
                ItemModel = new NamespacedKey(plugin, modelStr);
            }
        }

        // Load lore (optional)
        List<String> lore = section.getStringList("lore");
        if (lore.isEmpty()) {
            plugin.getLogger().info("  Note: Slot '" + key + "' has no lore defined.");
        }

        return new SlotConfiguration(key, name, icon, ItemModel, amount, lore);
    }

    public Map<String, SlotConfiguration> getSlotConfigurations() {
        return new HashMap<>(slotConfigurations);
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