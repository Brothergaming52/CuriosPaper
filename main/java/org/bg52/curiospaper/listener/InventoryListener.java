package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bg52.curiospaper.inventory.AccessoryGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryListener implements Listener {
    private final CuriosPaper plugin;
    private final AccessoryGUI gui;
    private final Map<Player, Map<String, List<ItemStack>>> previousInventoryState;

    public InventoryListener(CuriosPaper plugin, AccessoryGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
        this.previousInventoryState = new HashMap<>();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();

        if (AccessoryGUI.isMainGUI(title)) {
            handleMainGUIClick(event, player);
        } else if (AccessoryGUI.isSlotsGUI(title)) {
            handleSlotsGUIClick(event, player, title);
        }
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
        Inventory topInventory = event.getView().getTopInventory();

        // Check if clicking in the top (accessory) inventory
        if (rawSlot < topInventory.getSize()) {
            // Check if this is a valid accessory slot or a filler slot
            if (!gui.isAccessorySlot(topInventory, rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        Inventory clickedInventory = event.getClickedInventory();

        // Handle shift-click from player inventory to accessory GUI
        if (event.isShiftClick() && clickedInventory == player.getInventory()) {
            if (clickedItem != null && !clickedItem.getType().isAir()) {
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
        if (cursorItem != null && !cursorItem.getType().isAir() && clickedInventory == topInventory) {
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
        if (clickedItem != null && !clickedItem.getType().isAir() && clickedInventory == topInventory) {
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
            if (hotbarItem != null && !hotbarItem.getType().isAir()) {
                if (!plugin.getCuriosPaperAPI().isValidAccessory(hotbarItem, slotType)) {
                    event.setCancelled(true);
                    player.sendMessage("§cThat item cannot be placed in this slot type!");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();

        if (AccessoryGUI.isMainGUI(title)) {
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

            if (draggedItem != null && !draggedItem.getType().isAir()) {
                if (!plugin.getCuriosPaperAPI().isValidAccessory(draggedItem, slotType)) {
                    event.setCancelled(true);
                    player.sendMessage("§cThat item cannot be placed in this slot type!");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

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
                newItems.add(item != null && !item.getType().isAir() ? item.clone() : null);
            }

            // Get previous state to detect changes
            Map<String, List<ItemStack>> previousState = previousInventoryState.get(player);
            List<ItemStack> previousItems = previousState != null ? previousState.get(slotType) : new ArrayList<>();

            // Save the new state
            plugin.getSlotManager().setAccessories(player.getUniqueId(), slotType, newItems);
            plugin.getSlotManager().savePlayerData(player);

            // Fire events for changes
            fireEquipEvents(player, slotType, previousItems, newItems);

            // Clean up stored state
            previousInventoryState.remove(player);
        }
    }

    private void storeInventoryState(Player player, String slotType) {
        List<ItemStack> currentItems = plugin.getSlotManager().getAccessories(player.getUniqueId(), slotType);
        Map<String, List<ItemStack>> stateMap = previousInventoryState.computeIfAbsent(player, k -> new HashMap<>());
        stateMap.put(slotType, new ArrayList<>(currentItems));
    }

    private void fireEquipEvents(Player player, String slotType, List<ItemStack> oldItems, List<ItemStack> newItems) {
        int maxSize = Math.max(oldItems.size(), newItems.size());

        for (int i = 0; i < maxSize; i++) {
            ItemStack oldItem = i < oldItems.size() ? oldItems.get(i) : null;
            ItemStack newItem = i < newItems.size() ? newItems.get(i) : null;

            // Normalize nulls and air
            boolean oldEmpty = oldItem == null || oldItem.getType().isAir();
            boolean newEmpty = newItem == null || newItem.getType().isAir();

            if (oldEmpty && newEmpty) {
                continue; // No change
            }

            if (!oldEmpty && newEmpty) {
                // Item was unequipped
                AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
                        player, slotType, i, oldItem, null, AccessoryEquipEvent.Action.UNEQUIP
                );
                Bukkit.getPluginManager().callEvent(equipEvent);
            } else if (oldEmpty && !newEmpty) {
                // Item was equipped
                AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
                        player, slotType, i, null, newItem, AccessoryEquipEvent.Action.EQUIP
                );
                Bukkit.getPluginManager().callEvent(equipEvent);
            } else if (!oldItem.equals(newItem)) {
                // Item was swapped
                AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
                        player, slotType, i, oldItem, newItem, AccessoryEquipEvent.Action.SWAP
                );
                Bukkit.getPluginManager().callEvent(equipEvent);
            }
        }
    }

    private boolean hasInventorySpace(Player player, ItemStack item) {
        Inventory inv = player.getInventory();
        int amount = item.getAmount();

        for (ItemStack invItem : inv.getStorageContents()) {
            if (invItem == null || invItem.getType().isAir()) {
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
