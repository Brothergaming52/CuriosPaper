package org.bg52.curiospaper.storage;

import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Singleton manager for CuriosPaper's storage system.
 * Reads the config, instantiates the correct {@link StorageProvider}, and exposes
 * safe async methods for both core plugin use and addon data access.
 *
 * <p>Addons access this via {@code CuriosPaperAPI.getStorageAPI()} or
 * {@code CuriosStorageAPI.getInstance()} directly.</p>
 */
public final class CuriosStorageAPI {

    private static CuriosStorageAPI instance;

    private final StorageProvider provider;
    private final ExecutorService executor;
    private final Logger logger;
    private final String storageType;
    private final boolean databaseMode;
    private final File dataFolder;

    private CuriosStorageAPI(StorageProvider provider, ExecutorService executor,
                             Logger logger, String storageType, File dataFolder) {
        this.provider = provider;
        this.executor = executor;
        this.logger = logger;
        this.storageType = storageType;
        this.databaseMode = !storageType.equalsIgnoreCase("yaml");
        this.dataFolder = dataFolder;
    }

    /**
     * Initializes the storage API based on the plugin's config.yml.
     * Must be called during CuriosPaper.onEnable().
     *
     * @param plugin the main CuriosPaper plugin instance
     */
    public static void initialize(CuriosPaper plugin) {
        if (instance != null) {
            plugin.getLogger().warning("[Storage] CuriosStorageAPI already initialized! Skipping.");
            return;
        }

        Logger logger = plugin.getLogger();
        String type = plugin.getConfig().getString("storage.type", "yaml").toLowerCase().trim();
        ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "CuriosPaper-Storage");
            t.setDaemon(true);
            return t;
        });

        StorageProvider provider;

        switch (type) {
            case "sqlite": {
                String filePath = plugin.getConfig().getString("storage.sqlite.file", "storage/curiospaper.db");
                File dbFile = new File(plugin.getDataFolder(), filePath);
                provider = new SQLiteProvider(dbFile, logger);
                break;
            }

            case "mysql": {
                String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
                int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
                String database = plugin.getConfig().getString("storage.mysql.database", "curiospaper");
                String username = plugin.getConfig().getString("storage.mysql.username", "root");
                String password = plugin.getConfig().getString("storage.mysql.password", "");
                int poolSize = plugin.getConfig().getInt("storage.mysql.pool-size", 10);
                boolean useSSL = plugin.getConfig().getBoolean("storage.mysql.use-ssl", false);
                provider = new MySQLProvider(host, port, database, username, password, poolSize, useSSL, logger);
                break;
            }

            case "mongodb": {
                String connString = plugin.getConfig().getString("storage.mongodb.connection-string",
                        "mongodb://localhost:27017");
                String database = plugin.getConfig().getString("storage.mongodb.database", "curiospaper");
                provider = new MongoDBProvider(connString, database, logger);
                break;
            }

            case "yaml": {
                // No database provider needed — SlotManager handles YAML directly
                provider = null;
                break;
            }

            default: {
                logger.warning("[Storage] Unknown storage type '" + type + "'. Falling back to YAML.");
                type = "yaml";
                provider = null;
                break;
            }
        }

        instance = new CuriosStorageAPI(provider, executor, logger, type, plugin.getDataFolder());

        // Connect to the database if not YAML mode
        if (provider != null) {
            try {
                logger.info("[Storage] Connecting to " + type + " backend...");
                provider.connect().join();
                logger.info("[Storage] Successfully connected to " + type + " storage backend.");
            } catch (Exception e) {
                logger.severe("[Storage] Failed to connect to " + type + " storage backend!");
                logger.severe("[Storage] Error: " + e.getMessage());
                logger.severe("[Storage] Falling back to YAML storage.");
                e.printStackTrace();

                // Fall back to YAML
                instance = new CuriosStorageAPI(null, executor, logger, "yaml", plugin.getDataFolder());
            }
        } else {
            logger.info("[Storage] Using YAML file-based storage.");
        }
    }

    /**
     * Shuts down the storage API, disconnecting from the backend and
     * terminating the executor service.
     * Must be called during CuriosPaper.onDisable().
     */
    public static void shutdown() {
        if (instance == null) {
            return;
        }

        // Disconnect from the database
        if (instance.provider != null) {
            try {
                instance.provider.disconnect();
            } catch (Exception e) {
                instance.logger.severe("[Storage] Error during storage shutdown: " + e.getMessage());
            }
        }

        // Shutdown the executor
        if (instance.executor != null) {
            instance.executor.shutdown();
            try {
                if (!instance.executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    instance.executor.shutdownNow();
                    instance.logger.warning("[Storage] Executor did not terminate gracefully, forced shutdown.");
                }
            } catch (InterruptedException e) {
                instance.executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        instance.logger.info("[Storage] Storage API shut down.");
        instance = null;
    }

    /**
     * Gets the singleton instance of the storage API.
     *
     * @return the storage API instance, or null if not initialized
     */
    public static CuriosStorageAPI getInstance() {
        return instance;
    }

    // ========== Internal: Player Accessory Data ==========

    /**
     * Saves a player's accessory data to the database.
     * Used internally by SlotManager when in database mode.
     *
     * @param playerUUID  the player's UUID
     * @param accessories the accessory data to save
     * @return a future that completes when the data is saved
     */
    public CompletableFuture<Void> savePlayerAccessories(UUID playerUUID, Map<String, List<ItemStack>> accessories) {
        if (provider == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                provider.savePlayerAccessories(playerUUID, accessories).join();
            } catch (Exception e) {
                logger.severe("[Storage] Failed to save accessories for " + playerUUID + ": " + e.getMessage());
            }
        }, executor);
    }

    /**
     * Loads a player's accessory data from the database.
     * Used internally by SlotManager when in database mode.
     *
     * @param playerUUID the player's UUID
     * @return a future containing the accessories map
     */
    public CompletableFuture<Map<String, List<ItemStack>>> loadPlayerAccessories(UUID playerUUID) {
        if (provider == null) {
            return CompletableFuture.completedFuture(new java.util.HashMap<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return provider.loadPlayerAccessories(playerUUID).join();
            } catch (Exception e) {
                logger.severe("[Storage] Failed to load accessories for " + playerUUID + ": " + e.getMessage());
                return new java.util.HashMap<>();
            }
        }, executor);
    }

    // ========== Public API: Addon Data ==========

    /**
     * Saves addon-specific data for a player.
     * This is the primary API method for addon plugins.
     *
     * @param playerUUID the player's UUID
     * @param addonId    unique identifier for the addon
     * @param data       the serialized data string
     * @return a future that completes when the data is saved
     * @throws IllegalStateException if storage is not in database mode
     */
    public CompletableFuture<Void> saveData(UUID playerUUID, String addonId, String data) {
        validateAddonArgs(playerUUID, addonId);
        if (provider == null) {
            logger.warning("[Storage] Cannot save addon data — storage is in YAML mode. " +
                    "Addon data storage requires sqlite, mysql, or mongodb.");
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                provider.saveAddonData(playerUUID, addonId, data).join();
            } catch (Exception e) {
                logger.severe("[Storage] Failed to save addon data (" + addonId + ") for " + playerUUID + ": " + e.getMessage());
            }
        }, executor);
    }

    /**
     * Loads addon-specific data for a player.
     *
     * @param playerUUID the player's UUID
     * @param addonId    unique identifier for the addon
     * @return a future containing the data string, or null if not found
     */
    public CompletableFuture<String> loadData(UUID playerUUID, String addonId) {
        validateAddonArgs(playerUUID, addonId);
        if (provider == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return provider.loadAddonData(playerUUID, addonId).join();
            } catch (Exception e) {
                logger.severe("[Storage] Failed to load addon data (" + addonId + ") for " + playerUUID + ": " + e.getMessage());
                return null;
            }
        }, executor);
    }

    /**
     * Deletes addon-specific data for a player.
     *
     * @param playerUUID the player's UUID
     * @param addonId    unique identifier for the addon
     * @return a future that completes when the data is deleted
     */
    public CompletableFuture<Void> deleteData(UUID playerUUID, String addonId) {
        validateAddonArgs(playerUUID, addonId);
        if (provider == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                provider.deleteAddonData(playerUUID, addonId).join();
            } catch (Exception e) {
                logger.severe("[Storage] Failed to delete addon data (" + addonId + ") for " + playerUUID + ": " + e.getMessage());
            }
        }, executor);
    }

    /**
     * Loads all addon data entries for a player.
     *
     * @param playerUUID the player's UUID
     * @return a future containing a map of addonId -> data
     */
    public CompletableFuture<Map<String, String>> loadAllData(UUID playerUUID) {
        if (playerUUID == null) {
            throw new IllegalArgumentException("playerUUID cannot be null");
        }
        if (provider == null) {
            return CompletableFuture.completedFuture(new java.util.HashMap<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return provider.loadAllAddonData(playerUUID).join();
            } catch (Exception e) {
                logger.severe("[Storage] Failed to load all addon data for " + playerUUID + ": " + e.getMessage());
                return new java.util.HashMap<>();
            }
        }, executor);
    }

    /**
     * Migration API: any plugin can migrate its YAML data into CuriosPaper's database.
     * Must be called during the addon's onEnable() — runs synchronously (blocking).
     *
     * @param yamlFolder the folder containing UUID.yml files with an 'addons' section
     * @return the number of successfully migrated files
     */
    public int migrateFromYAML(File yamlFolder) {
        if (provider == null) {
            logger.warning("[Storage] Cannot migrate — storage is in YAML mode.");
            return 0;
        }
        return MigrationUtil.migrateAddonData(yamlFolder, provider, logger);
    }

    // ========== Getters ==========

    /**
     * Returns the name of the active storage backend (yaml, sqlite, mysql, mongodb).
     */
    public String getStorageType() {
        return storageType;
    }

    /**
     * Returns true if a database backend is active (not YAML).
     */
    public boolean isDatabaseMode() {
        return databaseMode && provider != null;
    }

    /**
     * Returns the underlying StorageProvider.
     * Used by MigrationUtil and internal components for direct migration access.
     */
    public StorageProvider getProvider() {
        return provider;
    }

    // ========== CUSTOM ADDON TABLES ==========

    /**
     * Registers a custom table for an addon. In database modes, delegates to the database provider.
     * In YAML mode, creates a file under tables/tableName.yml.
     */
    public CompletableFuture<Void> registerTable(String tableName, Map<String, String> sqliteColumns,
                                                 Map<String, String> mysqlColumns, List<String> primaryKeys) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("tableName cannot be null or empty");
        }
        if (provider != null) {
            return CompletableFuture.runAsync(() -> {
                try {
                    provider.registerTable(tableName, sqliteColumns, mysqlColumns, primaryKeys).join();
                } catch (Exception e) {
                    logger.severe("[Storage] Failed to register table '" + tableName + "': " + e.getMessage());
                }
            }, executor);
        }

        // YAML fallback
        return CompletableFuture.runAsync(() -> {
            try {
                File tablesDir = new File(dataFolder, "tables");
                if (!tablesDir.exists()) {
                    tablesDir.mkdirs();
                }
                File tableFile = new File(tablesDir, tableName + ".yml");
                if (!tableFile.exists()) {
                    tableFile.createNewFile();
                    org.bukkit.configuration.file.YamlConfiguration config =
                            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(tableFile);
                    config.set("rows", new java.util.ArrayList<>());
                    config.save(tableFile);
                }
            } catch (Exception e) {
                logger.severe("[Storage] Failed to register YAML table '" + tableName + "': " + e.getMessage());
            }
        }, executor);
    }

    /**
     * Inserts or replaces a row of data in a custom table/collection.
     */
    public CompletableFuture<Void> upsertRow(String tableName, Map<String, Object> rowData, List<String> primaryKeys) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("tableName cannot be null or empty");
        }
        if (provider != null) {
            return CompletableFuture.runAsync(() -> {
                try {
                    provider.upsertRow(tableName, rowData, primaryKeys).join();
                } catch (Exception e) {
                    logger.severe("[Storage] Failed to upsert row in table '" + tableName + "': " + e.getMessage());
                }
            }, executor);
        }

        // YAML fallback
        return CompletableFuture.runAsync(() -> {
            try {
                File tablesDir = new File(dataFolder, "tables");
                File tableFile = new File(tablesDir, tableName + ".yml");
                if (!tableFile.exists()) {
                    registerTable(tableName, null, null, null).join();
                }

                synchronized (this) {
                    org.bukkit.configuration.file.YamlConfiguration config =
                            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(tableFile);

                    List<Map<String, Object>> rows = new java.util.ArrayList<>();
                    List<?> rawList = config.getList("rows");
                    if (rawList != null) {
                        for (Object obj : rawList) {
                            if (obj instanceof Map) {
                                Map<String, Object> map = new java.util.LinkedHashMap<>();
                                for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                                    if (entry.getKey() != null) {
                                        map.put(entry.getKey().toString(), entry.getValue());
                                    }
                                }
                                rows.add(map);
                            }
                        }
                    }

                    int matchIndex = -1;
                    if (primaryKeys != null && !primaryKeys.isEmpty()) {
                        for (int i = 0; i < rows.size(); i++) {
                            Map<String, Object> existingRow = rows.get(i);
                            boolean matches = true;
                            for (String pk : primaryKeys) {
                                Object rowVal = rowData.get(pk);
                                Object existVal = existingRow.get(pk);
                                if (!valuesMatch(rowVal, existVal)) {
                                    matches = false;
                                    break;
                                }
                            }
                            if (matches) {
                                matchIndex = i;
                                break;
                            }
                        }
                    }

                    Map<String, Object> serializableRow = new java.util.LinkedHashMap<>();
                    for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                        Object val = entry.getValue();
                        if (val instanceof UUID) {
                            val = val.toString();
                        }
                        serializableRow.put(entry.getKey(), val);
                    }

                    if (matchIndex != -1) {
                        Map<String, Object> existingRow = rows.get(matchIndex);
                        existingRow.putAll(serializableRow);
                    } else {
                        rows.add(serializableRow);
                    }

                    config.set("rows", rows);
                    config.save(tableFile);
                }
            } catch (Exception e) {
                logger.severe("[Storage] Failed to upsert YAML row in '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
            }
        }, executor);
    }

    /**
     * Selects rows matching the given conditions.
     */
    public CompletableFuture<List<Map<String, Object>>> selectRows(String tableName, Map<String, Object> whereConditions) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("tableName cannot be null or empty");
        }
        if (provider != null) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return provider.selectRows(tableName, whereConditions).join();
                } catch (Exception e) {
                    logger.severe("[Storage] Failed to select rows from table '" + tableName + "': " + e.getMessage());
                    return new java.util.ArrayList<>();
                }
            }, executor);
        }

        // YAML fallback
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> results = new java.util.ArrayList<>();
            try {
                File tablesDir = new File(dataFolder, "tables");
                File tableFile = new File(tablesDir, tableName + ".yml");
                if (!tableFile.exists()) {
                    return results;
                }

                synchronized (this) {
                    org.bukkit.configuration.file.YamlConfiguration config =
                            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(tableFile);
                    List<?> rawList = config.getList("rows");
                    if (rawList != null) {
                        for (Object obj : rawList) {
                            if (obj instanceof Map) {
                                Map<String, Object> map = new java.util.LinkedHashMap<>();
                                for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                                    if (entry.getKey() != null) {
                                        map.put(entry.getKey().toString(), entry.getValue());
                                    }
                                }

                                boolean matches = true;
                                if (whereConditions != null && !whereConditions.isEmpty()) {
                                    for (Map.Entry<String, Object> cond : whereConditions.entrySet()) {
                                        Object mapVal = map.get(cond.getKey());
                                        Object condVal = cond.getValue();
                                        if (!valuesMatch(mapVal, condVal)) {
                                            matches = false;
                                            break;
                                        }
                                    }
                                }

                                if (matches) {
                                    results.add(map);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.severe("[Storage] Failed to select YAML rows from '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
            }
            return results;
        }, executor);
    }

    /**
     * Deletes rows matching the given conditions.
     */
    public CompletableFuture<Void> deleteRows(String tableName, Map<String, Object> whereConditions) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("tableName cannot be null or empty");
        }
        if (provider != null) {
            return CompletableFuture.runAsync(() -> {
                try {
                    provider.deleteRows(tableName, whereConditions).join();
                } catch (Exception e) {
                    logger.severe("[Storage] Failed to delete rows from table '" + tableName + "': " + e.getMessage());
                }
            }, executor);
        }

        // YAML fallback
        return CompletableFuture.runAsync(() -> {
            try {
                File tablesDir = new File(dataFolder, "tables");
                File tableFile = new File(tablesDir, tableName + ".yml");
                if (!tableFile.exists()) {
                    return;
                }

                synchronized (this) {
                    org.bukkit.configuration.file.YamlConfiguration config =
                            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(tableFile);

                    List<Map<String, Object>> rows = new java.util.ArrayList<>();
                    List<?> rawList = config.getList("rows");
                    boolean modified = false;
                    if (rawList != null) {
                        for (Object obj : rawList) {
                            if (obj instanceof Map) {
                                Map<String, Object> map = new java.util.LinkedHashMap<>();
                                for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                                    if (entry.getKey() != null) {
                                        map.put(entry.getKey().toString(), entry.getValue());
                                    }
                                }

                                boolean matches = true;
                                if (whereConditions != null && !whereConditions.isEmpty()) {
                                    for (Map.Entry<String, Object> cond : whereConditions.entrySet()) {
                                        Object mapVal = map.get(cond.getKey());
                                        Object condVal = cond.getValue();
                                        if (!valuesMatch(mapVal, condVal)) {
                                            matches = false;
                                            break;
                                        }
                                    }
                                }

                                if (matches) {
                                    modified = true;
                                } else {
                                    rows.add(map);
                                }
                            }
                        }
                    }

                    if (modified) {
                        config.set("rows", rows);
                        config.save(tableFile);
                    }
                }
            } catch (Exception e) {
                logger.severe("[Storage] Failed to delete YAML rows in '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
            }
        }, executor);
    }

    /**
     * Drops/deletes a custom table. In database modes, delegates to the database provider.
     * In YAML mode, deletes the corresponding file from tables/tableName.yml.
     *
     * @param tableName name of the table or collection
     * @return a future that completes when the table is dropped
     */
    public CompletableFuture<Void> dropTable(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("tableName cannot be null or empty");
        }
        if (provider != null) {
            return CompletableFuture.runAsync(() -> {
                try {
                    provider.dropTable(tableName).join();
                } catch (Exception e) {
                    logger.severe("[Storage] Failed to drop table '" + tableName + "': " + e.getMessage());
                }
            }, executor);
        }

        // YAML fallback: delete the table file from tables/tableName.yml
        return CompletableFuture.runAsync(() -> {
            try {
                File tablesDir = new File(dataFolder, "tables");
                File tableFile = new File(tablesDir, tableName + ".yml");
                if (tableFile.exists()) {
                    if (tableFile.delete()) {
                        logger.info("[Storage] Dropped YAML table file '" + tableName + ".yml'");
                    } else {
                        logger.warning("[Storage] Failed to delete YAML table file '" + tableName + ".yml'");
                    }
                }
            } catch (Exception e) {
                logger.severe("[Storage] Failed to drop YAML table '" + tableName + "': " + e.getMessage());
            }
        }, executor);
    }

    private boolean valuesMatch(Object val1, Object val2) {
        if (val1 == null && val2 == null) return true;
        if (val1 == null || val2 == null) return false;
        if (val1.equals(val2)) return true;
        return val1.toString().equals(val2.toString());
    }

    // ========== Validation ==========

    private void validateAddonArgs(UUID playerUUID, String addonId) {
        if (playerUUID == null) {
            throw new IllegalArgumentException("playerUUID cannot be null");
        }
        if (addonId == null || addonId.trim().isEmpty()) {
            throw new IllegalArgumentException("addonId cannot be null or empty");
        }
    }
}
