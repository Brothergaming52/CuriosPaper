//import org.bukkit.event.world.LootGenerateEvent cannot be resolvedpackage org.bg52.curiospaper.listener;
package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.AbilityData;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Listens for AccessoryEquipEvent and applies/removes abilities based on
 * equipped items
 */
public class AbilityListener implements Listener {
  private final CuriosPaper plugin;
  private final ItemDataManager itemDataManager;
  private final Map<UUID, Set<String>> activeModifiers; // player UUID -> set of modifier IDs
  private BukkitRunnable whileEquippedTask;

  private static final String MODIFIER_PREFIX = "curiospaper_ability_";

  public AbilityListener(CuriosPaper plugin) {
    this.plugin = plugin;
    this.itemDataManager = plugin.getItemDataManager();
    this.activeModifiers = new HashMap<>();
    startWhileEquippedTask();
    startModifierReconciliationTask();
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onAccessoryEquip(AccessoryEquipEvent event) {
    Player player = event.getPlayer();
    ItemStack previousItem = event.getPreviousItem();
    ItemStack newItem = event.getNewItem();

    // Remove abilities from previous item (if any)
    if (previousItem != null && previousItem.getType() != org.bukkit.Material.AIR) {
      String prevItemId = getItemId(previousItem);
      if (prevItemId != null) {
        ItemData prevData = itemDataManager.getItemData(prevItemId);
        if (prevData != null) {
          removeAbilities(player, prevData, AbilityData.TriggerType.EQUIP);
          removeAbilities(player, prevData, AbilityData.TriggerType.WHILE_EQUIPPED);
          // Apply DE_EQUIP trigger
          applyAbilities(player, prevData, AbilityData.TriggerType.DE_EQUIP);
        }
      }
    }

    // Apply abilities from new item (if any)
    if (newItem != null && newItem.getType() != org.bukkit.Material.AIR) {
      String newItemId = getItemId(newItem);
      if (newItemId != null) {
        ItemData newData = itemDataManager.getItemData(newItemId);
        if (newData != null) {
          // Apply EQUIP trigger
          applyAbilities(player, newData, AbilityData.TriggerType.EQUIP);
        }
      }
    }
  }

  /**
   * Applies abilities with the specified trigger type
   */
  private void applyAbilities(Player player, ItemData itemData, AbilityData.TriggerType trigger) {
    for (AbilityData ability : itemData.getAbilities()) {
      if (ability.getTrigger() == trigger) {
        applyAbility(player, ability, itemData.getItemId());
      }
    }
  }

  /**
   * Removes abilities with the specified trigger type
   */
  private void removeAbilities(Player player, ItemData itemData, AbilityData.TriggerType trigger) {
    for (AbilityData ability : itemData.getAbilities()) {
      if (ability.getTrigger() == trigger) {
        removeAbility(player, ability, itemData.getItemId());
      }
    }
  }

  /**
   * Applies a single ability to a player
   */
  private void applyAbility(Player player, AbilityData ability, String itemId) {
    if (ability.getEffectType() == AbilityData.EffectType.POTION_EFFECT) {
      applyPotionEffect(player, ability);
    } else if (ability.getEffectType() == AbilityData.EffectType.PLAYER_MODIFIER) {
      applyPlayerModifier(player, ability, itemId);
    }
  }

  /**
   * Removes a single ability from a player
   */
  private void removeAbility(Player player, AbilityData ability, String itemId) {
    if (ability.getEffectType() == AbilityData.EffectType.PLAYER_MODIFIER) {
      removePlayerModifier(player, ability, itemId);
    }
    // Potion effects expire naturally
  }

  private void applyPotionEffect(Player player, AbilityData ability) {
    try {
      PotionEffectType type = PotionEffectType.getByName(ability.getEffectName());
      if (type != null) {
        PotionEffect effect = new PotionEffect(type, ability.getDuration(),
            ability.getAmplifier(), false, true, true);
        player.addPotionEffect(effect);

        if (plugin.getConfig().getBoolean("debug.log-inventory-events", false)) {
          plugin.getLogger().info("Applied potion effect " + ability.getEffectName() +
              " to " + player.getName());
        }
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to apply potion effect: " + ability.getEffectName());
    }
  }

  private void applyPlayerModifier(Player player, AbilityData ability, String itemId) {
    Attribute attribute = getAttributeFromName(ability.getEffectName());
    if (attribute == null)
      return;

    AttributeInstance instance = player.getAttribute(attribute);
    if (instance == null)
      return;

    // Create unique modifier ID
    String modifierId = MODIFIER_PREFIX + itemId + "_" + ability.getEffectName();
    UUID modifierUUID = UUID.nameUUIDFromBytes(modifierId.getBytes());

    // Remove existing modifier if present
    // AttributeModifier existing = instance.getModifier(modifierUUID); // Missing
    // in 1.14
    AttributeModifier existing = null;
    for (AttributeModifier mod : instance.getModifiers()) {
      if (mod.getUniqueId().equals(modifierUUID)) {
        existing = mod;
        break;
      }
    }
    if (existing != null) {
      instance.removeModifier(existing);
    }

    // Calculate modifier value based on amplifier
    double value = calculateModifierValue(attribute, ability.getAmplifier());

    // Add new modifier
    AttributeModifier modifier = new AttributeModifier(
        modifierUUID,
        modifierId,
        value,
        AttributeModifier.Operation.ADD_NUMBER);

    instance.addModifier(modifier);

    // Track active modifier
    activeModifiers.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(modifierId);

    if (plugin.getConfig().getBoolean("debug.log-inventory-events", false)) {
      plugin.getLogger().info("Applied modifier " + ability.getEffectName() +
          " (" + value + ") to " + player.getName());
    }
  }

  private void removePlayerModifier(Player player, AbilityData ability, String itemId) {
    Attribute attribute = getAttributeFromName(ability.getEffectName());
    if (attribute == null)
      return;

    AttributeInstance instance = player.getAttribute(attribute);
    if (instance == null)
      return;

    String modifierId = MODIFIER_PREFIX + itemId + "_" + ability.getEffectName();
    UUID modifierUUID = UUID.nameUUIDFromBytes(modifierId.getBytes());

    // AttributeModifier existing = instance.getModifier(modifierUUID); // Missing
    // in 1.14
    AttributeModifier existing = null;
    for (AttributeModifier mod : instance.getModifiers()) {
      if (mod.getUniqueId().equals(modifierUUID)) {
        existing = mod;
        break;
      }
    }
    if (existing != null) {
      instance.removeModifier(existing);

      Set<String> playerModifiers = activeModifiers.get(player.getUniqueId());
      if (playerModifiers != null) {
        playerModifiers.remove(modifierId);
      }

      if (plugin.getConfig().getBoolean("debug.log-inventory-events", false)) {
        plugin.getLogger().info("Removed modifier " + ability.getEffectName() +
            " from " + player.getName());
      }
    }
  }

  /**
   * Periodic task to apply WHILE_EQUIPPED abilities
   */
  private void startWhileEquippedTask() {
    whileEquippedTask = new BukkitRunnable() {
      @Override
      public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
          processWhileEquippedAbilities(player);
        }
      }
    };
    whileEquippedTask.runTaskTimer(plugin, 20L, 20L); // Run every second
  }

  /**
   * Periodic reconciliation task that removes stale modifiers.
   * This catches edge cases where modifiers persist after unequip
   * (e.g. item moved to regular inventory, server crash, etc.)
   */
  private BukkitRunnable reconciliationTask;

  private void startModifierReconciliationTask() {
    reconciliationTask = new BukkitRunnable() {
      @Override
      public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
          reconcileModifiers(player);
        }
      }
    };
    reconciliationTask.runTaskTimer(plugin, 100L, 100L); // Run every 5 seconds
  }

  /**
   * Checks that all tracked modifiers still correspond to items in curios slots.
   * Removes any orphaned modifiers.
   */
  private void reconcileModifiers(Player player) {
    Set<String> playerModifiers = activeModifiers.get(player.getUniqueId());
    if (playerModifiers == null || playerModifiers.isEmpty()) return;

    // Collect all item IDs currently equipped in curios slots
    Set<String> equippedItemIds = new HashSet<>();
    for (String slotType : plugin.getCuriosPaperAPI().getAllSlotTypes()) {
      List<ItemStack> items = plugin.getCuriosPaperAPI().getEquippedItems(player, slotType);
      for (ItemStack item : items) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
        String itemId = getItemId(item);
        if (itemId != null) equippedItemIds.add(itemId);
      }
    }

    // Check each tracked modifier to see if its item is still equipped
    for (String modifierId : new HashSet<>(playerModifiers)) {
      // modifier format: "curiospaper_ability_<itemId>_<effectName>"
      String withoutPrefix = modifierId.substring(MODIFIER_PREFIX.length());
      // Split on first underscore to get the item ID (item IDs may not contain underscores,
      // but effect names do, so we split carefully)
      // The modifierId is: MODIFIER_PREFIX + itemId + "_" + effectName
      // We need to find the itemId by checking which known items match
      String matchedItemId = null;
      for (String equipped : equippedItemIds) {
        if (withoutPrefix.startsWith(equipped + "_")) {
          matchedItemId = equipped;
          break;
        }
      }

      if (matchedItemId == null) {
        // Also check against all known item IDs to find the right one to remove
        for (String knownId : itemDataManager.getAllItemIds()) {
          if (withoutPrefix.startsWith(knownId + "_")) {
            // This modifier belongs to a known item that is NOT equipped — remove it
            String effectName = withoutPrefix.substring(knownId.length() + 1);
            Attribute attribute = getAttributeFromName(effectName);
            if (attribute != null) {
              AttributeInstance instance = player.getAttribute(attribute);
              if (instance != null) {
                UUID modUUID = UUID.nameUUIDFromBytes(modifierId.getBytes());
                AttributeModifier existing = null;
                for (AttributeModifier mod : instance.getModifiers()) {
                  if (mod.getUniqueId().equals(modUUID)) {
                    existing = mod;
                    break;
                  }
                }
                if (existing != null) {
                  instance.removeModifier(existing);
                  if (plugin.getConfig().getBoolean("debug.log-inventory-events", false)) {
                    plugin.getLogger().info("Reconciled stale modifier " + effectName + " from " + player.getName());
                  }
                }
              }
            }
            playerModifiers.remove(modifierId);
            break;
          }
        }
      }
    }
  }

  private void processWhileEquippedAbilities(Player player) {
    // Get all equipped items
    for (String slotType : plugin.getCuriosPaperAPI().getAllSlotTypes()) {
      List<ItemStack> items = plugin.getCuriosPaperAPI().getEquippedItems(player, slotType);
      for (ItemStack item : items) {
        if (item == null || item.getType() == org.bukkit.Material.AIR)
          continue;

        String itemId = getItemId(item);
        if (itemId == null)
          continue;

        ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null)
          continue;

        // Apply WHILE_EQUIPPED abilities
        for (AbilityData ability : itemData.getAbilities()) {
          if (ability.getTrigger() == AbilityData.TriggerType.WHILE_EQUIPPED) {
            applyAbility(player, ability, itemId);
          }
        }
      }
    }
  }

  public void shutdown() {
    if (whileEquippedTask != null) {
      whileEquippedTask.cancel();
    }
    if (reconciliationTask != null) {
      reconciliationTask.cancel();
    }

    // Remove all modifiers — both tracked and any orphaned CuriosPaper modifiers
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      removeAllCuriosModifiers(player);
    }
    activeModifiers.clear();
  }

  /**
   * Removes ALL CuriosPaper attribute modifiers from a player.
   * This catches both tracked and orphaned modifiers.
   */
  private void removeAllCuriosModifiers(Player player) {
    for (Attribute attr : Attribute.values()) {
      try {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance == null) continue;
        
        List<AttributeModifier> toRemove = new ArrayList<>();
        for (AttributeModifier mod : instance.getModifiers()) {
          if (mod.getName() != null && mod.getName().startsWith(MODIFIER_PREFIX)) {
            toRemove.add(mod);
          }
        }
        for (AttributeModifier mod : toRemove) {
          instance.removeModifier(mod);
        }
      } catch (Throwable ignored) {
        // Some attributes may not exist on older versions
      }
    }
  }

  private Attribute getAttributeFromName(String name) {
    // Try direct match first
    for (Attribute attr : Attribute.values()) {
      if (attr.name().equalsIgnoreCase(name)) { // 1.14 compatible
        return attr;
      }
    }

    // Try formatted name match
    for (Attribute attr : Attribute.values()) {
      String formattedName = attr.name()
          .replace("GENERIC_", "")
          .replace("PLAYER_", "")
          .replace("_", " ");
      if (formattedName.equalsIgnoreCase(name.replace("_", " "))) {
        return attr;
      }
    }

    return null;
  }

  private double calculateModifierValue(Attribute attribute, int amplifier) {
    // For attributes: amplifier is stored as value * 100 to preserve decimals
    // So we divide by 100 to get the actual value
    return amplifier / 100.0;
  }

  /**
   * Gets the item ID from an ItemStack using PDC (reliable) with display name fallback.
   */
  private String getItemId(ItemStack itemStack) {
    if (itemStack == null || !itemStack.hasItemMeta())
      return null;

    // Primary: Use PDC-based curios_custom_id key (set by createItemStack)
    NamespacedKey itemIdKey = plugin.getCuriosPaperAPI().getItemIdKey();
    PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
    String pdcId = pdc.get(itemIdKey, PersistentDataType.STRING);
    if (pdcId != null && !pdcId.isEmpty()) {
      return pdcId;
    }

    // Fallback: Try to find matching item by display name (legacy items)
    String displayName = itemStack.getItemMeta().getDisplayName();
    if (displayName != null && !displayName.isEmpty()) {
      for (ItemData data : itemDataManager.getAllItems().values()) {
        if (displayName.equals(data.getDisplayName())) {
          return data.getItemId();
        }
      }
    }

    return null;
  }
}
