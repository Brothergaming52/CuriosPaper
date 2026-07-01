package org.bg52.curiospaper.handler;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.config.SlotConfiguration;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.io.File;

// import javax.annotation.Nullable;
// import javax.annotation.Nullable;

import java.util.*;

/**
 * Handles elytra equipping in back slots with automatic gliding attribute
 * management.
 * When an elytra is equipped in a back slot:
 * - If player has a chestplate: adds glider component to chestplate
 * - If player has no chestplate: secretly equips elytra with invisible item
 * model
 */
public class ElytraBackSlotHandler implements Listener {
  private final CuriosPaper plugin;
  private final NamespacedKey secretElytraKey;
  private final Set<UUID> playersWithSecretElytra;

  public ElytraBackSlotHandler(CuriosPaper plugin) {
    this.plugin = plugin;
    this.secretElytraKey = new NamespacedKey(plugin, "secret_elytra");
    this.playersWithSecretElytra = new HashSet<>();
  }

  /**
   * Ensure an Elytra has the "Required Slot: <Back Name>" lore and back-slot tag.
   * Returns the same instance if no change is needed, or a new tagged ItemStack
   * otherwise.
   */
  private ItemStack ensureBackTaggedElytra(ItemStack stack) {
    if (stack == null || stack.getType() != Material.ELYTRA) {
      return stack;
    }

    // Get back slot config (for the display name used in lore)
    SlotConfiguration backConfig = plugin.getConfigManager().getSlotConfiguration("back");
    if (backConfig == null) {
      return stack; // no back slot defined, bail out
    }

    String pName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', backConfig.getName()));
    String requiredLine = ChatColor.GOLD + "Slot: " + ChatColor.YELLOW + pName;

    ItemMeta meta = stack.getItemMeta();
    if (meta != null) {
      // Check NBT first - definitive source
      PersistentDataContainer container = meta.getPersistentDataContainer();
      String existingSlot = container.get(plugin.getCuriosPaperAPI().getSlotTypeKey(), PersistentDataType.STRING);
      if ("back".equalsIgnoreCase(existingSlot)) {
        return stack; // Already tagged via NBT
      }

      if (meta.hasLore()) {
        List<String> lore = meta.getLore();
        if (lore != null) {
          for (String line : lore) {
            if (requiredLine.equals(line)) {
              // Already has correct lore; assume already tagged
              return stack;
            }
          }
        }
      }
    }

    // Not tagged yet -> use your API to tag it for the back slot.
    // This MUST clone internally and preserve durability & other data.
    ItemStack tagged = plugin.getCuriosPaperAPI().tagAccessoryItem(stack, "back", true);

    // Make sure durability and data are preserved: tagAccessoryItem clones and only
    // adds PDC + lore, so no reset. If it didn't, you'd fix it there, not here.
    return tagged;
  }

  /**
   * Scan the player's inventory + armor/offhand and ensure all Elytras are tagged
   * for the back slot.
   * Idempotent: safe to call often.
   */
  private void retagAllPlayerElytras(Player player) {
    // Main inventory, hotbar, armor, etc. in one go
    ItemStack[] contents = player.getInventory().getContents();
    boolean changed = false;

    for (int i = 0; i < contents.length; i++) {
      ItemStack original = contents[i];
      ItemStack updated = ensureBackTaggedElytra(original);
      if (updated != original) {
        contents[i] = updated;
        changed = true;
      }
    }

    if (changed) {
      player.getInventory().setContents(contents);
    }

    // Offhand
    ItemStack off = player.getInventory().getItemInOffHand();
    ItemStack updatedOff = ensureBackTaggedElytra(off);
    if (updatedOff != off) {
      player.getInventory().setItemInOffHand(updatedOff);
    }

    // Chest slot (in case Elytra is there)
    ItemStack chest = player.getInventory().getChestplate();
    ItemStack updatedChest = ensureBackTaggedElytra(chest);
    if (updatedChest != chest) {
      player.getInventory().setChestplate(updatedChest);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onElytraPickup(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getEntity();

    ItemStack stack = event.getItem().getItemStack();
    if (stack == null || stack.getType() != Material.ELYTRA) {
      return;
    }

    // After pickup is processed, retag all Elytras in their inventory
    plugin.getServer().getScheduler().runTask(plugin, () -> retagAllPlayerElytras(player));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onAccessoryEquip(AccessoryEquipEvent event) {
    // Only process back slot events
    if (!"back".equalsIgnoreCase(event.getSlotType())) {
      return;
    }

    // Check if feature is enabled
    if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
      return;
    }

    Player player = event.getPlayer();
    ItemStack newItem = event.getNewItem();
    ItemStack previousItem = event.getPreviousItem();

    // Handle elytra being equipped
    if (newItem != null && newItem.getType() == Material.ELYTRA) {
      handleElytraEquipped(player);
    }
    // Handle elytra being unequipped
    else if (previousItem != null && previousItem.getType() == Material.ELYTRA) {
      handleElytraUnequipped(player);
    }
  }

  // @Nullable
  private ItemStack getBackSlotElytra(Player player) {
    ItemStack backItem = plugin.getCuriosPaperAPI().getEquippedItem(player, "back", 0);
    if (backItem != null && backItem.getType() == Material.ELYTRA) {
      return backItem;
    }
    return null;
  }

  private void setBackSlotElytra(Player player, /* @Nullable */ ItemStack item) {
    // Adjust this to your real API
    plugin.getCuriosPaperAPI().setEquippedItem(player, "back", 0, item);
  }

  /**
   * Called when an elytra is equipped in the back slot
   */
  private void handleElytraEquipped(Player player) {
    // Check the durability of the back-slot elytra: if it's at 1, treat as broken
    ItemStack back = getBackSlotElytra(player);
    if (back != null) {
      ItemMeta meta = back.getItemMeta();
      if (meta instanceof Damageable) {
        Damageable dmgMeta = (Damageable) meta;
        int max = org.bg52.curiospaper.util.VersionUtil.getMaxDurability(back);
        int maxUsableDamage = max - 1;
        if (dmgMeta.getDamage() >= maxUsableDamage) {
          // Elytra is effectively broken; don't enable any gliding
          // plugin.getLogger().info("Back-slot elytra for " + player.getName()
          // + " is already at 1 durability; not enabling glider/secret elytra.");
          return;
        }
      }
    }

    ItemStack chestplate = player.getInventory().getChestplate();

    if (chestplate != null && chestplate.getType() != Material.AIR && isChestplate(chestplate.getType())) {
      // Player has a chestplate - add glider component to it
      addGliderToChestplate(player, chestplate);
    } else {
      // No chestplate - equip secret elytra with invisible item model
      equipSecretElytra(player);
    }
  }

  /**
   * Handles right-click equipping of chestplates so the secret elytra
   * never ends up as a normal survival item.
   *
   * Flow:
   * - Player has secret elytra in chest slot (our fake wings)
   * - Player right-clicks with a real chestplate in hand
   * - Vanilla swaps: chestplate -> chest slot, secret elytra -> inventory
   * - We run 1 tick later, wipe all secret elytras from inventory,
   * and reapply GLIDER to the new chestplate.
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onChestplateRightClick(PlayerInteractEvent event) {
    if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
      return;
    }

    Action action = event.getAction();
    if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Player player = event.getPlayer();
    ItemStack item = event.getItem();
    if (item == null || item.getType() == Material.AIR) {
      return;
    }

    // Only care about right-clicking with chestplates
    if (!isChestplate(item.getType())) {
      return;
    }

    // Only relevant if this player currently has our secret elytra equipped
    if (!playersWithSecretElytra.contains(player.getUniqueId())) {
      return;
    }

    // And only if the system is actually active (elytra in back slot)
    if (!hasElytraInBackSlot(player)) {
      return;
    }

    // Let vanilla do the swap first, then clean up 1 tick later
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      ItemStack chestplate = player.getInventory().getChestplate();
      if (chestplate != null && isChestplate(chestplate.getType())) {
        // Remove all traces of secret elytra (including the one that got dumped into
        // inventory)
        playersWithSecretElytra.remove(player.getUniqueId());
        wipeSecretElytra(player);

        // Re-read chestplate in case anything changed
        ItemStack currentChest = player.getInventory().getChestplate();
        if (currentChest != null && isChestplate(currentChest.getType())) {
          addGliderToChestplate(player, currentChest);
        }
      }
    }, 1L);
  }

  /**
   * Called when an elytra is unequipped from the back slot
   */
  private void handleElytraUnequipped(Player player) {
    // Remove glider from chestplate if present
    ItemStack chestplate = player.getInventory().getChestplate();
    if (chestplate != null && chestplate.getType() != Material.AIR && isChestplate(chestplate.getType())) {
      removeGliderFromChestplate(player, chestplate);
    }

    // Remove secret elytra if equipped
    removeSecretElytra(player);

    // Wipe any secret elytras and glider/custom equippable components from inventory
    wipeSecretElytra(player);
  }

  private boolean isSecretElytra(ItemStack stack) {
    if (stack == null || stack.getType() != Material.ELYTRA)
      return false;
    ItemMeta meta = stack.getItemMeta();
    if (meta == null)
      return false;
    return meta.getPersistentDataContainer().has(secretElytraKey, PersistentDataType.BYTE);
  }

  private void wipeSecretElytra(Player player) {
    // 1. Chest slot (explicit check/removal) - only if they do not have a back slot elytra
    if (!hasElytraInBackSlot(player)) {
      ItemStack chest = player.getInventory().getChestplate();
      if (isSecretElytra(chest)) {
        player.getInventory().setChestplate(null);
      } else if (chest != null && isChestplate(chest.getType()) && org.bg52.curiospaper.util.VersionUtil.hasGlider(chest)) {
        org.bg52.curiospaper.util.VersionUtil.removeGlider(chest);
        player.getInventory().setChestplate(chest);
      }
    }

    // 2. Main inventory contents (storage contents only, size 36)
    ItemStack[] contents = player.getInventory().getStorageContents();
    boolean contentsChanged = false;
    for (int i = 0; i < contents.length; i++) {
      ItemStack item = contents[i];
      if (item == null || item.getType() == Material.AIR) {
        continue;
      }
      if (isSecretElytra(item)) {
        contents[i] = null;
        contentsChanged = true;
      } else if (isChestplate(item.getType()) && org.bg52.curiospaper.util.VersionUtil.hasGlider(item)) {
        org.bg52.curiospaper.util.VersionUtil.removeGlider(item);
        contentsChanged = true;
      }
    }
    if (contentsChanged) {
      player.getInventory().setStorageContents(contents);
    }

    // 3. Other armor slots (helmet, leggings, boots)
    ItemStack[] armor = player.getInventory().getArmorContents();
    boolean armorChanged = false;
    for (int i = 0; i < armor.length; i++) {
      if (i == 2) {
        continue; // Skip chestplate slot (handled explicitly in step 1)
      }
      ItemStack item = armor[i];
      if (item == null || item.getType() == Material.AIR) {
        continue;
      }
      if (isSecretElytra(item)) {
        armor[i] = null;
        armorChanged = true;
      } else if (isChestplate(item.getType()) && org.bg52.curiospaper.util.VersionUtil.hasGlider(item)) {
        org.bg52.curiospaper.util.VersionUtil.removeGlider(item);
        armorChanged = true;
      }
    }
    if (armorChanged) {
      player.getInventory().setArmorContents(armor);
    }

    // 4. Offhand slot
    ItemStack offhand = player.getInventory().getItemInOffHand();
    if (offhand != null && offhand.getType() != Material.AIR) {
      if (isSecretElytra(offhand)) {
        player.getInventory().setItemInOffHand(null);
      } else if (isChestplate(offhand.getType()) && org.bg52.curiospaper.util.VersionUtil.hasGlider(offhand)) {
        org.bg52.curiospaper.util.VersionUtil.removeGlider(offhand);
        player.getInventory().setItemInOffHand(offhand);
      }
    }

    // 5. Cursor item
    ItemStack cursor = player.getItemOnCursor();
    if (cursor != null && cursor.getType() != Material.AIR) {
      if (isSecretElytra(cursor)) {
        player.setItemOnCursor(null);
      } else if (isChestplate(cursor.getType()) && org.bg52.curiospaper.util.VersionUtil.hasGlider(cursor)) {
        org.bg52.curiospaper.util.VersionUtil.removeGlider(cursor);
        player.setItemOnCursor(cursor);
      }
    }

    player.updateInventory();
  }

  /**
   * Prevent picking up the secret elytra with empty hands.
   * Still allow swapping it with a chestplate item.
   */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onArmorSlotProtectSecretElytra(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getWhoClicked();

    if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
      return;
    }

    if (event.getClickedInventory() != player.getInventory()) {
      return;
    }

    // Chest armor slot
    if (event.getSlot() != 38) {
      return;
    }

    ItemStack chest = player.getInventory().getChestplate();
    if (!isSecretElytra(chest)) {
      return;
    }

    ItemStack cursor = event.getCursor();
    boolean cursorIsAir = (cursor == null || cursor.getType() == Material.AIR);

    // If cursor is empty, player is trying to pick up the secret elytra -> block it
    if (cursorIsAir) {
      event.setCancelled(true);
      return;
    }

    // Only allow swapping with a valid chestplate
    if (!isChestplate(cursor.getType())) {
      event.setCancelled(true);
      return;
    }

    // Now cursor is a valid chestplate.
    // We handle the swap manually to prevent the secret elytra from ever entering the cursor or inventory.
    ItemStack chestplateToEquip = cursor.clone();
    try {
      applyGliderToItem(player, chestplateToEquip);
      
      // If successful, cancel event to override vanilla behavior and apply slot contents instantly
      event.setCancelled(true);
      
      // Remove player from secret elytra tracking
      playersWithSecretElytra.remove(player.getUniqueId());
      
      // Set the chestplate in slot 38 and clear the cursor
      player.getInventory().setChestplate(chestplateToEquip);
      player.setItemOnCursor(null);
      
      // Sync client inventory
      player.updateInventory();
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to add glider to chestplate during click swap for " + player.getName() + ": " + e.getMessage());
      e.printStackTrace();
      
      // Cancel event and do nothing, keeping secret elytra in slot and chestplate on cursor safely
      event.setCancelled(true);
      player.updateInventory();
    }
  }

  /**
   * Prevent dragging items into the chestplate slot when secret elytra is active.
   */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onArmorSlotDragProtectSecretElytra(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getWhoClicked();

    if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
      return;
    }

    if (event.getRawSlots().contains(38)) {
      ItemStack chest = player.getInventory().getChestplate();
      if (isSecretElytra(chest)) {
        event.setCancelled(true);
      }
    }
  }

  /**
   * Wipe any secret elytra from cursor or inventory upon closing inventory to prevent ghost items/dupes.
   */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getPlayer();

    if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
      return;
    }

    wipeSecretElytra(player);
  }

  /**
   * Applies the glider and equippable modifications to a chestplate item.
   * Can throw an exception if applying the custom/vanilla flight components fails.
   */
  private void applyGliderToItem(Player player, ItemStack chestplate) throws Exception {
    if (chestplate == null || chestplate.getType() == Material.AIR) {
      return;
    }

    // Check if back slot Elytra has a custom equippable asset ID
    ItemStack backSlotElytra = getBackSlotElytra(player);
    org.bukkit.NamespacedKey customAsset = org.bg52.curiospaper.util.VersionUtil.getEquippableAsset(backSlotElytra);

    if (customAsset != null && !(customAsset.getNamespace().equalsIgnoreCase("minecraft") && customAsset.getKey().equalsIgnoreCase("elytra"))) {
      // It's a custom Elytra! Use the combined asset ID: curiospaper:elytra_<mat>_<sanitized_asset_key>
      String base = chestplate.getType().name()
          .toLowerCase(Locale.ROOT)
          .replace("_chestplate", "");
      String sanitized = org.bg52.curiospaper.util.VersionUtil.sanitizeAssetKey(customAsset.getNamespace(), customAsset.getKey());
      
      // Check if the generated combined asset file exists in the resource pack build directory
      File buildDir = new File(plugin.getDataFolder(), "resource-pack-build");
      File assetFile = new File(buildDir, "assets/curiospaper/equipment/elytra_" + base + "_" + sanitized + ".json");

      if (assetFile.exists()) {
        String combinedAsset = "elytra_" + base + "_" + sanitized;
        org.bg52.curiospaper.util.VersionUtil.applyElytraFlight(chestplate, "curiospaper", combinedAsset);
      } else {
        // Fall back to the vanilla wings asset for this chestplate (which is always present in the core pack)
        String assetId = resolveChestplateWingsAsset(chestplate.getType());
        if (assetId != null) {
          org.bg52.curiospaper.util.VersionUtil.applyElytraFlight(chestplate, "curiospaper", assetId);
        } else {
          plugin.getLogger().warning("No wings assetId mapping for chestplate material "
              + chestplate.getType() + " for " + player.getName());
        }
      }
    } else {
      // Fall back to vanilla: resolve wings asset ID by chestplate material
      String assetId = resolveChestplateWingsAsset(chestplate.getType());
      if (assetId != null) {
        org.bg52.curiospaper.util.VersionUtil.applyElytraFlight(chestplate, "curiospaper", assetId);
      } else {
        plugin.getLogger().warning("No wings assetId mapping for chestplate material "
            + chestplate.getType() + " for " + player.getName());
      }
    }
  }

  /**
   * Adds the glider component to a chestplate
   */
  private void addGliderToChestplate(Player player, ItemStack chestplate) {
    try {
      if (chestplate == null || chestplate.getType() == Material.AIR) {
        plugin.getLogger().warning("Tried to add glider to null/air chestplate for " + player.getName());
        return;
      }

      applyGliderToItem(player, chestplate);
      player.getInventory().setChestplate(chestplate);
    } catch (Exception e) {
      plugin.getLogger()
          .warning("Failed to add glider to chestplate for " + player.getName() + ": " + e.getMessage());
      e.printStackTrace();
      equipSecretElytra(player);
    }
  }

  private /* @Nullable */ String resolveChestplateWingsAsset(Material type) {
    if (!type.name().endsWith("_CHESTPLATE")) {
      return null;
    }

    // Turn "NETHERITE_CHESTPLATE" -> "netherite"
    String base = type.name()
        .toLowerCase(Locale.ROOT)
        .replace("_chestplate", "");

    // Final key suffix: elytra_<material>_chestplate
    return "elytra_" + base + "_chestplate";
  }

  /**
   * Redirect durability damage from chestplate/secret-elytra to the back-slot
   * elytra.
   * Caps at 1 durability (vanilla Elytra behavior).
   */
  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onItemDamage(PlayerItemDamageEvent event) {
    Player player = event.getPlayer();

    if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
      return;
    }

    // ✅ Only redirect damage while the player is actually gliding
    // If they're just wearing the chestplate and taking hits on the ground,
    // that damage should NOT be mirrored to the back-slot elytra.
    if (!player.isGliding()) {
      return;
    }

    ItemStack damagedItem = event.getItem();
    if (damagedItem == null) {
      return;
    }

    // We only want to redirect if:
    // - This item is our GLIDER chestplate, OR
    // - This item is our secret elytra
    boolean isGliderChestplate = isChestplate(damagedItem.getType()) &&
        org.bg52.curiospaper.util.VersionUtil.hasGlider(damagedItem);
    boolean isOurSecretElytra = isSecretElytra(damagedItem);

    if (!isGliderChestplate && !isOurSecretElytra) {
      return;
    }

    // And only if we actually have a back-slot elytra
    ItemStack backElytra = getBackSlotElytra(player);
    if (backElytra == null) {
      return;
    }

    int damage = event.getDamage();
    event.setCancelled(true); // don't damage the chest/secret item

    ItemMeta meta = backElytra.getItemMeta();
    if (!(meta instanceof Damageable)) {
      return; // shouldn't happen for Elytra
    }
    Damageable dmgMeta = (Damageable) meta;

    int currentDamage = dmgMeta.getDamage();
    int max = org.bg52.curiospaper.util.VersionUtil.getMaxDurability(backElytra);

    // Max usable damage is maxDurability - 1 (item disabled but not broken)
    int maxUsableDamage = max - 1;

    int newDamage = currentDamage + damage;
    if (newDamage > maxUsableDamage) {
      newDamage = maxUsableDamage;
    }

    // Apply capped damage
    dmgMeta.setDamage(newDamage);
    backElytra.setItemMeta(meta);
    setBackSlotElytra(player, backElytra);

    // If we've hit "broken but not destroyed" state -> disable gliding
    if (newDamage >= maxUsableDamage) {
      // Disable GLIDER chestplate OR secret-elytra chest behavior
      if (isGliderChestplate) {
        ItemStack chestplateNow = player.getInventory().getChestplate();
        if (chestplateNow != null && isChestplate(chestplateNow.getType())) {
          removeGliderFromChestplate(player, chestplateNow);
        }
      } else if (isOurSecretElytra) {
        removeSecretElytra(player);
      }

      plugin.getLogger().info("Back-slot elytra for " + player.getName()
          + " reached 1 durability; gliding disabled but item kept.");
    }
  }

  /**
   * Removes the glider component from a chestplate
   */
  private void removeGliderFromChestplate(Player player, ItemStack chestplate) {
    try {
      org.bg52.curiospaper.util.VersionUtil.removeGlider(chestplate);
      player.getInventory().setChestplate(chestplate);
      // plugin.getLogger().info("Removed glider + reset asset for " +
      // player.getName() + "'s chestplate");
    } catch (Exception e) {
      plugin.getLogger()
          .warning("Failed to remove glider from chestplate for " + player.getName() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  /*
   * Removed clearGliderComponents as it's handled by VersionUtil.removeGlider
   */

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();

    // Stop tracking this player
    playersWithSecretElytra.remove(player.getUniqueId());

    // Clean up drops:
    // - Remove secret (invisible) elytra entirely
    // - Convert any GLIDER chestplates back to normal items
    Iterator<ItemStack> it = event.getDrops().iterator();
    while (it.hasNext()) {
      ItemStack drop = it.next();
      if (drop == null || drop.getType() == Material.AIR)
        continue;

      if (isSecretElytra(drop)) {
        // Don't drop our internal "fake" elytra
        it.remove();
        continue;
      }

      if (isChestplate(drop.getType()) && org.bg52.curiospaper.util.VersionUtil.hasGlider(drop)) {
        org.bg52.curiospaper.util.VersionUtil.removeGlider(drop);
      }
    }
  }

  /**
   * Equips a secret elytra with invisible item model (inventory) but normal
   * entity model (wings in F5)
   */
  private void equipSecretElytra(Player player) {
    wipeSecretElytra(player);

    ItemStack secretElytra = new ItemStack(Material.ELYTRA);
    ItemMeta meta = secretElytra.getItemMeta();

    if (meta != null) {
      // Mark this as a secret elytra
      meta.getPersistentDataContainer().set(secretElytraKey, PersistentDataType.BYTE, (byte) 1);

      // Set invisible item model for inventory display
      // This makes the elytra invisible when seen in inventory
      // But the entity model (wings in F5) stays normal
      org.bg52.curiospaper.util.VersionUtil.setItemModelSafe(meta,
          org.bg52.curiospaper.util.VersionUtil.parseNamespacedKey("curiospaper:invisible"), null);

      secretElytra.setItemMeta(meta);
    }

    // Apply custom elytra asset if present on the back slot elytra
    ItemStack backSlotElytra = getBackSlotElytra(player);
    org.bukkit.NamespacedKey customAsset = org.bg52.curiospaper.util.VersionUtil.getEquippableAsset(backSlotElytra);
    if (customAsset != null && !(customAsset.getNamespace().equalsIgnoreCase("minecraft") && customAsset.getKey().equalsIgnoreCase("elytra"))) {
      org.bg52.curiospaper.util.VersionUtil.applyElytraFlight(secretElytra, customAsset.getNamespace(), customAsset.getKey());
    }

    player.getInventory().setChestplate(secretElytra);
    playersWithSecretElytra.add(player.getUniqueId());
  }

  /**
   * Removes the secret elytra from the chest slot
   */
  private void removeSecretElytra(Player player) {
    if (!playersWithSecretElytra.contains(player.getUniqueId())) {
      return;
    }

    ItemStack chestplate = player.getInventory().getChestplate();
    if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
      ItemMeta meta = chestplate.getItemMeta();
      if (meta != null && meta.getPersistentDataContainer().has(secretElytraKey, PersistentDataType.BYTE)) {
        // This is our secret elytra - remove it
        player.getInventory().setChestplate(null);
        playersWithSecretElytra.remove(player.getUniqueId());
        wipeSecretElytra(player);
        // plugin.getLogger().info("Removed secret elytra from " + player.getName());
      }
    }
  }

  /**
   * Checks if player has an elytra equipped in back slot
   */
  private boolean hasElytraInBackSlot(Player player) {
    ItemStack backItem = plugin.getCuriosPaperAPI().getEquippedItem(player, "back", 0);
    return backItem != null && backItem.getType() == Material.ELYTRA;
  }

  /**
   * Handles when a player changes their chestplate while having elytra in back
   * slot
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getWhoClicked();

    if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
      return;
    }

    // Check if they're interacting with their chest armor slot
    if (event.getSlot() == 38 && event.getClickedInventory() == player.getInventory()) {
      // Delay the check slightly to allow the item to be placed first
      plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
        if (hasElytraInBackSlot(player)) {
          handleChestplateChange(player);
        }
      }, 1L);
    }
  }

  /**
   * Handles chestplate changes when elytra is in back slot
   */
  private void handleChestplateChange(Player player) {
    ItemStack chestplate = player.getInventory().getChestplate();

    // Check if secret elytra was replaced with a chestplate
    if (playersWithSecretElytra.contains(player.getUniqueId())) {
      if (chestplate != null && isChestplate(chestplate.getType())) {
        // Remove ALL secret elytra traces (including cursor) first
        playersWithSecretElytra.remove(player.getUniqueId());
        wipeSecretElytra(player);

        // Re-read chestplate (in case chest slot was cleared during wipe)
        chestplate = player.getInventory().getChestplate();
        if (chestplate != null && isChestplate(chestplate.getType())) {
          addGliderToChestplate(player, chestplate);
        }
      }
    } else {
      // Player has chestplate with glider, check if it was removed or swapped
      if (chestplate == null || chestplate.getType() == Material.AIR) {
        // Chestplate removed - equip secret elytra
        equipSecretElytra(player);
      } else if (isChestplate(chestplate.getType())) {
        // Chestplate swapped - ensure new one has glider
        // First remove glider from old one if it's still in inventory
        // Then add to new one
        addGliderToChestplate(player, chestplate);
      }
    }
  }

  /**
   * Checks if a material is a chestplate
   */
  private boolean isChestplate(Material material) {
    return material == Material.LEATHER_CHESTPLATE ||
        material == Material.CHAINMAIL_CHESTPLATE ||
        material == Material.IRON_CHESTPLATE ||
        material == Material.GOLDEN_CHESTPLATE ||
        material == Material.DIAMOND_CHESTPLATE ||
        material.name().equals("NETHERITE_CHESTPLATE"); // Soft check
  }

  /**
   * Initialize elytra slots and gliders when player joins
   */
  @EventHandler
  public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
    if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
      return;
    }
    Player player = event.getPlayer();
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      if (player.isOnline() && hasElytraInBackSlot(player)) {
        handleElytraEquipped(player);
      }
    }, 1L);
  }

  /**
   * Clean up secret elytra tracking and wipe data when player quits
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    playersWithSecretElytra.remove(player.getUniqueId());
    wipeSecretElytra(player);
  }
}
