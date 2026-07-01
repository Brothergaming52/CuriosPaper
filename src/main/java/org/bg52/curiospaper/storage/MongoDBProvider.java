package org.bg52.curiospaper.storage;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * MongoDB implementation of {@link StorageProvider}.
 * Uses the official MongoDB Java Sync Driver.
 * Player accessories stored in 'player_data' collection.
 * Addon data uses $set/$unset operators for partial document updates.
 */
public class MongoDBProvider implements StorageProvider {

    private final String connectionString;
    private final String databaseName;
    private final Logger logger;

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> playerDataCollection;
    private MongoCollection<Document> addonDataCollection;

    public MongoDBProvider(String connectionString, String databaseName, Logger logger) {
        this.connectionString = connectionString;
        this.databaseName = databaseName;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                mongoClient = MongoClients.create(connectionString);
                database = mongoClient.getDatabase(databaseName);
                playerDataCollection = database.getCollection("player_data");
                addonDataCollection = database.getCollection("addon_data");

                // Test the connection by listing collection names
                database.listCollectionNames().first();

                logger.info("[Storage] Connected to MongoDB database: " + databaseName);
            } catch (Exception e) {
                logger.severe("[Storage] Failed to connect to MongoDB: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("MongoDB connection failed", e);
            }
        });
    }

    @Override
    public void disconnect() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                logger.info("[Storage] MongoDB connection closed.");
            } catch (Exception e) {
                logger.severe("[Storage] Error closing MongoDB connection: " + e.getMessage());
            }
        }
    }

    // ========== Player Accessory Data ==========

    @Override
    public CompletableFuture<Void> savePlayerAccessories(UUID playerUUID, Map<String, List<ItemStack>> accessories) {
        return CompletableFuture.runAsync(() -> {
            try {
                String base64 = ItemStackSerializer.serializeAccessories(accessories);
                playerDataCollection.updateOne(
                        Filters.eq("_id", playerUUID.toString()),
                        Updates.combine(
                                Updates.set("accessories", base64),
                                Updates.set("updated_at", new Date())
                        ),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception e) {
                logger.severe("[Storage] Failed to save player accessories for " + playerUUID + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, List<ItemStack>>> loadPlayerAccessories(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document doc = playerDataCollection.find(
                        Filters.eq("_id", playerUUID.toString())
                ).first();

                if (doc != null) {
                    String base64 = doc.getString("accessories");
                    return ItemStackSerializer.deserializeAccessories(base64);
                }
            } catch (Exception e) {
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
            try {
                // Use $set to update only the specific addon field without overwriting others
                addonDataCollection.updateOne(
                        Filters.eq("_id", playerUUID.toString()),
                        Updates.set("addons." + addonId, serializedData),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception e) {
                logger.severe("[Storage] Failed to save addon data (" + addonId + ") for " + playerUUID + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<String> loadAddonData(UUID playerUUID, String addonId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document doc = addonDataCollection.find(
                        Filters.eq("_id", playerUUID.toString())
                ).first();

                if (doc != null) {
                    Document addons = doc.get("addons", Document.class);
                    if (addons != null) {
                        return addons.getString(addonId);
                    }
                }
            } catch (Exception e) {
                logger.severe("[Storage] Failed to load addon data (" + addonId + ") for " + playerUUID + ": " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> deleteAddonData(UUID playerUUID, String addonId) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Use $unset to remove only the specific addon field
                addonDataCollection.updateOne(
                        Filters.eq("_id", playerUUID.toString()),
                        Updates.unset("addons." + addonId)
                );
            } catch (Exception e) {
                logger.severe("[Storage] Failed to delete addon data (" + addonId + ") for " + playerUUID + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, String>> loadAllAddonData(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> result = new HashMap<>();
            try {
                Document doc = addonDataCollection.find(
                        Filters.eq("_id", playerUUID.toString())
                ).first();

                if (doc != null) {
                    Document addons = doc.get("addons", Document.class);
                    if (addons != null) {
                        for (String key : addons.keySet()) {
                            Object value = addons.get(key);
                            if (value instanceof String) {
                                result.put(key, (String) value);
                            }
                        }
                    }
                }
            } catch (Exception e) {
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
        // MongoDB is schema-less: collections are created dynamically on first write.
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> upsertRow(String tableName, Map<String, Object> rowData, List<String> primaryKeys) {
        return CompletableFuture.runAsync(() -> {
            if (rowData == null || rowData.isEmpty()) return;
            
            MongoCollection<Document> collection = database.getCollection(tableName);
            
            org.bson.conversions.Bson filter;
            if (primaryKeys != null && !primaryKeys.isEmpty()) {
                List<org.bson.conversions.Bson> filters = new ArrayList<>();
                for (String pk : primaryKeys) {
                    filters.add(com.mongodb.client.model.Filters.eq(pk, rowData.get(pk)));
                }
                filter = com.mongodb.client.model.Filters.and(filters);
            } else {
                filter = com.mongodb.client.model.Filters.eq("_id", UUID.randomUUID().toString());
            }
            
            Document doc = new Document();
            for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                doc.put(entry.getKey(), entry.getValue());
            }
            
            try {
                collection.replaceOne(filter, doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
            } catch (Exception e) {
                logger.severe("[Storage] Failed to upsert row in MongoDB collection '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> selectRows(String tableName, Map<String, Object> whereConditions) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> results = new ArrayList<>();
            MongoCollection<Document> collection = database.getCollection(tableName);
            
            org.bson.conversions.Bson filter;
            if (whereConditions != null && !whereConditions.isEmpty()) {
                List<org.bson.conversions.Bson> filters = new ArrayList<>();
                for (Map.Entry<String, Object> entry : whereConditions.entrySet()) {
                    filters.add(com.mongodb.client.model.Filters.eq(entry.getKey(), entry.getValue()));
                }
                filter = com.mongodb.client.model.Filters.and(filters);
            } else {
                filter = new Document(); // empty filter matches all documents
            }
            
            try {
                for (Document doc : collection.find(filter)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> entry : doc.entrySet()) {
                        row.put(entry.getKey(), entry.getValue());
                    }
                    results.add(row);
                }
            } catch (Exception e) {
                logger.severe("[Storage] Failed to select rows from MongoDB collection '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
            }
            return results;
        });
    }

    @Override
    public CompletableFuture<Void> deleteRows(String tableName, Map<String, Object> whereConditions) {
        return CompletableFuture.runAsync(() -> {
            MongoCollection<Document> collection = database.getCollection(tableName);
            
            org.bson.conversions.Bson filter;
            if (whereConditions != null && !whereConditions.isEmpty()) {
                List<org.bson.conversions.Bson> filters = new ArrayList<>();
                for (Map.Entry<String, Object> entry : whereConditions.entrySet()) {
                    filters.add(com.mongodb.client.model.Filters.eq(entry.getKey(), entry.getValue()));
                }
                filter = com.mongodb.client.model.Filters.and(filters);
            } else {
                filter = new Document(); // empty filter matches all documents
            }
            
            try {
                collection.deleteMany(filter);
            } catch (Exception e) {
                logger.severe("[Storage] Failed to delete rows from MongoDB collection '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> dropTable(String tableName) {
        return CompletableFuture.runAsync(() -> {
            try {
                database.getCollection(tableName).drop();
            } catch (Exception e) {
                logger.severe("[Storage] Failed to drop MongoDB collection '" + tableName + "': " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
