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

import java.util.*;

/**
 * Handles custom item drops from mobs.
 * At most ONE custom item is dropped per mob death to avoid flooding.
 * All matching candidates are collected, shuffled, and the first one
 * that passes its chance roll is selected.
 */
public class MobDropListener implements Listener {
  private final CuriosPaper plugin;
  private final ItemDataManager itemDataManager;
  private final Random random;
  private final Map<org.bukkit.entity.LivingEntity, org.bukkit.entity.ArmorStand> trackedModels;

  public MobDropListener(CuriosPaper plugin, ItemDataManager itemDataManager) {
    this.plugin = plugin;
    this.itemDataManager = itemDataManager;
    this.random = new Random();
    this.trackedModels = new java.util.WeakHashMap<>();

    // Repeating task to sync the passenger armor stand rotation
    org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      Iterator<Map.Entry<org.bukkit.entity.LivingEntity, org.bukkit.entity.ArmorStand>> it = trackedModels.entrySet()
          .iterator();
      while (it.hasNext()) {
        Map.Entry<org.bukkit.entity.LivingEntity, org.bukkit.entity.ArmorStand> entry = it.next();
        org.bukkit.entity.LivingEntity mob = entry.getKey();
        org.bukkit.entity.ArmorStand stand = entry.getValue();

        if (mob == null || stand == null || !mob.isValid() || !stand.isValid()
            || !mob.getPassengers().contains(stand)) {
          it.remove();
          continue;
        }

        // Sync yaw but keep pitch at 0 (flat)
        float targetYaw = mob.getLocation().getYaw();
        float currentYaw = stand.getLocation().getYaw();
        if (Math.abs(targetYaw - currentYaw) > 1.0f) {
          stand.setRotation(targetYaw, 0f);
        }
      }
    }, 20L, 5L); // Run every tick
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onEntityDeath(EntityDeathEvent event) {
    org.bukkit.entity.LivingEntity entity = event.getEntity();
    EntityType entityType = event.getEntityType();
    boolean debug = plugin.getConfig().getBoolean("debug.log-inventory-events", false);

    // Clean up passenger armor stand and check for equipped model
    org.bukkit.NamespacedKey equippedModelKey = new org.bukkit.NamespacedKey(plugin, "mob_equipped_model_item_id");
    org.bukkit.NamespacedKey standTagKey = new org.bukkit.NamespacedKey(plugin, "curios_mob_drop_stand");

    String equippedItemId = entity.getPersistentDataContainer().get(equippedModelKey,
        org.bukkit.persistence.PersistentDataType.STRING);

    // Remove the passenger stand if it exists
    for (org.bukkit.entity.Entity passenger : entity.getPassengers()) {
      if (passenger instanceof org.bukkit.entity.ArmorStand
          && passenger.getPersistentDataContainer().has(standTagKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
        passenger.remove();
      }
    }

    if (equippedItemId != null) {
      if (random.nextDouble() < 0.5) {
        ItemData itemData = itemDataManager.getItemData(equippedItemId);
        if (itemData != null) {
          MobDropData matchDrop = null;
          for (MobDropData mobDrop : itemData.getMobDrops()) {
            if (matchesEntityType(entityType, mobDrop.getEntityType()) && mobDrop.isModelEnabled()) {
              matchDrop = mobDrop;
              break;
            }
          }
          if (matchDrop != null) {
            ItemStack item = createItemStack(itemData, matchDrop);
            if (item != null) {
              org.bg52.curiospaper.event.CuriosMobDropEvent dropEvent = new org.bg52.curiospaper.event.CuriosMobDropEvent(entity, itemData.getItemId(), item);
              plugin.getServer().getPluginManager().callEvent(dropEvent);
              if (!dropEvent.isCancelled() && dropEvent.getItem() != null) {
                event.getDrops().add(dropEvent.getItem());
                if (debug)
                  plugin.getLogger().info("[MobDrop] => Dropped equipped model item: " + equippedItemId);
              } else if (debug) {
                plugin.getLogger().info("[MobDrop] => Drop cancelled: " + equippedItemId);
              }
              return;
            }
          }
        }
      }
      // If equipped with a 3D model, they don't drop any other custom item
      return;
    }

    // Collect all candidates that match this entity type
    List<CandidateItem> candidates = new ArrayList<>();
    for (ItemData itemData : itemDataManager.getAllItems().values()) {
      for (MobDropData mobDrop : itemData.getMobDrops()) {
        if (matchesEntityType(entityType, mobDrop.getEntityType())) {
          candidates.add(new CandidateItem(itemData, mobDrop));
        }
      }
    }

    if (candidates.isEmpty())
      return;

    if (debug) {
      plugin.getLogger().info("[MobDrop] " + candidates.size() + " candidate(s) for " + entityType.name());
    }

    // Shuffle to fairly distribute among candidates, then pick first success
    Collections.shuffle(candidates, random);
    for (CandidateItem c : candidates) {
      if (random.nextDouble() < c.mobDrop.getChance()) {
        ItemStack item = createItemStack(c.itemData, c.mobDrop);
        if (item != null) {
          org.bg52.curiospaper.event.CuriosMobDropEvent dropEvent = new org.bg52.curiospaper.event.CuriosMobDropEvent(entity, c.itemData.getItemId(), item);
          plugin.getServer().getPluginManager().callEvent(dropEvent);
          if (!dropEvent.isCancelled() && dropEvent.getItem() != null) {
            event.getDrops().add(dropEvent.getItem());
            if (debug) {
              plugin.getLogger().info("[MobDrop] => Selected: " + c.itemData.getItemId());
            }
          } else if (debug) {
            plugin.getLogger().info("[MobDrop] => Drop cancelled: " + c.itemData.getItemId());
          }
          return; // At most ONE custom drop per death
        }
      } else if (debug) {
        plugin.getLogger().info("[MobDrop] => Chance roll failed for: " + c.itemData.getItemId());
      }
    }

    if (debug) {
      plugin.getLogger().info("[MobDrop] => No item passed its chance roll");
    }
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onEntitySpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
    if (event.isCancelled())
      return;
    org.bukkit.entity.LivingEntity entity = event.getEntity();
    EntityType entityType = entity.getType();

    // Custom mobs might have model items configured
    for (ItemData itemData : itemDataManager.getAllItems().values()) {
      for (MobDropData mobDrop : itemData.getMobDrops()) {
        if (mobDrop.isModelEnabled() && matchesEntityType(entityType, mobDrop.getEntityType())) {
          if (mobDrop.getModelItem() == null)
            continue; // safety check

          if (random.nextDouble() < (mobDrop.getChance() * 2.0)) {
            equipModelOnMob(entity, itemData, mobDrop);
            return; // Equip at most one model
          }
        }
      }
    }
  }

  private void equipModelOnMob(org.bukkit.entity.LivingEntity mob, ItemData itemData, MobDropData mobDrop) {
    org.bukkit.NamespacedKey equippedModelKey = new org.bukkit.NamespacedKey(plugin, "mob_equipped_model_item_id");
    mob.getPersistentDataContainer().set(equippedModelKey, org.bukkit.persistence.PersistentDataType.STRING,
        itemData.getItemId());

    org.bukkit.World world = mob.getWorld();
    org.bukkit.Location loc = mob.getLocation();

    org.bukkit.entity.ArmorStand stand = world.spawn(loc, org.bukkit.entity.ArmorStand.class, as -> {
      as.setVisible(false);
      as.setBasePlate(false);
      as.setGravity(false);
      as.setInvulnerable(true);
      as.setArms(false);
      as.setSmall(false);
      as.setMarker(true);
      as.setSilent(true);
      as.setPersistent(false);
      as.setCanPickupItems(false);
      as.setCollidable(false);
    });

    org.bukkit.NamespacedKey standTagKey = new org.bukkit.NamespacedKey(plugin, "curios_mob_drop_stand");
    stand.getPersistentDataContainer().set(standTagKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

    org.bukkit.inventory.ItemStack modelHelmet = new org.bukkit.inventory.ItemStack(
        org.bukkit.Material.valueOf(mobDrop.getModelItem().toUpperCase()));
    org.bukkit.inventory.meta.ItemMeta meta = modelHelmet.getItemMeta();
    if (meta != null) {
      if (itemData.getDisplayName() != null) {
        meta.setDisplayName(itemData.getDisplayName());
      }
      org.bg52.curiospaper.util.VersionUtil.setItemModelSafe(meta, mobDrop.getModelItemModel(),
          mobDrop.getModelCustomModelData());
      modelHelmet.setItemMeta(meta);
    }

    org.bukkit.inventory.EntityEquipment equip = stand.getEquipment();
    if (equip != null) {
      equip.setHelmet(modelHelmet);
    }

    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (mob.isValid() && stand.isValid()) {
        mob.addPassenger(stand);
        trackedModels.put(mob, stand);
      }
    }, 1L);
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

  // ==================== HELPER CLASS ====================

  private static class CandidateItem {
    final ItemData itemData;
    final MobDropData mobDrop;

    CandidateItem(ItemData itemData, MobDropData mobDrop) {
      this.itemData = itemData;
      this.mobDrop = mobDrop;
    }
  }
}
