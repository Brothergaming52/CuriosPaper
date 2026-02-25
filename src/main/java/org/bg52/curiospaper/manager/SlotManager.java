package org.bg52.curiospaper.manager;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.config.SlotConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SlotManager {
    private final CuriosPaper plugin;
    private final Map<UUID, Map<String, List<ItemStack>>> playerAccessories;
    private final File dataFolder;

    public SlotManager(CuriosPaper plugin) {
        this.plugin = plugin;
        this.playerAccessories = new HashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                plugin.getLogger().info("Created playerdata directory");
            } else {
                plugin.getLogger().severe("Failed to create playerdata directory!");
            }
        }
    }

    public void loadPlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");

        if (!playerFile.exists()) {
            playerAccessories.put(playerId, new HashMap<>());
            plugin.getLogger().fine("No existing data for player: " + player.getName());
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            Map<String, List<ItemStack>> accessories = new HashMap<>();

            ConfigurationSection accessoriesSection = config.getConfigurationSection("accessories");
            if (accessoriesSection != null) {
                for (String slotType : accessoriesSection.getKeys(false)) {
                    List<ItemStack> items = loadSlotItems(slotType,
                            accessoriesSection.getConfigurationSection(slotType));
                    accessories.put(slotType.toLowerCase(), items);
                }
            }

            playerAccessories.put(playerId, accessories);
            plugin.getLogger().info("Loaded accessory data for player: " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load player data for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            // Initialize with empty data to prevent null pointer issues
            playerAccessories.put(playerId, new HashMap<>());
        }
    }

    private List<ItemStack> loadSlotItems(String slotType, ConfigurationSection section) {
        List<ItemStack> items = new ArrayList<>();

        if (section == null) {
            return items;
        }

        SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(slotType);
        int maxSlots = config != null ? config.getAmount() : Integer.MAX_VALUE;

        // Sort keys numerically
        List<String> keys = new ArrayList<>(section.getKeys(false));
        keys.sort(Comparator.comparingInt(k -> {
            try {
                return Integer.parseInt(k);
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }));

        int loadedCount = 0;
        for (String key : keys) {
            try {
                int index = Integer.parseInt(key);
                ItemStack item = section.getItemStack(key);

                if (item != null) {
                    // Ensure list is large enough
                    while (items.size() <= index) {
                        items.add(null);
                    }
                    items.set(index, item);
                    loadedCount++;
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid item index '" + key + "' in slot type '" + slotType + "'");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load item at index '" + key + "' in slot type '" + slotType
                        + "': " + e.getMessage());
            }
        }

        // Truncate if exceeds configured amount
        if (items.size() > maxSlots) {
            plugin.getLogger().warning("Player data for slot type '" + slotType + "' has " + items.size() +
                    " items but only " + maxSlots + " are configured. Truncating excess items.");
            items = new ArrayList<>(items.subList(0, maxSlots));
        }

        plugin.getLogger().fine("Loaded " + loadedCount + " items for slot type: " + slotType);
        return items;
    }

    public void savePlayerData(Player player) {
        savePlayerData(player.getUniqueId());
    }

    public void savePlayerData(UUID playerId) {
        Map<String, List<ItemStack>> accessories = playerAccessories.get(playerId);
        if (accessories == null) {
            return;
        }

        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        int totalSaved = 0;
        for (Map.Entry<String, List<ItemStack>> entry : accessories.entrySet()) {
            String slotType = entry.getKey();
            List<ItemStack> items = entry.getValue();

            // Validate slot type still exists
            if (!plugin.getConfigManager().hasSlotType(slotType)) {
                plugin.getLogger().warning("Skipping save for invalid slot type: " + slotType);
                continue;
            }

            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    config.set("accessories." + slotType + "." + i, item);
                    totalSaved++;
                }
            }
        }

        try {
            config.save(playerFile);
            plugin.getLogger().fine("Saved " + totalSaved + " items for player: " + playerId);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save accessory data for player: " + playerId);
            e.printStackTrace();
        }
    }

    public void saveAllPlayerData() {
        int saved = 0;
        int failed = 0;

        for (UUID playerId : new HashSet<>(playerAccessories.keySet())) {
            try {
                savePlayerData(playerId);
                saved++;
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save data for player " + playerId + ": " + e.getMessage());
                failed++;
            }
        }

        plugin.getLogger().info("Saved data for " + saved + " player(s)" +
                (failed > 0 ? " (" + failed + " failed)" : ""));
    }

    public List<ItemStack> getAccessories(UUID playerId, String slotType) {
        Map<String, List<ItemStack>> accessories = playerAccessories.get(playerId);
        if (accessories == null) {
            return new ArrayList<>();
        }

        List<ItemStack> items = accessories.get(slotType.toLowerCase());
        if (items == null) {
            return new ArrayList<>();
        }

        // Validate against current configuration
        SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(slotType);
        if (config != null && items.size() > config.getAmount()) {
            plugin.getLogger().warning("Player " + playerId + " has more items than configured for slot type '" +
                    slotType + "'. Truncating.");
            items = new ArrayList<>(items.subList(0, config.getAmount()));
            accessories.put(slotType.toLowerCase(), items);
        }

        return new ArrayList<>(items);
    }

    public void setAccessories(UUID playerId, String slotType, List<ItemStack> items) {
        // Validate slot type
        if (!plugin.getConfigManager().hasSlotType(slotType)) {
            plugin.getLogger().warning("Attempted to set accessories for invalid slot type: " + slotType);
            return;
        }

        // Validate item count
        SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(slotType);
        if (config != null && items.size() > config.getAmount()) {
            plugin.getLogger().warning("Attempted to set " + items.size() + " items for slot type '" +
                    slotType + "' which only has " + config.getAmount() + " slots. Truncating.");
            items = new ArrayList<>(items.subList(0, config.getAmount()));
        }

        Map<String, List<ItemStack>> accessories = playerAccessories.computeIfAbsent(playerId, k -> new HashMap<>());
        accessories.put(slotType.toLowerCase(), new ArrayList<>(items));
    }

    public void setAccessoryItem(UUID playerId, String slotType, int index, ItemStack item) {
        if (!plugin.getConfigManager().hasSlotType(slotType)) {
            plugin.getLogger().warning("Attempted to set item for invalid slot type: " + slotType);
            return;
        }

        SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(slotType);
        if (config != null && index >= config.getAmount()) {
            plugin.getLogger().warning("Attempted to set item at index " + index +
                    " for slot type '" + slotType + "' which only has " +
                    config.getAmount() + " slots");
            return;
        }

        Map<String, List<ItemStack>> accessories = playerAccessories.computeIfAbsent(playerId, k -> new HashMap<>());
        List<ItemStack> items = accessories.computeIfAbsent(slotType.toLowerCase(), k -> new ArrayList<>());

        while (items.size() <= index) {
            items.add(null);
        }

        items.set(index, item);
    }

    public ItemStack getAccessoryItem(UUID playerId, String slotType, int index) {
        List<ItemStack> items = getAccessories(playerId, slotType);
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return null;
    }

    public void unloadPlayerData(UUID playerId) {
        playerAccessories.remove(playerId);
        plugin.getLogger().fine("Unloaded data for player: " + playerId);
    }

    public boolean hasPlayerData(UUID playerId) {
        return playerAccessories.containsKey(playerId);
    }

    /**
     * Gets the number of loaded player data entries
     */
    public int getLoadedPlayerCount() {
        return playerAccessories.size();
    }

    /**
     * Cleans up orphaned player data files (optional maintenance method)
     */
    public int cleanupOrphanedData(Set<UUID> validPlayers) {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return 0;

        int removed = 0;
        for (File file : files) {
            String name = file.getName().replace(".yml", "");
            try {
                UUID uuid = UUID.fromString(name);
                if (!validPlayers.contains(uuid)) {
                    if (file.delete()) {
                        removed++;
                    }
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in filename: " + name);
            }
        }

        return removed;
    }
}