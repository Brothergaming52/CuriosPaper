package org.bg52.curiospaper.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * MySQL implementation of {@link StorageProvider}.
 * Uses HikariCP for connection pooling to handle concurrent access efficiently.
 * All JDBC operations use try-with-resources to prevent resource leaks.
 */
public class MySQLProvider implements StorageProvider {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int poolSize;
    private final boolean useSSL;
    private final Logger logger;

    private HikariDataSource dataSource;

    private static final String CREATE_PLAYER_TABLE = "CREATE TABLE IF NOT EXISTS curios_player_data (" +
            "uuid VARCHAR(36) PRIMARY KEY, " +
            "accessories LONGTEXT, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static final String CREATE_ADDON_TABLE = "CREATE TABLE IF NOT EXISTS curios_addon_data (" +
            "uuid VARCHAR(36) NOT NULL, " +
            "addon_id VARCHAR(64) NOT NULL, " +
            "data LONGTEXT, " +
            "PRIMARY KEY (uuid, addon_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static final String UPSERT_PLAYER = "INSERT INTO curios_player_data (uuid, accessories) VALUES (?, ?) " +
            "ON DUPLICATE KEY UPDATE accessories = VALUES(accessories)";

    private static final String SELECT_PLAYER = "SELECT accessories FROM curios_player_data WHERE uuid = ?";

    private static final String UPSERT_ADDON = "INSERT INTO curios_addon_data (uuid, addon_id, data) VALUES (?, ?, ?) "
            +
            "ON DUPLICATE KEY UPDATE data = VALUES(data)";

    private static final String SELECT_ADDON = "SELECT data FROM curios_addon_data WHERE uuid = ? AND addon_id = ?";

    private static final String DELETE_ADDON = "DELETE FROM curios_addon_data WHERE uuid = ? AND addon_id = ?";

    private static final String SELECT_ALL_ADDONS = "SELECT addon_id, data FROM curios_addon_data WHERE uuid = ?";

    public MySQLProvider(String host, int port, String database, String username,
            String password, int poolSize, boolean useSSL, Logger logger) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.useSSL = useSSL;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Load and register driver class explicitly to make it discoverable by
                // DriverManager after shade relocation
                String driverClass;
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    driverClass = "com.mysql.cj.jdbc.Driver";
                } catch (ClassNotFoundException e) {
                    try {
                        Class.forName("com.mysql.jdbc.Driver"); // Older MySQL driver
                        driverClass = "com.mysql.jdbc.Driver";
                    } catch (ClassNotFoundException ex) {
                        try {
                            Class.forName("org.mariadb.jdbc.Driver"); // MariaDB driver fallback
                            driverClass = "org.mariadb.jdbc.Driver";
                        } catch (ClassNotFoundException exc) {
                            throw new RuntimeException("No suitable MySQL or MariaDB JDBC driver found on the server!");
                        }
                    }
                }

                HikariConfig config = new HikariConfig();
                config.setDriverClassName(driverClass);

                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=" + useSSL
                        + "&allowPublicKeyRetrieval=true"
                        + "&characterEncoding=UTF-8"
                        + "&useUnicode=true");
                config.setUsername(username);
                config.setPassword(password);
                config.setMaximumPoolSize(poolSize);
                config.setMinimumIdle(2);
                config.setConnectionTimeout(5000);
                config.setIdleTimeout(600000); // 10 minutes
                config.setMaxLifetime(1800000); // 30 minutes
                config.setPoolName("CuriosPaper-MySQL");

                // Performance optimizations
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");

                dataSource = new HikariDataSource(config);

                // Create tables
                try (Connection conn = dataSource.getConnection();
                        Statement stmt = conn.createStatement()) {
                    stmt.execute(CREATE_PLAYER_TABLE);
                    stmt.execute(CREATE_ADDON_TABLE);
                }

                logger.info("[Storage] Connected to MySQL database at " + host + ":" + port + "/" + database);
                logger.info("[Storage] HikariCP pool initialized with max size: " + poolSize);
            } catch (Exception e) {
                logger.severe("[Storage] Failed to connect to MySQL: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("MySQL connection failed", e);
            }
        });
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("[Storage] MySQL connection pool closed.");
        }
    }

    // ========== Player Accessory Data ==========

    @Override
    public CompletableFuture<Void> savePlayerAccessories(UUID playerUUID, Map<String, List<ItemStack>> accessories) {
        return CompletableFuture.runAsync(() -> {
            String base64 = ItemStackSerializer.serializeAccessories(accessories);
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(UPSERT_PLAYER)) {
                ps.setString(1, playerUUID.toString());
                ps.setString(2, base64);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.severe("[Storage] Failed to save player accessories for " + playerUUID + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, List<ItemStack>>> loadPlayerAccessories(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(SELECT_PLAYER)) {
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
            return new HashMap<>();
        });
    }

    // ========== Addon Data ==========

    @Override
    public CompletableFuture<Void> saveAddonData(UUID playerUUID, String addonId, String serializedData) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(UPSERT_ADDON)) {
                ps.setString(1, playerUUID.toString());
                ps.setString(2, addonId);
                ps.setString(3, serializedData);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.severe("[Storage] Failed to save addon data (" + addonId + ") for " + playerUUID + ": "
                        + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<String> loadAddonData(UUID playerUUID, String addonId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(SELECT_ADDON)) {
                ps.setString(1, playerUUID.toString());
                ps.setString(2, addonId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("data");
                    }
                }
            } catch (SQLException e) {
                logger.severe("[Storage] Failed to load addon data (" + addonId + ") for " + playerUUID + ": "
                        + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> deleteAddonData(UUID playerUUID, String addonId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(DELETE_ADDON)) {
                ps.setString(1, playerUUID.toString());
                ps.setString(2, addonId);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.severe("[Storage] Failed to delete addon data (" + addonId + ") for " + playerUUID + ": "
                        + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, String>> loadAllAddonData(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> result = new HashMap<>();
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(SELECT_ALL_ADDONS)) {
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
            return result;
        });
    }

    // ========== CUSTOM ADDON TABLES ==========

    @Override
    public CompletableFuture<Void> registerTable(String tableName, Map<String, String> sqliteColumns,
                                                 Map<String, String> mysqlColumns, List<String> primaryKeys) {
        return CompletableFuture.runAsync(() -> {
            if (mysqlColumns == null || mysqlColumns.isEmpty()) return;
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
            
            for (Map.Entry<String, String> entry : mysqlColumns.entrySet()) {
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
            sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            
            try (Connection conn = dataSource.getConnection()) {
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
                for (Map.Entry<String, String> entry : mysqlColumns.entrySet()) {
                    String colName = entry.getKey();
                    if (!existingCols.contains(colName.toLowerCase())) {
                        String colType = entry.getValue();
                        String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN " + colName + " " + colType;
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(alterSql);
                            logger.info("[Storage] Added missing column '" + colName + "' to MySQL table '" + tableName + "'");
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("[Storage] Failed to register or update MySQL table '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> upsertRow(String tableName, Map<String, Object> rowData, List<String> primaryKeys) {
        return CompletableFuture.runAsync(() -> {
            if (rowData == null || rowData.isEmpty()) return;
            
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(tableName).append(" (");
            
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
            sb.append(") ON DUPLICATE KEY UPDATE ");
            
            boolean first = true;
            for (String col : columns) {
                if (primaryKeys != null && primaryKeys.contains(col)) {
                    continue; // Do not update primary keys
                }
                if (!first) {
                    sb.append(", ");
                }
                sb.append(col).append(" = VALUES(").append(col).append(")");
                first = false;
            }
            
            // If all columns are primary keys, duplicate key update is a no-op
            if (first) {
                if (primaryKeys != null && !primaryKeys.isEmpty()) {
                    String pk = primaryKeys.get(0);
                    sb.append(pk).append(" = ").append(pk);
                } else {
                    sb.setLength(sb.lastIndexOf(" ON DUPLICATE KEY UPDATE "));
                }
            }
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < columns.size(); i++) {
                    ps.setObject(i + 1, rowData.get(columns.get(i)));
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.severe("[Storage] Failed to upsert row in MySQL table '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
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
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sb.toString())) {
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
                logger.severe("[Storage] Failed to select rows from MySQL table '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
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
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < conditions.size(); i++) {
                    ps.setObject(i + 1, whereConditions.get(conditions.get(i)));
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.severe("[Storage] Failed to delete rows from MySQL table '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> dropTable(String tableName) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS " + tableName);
            } catch (SQLException e) {
                logger.severe("[Storage] Failed to drop MySQL table '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
