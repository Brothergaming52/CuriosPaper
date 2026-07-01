package org.bg52.curiospaper.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.StringReader;
import java.util.*;

/**
 * Utility for serializing/deserializing ItemStacks to/from Base64 strings.
 * Uses Bukkit's YamlConfiguration as the intermediate format, which natively
 * handles all item serialization including enchantments, PDC data, NBT, etc.
 *
 * The flow is: ItemStack -> YamlConfiguration -> YAML string -> Base64
 */
public final class ItemStackSerializer {

    private ItemStackSerializer() {
        // Utility class
    }

    /**
     * Serializes a player's entire accessories map to a single Base64 string.
     * Format: YAML with sections for each slot type, each containing indexed items.
     *
     * @param accessories map of slotType -> list of ItemStacks
     * @return Base64-encoded string, or null if the map is empty
     */
    public static String serializeAccessories(Map<String, List<ItemStack>> accessories) {
        if (accessories == null || accessories.isEmpty()) {
            return null;
        }

        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, List<ItemStack>> entry : accessories.entrySet()) {
            String slotType = entry.getKey();
            List<ItemStack> items = entry.getValue();

            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    config.set("accessories." + slotType + "." + i, item);
                }
            }
        }

        String yaml = config.saveToString();
        if (yaml.trim().isEmpty()) {
            return null;
        }

        return Base64.getEncoder().encodeToString(yaml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Deserializes a Base64 string back into a player's accessories map.
     *
     * @param base64 the Base64-encoded YAML string
     * @return map of slotType -> list of ItemStacks, or empty map if input is null/empty
     */
    public static Map<String, List<ItemStack>> deserializeAccessories(String base64) {
        Map<String, List<ItemStack>> accessories = new HashMap<>();

        if (base64 == null || base64.trim().isEmpty()) {
            return accessories;
        }

        try {
            String yaml = new String(
                    Base64.getDecoder().decode(base64),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            YamlConfiguration config = new YamlConfiguration();
            config.load(new StringReader(yaml));

            org.bukkit.configuration.ConfigurationSection accessoriesSection =
                    config.getConfigurationSection("accessories");
            if (accessoriesSection == null) {
                return accessories;
            }

            for (String slotType : accessoriesSection.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection slotSection =
                        accessoriesSection.getConfigurationSection(slotType);
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

                        // Ensure list is large enough
                        while (items.size() <= index) {
                            items.add(null);
                        }
                        items.set(index, item);
                    } catch (NumberFormatException e) {
                        // Skip invalid keys
                    }
                }

                accessories.put(slotType.toLowerCase(), items);
            }
        } catch (Exception e) {
            // Return whatever we managed to parse
        }

        return accessories;
    }

    /**
     * Serializes a single ItemStack to a Base64 string.
     *
     * @param item the ItemStack to serialize
     * @return Base64-encoded string, or null if item is null/air
     */
    public static String itemToBase64(ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return null;
        }

        YamlConfiguration config = new YamlConfiguration();
        config.set("item", item);
        String yaml = config.saveToString();
        return Base64.getEncoder().encodeToString(yaml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Deserializes a single ItemStack from a Base64 string.
     *
     * @param base64 the Base64-encoded YAML string
     * @return the ItemStack, or null if deserialization fails
     */
    public static ItemStack itemFromBase64(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }

        try {
            String yaml = new String(
                    Base64.getDecoder().decode(base64),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            YamlConfiguration config = new YamlConfiguration();
            config.load(new StringReader(yaml));
            return config.getItemStack("item");
        } catch (Exception e) {
            return null;
        }
    }
}
