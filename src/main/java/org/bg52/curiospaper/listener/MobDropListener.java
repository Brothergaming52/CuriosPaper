package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.MobDropData;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles custom item drops from mobs
 */
public class MobDropListener implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final Random random;

    public MobDropListener(CuriosPaper plugin, ItemDataManager itemDataManager) {
        this.plugin = plugin;
        this.itemDataManager = itemDataManager;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        EntityType entityType = event.getEntityType();

        // Get all items that should drop from this mob
        List<ItemStack> customDrops = new ArrayList<>();

        for (ItemData itemData : itemDataManager.getAllItems().values()) {
            for (MobDropData mobDrop : itemData.getMobDrops()) {
                if (matchesEntityType(entityType, mobDrop.getEntityType())) {
                    // Roll for drop chance
                    if (random.nextDouble() < mobDrop.getChance()) {
                        ItemStack item = createItemStack(itemData, mobDrop);
                        if (item != null) {
                            customDrops.add(item);
                        }
                    }
                }
            }
        }

        // Add custom drops to the mob's drops
        if (!customDrops.isEmpty()) {
            event.getDrops().addAll(customDrops);

            if (plugin.getConfig().getBoolean("debug.log-inventory-events", false)) {
                plugin.getLogger().info("Added " + customDrops.size() + " custom drops to " + entityType.name());
            }
        }
    }

    /**
     * Checks if an entity type matches the configured entity type
     */
    private boolean matchesEntityType(EntityType actual, String configured) {
        try {
            EntityType configuredType = EntityType.valueOf(configured.toUpperCase());
            return actual == configuredType;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Creates an ItemStack from ItemData and MobDropData
     */
    private ItemStack createItemStack(ItemData itemData, MobDropData mobDrop) {
        try {
            Material material = Material.valueOf(itemData.getMaterial().toUpperCase());

            // Calculate random amount
            int amount = mobDrop.getMinAmount();
            if (mobDrop.getMaxAmount() > mobDrop.getMinAmount()) {
                amount = random.nextInt(mobDrop.getMaxAmount() - mobDrop.getMinAmount() + 1) + mobDrop.getMinAmount();
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
