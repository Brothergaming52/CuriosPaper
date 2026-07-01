package org.bg52.curiospaper.storage;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction layer for player data persistence.
 * Implementations handle the actual storage mechanism (SQLite, MySQL, MongoDB).
 * All async methods return CompletableFuture to avoid blocking the main server thread.
 */
public interface StorageProvider {

    // ========== LIFECYCLE ==========

    /**
     * Connects to the storage backend and initializes tables/collections.
     * Must be called before any data operations.
     *
     * @return a future that completes when the connection is established
     */
    CompletableFuture<Void> connect();

    /**
     * Disconnects from the storage backend and releases resources.
     * This method is synchronous to ensure clean shutdown during onDisable.
     */
    void disconnect();

    // ========== PLAYER ACCESSORY DATA (SlotManager) ==========

    /**
     * Saves a player's entire accessory inventory to the storage backend.
     * The accessories map is serialized to Base64 via {@link ItemStackSerializer}.
     *
     * @param playerUUID the player's UUID
     * @param accessories map of slot type -> list of ItemStacks
     * @return a future that completes when the data is saved
     */
    CompletableFuture<Void> savePlayerAccessories(UUID playerUUID, Map<String, List<ItemStack>> accessories);

    /**
     * Loads a player's entire accessory inventory from the storage backend.
     *
     * @param playerUUID the player's UUID
     * @return a future containing the accessories map, or an empty map if no data exists
     */
    CompletableFuture<Map<String, List<ItemStack>>> loadPlayerAccessories(UUID playerUUID);

    // ========== ADDON DATA ==========

    /**
     * Saves addon-specific data for a player.
     * Addons pass serialized string data — the storage layer does not interpret it.
     *
     * @param playerUUID the player's UUID
     * @param addonId unique identifier for the addon (e.g., "backpacks", "cosmetics")
     * @param serializedData the addon's serialized data string
     * @return a future that completes when the data is saved
     */
    CompletableFuture<Void> saveAddonData(UUID playerUUID, String addonId, String serializedData);

    /**
     * Loads addon-specific data for a player.
     *
     * @param playerUUID the player's UUID
     * @param addonId unique identifier for the addon
     * @return a future containing the serialized data string, or null if no data exists
     */
    CompletableFuture<String> loadAddonData(UUID playerUUID, String addonId);

    /**
     * Deletes addon-specific data for a player.
     *
     * @param playerUUID the player's UUID
     * @param addonId unique identifier for the addon
     * @return a future that completes when the data is deleted
     */
    CompletableFuture<Void> deleteAddonData(UUID playerUUID, String addonId);

    /**
     * Loads all addon data entries for a player.
     *
     * @param playerUUID the player's UUID
     * @return a future containing a map of addonId -> serialized data
     */
    CompletableFuture<Map<String, String>> loadAllAddonData(UUID playerUUID);

    // ========== CUSTOM ADDON TABLES ==========

    /**
     * Registers a custom table for an addon. In SQL databases, this creates the table.
     * In schema-less databases (like MongoDB), this is a no-op as collections are created dynamically.
     *
     * @param tableName name of the table or collection
     * @param sqliteColumns map of column name -> SQLite data type (e.g. "VARCHAR(36)")
     * @param mysqlColumns map of column name -> MySQL data type (e.g. "LONGTEXT")
     * @param primaryKeys list of column names that form the primary key
     * @return a future that completes when the table is registered
     */
    CompletableFuture<Void> registerTable(String tableName, Map<String, String> sqliteColumns,
                                         Map<String, String> mysqlColumns, List<String> primaryKeys);

    /**
     * Inserts or replaces a row of data in a custom table/collection.
     *
     * @param tableName name of the table or collection
     * @param rowData map of column name -> value
     * @param primaryKeys list of column names that form the primary key (used for SQL upsert matching and MongoDB replace filters)
     * @return a future that completes when the row is upserted
     */
    CompletableFuture<Void> upsertRow(String tableName, Map<String, Object> rowData, List<String> primaryKeys);

    /**
     * Selects rows matching the given conditions.
     *
     * @param tableName name of the table or collection
     * @param whereConditions map of column name -> expected value (conditions are joined with AND)
     * @return a future containing a list of matching row maps
     */
    CompletableFuture<List<Map<String, Object>>> selectRows(String tableName, Map<String, Object> whereConditions);

    /**
     * Deletes rows matching the given conditions.
     *
     * @param tableName name of the table or collection
     * @param whereConditions map of column name -> expected value (conditions are joined with AND)
     * @return a future that completes when the rows are deleted
     */
    CompletableFuture<Void> deleteRows(String tableName, Map<String, Object> whereConditions);

    /**
     * Drops the specified table or collection.
     *
     * @param tableName name of the table or collection
     * @return a future that completes when the table is dropped
     */
    CompletableFuture<Void> dropTable(String tableName);
}
