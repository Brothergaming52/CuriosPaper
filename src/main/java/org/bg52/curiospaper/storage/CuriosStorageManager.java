package org.bg52.curiospaper.storage;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.inventory.PlayerCuriosInventory;
import org.bg52.synthetix.api.SynthetixAPI;
import org.bg52.synthetix.storage.AddonDataStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bg52.curiospaper.scheduler.PlatformScheduler;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise storage manager for CuriosPaper.
 * Automatically utilizes Synthetix's AddonDataStore (SQLite, MySQL, MongoDB, YAML) when available,
 * with graceful fallback to local YAML storage when running standalone.
 */
public class CuriosStorageManager {

    private final CuriosPaper plugin;
    private final Map<UUID, PlayerCuriosInventory> inventoryCache = new ConcurrentHashMap<>();
    private AddonDataStore synthetixStore;
    private File fallbackDir;
    private PlatformScheduler.TaskHandle autoSaveTask;

    public CuriosStorageManager(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // 1. Check Synthetix AddonDataStore
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Synthetix") && SynthetixAPI.getOptional().isPresent()) {
                this.synthetixStore = SynthetixAPI.get().getDataStore("curiospaper");
                plugin.getLogger().info("[Storage] Hooked into Synthetix enterprise AddonDataStore!");
                // Trigger legacy database and item migration via Synthetix
                if (SynthetixAPI.get().getLegacyMigrationManager() != null) {
                    SynthetixAPI.get().getLegacyMigrationManager().runMigration();
                }
            }
        } catch (Throwable t) {
            this.synthetixStore = null;
        }

        // 2. Setup fallback directory if running standalone
        if (this.synthetixStore == null) {
            this.fallbackDir = new File(plugin.getDataFolder(), "data/players");
            if (!this.fallbackDir.exists()) {
                this.fallbackDir.mkdirs();
            }
            plugin.getLogger().info("[Storage] Using local standalone storage engine (YAML).");
            migrateStandaloneOldCuriosData();
        }

        // 3. Start auto-save task
        int intervalMinutes = plugin.getConfig().getInt("storage.auto-save-interval", -1);
        if (intervalMinutes <= 0) {
            intervalMinutes = plugin.getConfig().getInt("features.auto-save-interval", 5);
        }
        if (intervalMinutes > 0) {
            long ticks = intervalMinutes * 60L * 20L;
            this.autoSaveTask = PlatformScheduler.get().runAsyncTimer(this::saveAll, ticks, ticks);
        }
    }

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        saveAll();
        inventoryCache.clear();
    }

    /**
     * Gets the cached PlayerCuriosInventory for an online player, or creates a new one.
     */
    public PlayerCuriosInventory getInventory(UUID playerUUID) {
        return inventoryCache.computeIfAbsent(playerUUID, PlayerCuriosInventory::new);
    }

    /**
     * Asynchronously loads a player's curios inventory from storage.
     */
    public CompletableFuture<PlayerCuriosInventory> loadPlayer(UUID playerUUID) {
        PlayerCuriosInventory inv = getInventory(playerUUID);

        if (synthetixStore != null) {
            return synthetixStore.loadItemMap(playerUUID, "inventory").thenApply(map -> {
                boolean mapHasItems = false;
                if (map != null) {
                    for (List<ItemStack> list : map.values()) {
                        if (list != null) {
                            for (ItemStack item : list) {
                                if (item != null && item.getType() != Material.AIR) {
                                    mapHasItems = true;
                                    break;
                                }
                            }
                        }
                        if (mapHasItems) break;
                    }
                }

                if (mapHasItems) {
                    inv.loadFromMap(map);
                } else if (inv.hasAnyEquippedItems()) {
                    plugin.getLogger().warning("Retained in-memory curios inventory for " + playerUUID + " because loaded data was empty while memory had active items.");
                } else {
                    inv.loadFromMap(map);
                }
                checkAndRebuildLoadedInventory(playerUUID, inv);
                return inv;
            }).exceptionally(ex -> {
                plugin.getLogger().warning("Failed to load curios data from Synthetix store for " + playerUUID + ": " + ex.getMessage());
                return inv;
            });
        }

        // Fallback YAML loader
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(fallbackDir, playerUUID.toString() + ".yml");
            if (file.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ConfigurationSection root = config.getConfigurationSection("slots");
                if (root != null) {
                    Map<String, List<ItemStack>> map = new HashMap<>();
                    boolean mapHasItems = false;
                    for (String slotType : root.getKeys(false)) {
                        List<?> rawList = root.getList(slotType);
                        if (rawList != null) {
                            List<ItemStack> items = new ArrayList<>();
                            for (Object obj : rawList) {
                                if (obj instanceof ItemStack is && is.getType() != Material.AIR) {
                                    items.add(is);
                                    mapHasItems = true;
                                } else {
                                    items.add(null);
                                }
                            }
                            map.put(slotType, items);
                        }
                    }
                    if (mapHasItems || !inv.hasAnyEquippedItems()) {
                        inv.loadFromMap(map);
                    } else {
                        plugin.getLogger().warning("Retained in-memory curios inventory for " + playerUUID + " because loaded YAML was empty while memory had active items.");
                    }
                }
            }
            checkAndRebuildLoadedInventory(playerUUID, inv);
            return inv;
        });
    }

    /**
     * Asynchronously saves a player's curios inventory to storage.
     */
    public CompletableFuture<Void> savePlayer(UUID playerUUID) {
        PlayerCuriosInventory inv = inventoryCache.get(playerUUID);
        if (inv == null) {
            return CompletableFuture.completedFuture(null);
        }

        Map<String, List<ItemStack>> copy = inv.getAllSlots();

        if (synthetixStore != null) {
            return synthetixStore.saveItemMap(playerUUID, "inventory", copy);
        }

        // Fallback YAML saver
        return CompletableFuture.runAsync(() -> {
            File file = new File(fallbackDir, playerUUID.toString() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            ConfigurationSection sec = config.createSection("slots");
            for (Map.Entry<String, List<ItemStack>> entry : copy.entrySet()) {
                sec.set(entry.getKey(), entry.getValue());
            }
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save local curios data for " + playerUUID + ": " + e.getMessage());
            }
        });
    }

    /**
     * Saves all online players' inventories.
     */
    public void saveAll() {
        for (UUID uuid : inventoryCache.keySet()) {
            savePlayer(uuid);
        }
    }

    /**
     * Unloads a player's cached data when they disconnect.
     */
    public void unloadPlayer(UUID playerUUID) {
        savePlayer(playerUUID).thenRun(() -> {
            Player online = Bukkit.getPlayer(playerUUID);
            if (online == null || !online.isOnline()) {
                inventoryCache.remove(playerUUID);
            }
        });
    }

    /**
     * Inspects loaded accessories and automatically rebuilds any outdated or modified items.
     */
    private void checkAndRebuildLoadedInventory(UUID playerUUID, PlayerCuriosInventory inv) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Synthetix") || !SynthetixAPI.getOptional().isPresent()) return;
        var updateMgr = SynthetixAPI.get().getItemUpdateManager();
        if (updateMgr == null) return;

        boolean updatedAny = false;
        Map<String, List<ItemStack>> slots = inv.getAllSlots();
        for (Map.Entry<String, List<ItemStack>> entry : slots.entrySet()) {
            List<ItemStack> list = entry.getValue();
            if (list == null) continue;
            for (int i = 0; i < list.size(); i++) {
                ItemStack item = list.get(i);
                if (item != null && item.getType() != Material.AIR && updateMgr.isItemOutdated(item)) {
                    ItemStack rebuilt = updateMgr.rebuildItem(item);
                    inv.setItem(entry.getKey(), i, rebuilt);
                    updatedAny = true;
                }
            }
        }

        if (updatedAny) {
            savePlayer(playerUUID);
            plugin.getLogger().info("[Storage] Rebuilt outdated curios items for " + playerUUID + " and synchronized storage.");
        }
    }

    /**
     * Standalone migration helper for migrating legacy CuriosPaper playerdata/*.yml files into 3.0 YAML format.
     */
    private void migrateStandaloneOldCuriosData() {
        File oldPlayerdataDir = new File(plugin.getDataFolder(), "playerdata");
        if (!oldPlayerdataDir.exists() || !oldPlayerdataDir.isDirectory()) return;

        File[] files = oldPlayerdataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) return;

        int migrated = 0;
        for (File file : files) {
            String name = file.getName().replace(".yml", "");
            try {
                UUID uuid = UUID.fromString(name);
                File targetFile = new File(fallbackDir, uuid.toString() + ".yml");
                if (!targetFile.exists()) {
                    YamlConfiguration oldCfg = YamlConfiguration.loadConfiguration(file);
                    ConfigurationSection accSec = oldCfg.getConfigurationSection("accessories");
                    if (accSec == null) accSec = oldCfg.getConfigurationSection("inventories.1.accessories");

                    if (accSec != null) {
                        YamlConfiguration newCfg = new YamlConfiguration();
                        ConfigurationSection slotsSec = newCfg.createSection("slots");
                        for (String slotType : accSec.getKeys(false)) {
                            ConfigurationSection sSec = accSec.getConfigurationSection(slotType);
                            if (sSec != null) {
                                List<ItemStack> items = new ArrayList<>();
                                List<String> keys = new ArrayList<>(sSec.getKeys(false));
                                keys.sort(Comparator.comparingInt(k -> {
                                    try { return Integer.parseInt(k); } catch (Exception ignored) { return Integer.MAX_VALUE; }
                                }));
                                for (String k : keys) {
                                    items.add(sSec.getItemStack(k));
                                }
                                slotsSec.set(slotType.toLowerCase(Locale.ROOT), items);
                            }
                        }
                        newCfg.save(targetFile);
                        migrated++;
                    }
                }
                File mig = new File(oldPlayerdataDir, file.getName() + ".migrated");
                file.renameTo(mig);
            } catch (Exception ignored) {}
        }

        if (migrated > 0) {
            plugin.getLogger().info("[Storage] Migrated " + migrated + " legacy player curios profiles into standalone YAML storage.");
        }
    }
}

