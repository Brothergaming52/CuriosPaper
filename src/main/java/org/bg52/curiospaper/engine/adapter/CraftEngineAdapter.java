package org.bg52.curiospaper.engine.adapter;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.engine.EngineAdapter;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;

/**
 * Adapter integrating CuriosPaper with the CraftEngine custom item engine.
 */
public class CraftEngineAdapter implements EngineAdapter {

    private final CuriosPaper plugin;
    private boolean available = false;

    private Class<?> craftEngineItemsClass;
    private Method getCustomItemIdMethod;
    private Method byIdMethod;

    public CraftEngineAdapter(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getEngineId() {
        return "craftengine";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void initialize() {
        if (!Bukkit.getPluginManager().isPluginEnabled("CraftEngine")) {
            available = false;
            return;
        }

        try {
            craftEngineItemsClass = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            getCustomItemIdMethod = craftEngineItemsClass.getMethod("getCustomItemId", ItemStack.class);
            try {
                byIdMethod = craftEngineItemsClass.getMethod("byId", String.class);
            } catch (NoSuchMethodException e) {
                // Some versions use Key
                Class<?> keyClass = Class.forName("net.momirealms.craftengine.core.util.Key");
                byIdMethod = craftEngineItemsClass.getMethod("byId", keyClass);
            }
            available = true;
            plugin.getLogger().info("[ItemEngine] Successfully linked with CraftEngine item engine!");
        } catch (Throwable t) {
            available = false;
            plugin.getLogger().warning("[ItemEngine] Could not link CraftEngine: " + t.getMessage());
        }
    }

    @Override
    public void shutdown() {
        available = false;
    }

    @Override
    public String getItemId(ItemStack item) {
        if (!available || item == null || getCustomItemIdMethod == null) return null;
        try {
            Object keyObj = getCustomItemIdMethod.invoke(null, item);
            if (keyObj != null) {
                Method asStringMethod = keyObj.getClass().getMethod("asString");
                Object str = asStringMethod.invoke(keyObj);
                return str instanceof String ? (String) str : keyObj.toString();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public ItemStack createItemStack(String itemId) {
        if (!available || itemId == null || byIdMethod == null) return null;
        try {
            Object itemObj;
            if (byIdMethod.getParameterTypes()[0] == String.class) {
                itemObj = byIdMethod.invoke(null, itemId);
            } else {
                Class<?> keyClass = Class.forName("net.momirealms.craftengine.core.util.Key");
                Method ofMethod = keyClass.getMethod("of", String.class);
                Object key = ofMethod.invoke(null, itemId);
                itemObj = byIdMethod.invoke(null, key);
            }

            if (itemObj instanceof ItemStack) {
                return ((ItemStack) itemObj).clone();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public boolean isCustomItem(ItemStack item) {
        return getItemId(item) != null;
    }

    @Override
    public String resolveCuriosSlot(ItemStack item) {
        if (!available || item == null || !item.hasItemMeta()) return null;
        try {
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            NamespacedKey slotKey = new NamespacedKey("craftengine", "curios_slot");
            if (pdc.has(slotKey, PersistentDataType.STRING)) {
                return pdc.get(slotKey, PersistentDataType.STRING);
            }
        } catch (Throwable ignored) {}
        return null;
    }
}

