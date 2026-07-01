package org.bg52.curiospaper.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Utility for migrating data from YAML flat files to the active database backend.
 * All methods run synchronously (blocking) — designed to be called during onEnable()
 * to prevent data races with player logins.
 */
public final class MigrationUtil {

    private MigrationUtil() {
        // Utility class
    }

    /**
     * Migrates core player accessory data from CuriosPaper's playerdata/ folder.
     * Reads UUID.yml files containing 'accessories' sections with serialized ItemStacks.
     * Successfully migrated files are renamed to UUID.yml.migrated.
     *
     * @param playerdataFolder the folder containing UUID.yml files
     * @param provider         the active StorageProvider to write data into
     * @param logger           logger for progress and error reporting
     * @return the number of successfully migrated files
     */
    public static int migratePlayerData(File playerdataFolder, StorageProvider provider, Logger logger) {
        if (playerdataFolder == null || !playerdataFolder.exists() || !playerdataFolder.isDirectory()) {
            logger.info("[Migration] No playerdata folder found. Skipping migration.");
            return 0;
        }

        File[] ymlFiles = playerdataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (ymlFiles == null || ymlFiles.length == 0) {
            logger.info("[Migration] No YAML player data files found. Skipping migration.");
            return 0;
        }

        logger.info("[Migration] Found " + ymlFiles.length + " player data files to migrate...");

        int migrated = 0;
        int failed = 0;

        for (File file : ymlFiles) {
            String fileName = file.getName().replace(".yml", "");

            // Parse UUID from filename
            UUID playerUUID;
            try {
                playerUUID = UUID.fromString(fileName);
            } catch (IllegalArgumentException e) {
                logger.warning("[Migration] Skipping file with invalid UUID name: " + file.getName());
                continue;
            }

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ConfigurationSection accessoriesSection = config.getConfigurationSection("accessories");

                if (accessoriesSection == null) {
                    logger.fine("[Migration] No accessories section in " + file.getName() + ", skipping.");
                    // Still rename — file has no useful data
                    renameToMigrated(file, logger);
                    migrated++;
                    continue;
                }

                // Load accessories into a map using Bukkit's deserialization
                Map<String, List<ItemStack>> accessories = new HashMap<>();
                for (String slotType : accessoriesSection.getKeys(false)) {
                    ConfigurationSection slotSection = accessoriesSection.getConfigurationSection(slotType);
                    if (slotSection == null) {
                        continue;
                    }

                    List<ItemStack> items = new ArrayList<>();

                    // Sort keys numerically
                    List<String> keys = new ArrayList<>(slotSection.getKeys(false));
                    keys.sort(Comparator.comparingInt(k -> {
                        try {
                            return Integer.parseInt(k);
                        } catch (NumberFormatException e) {
                            return Integer.MAX_VALUE;
                        }
                    }));

                    for (String key : keys) {
                        try {
                            int index = Integer.parseInt(key);
                            ItemStack item = slotSection.getItemStack(key);

                            while (items.size() <= index) {
                                items.add(null);
                            }
                            if (item != null) {
                                items.set(index, item);
                            }
                        } catch (NumberFormatException e) {
                            logger.warning("[Migration] Invalid item index '" + key + "' in " + file.getName());
                        }
                    }

                    accessories.put(slotType.toLowerCase(), items);
                }

                // Save to database (blocking call)
                provider.savePlayerAccessories(playerUUID, accessories).join();

                // Rename the file to mark it as migrated
                renameToMigrated(file, logger);
                migrated++;

                if (migrated % 50 == 0) {
                    logger.info("[Migration] Progress: " + migrated + "/" + ymlFiles.length + " player files migrated...");
                }
            } catch (Exception e) {
                logger.severe("[Migration] Failed to migrate " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
                failed++;
            }
        }

        logger.info("[Migration] Player data migration complete. Migrated: " + migrated + ", Failed: " + failed);
        return migrated;
    }

    /**
     * Migrates addon YAML data from any folder into the active database.
     * Reads UUID.yml files containing an 'addons' section with key-value string pairs.
     * This is the public API method — addons can call this to migrate their own YAML data.
     * Successfully migrated files are renamed to UUID.yml.migrated.
     *
     * @param yamlFolder the folder containing UUID.yml files with addon data
     * @param provider   the active StorageProvider to write data into
     * @param logger     logger for progress and error reporting
     * @return the number of successfully migrated files
     */
    public static int migrateAddonData(File yamlFolder, StorageProvider provider, Logger logger) {
        if (yamlFolder == null || !yamlFolder.exists() || !yamlFolder.isDirectory()) {
            logger.info("[Migration] No addon data folder found at: " +
                    (yamlFolder != null ? yamlFolder.getAbsolutePath() : "null"));
            return 0;
        }

        File[] ymlFiles = yamlFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (ymlFiles == null || ymlFiles.length == 0) {
            logger.info("[Migration] No YAML addon data files found in " + yamlFolder.getAbsolutePath());
            return 0;
        }

        logger.info("[Migration] Found " + ymlFiles.length + " addon data files to migrate from " +
                yamlFolder.getAbsolutePath() + "...");

        int migrated = 0;
        int failed = 0;

        for (File file : ymlFiles) {
            String fileName = file.getName().replace(".yml", "");

            // Parse UUID from filename
            UUID playerUUID;
            try {
                playerUUID = UUID.fromString(fileName);
            } catch (IllegalArgumentException e) {
                logger.warning("[Migration] Skipping file with invalid UUID name: " + file.getName());
                continue;
            }

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ConfigurationSection addonsSection = config.getConfigurationSection("addons");

                if (addonsSection == null) {
                    logger.fine("[Migration] No addons section in " + file.getName() + ", skipping.");
                    renameToMigrated(file, logger);
                    migrated++;
                    continue;
                }

                // Iterate addon keys and save each one
                for (String addonId : addonsSection.getKeys(false)) {
                    String data = addonsSection.getString(addonId);
                    if (data != null && !data.isEmpty()) {
                        provider.saveAddonData(playerUUID, addonId, data).join();
                    }
                }

                // Rename the file to mark it as migrated
                renameToMigrated(file, logger);
                migrated++;

                if (migrated % 50 == 0) {
                    logger.info("[Migration] Progress: " + migrated + "/" + ymlFiles.length + " addon files migrated...");
                }
            } catch (Exception e) {
                logger.severe("[Migration] Failed to migrate addon data from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
                failed++;
            }
        }

        logger.info("[Migration] Addon data migration complete. Migrated: " + migrated + ", Failed: " + failed);
        return migrated;
    }

    /**
     * Renames a file from UUID.yml to UUID.yml.migrated.
     */
    private static void renameToMigrated(File file, Logger logger) {
        File migrated = new File(file.getParentFile(), file.getName() + ".migrated");
        if (!file.renameTo(migrated)) {
            logger.warning("[Migration] Could not rename " + file.getName() + " to .migrated");
        }
    }
}
