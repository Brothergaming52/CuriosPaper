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
 * Adapter integrating CuriosPaper with the Oraxen custom item engine.
 */
public class OraxenAdapter implements EngineAdapter {

    private final CuriosPaper plugin;
    private boolean available = false;

    private Class<?> oraxenItemsClass;
    private Method getIdByItemMethod;
    private Method getItemByIdMethod;

    public OraxenAdapter(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getEngineId() {
        return "oraxen";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void initialize() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Oraxen")) {
            available = false;
            return;
        }

        try {
            oraxenItemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            getIdByItemMethod = oraxenItemsClass.getMethod("getIdByItem", ItemStack.class);
            getItemByIdMethod = oraxenItemsClass.getMethod("getItemById", String.class);
            available = true;
            plugin.getLogger().info("[ItemEngine] Successfully linked with Oraxen item engine!");
        } catch (Throwable t) {
            available = false;
            plugin.getLogger().warning("[ItemEngine] Could not link Oraxen: " + t.getMessage());
        }
    }

    @Override
    public void shutdown() {
        available = false;
    }

    @Override
    public String getItemId(ItemStack item) {
        if (!available || item == null || getIdByItemMethod == null) return null;
        try {
            Object result = getIdByItemMethod.invoke(null, item);
            return result instanceof String ? (String) result : null;
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public ItemStack createItemStack(String itemId) {
        if (!available || itemId == null || getItemByIdMethod == null) return null;
        try {
            Object builder = getItemByIdMethod.invoke(null, itemId);
            if (builder != null) {
                Method buildMethod = builder.getClass().getMethod("build");
                Object itemObj = buildMethod.invoke(builder);
                if (itemObj instanceof ItemStack) {
                    return (ItemStack) itemObj;
                }
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
            NamespacedKey slotKey = new NamespacedKey("oraxen", "curios_slot");
            if (pdc.has(slotKey, PersistentDataType.STRING)) {
                return pdc.get(slotKey, PersistentDataType.STRING);
            }
        } catch (Throwable ignored) {}
        return null;
    }
}

