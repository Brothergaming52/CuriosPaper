package org.bg52.curiospaper.util;

import org.bukkit.Bukkit;

/**
 * Utility class for detecting Minecraft server version and feature
 * availability.
 */
public class VersionUtil {

    private static int majorVersion = -1;
    private static int minorVersion = -1;
    private static int patchVersion = -1;
    private static Boolean supportsItemModel = null;
    private static Boolean supportsDataComponents = null;
    private static Boolean supportsSmithingTemplate = null;

    static {
        parseVersion();
    }

    /**
     * Parse the server version from Bukkit.getVersion()
     * Examples: "1.14.4", "1.21.3", "1.20.1"
     */
    private static void parseVersion() {
        try {
            String versionString = Bukkit.getBukkitVersion();
            // Format: "1.21.3-R0.1-SNAPSHOT" or "1.14.4-R0.1-SNAPSHOT"
            String[] parts = versionString.split("-")[0].split("\\.");

            if (parts.length >= 2) {
                majorVersion = Integer.parseInt(parts[0]);
                minorVersion = Integer.parseInt(parts[1]);
            }
            if (parts.length >= 3) {
                patchVersion = Integer.parseInt(parts[2]);
            } else {
                patchVersion = 0;
            }
        } catch (Exception e) {
            // Fallback to safe defaults if parsing fails
            majorVersion = 1;
            minorVersion = 14;
            patchVersion = 0;
        }
    }

    /**
     * Get the major version number (e.g., 1 from "1.21.3")
     */
    public static int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Get the minor version number (e.g., 21 from "1.21.3")
     */
    public static int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Get the patch version number (e.g., 3 from "1.21.3")
     */
    public static int getPatchVersion() {
        return patchVersion;
    }

    /**
     * Check if the server is running at least the specified version.
     * 
     * @param major Major version (usually 1)
     * @param minor Minor version (e.g., 21 for 1.21)
     * @param patch Patch version (e.g., 3 for 1.21.3)
     * @return true if server version >= specified version
     */
    public static boolean isAtLeast(int major, int minor, int patch) {
        if (majorVersion > major)
            return true;
        if (majorVersion < major)
            return false;
        if (minorVersion > minor)
            return true;
        if (minorVersion < minor)
            return false;
        return patchVersion >= patch;
    }

    /**
     * Check if the server is running at least the specified version.
     * 
     * @param major Major version (usually 1)
     * @param minor Minor version (e.g., 21 for 1.21)
     * @return true if server version >= specified version
     */
    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }

    /**
     * Check if the server supports ItemMeta.setItemModel(NamespacedKey)
     * This was added in MC 1.21.3 (Paper)
     */
    public static boolean supportsItemModel() {
        if (supportsItemModel == null) {
            if (!isAtLeast(1, 21, 3)) {
                supportsItemModel = false;
            } else {
                // Additional runtime check via reflection
                try {
                    Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
                    itemMetaClass.getMethod("setItemModel", org.bukkit.NamespacedKey.class);
                    supportsItemModel = true;
                } catch (Exception e) {
                    supportsItemModel = false;
                }
            }
        }
        return supportsItemModel;
    }

    /**
     * Check if the server supports DataComponentTypes (Paper 1.21+)
     * This is required for GLIDER and EQUIPPABLE components
     */
    public static boolean supportsDataComponents() {
        if (supportsDataComponents == null) {
            if (!isAtLeast(1, 21, 0)) {
                supportsDataComponents = false;
            } else {
                // Additional runtime check via reflection
                try {
                    Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
                    supportsDataComponents = true;
                } catch (Exception e) {
                    supportsDataComponents = false;
                }
            }
        }
        return supportsDataComponents;
    }

    /**
     * Check if the server supports SmithingTransformRecipe (1.20+)
     * with template slot. Older versions use SmithingRecipe with only
     * base + addition.
     */
    public static boolean supportsSmithingTemplate() {
        if (supportsSmithingTemplate == null) {
            if (!isAtLeast(1, 20)) {
                supportsSmithingTemplate = false;
            } else {
                try {
                    Class.forName("org.bukkit.inventory.SmithingTransformRecipe");
                    supportsSmithingTemplate = true;
                } catch (Exception e) {
                    supportsSmithingTemplate = false;
                }
            }
        }
        return supportsSmithingTemplate;
    }

    /**
     * Get a formatted version string for logging
     */
    public static String getVersionString() {
        return majorVersion + "." + minorVersion + "." + patchVersion;
    }

    /**
     * Parse a namespaced key string into a NamespacedKey object.
     * Compatible with Spigot 1.14+ which doesn't have NamespacedKey.fromString()
     * 
     * @param key The key string in format "namespace:key" or just "key" (defaults
     *            to minecraft namespace)
     * @return The NamespacedKey, or null if parsing fails
     */
    @SuppressWarnings("deprecation")
    public static org.bukkit.NamespacedKey parseNamespacedKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        try {
            String[] parts = key.split(":", 2);
            if (parts.length == 2) {
                return new org.bukkit.NamespacedKey(parts[0], parts[1]);
            } else {
                // No namespace, default to minecraft
                return org.bukkit.NamespacedKey.minecraft(key);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Set item model on ItemMeta in a version-safe way.
     * Uses setItemModel(NamespacedKey) on 1.21.3+, falls back to setCustomModelData
     * on older versions.
     * 
     * @param meta            The ItemMeta to modify
     * @param itemModel       The item model string (namespace:key format)
     * @param customModelData The CustomModelData value for older versions (can be
     *                        null)
     */
    public static void setItemModelSafe(org.bukkit.inventory.meta.ItemMeta meta, String itemModel,
            Integer customModelData) {
        if (meta == null)
            return;

        if (supportsItemModel() && itemModel != null && !itemModel.trim().isEmpty()) {
            try {
                org.bukkit.NamespacedKey key = parseNamespacedKey(itemModel);
                if (key != null) {
                    // Use the interface class for reflection to ensure compatibility
                    Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
                    java.lang.reflect.Method method = itemMetaClass.getMethod("setItemModel",
                            org.bukkit.NamespacedKey.class);
                    method.invoke(meta, key);
                    return;
                }
            } catch (Exception e) {
                // Log and fall through to CustomModelData
                org.bukkit.Bukkit.getLogger()
                        .warning("[CuriosPaper] Failed to set item model via reflection: " + e.getMessage());
            }
        }

        // Fallback: Use CustomModelData
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        } else if (itemModel != null) {
            // Try to parse itemModel string as integer for CustomModelData
            try {
                int cmd = Integer.parseInt(itemModel);
                meta.setCustomModelData(cmd);
            } catch (NumberFormatException e) {
                // Cannot convert to CustomModelData, skip
            }
        }
    }

    /**
     * Set item model on ItemMeta using a NamespacedKey directly (for slot
     * configurations).
     * 
     * @param meta            The ItemMeta to modify
     * @param itemModel       The NamespacedKey item model
     * @param customModelData The CustomModelData value for older versions (can be
     *                        null)
     */
    // Reflection Cache
    private static Class<?> dataComponentTypesClass;
    private static Object typeGlider; // DataComponentType.NonValued
    private static Object typeEquippable; // DataComponentType.Valued<Equippable>
    private static java.lang.reflect.Method getDataMethod; // getData(DataComponentType.Valued)
    private static java.lang.reflect.Method setDataValuedMethod; // setData(DataComponentType.Valued, Object)
    private static java.lang.reflect.Method setDataNonValuedMethod; // setData(DataComponentType.NonValued)
    private static java.lang.reflect.Method unsetDataMethod; // unsetData(DataComponentType)
    private static java.lang.reflect.Method hasDataMethod; // hasData(DataComponentType)
    private static java.lang.reflect.Method resetDataMethod; // resetData(DataComponentType)
    private static java.lang.reflect.Method keyMethod;
    private static boolean dataComponentsInitAttempted = false;

    /**
     * Tries to initialize reflection for Data Components (1.21+ functionality).
     * GLIDER is a NonValued type (single-arg setData), EQUIPPABLE is Valued
     * (two-arg setData).
     */
    private static void initDataComponents() {
        if (dataComponentsInitAttempted)
            return;
        dataComponentsInitAttempted = true;
        if (!supportsDataComponents())
            return;
        try {
            dataComponentTypesClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            typeGlider = dataComponentTypesClass.getField("GLIDER").get(null);
            typeEquippable = dataComponentTypesClass.getField("EQUIPPABLE").get(null);

            Class<?> itemStackClass = org.bukkit.inventory.ItemStack.class;

            // Base type for hasData / unsetData / resetData
            Class<?> dataComponentTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType");
            hasDataMethod = itemStackClass.getMethod("hasData", dataComponentTypeClass);
            unsetDataMethod = itemStackClass.getMethod("unsetData", dataComponentTypeClass);
            resetDataMethod = itemStackClass.getMethod("resetData", dataComponentTypeClass);

            // Valued subtype for getData / setData with value
            Class<?> valuedClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
            getDataMethod = itemStackClass.getMethod("getData", valuedClass);
            setDataValuedMethod = itemStackClass.getMethod("setData", valuedClass, Object.class);

            // NonValued subtype for setData without value (unit types like GLIDER)
            Class<?> nonValuedClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$NonValued");
            setDataNonValuedMethod = itemStackClass.getMethod("setData", nonValuedClass);

            // Key factory
            Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");
            keyMethod = keyClass.getMethod("key", String.class, String.class);

        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger()
                    .warning("[CuriosPaper] Failed to init data components via reflection: " + e.getMessage());
            supportsDataComponents = false;
        }
    }

    public static boolean hasGlider(org.bukkit.inventory.ItemStack item) {
        if (!supportsDataComponents() || item == null)
            return false;
        try {
            initDataComponents();
            if (hasDataMethod == null)
                return false;
            return (boolean) hasDataMethod.invoke(item, typeGlider);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setGlider(org.bukkit.inventory.ItemStack item, boolean enabled) {
        if (!supportsDataComponents() || item == null)
            return;
        try {
            initDataComponents();
            if (enabled) {
                // GLIDER is NonValued — single-arg setData
                if (setDataNonValuedMethod == null)
                    return;
                setDataNonValuedMethod.invoke(item, typeGlider);
            } else {
                if (unsetDataMethod == null)
                    return;
                unsetDataMethod.invoke(item, typeGlider);
            }
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger()
                    .warning("[CuriosPaper] Failed to set/unset glider: " + e.getMessage());
        }
    }

    /**
     * Applies the Glider component and sets the Equippable asset to make it look
     * like elytra.
     */
    public static void applyElytraFlight(org.bukkit.inventory.ItemStack chestplate, String assetNamespace,
            String assetKey) {
        if (!supportsDataComponents() || chestplate == null)
            return;
        try {
            initDataComponents();

            // 1. Set Glider — GLIDER is NonValued, just mark it present
            if (setDataNonValuedMethod == null) {
                org.bukkit.Bukkit.getLogger()
                        .warning("[CuriosPaper] setDataNonValuedMethod is null, cannot apply glider");
                return;
            }
            setDataNonValuedMethod.invoke(chestplate, typeGlider);

            // 2. Set Equippable asset ID to show wings model
            // Equippable equippable = chestplate.getData(DataComponentTypes.EQUIPPABLE);
            // Equippable built = equippable.toBuilder().assetId(Key.key(ns, key)).build();
            // chestplate.setData(DataComponentTypes.EQUIPPABLE, built);
            if (getDataMethod == null || setDataValuedMethod == null)
                return;

            Object currentEquippable = getDataMethod.invoke(chestplate, typeEquippable);
            if (currentEquippable != null) {
                // Use the public interface classes, not the package-private impl classes
                Class<?> equippableClass = Class.forName("io.papermc.paper.datacomponent.item.Equippable");
                Class<?> builderClass = Class.forName("io.papermc.paper.datacomponent.item.Equippable$Builder");

                java.lang.reflect.Method toBuilder = equippableClass.getMethod("toBuilder");
                Object builder = toBuilder.invoke(currentEquippable);

                Object key = keyMethod.invoke(null, assetNamespace, assetKey);
                java.lang.reflect.Method assetIdMethod = builderClass.getMethod("assetId",
                        Class.forName("net.kyori.adventure.key.Key"));
                assetIdMethod.invoke(builder, key);

                java.lang.reflect.Method buildMethod = builderClass.getMethod("build");
                Object newEquippable = buildMethod.invoke(builder);

                setDataValuedMethod.invoke(chestplate, typeEquippable, newEquippable);
            }

        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger()
                    .warning("[CuriosPaper] Failed to apply elytra flight: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void removeGlider(org.bukkit.inventory.ItemStack chestplate) {
        if (!supportsDataComponents() || chestplate == null)
            return;
        try {
            initDataComponents();

            // Unset the GLIDER component
            if (unsetDataMethod == null) {
                org.bukkit.Bukkit.getLogger()
                        .warning("[CuriosPaper] unsetDataMethod is null, cannot remove glider");
                return;
            }
            unsetDataMethod.invoke(chestplate, typeGlider);

            // Reset Equippable to the item type's prototype default
            // This restores the original asset ID (e.g. diamond_chestplate instead of
            // elytra wings)
            if (resetDataMethod != null) {
                resetDataMethod.invoke(chestplate, typeEquippable);
            }

        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger()
                    .warning("[CuriosPaper] Failed to remove glider: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void setItemModelSafe(org.bukkit.inventory.meta.ItemMeta meta, org.bukkit.NamespacedKey itemModel,
            Integer customModelData) {
        if (meta == null)
            return;

        if (supportsItemModel() && itemModel != null) {
            try {
                // Use the interface class for reflection to ensure compatibility
                Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
                java.lang.reflect.Method method = itemMetaClass.getMethod("setItemModel",
                        org.bukkit.NamespacedKey.class);
                method.invoke(meta, itemModel);
                return;
            } catch (Exception e) {
                // Log and fall through to CustomModelData
                org.bukkit.Bukkit.getLogger().warning(
                        "[CuriosPaper] Failed to set item model (NamespacedKey) via reflection: " + e.getMessage());
            }
        }

        // Fallback to CustomModelData
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
    }
}
