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
 * Adapter integrating CuriosPaper with the ItemsAdder custom item engine.
 */
public class ItemsAdderAdapter implements EngineAdapter {

    private final CuriosPaper plugin;
    private boolean available = false;

    private Class<?> customStackClass;
    private Method byItemStackMethod;
    private Method getInstanceMethod;
    private Method getNamespacedIDMethod;
    private Method getItemStackMethod;

    public ItemsAdderAdapter(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getEngineId() {
        return "itemsadder";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void initialize() {
        if (!Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            available = false;
            return;
        }

        try {
            customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            byItemStackMethod = customStackClass.getMethod("byItemStack", ItemStack.class);
            getInstanceMethod = customStackClass.getMethod("getInstance", String.class);
            getNamespacedIDMethod = customStackClass.getMethod("getNamespacedID");
            getItemStackMethod = customStackClass.getMethod("getItemStack");
            available = true;
            plugin.getLogger().info("[ItemEngine] Successfully linked with ItemsAdder item engine!");
        } catch (Throwable t) {
            available = false;
            plugin.getLogger().warning("[ItemEngine] Could not link ItemsAdder: " + t.getMessage());
        }
    }

    @Override
    public void shutdown() {
        available = false;
    }

    @Override
    public String getItemId(ItemStack item) {
        if (!available || item == null || byItemStackMethod == null) return null;
        try {
            Object stack = byItemStackMethod.invoke(null, item);
            if (stack != null && getNamespacedIDMethod != null) {
                Object id = getNamespacedIDMethod.invoke(stack);
                return id instanceof String ? (String) id : null;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public ItemStack createItemStack(String itemId) {
        if (!available || itemId == null || getInstanceMethod == null) return null;
        try {
            Object stack = getInstanceMethod.invoke(null, itemId);
            if (stack != null && getItemStackMethod != null) {
                Object itemObj = getItemStackMethod.invoke(stack);
                if (itemObj instanceof ItemStack) {
                    return ((ItemStack) itemObj).clone();
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
            NamespacedKey slotKey = new NamespacedKey("itemsadder", "curios_slot");
            if (pdc.has(slotKey, PersistentDataType.STRING)) {
                return pdc.get(slotKey, PersistentDataType.STRING);
            }
        } catch (Throwable ignored) {}
        return null;
    }
}

