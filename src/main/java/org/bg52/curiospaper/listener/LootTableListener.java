package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.LootTableData;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;

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

    @EventHandler(priority = EventPriority.HIGH)
    public void onLootGenerate(LootGenerateEvent event) {
        LootContext context = event.getLootContext();

        if (context == null || event.getLootTable() == null) {
            return;
        }

        String lootTableKey = event.getLootTable().getKey().getKey();

        // Get all items that should be in this loot table
        List<ItemStack> customDrops = new ArrayList<>();

        for (ItemData itemData : itemDataManager.getAllItems().values()) {
            for (LootTableData lootData : itemData.getLootTables()) {
                if (matchesLootTable(lootTableKey, lootData.getLootTableType())) {
                    // Roll for drop chance
                    if (random.nextDouble() < lootData.getChance()) {
                        ItemStack item = createItemStack(itemData, lootData);
                        if (item != null) {
                            customDrops.add(item);
                        }
                    }
                }
            }
        }

        // Add custom drops to the loot
        if (!customDrops.isEmpty()) {
            Collection<ItemStack> loot = event.getLoot();
            loot.addAll(customDrops);

            if (plugin.getConfig().getBoolean("debug.log-inventory-events", false)) {
                plugin.getLogger().info("Added " + customDrops.size() + " custom items to loot table: " + lootTableKey);
            }
        }
    }

    /**
     * Checks if a loot table key matches the configured loot table type
     */
    private boolean matchesLootTable(String lootTableKey, String configuredType) {
        // Normalize both strings for comparison
        String normalizedKey = lootTableKey.toLowerCase().replace("_", "");
        String normalizedType = configuredType.toLowerCase().replace("_", "");

        // Direct match
        if (normalizedKey.equals(normalizedType)) {
            return true;
        }

        // Partial match (e.g., "abandoned_mineshaft" contains "mineshaft")
        if (normalizedKey.contains(normalizedType) || normalizedType.contains(normalizedKey)) {
            return true;
        }

        // Check for common loot table prefixes
        // e.g., "chests/simple_dungeon" should match "dungeon"
        if (lootTableKey.contains("/")) {
            String[] parts = lootTableKey.split("/");
            for (String part : parts) {
                if (matchesLootTable(part, configuredType)) {
                    return true;
                }
            }
        }

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
                item.editMeta(meta -> {
                    meta.setDisplayName(itemData.getDisplayName());
                    if (!itemData.getLore().isEmpty()) {
                        meta.setLore(itemData.getLore());
                    }
                    // Apply item model if specified
                    if (itemData.getItemModel() != null && !itemData.getItemModel().isEmpty()) {
                        meta.setItemModel(org.bukkit.NamespacedKey.fromString(itemData.getItemModel()));
                    }
                });
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
