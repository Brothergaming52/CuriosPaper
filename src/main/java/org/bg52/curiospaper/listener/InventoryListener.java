package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bg52.curiospaper.inventory.AccessoryGUI;
import org.bg52.curiospaper.inventory.EditMenuGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryListener implements Listener {
  private final CuriosPaper plugin;
  private final AccessoryGUI gui;
  private final Map<Player, Map<String, List<ItemStack>>> previousInventoryState;

  // Edit menu selection state: tracks selected slot index per player
  private final Map<Player, Integer> editMenuSelectedSlot = new HashMap<>();
  // Edit menu selection state: tracks the original ItemStack that was selected
  private final Map<Player, ItemStack> editMenuSelectedItem = new HashMap<>();

  public InventoryListener(CuriosPaper plugin, AccessoryGUI gui) {
    this.plugin = plugin;
    this.gui = gui;
    this.previousInventoryState = new HashMap<>();
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getWhoClicked();

    String title = event.getView().getTitle();

    if (AccessoryGUI.isMainGUI(title)) {
      handleMainGUIClick(event, player);
    } else if (AccessoryGUI.isSlotsGUI(title)) {
      handleSlotsGUIClick(event, player, title);
    } else if (EditMenuGUI.isEditMenu(title)) {
      handleEditMenuClick(event, player);
    }
  }

  private void handleEditMenuClick(InventoryClickEvent event, Player player) {
    // Cancel ALL vanilla inventory behavior — no items on cursor, ever
    event.setCancelled(true);

    // Block shift-clicks, hotbar swaps, and any automatic moves
    if (event.isShiftClick() || event.getClick().isKeyboardClick()
        || event.getAction().name().contains("MOVE_TO_OTHER_INVENTORY")) {
      return;
    }

    Inventory topInv = event.getView().getTopInventory();

    // Block all clicks outside the top inventory (player inventory area)
    if (event.getClickedInventory() != topInv) {
      return;
    }

    int clickedSlot = event.getSlot();
    ItemStack clicked = event.getCurrentItem();

    boolean hasSelection = editMenuSelectedSlot.containsKey(player);
    boolean clickedIsButton = clicked != null && clicked.getType() != Material.AIR
        && !EditMenuGUI.isFiller(clicked) && !EditMenuGUI.isSelectedMarker(clicked);
    boolean clickedIsMarker = EditMenuGUI.isSelectedMarker(clicked);

    if (!hasSelection) {
      // --- No button currently selected ---
      if (clickedIsButton) {
        // Select this button: store it and replace with green glass
        editMenuSelectedSlot.put(player, clickedSlot);
        editMenuSelectedItem.put(player, clicked.clone());

        String displayName = clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : null;
        topInv.setItem(clickedSlot, EditMenuGUI.createSelectedMarker(displayName));
      }
      // Clicked filler or air — do nothing
    } else {
      // --- A button IS currently selected ---
      int selectedSlot = editMenuSelectedSlot.get(player);
      ItemStack selectedItem = editMenuSelectedItem.get(player);

      if (clickedIsMarker && clickedSlot == selectedSlot) {
        // Clicked the same green pane — deselect, restore original button
        topInv.setItem(selectedSlot, selectedItem);
      } else if (clickedIsButton) {
        // Clicked a different button — swap the two
        topInv.setItem(selectedSlot, clicked.clone());
        topInv.setItem(clickedSlot, selectedItem);
      } else {
        // Clicked filler/air — move selected button here
        topInv.setItem(selectedSlot, EditMenuGUI.createFiller());
        topInv.setItem(clickedSlot, selectedItem);
      }

      // Clear selection state
      editMenuSelectedSlot.remove(player);
      editMenuSelectedItem.remove(player);
    }

    // Force inventory update to keep client in sync
    Bukkit.getScheduler().runTask(plugin, player::updateInventory);
  }

  private void handleMainGUIClick(InventoryClickEvent event, Player player) {
    event.setCancelled(true);

    ItemStack clickedItem = event.getCurrentItem();
    if (clickedItem == null || !clickedItem.hasItemMeta()) {
      return;
    }

    String slotType = clickedItem.getItemMeta().getPersistentDataContainer()
        .get(plugin.getSlotTypeKey(), PersistentDataType.STRING);

    if (slotType != null) {
      // Store current state before opening Tier 2 GUI
      storeInventoryState(player, slotType);
      gui.openSlotItemsGUI(player, slotType);
    }
  }

  private void handleSlotsGUIClick(InventoryClickEvent event, Player player, String title) {
    String slotType = AccessoryGUI.extractSlotTypeFromTitle(title);
    if (slotType == null) {
      return;
    }

    int rawSlot = event.getRawSlot();
    if (rawSlot < 0) {
      return;
    }
    Inventory topInventory = event.getView().getTopInventory();

    // Check if clicking in the top (accessory) inventory
    if (rawSlot < topInventory.getSize()) {
      // Check if the back button was clicked
      ItemStack clickedForBack = event.getCurrentItem();
      if (gui.isBackButton(clickedForBack)) {
        event.setCancelled(true);

        // Save current accessories before going back
        int[] accessorySlots = gui.getAccessorySlots(slotType);
        java.util.List<ItemStack> currentItems = new java.util.ArrayList<>();
        for (int slot : accessorySlots) {
          ItemStack item = topInventory.getItem(slot);
          currentItems.add(item != null && item.getType() != org.bukkit.Material.AIR ? item.clone() : null);
        }
        plugin.getSlotManager().setAccessories(player.getUniqueId(), slotType, currentItems);
        plugin.getSlotManager().savePlayerData(player);

        // Re-open the main GUI on next tick to avoid close-event conflicts
        Bukkit.getScheduler().runTask(plugin, () -> gui.openMainGUI(player));
        return;
      }

      // Check if this is a valid accessory slot or a filler slot
      if (!gui.isAccessorySlot(topInventory, rawSlot)) {
        event.setCancelled(true);
        return;
      }
    }

    ItemStack clickedItem = event.getCurrentItem();
    ItemStack cursorItem = event.getCursor();
    Inventory clickedInventory = event.getClickedInventory();

    // Handle RIGHT-click on an equipped item to toggle 3D model visibility
    if (event.isRightClick() && !event.isShiftClick()
        && clickedInventory == topInventory
        && clickedItem != null && clickedItem.getType() != org.bukkit.Material.AIR
        && gui.isAccessorySlot(topInventory, rawSlot)
        && (cursorItem == null || cursorItem.getType() == org.bukkit.Material.AIR)) {

      // Only allow toggle if the item actually has a 3D model defined
      org.bukkit.inventory.meta.ItemMeta meta = clickedItem.getItemMeta();
      if (meta != null) {
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        org.bukkit.NamespacedKey itemIdKey = plugin.getCuriosPaperAPI().getItemIdKey();
        String itemId = pdc.get(itemIdKey, PersistentDataType.STRING);

        if (itemId != null) {
          org.bg52.curiospaper.data.ItemData itemData = plugin.getCuriosPaperAPI().getItemData(itemId);
          if (itemData != null && itemData.isModelEnabled()) {
            org.bukkit.NamespacedKey modelHiddenKey = new org.bukkit.NamespacedKey(plugin, "curios_model_hidden");
            boolean isHidden = pdc.getOrDefault(modelHiddenKey, PersistentDataType.BYTE, (byte) 0) == 1;
            if (isHidden) {
              pdc.remove(modelHiddenKey);
              player.sendMessage("§a3D model display §2enabled§a for this item.");
            } else {
              pdc.set(modelHiddenKey, PersistentDataType.BYTE, (byte) 1);
              player.sendMessage("§c3D model display §4disabled§c for this item.");
            }
            clickedItem.setItemMeta(meta);

            // Save immediately so the toggle persists across restarts
            int[] accessorySlots = gui.getAccessorySlots(slotType);
            List<ItemStack> currentItems = new ArrayList<>();
            for (int slot : accessorySlots) {
              ItemStack item = topInventory.getItem(slot);
              currentItems.add(item != null && item.getType() != org.bukkit.Material.AIR ? item.clone() : null);
            }
            plugin.getSlotManager().setAccessories(player.getUniqueId(), slotType, currentItems);
            plugin.getSlotManager().savePlayerData(player);

            // Immediately update 3D model stands so the change is visible right away
            Bukkit.getScheduler().runTask(plugin, () -> {
              plugin.getModelStandManager().rescanPlayer(player);
            });

            event.setCancelled(true);
            return;
          }
        }
      }
    }

    // Handle shift-click from player inventory to accessory GUI
    if (event.isShiftClick() && clickedInventory == player.getInventory()) {
      if (clickedItem != null && clickedItem.getType() != org.bukkit.Material.AIR) {
        if (!plugin.getCuriosPaperAPI().isValidAccessory(clickedItem, slotType)) {
          event.setCancelled(true);
          player.sendMessage("§cThat item cannot be placed in this slot type!");
          return;
        }

        // Check if there's space in accessory slots
        if (!gui.hasEmptyAccessorySlot(topInventory)) {
          event.setCancelled(true);
          player.sendMessage("§cNo empty slots available!");
          return;
        }
      }
    }

    // Handle placing item from cursor into accessory GUI
    if (cursorItem != null && cursorItem.getType() != org.bukkit.Material.AIR && clickedInventory == topInventory) {
      if (!gui.isAccessorySlot(topInventory, rawSlot)) {
        event.setCancelled(true);
        return;
      }

      if (!plugin.getCuriosPaperAPI().isValidAccessory(cursorItem, slotType)) {
        event.setCancelled(true);
        player.sendMessage("§cThat item cannot be placed in this slot type!");
        return;
      }
    }

    // Handle removing item from accessory GUI
    if (clickedItem != null && clickedItem.getType() != org.bukkit.Material.AIR
        && clickedInventory == topInventory) {
      // Check if player inventory has space
      if (event.isShiftClick()) {
        if (!hasInventorySpace(player, clickedItem)) {
          event.setCancelled(true);
          player.sendMessage("§cYour inventory is full!");
          return;
        }
      }
    }

    // Handle hotbar swapping
    if (event.getClick().isKeyboardClick() && clickedInventory == topInventory) {
      if (!gui.isAccessorySlot(topInventory, rawSlot)) {
        event.setCancelled(true);
        return;
      }

      ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
      if (hotbarItem != null && hotbarItem.getType() != org.bukkit.Material.AIR) {
        if (!plugin.getCuriosPaperAPI().isValidAccessory(hotbarItem, slotType)) {
          event.setCancelled(true);
          player.sendMessage("§cThat item cannot be placed in this slot type!");
          return;
        }
      }
    }

    Bukkit.getScheduler().runTask(plugin, () -> {
      Inventory topInventoryAfter = event.getView().getTopInventory();
      enforceSingleItemPerAccessorySlot(player, topInventoryAfter, slotType);
    });
  }

  private void enforceSingleItemPerAccessorySlot(Player player, Inventory topInventory, String slotType) {
    // Get the real accessory slot positions for this slot type
    int[] accessorySlots = gui.getAccessorySlots(slotType);
    if (accessorySlots == null || accessorySlots.length == 0) {
      return;
    }

    for (int slot : accessorySlots) {
      if (slot < 0 || slot >= topInventory.getSize())
        continue;

      ItemStack item = topInventory.getItem(slot);
      if (item == null || item.getType() == org.bukkit.Material.AIR)
        continue;

      int amount = item.getAmount();
      if (amount <= 1)
        continue;

      // Keep exactly 1 in the accessory slot
      item.setAmount(1);

      int extra = amount - 1;
      ItemStack extraStack = item.clone();
      extraStack.setAmount(extra);

      // Try to give extras back to the player's inventory
      Map<Integer, ItemStack> leftovers = player.getInventory().addItem(extraStack);

      // If inventory is full, drop leftovers on the ground
      if (!leftovers.isEmpty()) {
        leftovers.values()
            .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
      }
    }
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getWhoClicked();

    String title = event.getView().getTitle();

    if (AccessoryGUI.isMainGUI(title)) {
      event.setCancelled(true);
    } else if (EditMenuGUI.isEditMenu(title)) {
      event.setCancelled(true);
    } else if (AccessoryGUI.isSlotsGUI(title)) {
      String slotType = AccessoryGUI.extractSlotTypeFromTitle(title);
      if (slotType == null) {
        return;
      }

      Inventory topInventory = event.getView().getTopInventory();
      ItemStack draggedItem = event.getOldCursor();

      // Check if any dragged slots are filler slots or invalid
      for (int rawSlot : event.getRawSlots()) {
        if (rawSlot < topInventory.getSize()) {
          if (!gui.isAccessorySlot(topInventory, rawSlot)) {
            event.setCancelled(true);
            return;
          }
        }
      }

      if (draggedItem != null && draggedItem.getType() != org.bukkit.Material.AIR) {
        if (!plugin.getCuriosPaperAPI().isValidAccessory(draggedItem, slotType)) {
          event.setCancelled(true);
          player.sendMessage("§cThat item cannot be placed in this slot type!");
        }
      }
      Bukkit.getScheduler().runTask(plugin, () -> {
        Inventory topInventoryAfter = event.getView().getTopInventory();
        enforceSingleItemPerAccessorySlot(player, topInventoryAfter, slotType);
      });
    }
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getPlayer();

    String title = event.getView().getTitle();

    if (AccessoryGUI.isSlotsGUI(title)) {
      String slotType = AccessoryGUI.extractSlotTypeFromTitle(title);
      if (slotType == null) {
        return;
      }

      Inventory inventory = event.getInventory();

      // Get only the actual accessory slots (not filler items)
      int[] accessorySlots = gui.getAccessorySlots(slotType);
      List<ItemStack> newItems = new ArrayList<>();

      for (int slot : accessorySlots) {
        ItemStack item = inventory.getItem(slot);
        newItems.add(item != null && item.getType() != org.bukkit.Material.AIR ? item.clone() : null);
      }

      // Get previous state to detect changes
      Map<String, List<ItemStack>> previousState = previousInventoryState.get(player);
      List<ItemStack> previousItems = null;
      if (previousState != null && previousState.containsKey(slotType)) {
        previousItems = previousState.get(slotType);
      } else {
        List<ItemStack> saved = plugin.getSlotManager().getAccessories(player.getUniqueId(), slotType);
        previousItems = saved != null ? new ArrayList<>(saved) : new ArrayList<>();
      }

      // Save the new state
      plugin.getSlotManager().setAccessories(player.getUniqueId(), slotType, newItems);
      plugin.getSlotManager().savePlayerData(player);

      // Fire events for changes
      fireEquipEvents(player, slotType, previousItems, newItems);

      // Clean up stored state
      previousInventoryState.remove(player);
    } else if (EditMenuGUI.isEditMenu(title)) {
      handleEditMenuClose(event);
    }
  }

  private void handleEditMenuClose(InventoryCloseEvent event) {
    Player player = (Player) event.getPlayer();
    Inventory inv = event.getInventory();

    // If a button was selected (green pane showing), restore it before saving
    if (editMenuSelectedSlot.containsKey(player)) {
      int selectedSlot = editMenuSelectedSlot.get(player);
      ItemStack selectedItem = editMenuSelectedItem.get(player);
      if (selectedSlot >= 0 && selectedSlot < inv.getSize() && selectedItem != null) {
        inv.setItem(selectedSlot, selectedItem);
      }
      editMenuSelectedSlot.remove(player);
      editMenuSelectedItem.remove(player);
    }

    // Clear cursor just in case (safety for all MC versions)
    player.setItemOnCursor(null);

    // Build new layout from final inventory state
    Map<String, Integer> newLayout = new java.util.HashMap<>();
    for (int i = 0; i < inv.getSize(); i++) {
      ItemStack item = inv.getItem(i);
      if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
        continue;
      }

      String slotType = item.getItemMeta().getPersistentDataContainer()
          .get(plugin.getSlotTypeKey(), PersistentDataType.STRING);

      if (slotType != null) {
        newLayout.put(slotType.toLowerCase(), i);
      }
    }

    // Apply and save as the permanent custom layout override
    plugin.getGUI().saveCustomLayout(newLayout, inv.getSize());
    plugin.getLogger().info("[CuriosPaper] Edit menu closed by " + player.getName()
        + " — custom layout saved to config (" + newLayout.size() + " slots).");

    // Force-close any open main AccessoryGUI for all players so they see the
    // updated layout
    Bukkit.getScheduler().runTask(plugin, () -> {
      for (Player online : Bukkit.getOnlinePlayers()) {
        if (online.getOpenInventory() != null
            && AccessoryGUI.isMainGUI(online.getOpenInventory().getTitle())) {
          online.closeInventory();
          online.sendMessage("§e§l[CuriosPaper] §7The accessory menu layout was updated.");
        }
      }
    });
  }

  private void storeInventoryState(Player player, String slotType) {
    List<ItemStack> currentItems = plugin.getSlotManager().getAccessories(player.getUniqueId(), slotType);
    Map<String, List<ItemStack>> stateMap = previousInventoryState.computeIfAbsent(player, k -> new HashMap<>());
    stateMap.put(slotType.toLowerCase(), new ArrayList<>(currentItems));
  }

  private void fireEquipEvents(Player player, String slotType, List<ItemStack> oldItems, List<ItemStack> newItems) {
    if (oldItems == null) oldItems = new ArrayList<>();
    if (newItems == null) newItems = new ArrayList<>();
    int maxSize = Math.max(oldItems.size(), newItems.size());

    for (int i = 0; i < maxSize; i++) {
      ItemStack oldItem = i < oldItems.size() ? oldItems.get(i) : null;
      ItemStack newItem = i < newItems.size() ? newItems.get(i) : null;

      // Normalize nulls and air
      boolean oldEmpty = oldItem == null || oldItem.getType() == org.bukkit.Material.AIR;
      boolean newEmpty = newItem == null || newItem.getType() == org.bukkit.Material.AIR;

      if (oldEmpty && newEmpty) {
        continue; // No change
      }

      if (!oldEmpty && newEmpty) {
        // Item was unequipped
        AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
            player, slotType, i, oldItem, null, AccessoryEquipEvent.Action.UNEQUIP);
        Bukkit.getPluginManager().callEvent(equipEvent);
      } else if (oldEmpty && !newEmpty) {
        // Item was equipped
        AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
            player, slotType, i, null, newItem, AccessoryEquipEvent.Action.EQUIP);
        Bukkit.getPluginManager().callEvent(equipEvent);
      } else if (!oldItem.equals(newItem)) {
        // Item was swapped
        AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
            player, slotType, i, oldItem, newItem, AccessoryEquipEvent.Action.SWAP);
        Bukkit.getPluginManager().callEvent(equipEvent);
      }
    }
  }

  private boolean hasInventorySpace(Player player, ItemStack item) {
    Inventory inv = player.getInventory();
    int amount = item.getAmount();

    for (ItemStack invItem : inv.getStorageContents()) {
      if (invItem == null || invItem.getType() == org.bukkit.Material.AIR) {
        return true; // Found empty slot
      }

      if (invItem.isSimilar(item)) {
        int remaining = invItem.getMaxStackSize() - invItem.getAmount();
        amount -= remaining;
        if (amount <= 0) {
          return true;
        }
      }
    }

    return false;
  }
}