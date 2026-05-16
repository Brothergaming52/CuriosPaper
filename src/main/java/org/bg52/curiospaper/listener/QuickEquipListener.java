package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.config.SlotConfiguration;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class QuickEquipListener implements Listener {
  private final CuriosPaper plugin;

  public QuickEquipListener(CuriosPaper plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    // Only trigger on shift + right click (air or block)
    if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Player player = event.getPlayer();
    if (!player.isSneaking()) {
      return;
    }

    // Only process main hand to avoid double-fire
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }

    ItemStack heldItem = player.getInventory().getItemInMainHand();
    if (heldItem == null || heldItem.getType() == Material.AIR) {
      return;
    }

    // Check if the item is tagged for a curios slot
    String slotTypeTag = plugin.getCuriosPaperAPI().getAccessorySlotType(heldItem);
    if (slotTypeTag == null || slotTypeTag.isEmpty()) {
      return;
    }

    // Ensure player data is loaded
    if (!plugin.getSlotManager().hasPlayerData(player.getUniqueId())) {
      plugin.getSlotManager().loadPlayerData(player);
    }

    // Support multi-slot tags e.g. "ring, charm"
    String[] slotTypes = slotTypeTag.split(",\\s*");

    for (String slotType : slotTypes) {
      slotType = slotType.trim().toLowerCase();

      SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(slotType);
      if (config == null) {
        continue;
      }

      List<ItemStack> currentItems = plugin.getSlotManager().getAccessories(player.getUniqueId(), slotType);
      int slotAmount = config.getAmount();

      // Find the first empty slot
      int emptyIndex = -1;
      for (int i = 0; i < slotAmount; i++) {
        ItemStack existing = i < currentItems.size() ? currentItems.get(i) : null;
        if (existing == null || existing.getType() == Material.AIR) {
          emptyIndex = i;
          break;
        }
      }

      if (emptyIndex == -1) {
        continue; // No space in this slot type, try next
      }

      // Equip the item (clone with amount 1)
      ItemStack toEquip = heldItem.clone();
      toEquip.setAmount(1);

      // Set the item in the slot
      plugin.getSlotManager().setAccessoryItem(player.getUniqueId(), slotType, emptyIndex, toEquip);
      plugin.getSlotManager().savePlayerData(player);

      // Remove one from hand
      if (heldItem.getAmount() > 1) {
        heldItem.setAmount(heldItem.getAmount() - 1);
      } else {
        player.getInventory().setItemInMainHand(null);
      }

      // Fire equip event
      AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
          player, slotType, emptyIndex, null, toEquip, AccessoryEquipEvent.Action.EQUIP);
      Bukkit.getPluginManager().callEvent(equipEvent);

      String slotName = org.bukkit.ChatColor.stripColor(config.getName());
      player.sendMessage(plugin.getMessagesManager().get("quick-equip.equipped", "slot", slotName));

      // Cancel the interact event to prevent placing blocks, etc.
      event.setCancelled(true);
      return; // Successfully equipped, done
    }

    // If we get here, all matching slots are full
    player.sendMessage(plugin.getMessagesManager().get("quick-equip.no-slots"));
    event.setCancelled(true);
  }
}
