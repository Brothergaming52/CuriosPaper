package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.LootTableData;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Handles injection of custom items into loot tables.
 *
 * Supports multiple loot generation systems via reflection:
 * 1. LootGenerateEvent (1.15+) — chests, barrels, dispensers, shulker boxes, etc.
 * 2. BlockDropItemEvent + BrushableBlock (1.20+) — archaeology (suspicious sand/gravel)
 *
 * All event registration is done via reflection to maintain 1.14+ compilation compatibility.
 * At most ONE custom item is injected per loot generation to avoid flooding.
 */
public class LootTableListener implements Listener {
  private final CuriosPaper plugin;
  private final ItemDataManager itemDataManager;
  private final Random random;

  // === Reflection cache: LootGenerateEvent (1.15+) ===
  private static Method lge_getLootTable;
  private static Method lge_getLoot;
  private static boolean lootGenerateSupported = false;

  // === Reflection cache: BlockDropItemEvent + BrushableBlock (1.20+) ===
  private static Class<?> blockDropClass;
  private static Method bde_getBlockState;
  private static Method bde_getBlock;
  private static Class<?> brushableBlockClass;
  private static Method bb_getLootTable;
  private static boolean archaeologySupported = false;

  static {
    // Probe LootGenerateEvent (1.15+)
    try {
      Class<?> c = Class.forName("org.bukkit.event.world.LootGenerateEvent");
      lge_getLootTable = c.getMethod("getLootTable");
      lge_getLoot = c.getMethod("getLoot");
      lootGenerateSupported = true;
    } catch (Exception ignored) {
    }

    // Probe BlockDropItemEvent + BrushableBlock for archaeology (1.20+)
    try {
      blockDropClass = Class.forName("org.bukkit.event.block.BlockDropItemEvent");
      bde_getBlockState = blockDropClass.getMethod("getBlockState");
      bde_getBlock = blockDropClass.getMethod("getBlock");
      brushableBlockClass = Class.forName("org.bukkit.block.BrushableBlock");
      bb_getLootTable = brushableBlockClass.getMethod("getLootTable");
      archaeologySupported = true;
    } catch (Exception ignored) {
    }
  }

  @SuppressWarnings("unchecked")
  public LootTableListener(CuriosPaper plugin, ItemDataManager itemDataManager) {
    this.plugin = plugin;
    this.itemDataManager = itemDataManager;
    this.random = new Random();

    // Register LootGenerateEvent (1.15+) for chests, barrels and other containers
    if (lootGenerateSupported) {
      try {
        Class<? extends org.bukkit.event.Event> evtClass =
            (Class<? extends org.bukkit.event.Event>) Class.forName("org.bukkit.event.world.LootGenerateEvent");
        plugin.getServer().getPluginManager().registerEvent(
            evtClass, this, EventPriority.HIGH,
            (l, e) -> {
              if (evtClass.isInstance(e))
                handleLootGenerate(e);
            },
            plugin, true
        );
        plugin.getLogger().info("[LootTable] Registered LootGenerateEvent handler (chests, containers)");
      } catch (Exception e) {
        plugin.getLogger().warning("[LootTable] Failed to register LootGenerateEvent: " + e.getMessage());
      }
    } else {
      plugin.getLogger().info("[LootTable] LootGenerateEvent not available (pre-1.15), chest loot injection disabled");
    }

    // Register BlockDropItemEvent for archaeology (1.20+)
    if (archaeologySupported) {
      try {
        Class<? extends org.bukkit.event.Event> evtClass =
            (Class<? extends org.bukkit.event.Event>) blockDropClass;
        plugin.getServer().getPluginManager().registerEvent(
            evtClass, this, EventPriority.HIGH,
            (l, e) -> {
              if (evtClass.isInstance(e))
                handleArchaeologyDrop(e);
            },
            plugin, true
        );
        plugin.getLogger().info("[LootTable] Registered BlockDropItemEvent handler (archaeology)");
      } catch (Exception e) {
        plugin.getLogger().warning("[LootTable] Failed to register BlockDropItemEvent: " + e.getMessage());
      }
    }
  }

  // ==================== EVENT HANDLERS ====================

  /**
   * Handler for LootGenerateEvent — fires when chests, barrels, dispensers,
   * shulker boxes, etc. generate their loot from a loot table.
   */
  private void handleLootGenerate(org.bukkit.event.Event event) {
    try {
      Object lootTableObj = lge_getLootTable.invoke(event);
      if (lootTableObj == null || !(lootTableObj instanceof org.bukkit.loot.LootTable))
        return;

      org.bukkit.loot.LootTable table = (org.bukkit.loot.LootTable) lootTableObj;
      String keyStr = table.getKey().toString();

      boolean debug = plugin.getConfig().getBoolean("debug.log-loot-events", false);
      if (debug) {
        plugin.getLogger().info("[LootTable] LootGenerateEvent fired for: " + keyStr);
      }

      @SuppressWarnings("unchecked")
      List<ItemStack> loot = (List<ItemStack>) lge_getLoot.invoke(event);
      if (loot == null)
        return;

      ItemStack selected = selectOneItem(keyStr, debug);
      if (selected != null) {
        loot.add(selected);
      }
    } catch (Exception e) {
      if (plugin.getConfig().getBoolean("debug.enabled", false)) {
        plugin.getLogger().warning("[LootTable] Error in handleLootGenerate: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  /**
   * Handler for BlockDropItemEvent — used specifically for archaeology.
   * Fires when a brushable block (suspicious sand/gravel) drops its item.
   * LootGenerateEvent does NOT fire for archaeology, so this is the fallback.
   */
  private void handleArchaeologyDrop(org.bukkit.event.Event event) {
    try {
      Object blockState = bde_getBlockState.invoke(event);
      if (blockState == null || !brushableBlockClass.isInstance(blockState))
        return;

      // Get loot table key from the BrushableBlock state snapshot
      Object lootTableObj = bb_getLootTable.invoke(blockState);
      String keyStr = null;
      if (lootTableObj instanceof org.bukkit.loot.LootTable) {
        keyStr = ((org.bukkit.loot.LootTable) lootTableObj).getKey().toString();
      }

      boolean debug = plugin.getConfig().getBoolean("debug.log-loot-events", false);
      if (debug) {
        plugin.getLogger()
            .info("[LootTable] Archaeology BlockDropItemEvent: lootTable=" + keyStr);
      }

      // If loot table key is null (already consumed), skip injection
      if (keyStr == null || keyStr.isEmpty())
        return;

      ItemStack selected = selectOneItem(keyStr, debug);
      if (selected == null)
        return;

      // Drop the custom item at the block location
      Object block = bde_getBlock.invoke(event);
      if (block instanceof org.bukkit.block.Block) {
        Location loc = ((org.bukkit.block.Block) block).getLocation().add(0.5, 0.5, 0.5);
        loc.getWorld().dropItemNaturally(loc, selected);
        if (debug) {
          plugin.getLogger().info("[LootTable] Dropped archaeology custom item at " + loc);
        }
      }
    } catch (Exception e) {
      if (plugin.getConfig().getBoolean("debug.enabled", false)) {
        plugin.getLogger().warning("[LootTable] Error in handleArchaeologyDrop: " + e.getMessage());
      }
    }
  }

  // ==================== CORE LOGIC ====================

  /**
   * Selects at most ONE custom item for the given loot table key.
   * Collects all matching candidates, shuffles them randomly,
   * and picks the first one that passes its chance roll.
   *
   * @param keyStr the full NamespacedKey string of the loot table (e.g. "minecraft:chests/simple_dungeon")
   * @param debug  whether to log detailed information
   * @return the selected ItemStack, or null if no item was selected
   */
  private ItemStack selectOneItem(String keyStr, boolean debug) {
    List<CandidateItem> candidates = new ArrayList<>();
    for (ItemData itemData : itemDataManager.getAllItems().values()) {
      for (LootTableData lootData : itemData.getLootTables()) {
        if (matchesLootTable(keyStr, lootData.getLootTableType())) {
          candidates.add(new CandidateItem(itemData, lootData));
        }
      }
    }

    if (candidates.isEmpty()) {
      if (debug) {
        plugin.getLogger().info("[LootTable]   No candidates match: " + keyStr);
      }
      return null;
    }

    if (debug) {
      plugin.getLogger().info("[LootTable]   " + candidates.size() + " candidate(s):");
      for (CandidateItem c : candidates) {
        plugin.getLogger().info("[LootTable]     - " + c.itemData.getItemId()
            + " (configured=" + c.lootData.getLootTableType()
            + ", chance=" + (c.lootData.getChance() * 100) + "%)");
      }
    }

    // Shuffle to fairly distribute among candidates, then pick first success
    Collections.shuffle(candidates, random);
    for (CandidateItem c : candidates) {
      if (random.nextDouble() < c.lootData.getChance()) {
        ItemStack item = createItemStack(c.itemData, c.lootData);
        if (item != null) {
          org.bg52.curiospaper.event.CuriosLootGenerateEvent genEvent = new org.bg52.curiospaper.event.CuriosLootGenerateEvent(keyStr, c.itemData.getItemId(), item);
          plugin.getServer().getPluginManager().callEvent(genEvent);
          if (genEvent.isCancelled() || genEvent.getItem() == null) {
            if (debug) {
              plugin.getLogger().info("[LootTable]   => Generation cancelled for: " + c.itemData.getItemId());
            }
            return null;
          }
          if (debug) {
            plugin.getLogger().info("[LootTable]   => Selected: " + c.itemData.getItemId());
          }
          return genEvent.getItem();
        }
      } else if (debug) {
        plugin.getLogger().info("[LootTable]   => Chance roll failed for: " + c.itemData.getItemId());
      }
    }

    if (debug) {
      plugin.getLogger().info("[LootTable]   => No item passed its chance roll");
    }
    return null;
  }

  /**
   * Checks if a loot table key matches the configured type.
   * Supports multiple matching strategies for flexibility:
   * 1. Exact match: "minecraft:chests/simple_dungeon" == "minecraft:chests/simple_dungeon"
   * 2. Path match: strips "minecraft:" prefix from both sides
   * 3. Partial match: "simple_dungeon" matches "minecraft:chests/simple_dungeon"
   */
  private boolean matchesLootTable(String eventKey, String configuredType) {
    if (eventKey == null || configuredType == null)
      return false;

    // 1. Exact match (case-insensitive)
    if (eventKey.equalsIgnoreCase(configuredType))
      return true;

    // 2. Strip "minecraft:" prefix from both for flexible path matching
    String eventPath = stripMinecraftPrefix(eventKey);
    String configPath = stripMinecraftPrefix(configuredType);

    if (eventPath.equalsIgnoreCase(configPath))
      return true;

    // 3. Partial: configuredType is just the final segment
    //    e.g. "simple_dungeon" should match "chests/simple_dungeon"
    if (eventPath.toLowerCase().endsWith("/" + configPath.toLowerCase()))
      return true;

    return false;
  }

  private static String stripMinecraftPrefix(String key) {
    if (key.toLowerCase().startsWith("minecraft:")) {
      return key.substring("minecraft:".length());
    }
    return key;
  }

  // ==================== ITEM CREATION ====================

  /**
   * Creates an ItemStack from ItemData and LootTableData
   */
  private ItemStack createItemStack(ItemData itemData, LootTableData lootData) {
    try {
      // Calculate random amount
      int amount = lootData.getMinAmount();
      if (lootData.getMaxAmount() > lootData.getMinAmount()) {
        amount = random.nextInt(lootData.getMaxAmount() - lootData.getMinAmount() + 1)
            + lootData.getMinAmount();
      }

      // Create base item with all required metadata (ID, Slot, etc.)
      ItemStack item = plugin.getCuriosPaperAPI().createItemStack(itemData.getItemId());
      if (item == null) {
        // Fallback to manual creation if API fails (unlikely but possible during reloads)
        Material material;
        try {
          material = Material.valueOf(itemData.getMaterial().toUpperCase());
        } catch (Exception e) {
          return null;
        }
        item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
          if (itemData.getDisplayName() != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', itemData.getDisplayName()));
          }
          if (!itemData.getLore().isEmpty()) {
            meta.setLore(itemData.getLore());
          }
          // Apply item model if specified (version-aware)
          if (itemData.getItemModel() != null && !itemData.getItemModel().isEmpty()) {
            org.bg52.curiospaper.util.VersionUtil.setItemModelSafe(meta, itemData.getItemModel(),
                itemData.getCustomModelData());
          }
          // Set custom ID in PDC
          meta.getPersistentDataContainer().set(plugin.getCuriosPaperAPI().getItemIdKey(),
              org.bukkit.persistence.PersistentDataType.STRING, itemData.getItemId());
          item.setItemMeta(meta);
        }
        if (itemData.getSlotType() != null && !itemData.getSlotType().isEmpty()) {
          item = plugin.getCuriosPaperAPI().tagAccessoryItem(item, itemData.getSlotType());
        }
      }

      item.setAmount(amount);
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
    final LootTableData lootData;

    CandidateItem(ItemData itemData, LootTableData lootData) {
      this.itemData = itemData;
      this.lootData = lootData;
    }
  }
}
