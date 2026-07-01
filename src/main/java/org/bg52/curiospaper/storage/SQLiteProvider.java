package org.bg52.curiospaper.storage;

import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * SQLite implementation of {@link StorageProvider}.
 * Uses standard JDBC with a single shared connection (SQLite is single-writer).
 * All JDBC operations use try-with-resources to prevent resource leaks.
 */
public class SQLiteProvider implements StorageProvider {

    private final File databaseFile;
    private final Logger logger;
    private Connection connection;
    private final Object connectionLock = new Object();

    private static final String CREATE_PLAYER_TABLE =
            "CREATE TABLE IF NOT EXISTS curios_player_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "accessories LONGTEXT, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

    private static final String CREATE_ADDON_TABLE =
            "CREATE TABLE IF NOT EXISTS curios_addon_data (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "addon_id VARCHAR(64) NOT NULL, " +
                    "data LONGTEXT, " +
                    "PRIMARY KEY (uuid, addon_id)" +
                    ")";

    private static final String UPSERT_PLAYER =
            "INSERT OR REPLACE INTO curios_player_data (uuid, accessories, updated_at) VALUES (?, ?, datetime('now'))";

    private static final String SELECT_PLAYER =
            "SELECT accessories FROM curios_player_data WHERE uuid = ?";

    private static final String UPSERT_ADDON =
            "INSERT OR REPLACE INTO curios_addon_data (uuid, addon_id, data) VALUES (?, ?, ?)";

    private static final String SELECT_ADDON =
            "SELECT data FROM curios_addon_data WHERE uuid = ? AND addon_id = ?";

    private static final String DELETE_ADDON =
            "DELETE FROM curios_addon_data WHERE uuid = ? AND addon_id = ?";

    private static final String SELECT_ALL_ADDONS =
            "SELECT addon_id, data FROM curios_addon_data WHERE uuid = ?";

    public SQLiteProvider(File databaseFile, Logger logger) {
        this.databaseFile = databaseFile;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Ensure parent directory exists
                File parentDir = databaseFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        throw new RuntimeException("Failed to create database directory: " + parentDir.getAbsolutePath());
                    }
                }

                // Load the SQLite JDBC driver
                Class.forName("org.sqlite.JDBC");

                synchronized (connectionLock) {
                    connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());

                    // Enable WAL mode for better concurrent read performance
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("PRAGMA journal_mode=WAL");
                        stmt.execute("PRAGMA synchronous=NORMAL");
                    }

                    // Create tables
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(CREATE_PLAYER_TABLE);
                        stmt.execute(CREATE_ADDON_TABLE);
                    }
                }

                logger.info("[Storage] Connected to SQLite database: " + databaseFile.getName());
            } catch (Exception e) {
                logger.severe("[Storage] Failed to connect to SQLite: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("SQLite connection failed", e);
            }
        });
    }

    @Override
    public void disconnect() {
        synchronized (connectionLock) {
            if (connection != null) {
                try {
                    connection.close();
                    logger.info("[Storage] SQLite connection closed.");
                } catch (SQLException e) {
                    logger.severe("[Storage] Error closing SQLite connection: " + e.getMessage());
                }
                connection = null;
            }
        }
    }

    private Connection getConnection() throws SQLException {
        synchronized (connectionLock) {
            if (connection == null || connection.isClosed()) {
                throw new SQLException("SQLite connection is not available. Was connect() called?");
            }
            return connection;
        }
    }

    // ========== Player Accessory Data ==========

    @Override
    public CompletableFuture<Void> savePlayerAccessories(UUID playerUUID, Map<String, List<ItemStack>> accessories) {
        return CompletableFuture.runAsync(() -> {
            String base64 = ItemStackSerializer.serializeAccessories(accessories);
            synchronized (connectionLock) {
                try (PreparedStatement ps = getConnection().prepareStatement(UPSERT_PLAYER)) {
                    ps.setString(1, playerUUID.toString());
                    ps.setString(2, base64);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.severe("[Storage] Failed to save player accessories for " + playerUUID + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, List<ItemStack>>> loadPlayerAccessories(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (connectionLock) {
                try (PreparedStatement ps = getConnection().prepareStatement(SELECT_PLAYER)) {
                    ps.setString(1, playerUUID.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String base64 = rs.getString("accessories");
                            return ItemStackSerializer.deserializeAccessories(base64);
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("[Storage] Failed to load player accessories for " + playerUUID + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return new HashMap<>();
        });
    }

    // ========== Addon Data ==========

    @Override
    public CompletableFuture<Void> saveAddonData(UUID playerUUID, String addonId, String serializedData) {
        return CompletableFuture.runAsync(() -> {
            synchronized (connectionLock) {
                try (PreparedStatement ps = getConnection().prepareStatement(UPSERT_ADDON)) {
                    ps.setString(1, playerUUID.toString());
                    ps.setString(2, addonId);
                    ps.setString(3, serializedData);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.severe("[Storage] Failed to save addon data (" + addonId + ") for " + playerUUID + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public CompletableFuture<String> loadAddonData(UUID playerUUID, String addonId) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (connectionLock) {
                try (PreparedStatement ps = getConnection().prepareStatement(SELECT_ADDON)) {
                    ps.setString(1, playerUUID.toString());
                    ps.setString(2, addonId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("data");
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("[Storage] Failed to load addon data (" + addonId + ") for " + playerUUID + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> deleteAddonData(UUID playerUUID, String addonId) {
        return CompletableFuture.runAsync(() -> {
            synchronized (connectionLock) {
                try (PreparedStatement ps = getConnection().prepareStatement(DELETE_ADDON)) {
                    ps.setString(1, playerUUID.toString());
                    ps.setString(2, addonId);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.severe("[Storage] Failed to delete addon data (" + addonId + ") for " + playerUUID + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, String>> loadAllAddonData(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> result = new HashMap<>();
            synchronized (connectionLock) {
                try (PreparedStatement ps = getConnection().prepareStatement(SELECT_ALL_ADDONS)) {
                    ps.setString(1, playerUUID.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            result.put(rs.getString("addon_id"), rs.getString("data"));
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("[Storage] Failed to load all addon data for " + playerUUID + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return result;
        });
    }

    // ========== CUSTOM ADDON TABLES ==========

    @Override
    public CompletableFuture<Void> registerTable(String tableName, Map<String, String> sqliteColumns,
                                                 Map<String, String> mysqlColumns, List<String> primaryKeys) {
        return CompletableFuture.runAsync(() -> {
            if (sqliteColumns == null || sqliteColumns.isEmpty()) return;
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
            
            for (Map.Entry<String, String> entry : sqliteColumns.entrySet()) {
                sb.append(entry.getKey()).append(" ").append(entry.getValue()).append(", ");
            }
            
            if (primaryKeys != null && !primaryKeys.isEmpty()) {
                sb.append("PRIMARY KEY (");
                for (int i = 0; i < primaryKeys.size(); i++) {
                    sb.append(primaryKeys.get(i));
                    if (i < primaryKeys.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(")");
            } else {
                // remove trailing comma and space
                sb.setLength(sb.length() - 2);
            }
            sb.append(")");
            
            synchronized (connectionLock) {
                try {
                    Connection conn = getConnection();
                    // 1. Create table if not exists
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(sb.toString());
                    }

                    // 2. Query columns to see if we need to add any missing ones
                    Set<String> existingCols = new HashSet<>();
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT 0")) {
                        ResultSetMetaData rsmd = rs.getMetaData();
                        int colCount = rsmd.getColumnCount();
                        for (int i = 1; i <= colCount; i++) {
                            existingCols.add(rsmd.getColumnName(i).toLowerCase());
                        }
                    }

                    // 3. Alter table to add missing columns
                    for (Map.Entry<String, String> entry : sqliteColumns.entrySet()) {
                        String colName = entry.getKey();
                        if (!existingCols.contains(colName.toLowerCase())) {
                            String colType = entry.getValue();
                            String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN " + colName + " " + colType;
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute(alterSql);
                                logger.info("[Storage] Added missing column '" + colName + "' to SQLite table '" + tableName + "'");
                            }
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("[Storage] Failed to register or update SQLite table '" + tableName + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> upsertRow(String tableName, Map<String, Object> rowData, List<String> primaryKeys) {
        return CompletableFuture.runAsync(() -> {
            if (rowData == null || rowData.isEmpty()) return;
            
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT OR REPLACE INTO ").append(tableName).append(" (");
            
            List<String> columns = new ArrayList<>(rowData.keySet());
            for (int i = 0; i < columns.size(); i++) {
                sb.append(columns.get(i));
                if (i < columns.size() - 1) sb.append(", ");
            }
            sb.append(") VALUES (");
            for (int i = 0; i < columns.size(); i++) {
                sb.append("?");
                if (i < columns.size() - 1) sb.append(", ");
            }
            sb.append(")");
            
            synchronized (connectionLock) {
                try (PreparedStatement ps = getConnection().prepareStatement(sb.toString())) {
                    for (int i = 0; i < columns.size(); i++) {
                        ps.setObject(i + 1, rowData.get(columns.get(i)));
                    }
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.severe("[Storage] Failed to upsert row in SQLite table '" + tableName + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> selectRows(String tableName, Map<String, Object> whereConditions) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> results = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT * FROM ").append(tableName);
            
            List<String> conditions = whereConditions != null ? new ArrayList<>(whereConditions.keySet()) : Collections.emptyList();
            if (!conditions.isEmpty()) {
                sb.append(" WHERE ");
                for (int i = 0; i < conditions.size(); i++) {
                    sb.append(conditions.get(i)).append(" = ?");
                    if (i < conditions.size() - 1) {
                        sb.append(" AND ");
                    }
                }
            }
            
            synchronized (connectionLock) {
                try (PreparedStatement ps = getConnection().prepareStatement(sb.toString())) {
                    for (int i = 0; i < conditions.size(); i++) {
                        ps.setObject(i + 1, whereConditions.get(conditions.get(i)));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();
                        while (rs.next()) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= colCount; i++) {
                                row.put(meta.getColumnName(i), rs.getObject(i));
                            }
                            results.add(row);
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("[Storage] Failed to select rows from SQLite table '" + tableName + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return results;
        });
    }

    @Override
    public CompletableFuture<Void> deleteRows(String tableName, Map<String, Object> whereConditions) {
        return CompletableFuture.runAsync(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE FROM ").append(tableName);
            
            List<String> conditions = whereConditions != null ? new ArrayList<>(whereConditions.keySet()) : Collections.emptyList();
            if (!conditions.isEmpty()) {
                sb.append(" WHERE ");
                for (int i = 0; i < conditions.size(); i++) {
                    sb.append(conditions.get(i)).append(" = ?");
                    if (i < conditions.size() - 1) {
                        sb.append(" AND ");
                    }
                }
            }
            
            synchronized (connectionLock) {
                try (PreparedStatement ps = getConnection().prepareStatement(sb.toString())) {
                    for (int i = 0; i < conditions.size(); i++) {
                        ps.setObject(i + 1, whereConditions.get(conditions.get(i)));
                    }
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.severe("[Storage] Failed to delete rows from SQLite table '" + tableName + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> dropTable(String tableName) {
        return CompletableFuture.runAsync(() -> {
            synchronized (connectionLock) {
                try (Statement stmt = getConnection().createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS " + tableName);
                } catch (SQLException e) {
                    logger.severe("[Storage] Failed to drop SQLite table '" + tableName + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
}
