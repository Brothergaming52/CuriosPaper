package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.LootTableData;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Material;
// import org.bukkit.event.EventHandler;
// import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
// import org.bukkit.event.world.LootGenerateEvent; (1.15+)
import org.bukkit.inventory.ItemStack;
// import org.bukkit.loot.LootContext; (1.15+)

import java.util.*;

/**
 * Handles injection of custom items into loot tables
 */
public class LootTableListener implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final Random random;

    public LootTableListener(CuriosPaper plugin, ItemDataManager itemDataManager) {
        this.plugin = plugin;
        this.itemDataManager = itemDataManager;
        this.random = new Random();
    }

    /*
     * @EventHandler(priority = EventPriority.HIGH)
     * public void onLootGenerate(org.bukkit.event.Event event) {
     * // Disabled for 1.14 compatibility (LootGenerateEvent is 1.15+)
     * // If updating to 1.15+, uncomment and fix imports.
     * // LootContext context = event.getLootContext();
     * // if (context == null || event.getLootTable() == null) return;
     * // // ... implementation ...
     * }
     */

    /**
     * Checks if a loot table key matches the configured loot table type
     */
    private boolean matchesLootTable(String lootTableKey, String configuredType) {
        return false;
    }

    /**
     * Creates an ItemStack from ItemData and LootTableData
     */
    private ItemStack createItemStack(ItemData itemData, LootTableData lootData) {
        try {
            Material material = Material.valueOf(itemData.getMaterial().toUpperCase());

            // Calculate random amount
            int amount = lootData.getMinAmount();
            if (lootData.getMaxAmount() > lootData.getMinAmount()) {
                amount = random.nextInt(lootData.getMaxAmount() - lootData.getMinAmount() + 1)
                        + lootData.getMinAmount();
            }

            ItemStack item = new ItemStack(material, amount);

            // Set display name, lore, and item model
            if (itemData.getDisplayName() != null) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(itemData.getDisplayName());
                    if (!itemData.getLore().isEmpty()) {
                        meta.setLore(itemData.getLore());
                    }
                    // Apply item model if specified (version-aware)
                    if (itemData.getItemModel() != null && !itemData.getItemModel().isEmpty()) {
                        org.bg52.curiospaper.util.VersionUtil.setItemModelSafe(meta, itemData.getItemModel(),
                                itemData.getCustomModelData());
                    }
                    item.setItemMeta(meta);
                }
            }

            // Tag the item for the appropriate slot if specified
            if (itemData.getSlotType() != null && !itemData.getSlotType().isEmpty()) {
                item = plugin.getCuriosPaperAPI().tagAccessoryItem(item, itemData.getSlotType());
            }

            return item;
        } catch (IllegalArgumentException e) {
            plugin.getLogger()
                    .warning("Invalid material '" + itemData.getMaterial() + "' for item " + itemData.getItemId());
            return null;
        }
    }
}
