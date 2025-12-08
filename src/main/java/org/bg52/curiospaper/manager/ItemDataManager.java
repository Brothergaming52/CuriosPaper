package org.bg52.curiospaper.manager;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages custom item data including recipes, loot tables, and mob drops.
 * Handles loading, saving, and caching of item metadata.
 */
public class ItemDataManager {
    private final CuriosPaper plugin;
    private final File itemsFolder;
    private final Map<String, ItemData> loadedItems;

    public ItemDataManager(CuriosPaper plugin) {
        this.plugin = plugin;
        this.itemsFolder = new File(plugin.getDataFolder(), "items");
        this.loadedItems = new HashMap<>();

        // Create items folder if it doesn't exist
        if (!itemsFolder.exists()) {
            if (itemsFolder.mkdirs()) {
                plugin.getLogger().info("Created items directory: " + itemsFolder.getPath());
            }
        }

        loadAllItems();
    }

    /**
     * Loads all item data files from the items folder
     */
    public void loadAllItems() {
        loadedItems.clear();

        if (!itemsFolder.exists() || !itemsFolder.isDirectory()) {
            plugin.getLogger().warning("Items folder does not exist or is not a directory!");
            return;
        }

        File[] files = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("No custom items found in items folder.");
            return;
        }

        int loaded = 0;
        int failed = 0;

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ItemData data = ItemData.loadFromConfig(config);

                if (data != null && data.isValid()) {
                    loadedItems.put(data.getItemId(), data);
                    loaded++;
                    plugin.getLogger().info("✓ Loaded item: " + data.getItemId());
                } else {
                    plugin.getLogger().warning("✗ Invalid item data in file: " + file.getName());
                    failed++;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("✗ Failed to load item from " + file.getName() + ": " + e.getMessage());
                failed++;
            }
        }

        plugin.getLogger().info("Item data loading complete:");
        plugin.getLogger().info("  Successfully loaded: " + loaded);
        if (failed > 0) {
            plugin.getLogger().warning("  Failed to load: " + failed);
        }
    }

    /**
     * Creates a new item with the given ID
     */
    public ItemData createItem(String itemId) {
        if (loadedItems.containsKey(itemId)) {
            plugin.getLogger().warning("Item with ID '" + itemId + "' already exists!");
            return null;
        }

        ItemData data = new ItemData(itemId);
        data.setDisplayName(itemId); // Default display name
        data.setMaterial("PAPER"); // Default material
        loadedItems.put(itemId, data);

        return data;
    }

    /**
     * Saves item data to disk
     */
    public boolean saveItemData(String itemId) {
        ItemData data = loadedItems.get(itemId);
        if (data == null) {
            plugin.getLogger().warning("Cannot save item '" + itemId + "': not loaded");
            return false;
        }

        return saveItemData(data);
    }

    /**
     * Saves item data to disk
     */
    public boolean saveItemData(ItemData data) {
        if (!data.isValid()) {
            plugin.getLogger().warning("Cannot save invalid item data: " + data.getItemId());
            return false;
        }

        File file = new File(itemsFolder, data.getItemId() + ".yml");

        try {
            YamlConfiguration config = new YamlConfiguration();
            data.saveToConfig(config);
            config.save(file);

            plugin.getLogger().info("✓ Saved item: " + data.getItemId());
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("✗ Failed to save item " + data.getItemId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads item data from disk
     */
    public ItemData loadItemData(String itemId) {
        // Check if already loaded
        if (loadedItems.containsKey(itemId)) {
            return loadedItems.get(itemId);
        }

        File file = new File(itemsFolder, itemId + ".yml");
        if (!file.exists()) {
            return null;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ItemData data = ItemData.loadFromConfig(config);

            if (data != null && data.isValid()) {
                loadedItems.put(itemId, data);
                return data;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load item " + itemId + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * Gets item data by ID
     */
    public ItemData getItemData(String itemId) {
        return loadedItems.get(itemId);
    }

    /**
     * Checks if an item exists
     */
    public boolean hasItem(String itemId) {
        return loadedItems.containsKey(itemId);
    }

    /**
     * Deletes an item
     */
    public boolean deleteItem(String itemId) {
        ItemData data = loadedItems.remove(itemId);
        if (data == null) {
            return false;
        }

        File file = new File(itemsFolder, itemId + ".yml");
        if (file.exists()) {
            if (file.delete()) {
                plugin.getLogger().info("✓ Deleted item: " + itemId);
                return true;
            } else {
                plugin.getLogger().warning("Failed to delete item file: " + itemId + ".yml");
                // Re-add to loaded items since file deletion failed
                loadedItems.put(itemId, data);
                return false;
            }
        }

        return true;
    }

    /**
     * Gets all loaded item IDs
     */
    public Set<String> getAllItemIds() {
        return loadedItems.keySet();
    }

    /**
     * Gets all loaded items
     */
    public Map<String, ItemData> getAllItems() {
        return new HashMap<>(loadedItems);
    }

    /**
     * Saves all loaded items to disk
     */
    public void saveAllItems() {
        int saved = 0;
        int failed = 0;

        for (ItemData data : loadedItems.values()) {
            if (saveItemData(data)) {
                saved++;
            } else {
                failed++;
            }
        }

        plugin.getLogger().info("Saved " + saved + " items" + (failed > 0 ? " (" + failed + " failed)" : ""));
    }

    /**
     * Reloads all item data from disk
     */
    public void reload() {
        plugin.getLogger().info("Reloading item data...");
        loadAllItems();
    }
}
