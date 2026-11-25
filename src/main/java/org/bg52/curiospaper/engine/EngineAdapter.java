package org.bg52.curiospaper.engine;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Universal adapter interface for custom item engines.
 */
public interface EngineAdapter {

    String getEngineId();

    boolean isAvailable();

    void initialize();

    void shutdown();

    String getItemId(ItemStack item);

    ItemStack createItemStack(String itemId);

    boolean isCustomItem(ItemStack item);

    String resolveCuriosSlot(ItemStack item);

    default boolean shouldRenderModel(ItemStack item) {
        return false;
    }

    default java.util.Set<String> getRegisteredSlotTypes() {
        return java.util.Collections.emptySet();
    }

    default void onEquip(Player player, ItemStack item) {}

    default void onUnequip(Player player, ItemStack item) {}
}

