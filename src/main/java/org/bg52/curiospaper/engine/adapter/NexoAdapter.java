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
 * Adapter integrating CuriosPaper with the Nexo custom item engine.
 */
public class NexoAdapter implements EngineAdapter {

    private final CuriosPaper plugin;
    private boolean available = false;

    private Class<?> nexoItemsClass;
    private Method idFromItemMethod;
    private Method itemFromIdMethod;
    private Method existsMethod;

    public NexoAdapter(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getEngineId() {
        return "nexo";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void initialize() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            available = false;
            return;
        }

        try {
            nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            idFromItemMethod = nexoItemsClass.getMethod("idFromItem", ItemStack.class);
            itemFromIdMethod = nexoItemsClass.getMethod("itemFromId", String.class);
            existsMethod = nexoItemsClass.getMethod("exists", String.class);
            available = true;
            plugin.getLogger().info("[ItemEngine] Successfully linked with Nexo item engine!");
        } catch (Throwable t) {
            available = false;
            plugin.getLogger().warning("[ItemEngine] Could not link Nexo: " + t.getMessage());
        }
    }

    @Override
    public void shutdown() {
        available = false;
    }

    @Override
    public String getItemId(ItemStack item) {
        if (!available || item == null || idFromItemMethod == null) return null;
        try {
            Object result = idFromItemMethod.invoke(null, item);
            return result instanceof String ? (String) result : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public ItemStack createItemStack(String itemId) {
        if (!available || itemId == null || itemFromIdMethod == null) return null;
        try {
            Object builder = itemFromIdMethod.invoke(null, itemId);
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
            // Check nexo curios tag
            NamespacedKey slotKey = new NamespacedKey("nexo", "curios_slot");
            if (pdc.has(slotKey, PersistentDataType.STRING)) {
                return pdc.get(slotKey, PersistentDataType.STRING);
            }
        } catch (Throwable ignored) {}
        return null;
    }
}

