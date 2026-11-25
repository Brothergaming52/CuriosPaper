package org.bg52.curiospaper.util;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modern Paper capability and data components utility supporting 1.20.6 through 1.21.x+.
 */
public final class VersionUtil {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static int major = 1;
    private static int minor = 20;
    private static int patch = 6;

    private static boolean dataComponentsSupported = false;
    private static Boolean itemModelSupported = null;
    private static Object typeGlider = null;
    private static Object typeEquippable = null;

    private static Method hasDataMethod = null;
    private static Method unsetDataMethod = null;
    private static Method resetDataMethod = null;
    private static Method setDataNonValuedMethod = null;
    private static Method getDataMethod = null;
    private static Method setDataValuedMethod = null;
    private static Method keyMethod = null;

    static {
        try {
            String v = Bukkit.getBukkitVersion();
            Matcher m = VERSION_PATTERN.matcher(v);
            if (m.find()) {
                major = Integer.parseInt(m.group(1));
                minor = Integer.parseInt(m.group(2));
                patch = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
            }
        } catch (Throwable ignored) {}

        initDataComponents();
    }

    private VersionUtil() {}

    private static void initDataComponents() {
        try {
            Class<?> dataComponentTypesClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            try {
                typeGlider = dataComponentTypesClass.getField("GLIDER").get(null);
            } catch (Throwable ignored) {}
            try {
                typeEquippable = dataComponentTypesClass.getField("EQUIPPABLE").get(null);
            } catch (Throwable ignored) {}

            Class<?> itemStackClass = ItemStack.class;
            Class<?> dataComponentTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType");
            hasDataMethod = itemStackClass.getMethod("hasData", dataComponentTypeClass);
            unsetDataMethod = itemStackClass.getMethod("unsetData", dataComponentTypeClass);
            try {
                resetDataMethod = itemStackClass.getMethod("resetData", dataComponentTypeClass);
            } catch (Throwable ignored) {}

            Class<?> nonValuedClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$NonValued");
            setDataNonValuedMethod = itemStackClass.getMethod("setData", nonValuedClass);

            Class<?> valuedClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
            getDataMethod = itemStackClass.getMethod("getData", valuedClass);
            setDataValuedMethod = itemStackClass.getMethod("setData", valuedClass, Object.class);

            Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");
            keyMethod = keyClass.getMethod("key", String.class, String.class);

            dataComponentsSupported = true;
        } catch (Throwable t) {
            dataComponentsSupported = false;
        }
    }

    public static boolean isDataComponentsSupported() {
        return dataComponentsSupported;
    }

    public static boolean supportsItemModel() {
        if (itemModelSupported == null) {
            try {
                ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
                itemModelSupported = true;
            } catch (Throwable t) {
                itemModelSupported = false;
            }
        }
        return itemModelSupported;
    }

    public static NamespacedKey parseNamespacedKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        NamespacedKey parsed = NamespacedKey.fromString(key.trim());
        if (parsed != null) {
            return parsed;
        }
        try {
            String[] parts = key.trim().split(":", 2);
            if (parts.length == 2) {
                return new NamespacedKey(parts[0], parts[1]);
            } else {
                return NamespacedKey.minecraft(key.trim());
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public static void setItemModelSafe(ItemMeta meta, String itemModel, Integer customModelData) {
        if (meta == null) return;

        boolean isNumericOnly = itemModel != null && itemModel.trim().matches("\\d+");
        if (supportsItemModel() && itemModel != null && !itemModel.trim().isEmpty() && !isNumericOnly) {
            try {
                NamespacedKey key = parseNamespacedKey(itemModel);
                if (key != null) {
                    Method method = ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
                    method.invoke(meta, key);
                }
            } catch (Throwable ignored) {}
        }

        if (customModelData != null) {
            try {
                meta.setCustomModelData(customModelData);
            } catch (Throwable ignored) {}
        } else if (isNumericOnly) {
            try {
                int cmd = Integer.parseInt(itemModel.trim());
                meta.setCustomModelData(cmd);
            } catch (Throwable ignored) {}
        }
    }

    public static void setItemModelSafe(ItemMeta meta, NamespacedKey itemModel, Integer customModelData) {
        if (meta == null) return;
        if (supportsItemModel() && itemModel != null) {
            try {
                Method method = ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
                method.invoke(meta, itemModel);
            } catch (Throwable ignored) {}
        }
        if (customModelData != null) {
            try {
                meta.setCustomModelData(customModelData);
            } catch (Throwable ignored) {}
        }
    }

    public static String getItemModelSafe(ItemMeta meta) {
        if (meta == null || !supportsItemModel()) return null;
        try {
            Method method = ItemMeta.class.getMethod("getItemModel");
            Object result = method.invoke(meta);
            return result != null ? result.toString() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Integer getCustomModelDataSafe(ItemMeta meta) {
        if (meta == null) return null;
        try {
            if (meta.hasCustomModelData()) {
                return meta.getCustomModelData();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static NamespacedKey getEquippableAsset(ItemStack item) {
        if (!dataComponentsSupported || item == null || getDataMethod == null || typeEquippable == null) {
            return null;
        }
        try {
            Object currentEquippable = getDataMethod.invoke(item, typeEquippable);
            if (currentEquippable != null) {
                Class<?> equippableClass = Class.forName("io.papermc.paper.datacomponent.item.Equippable");
                Method assetIdMethod = equippableClass.getMethod("assetId");
                Object keyObj = assetIdMethod.invoke(currentEquippable);
                if (keyObj != null) {
                    Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");
                    Method asStringMethod = keyClass.getMethod("asString");
                    String s = (String) asStringMethod.invoke(keyObj);
                    return parseNamespacedKey(s);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static String sanitizeAssetKey(String namespace, String key) {
        String combined = (namespace + "_" + key).toLowerCase(Locale.ROOT);
        return combined.replaceAll("[^a-z0-9_.-]", "_");
    }

    /**
     * Checks if the item has the GLIDER component active.
     */
    public static boolean hasGlider(ItemStack item) {
        if (!dataComponentsSupported || item == null || hasDataMethod == null || typeGlider == null) {
            return false;
        }
        try {
            return (boolean) hasDataMethod.invoke(item, typeGlider);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Enables or removes the GLIDER component from an item.
     */
    public static void setGlider(ItemStack item, boolean enabled) {
        if (!dataComponentsSupported || item == null || typeGlider == null) {
            return;
        }
        try {
            if (enabled && setDataNonValuedMethod != null) {
                setDataNonValuedMethod.invoke(item, typeGlider);
            } else if (!enabled && unsetDataMethod != null) {
                unsetDataMethod.invoke(item, typeGlider);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Applies elytra flight capabilities (GLIDER component + optional equippable asset) to a chestplate.
     */
    public static void applyElytraFlight(ItemStack chestplate, String assetNamespace, String assetKey) {
        if (!dataComponentsSupported || chestplate == null) return;
        try {
            // 1. Set Glider
            if (setDataNonValuedMethod != null && typeGlider != null) {
                setDataNonValuedMethod.invoke(chestplate, typeGlider);
            }

            // 2. Set Equippable asset if provided
            if (assetNamespace != null && assetKey != null && getDataMethod != null && setDataValuedMethod != null && keyMethod != null) {
                Object currentEquippable = getDataMethod.invoke(chestplate, typeEquippable);
                Class<?> equippableClass = Class.forName("io.papermc.paper.datacomponent.item.Equippable");
                Class<?> builderClass = Class.forName("io.papermc.paper.datacomponent.item.Equippable$Builder");
                Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");

                Object builder;
                if (currentEquippable != null) {
                    Method toBuilderMethod = equippableClass.getMethod("toBuilder");
                    builder = toBuilderMethod.invoke(currentEquippable);
                } else {
                    Method equippableFactory = equippableClass.getMethod("equippable", org.bukkit.inventory.EquipmentSlot.class);
                    builder = equippableFactory.invoke(null, org.bukkit.inventory.EquipmentSlot.CHEST);
                }

                if (builder != null) {
                    Object keyObj = keyMethod.invoke(null, assetNamespace, assetKey);
                    Method assetIdMethod = builderClass.getMethod("assetId", keyClass);
                    assetIdMethod.invoke(builder, keyObj);

                    Method buildMethod = builderClass.getMethod("build");
                    Object newEquippable = buildMethod.invoke(builder);

                    setDataValuedMethod.invoke(chestplate, typeEquippable, newEquippable);
                }
            }
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().warning("[CuriosPaper] Failed to apply elytra flight (" + assetNamespace + ":" + assetKey + "): " + t.getMessage());
        }
    }

    /**
     * Removes the GLIDER component from an item and resets original equippable asset.
     */
    public static void removeGlider(ItemStack item) {
        if (!dataComponentsSupported || item == null) return;
        setGlider(item, false);
        if (resetDataMethod != null && typeEquippable != null) {
            try {
                resetDataMethod.invoke(item, typeEquippable);
            } catch (Throwable ignored) {}
        }
    }

    public static int getMaxDurability(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Damageable dmgMeta) {
            try {
                if (dmgMeta.hasMaxDamage()) {
                    return dmgMeta.getMaxDamage();
                }
            } catch (Throwable ignored) {}
        }
        int max = item.getType().getMaxDurability();
        return max > 0 ? max : 432;
    }
}
