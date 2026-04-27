package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.manager.ChatInputManager;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for configuring 3D model attachment settings on custom items.
 * Allows setting modelEnabled, modelItem, modelCustomModelData,
 * and pitch/yaw visibility limits.
 */
public class ModelConfigGUI implements Listener {
  private final CuriosPaper plugin;
  private final ItemDataManager itemDataManager;
  private final ChatInputManager chatInputManager;
  private final Map<UUID, String> currentItemId;

  public static final String TITLE = "§8 3D Model Settings";

  public ModelConfigGUI(CuriosPaper plugin) {
    this.plugin = plugin;
    this.itemDataManager = plugin.getItemDataManager();
    this.chatInputManager = plugin.getChatInputManager();
    this.currentItemId = new HashMap<>();
  }

  /**
   * Opens the 3D model configuration GUI for a given item
   */
  public void open(Player player, String itemId) {
    ItemData itemData = itemDataManager.getItemData(itemId);
    if (itemData == null) {
      player.sendMessage("§cItem not found!");
      return;
    }

    currentItemId.put(player.getUniqueId(), itemId);

    Inventory gui = Bukkit.createInventory(null, 54, TITLE);

    // Header
    ItemStack headerItem = createGuiItem(Material.ARMOR_STAND, "§6§l3D Model Configuration",
        "§7Configure how this item appears",
        "§7on the player's body when equipped",
        "",
        "§7Item: §e" + itemId);
    
    if (itemData.isModelEnabled() && itemData.getModelItem() != null) {
      try {
        Material mat = Material.valueOf(itemData.getModelItem().toUpperCase());
        headerItem.setType(mat);
        ItemMeta meta = headerItem.getItemMeta();
        if (meta != null) {
          org.bg52.curiospaper.util.VersionUtil.setItemModelSafe(meta, itemData.getModelItemModel(), itemData.getModelCustomModelData());
          headerItem.setItemMeta(meta);
        }
      } catch (Exception ignored) {
      }
    }
    gui.setItem(4, headerItem);

    // Toggle modelEnabled (slot 19)
    boolean enabled = itemData.isModelEnabled();
    gui.setItem(19, createGuiItem(
        enabled ? Material.LIME_DYE : Material.GRAY_DYE,
        enabled ? "§a Model Enabled" : "§c Model Disabled",
        "§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"),
        "",
        "§eClick to toggle"));

    // Model Item material (slot 21)
    gui.setItem(21, createGuiItem(Material.IRON_BLOCK, "§e Model Item Material",
        "§7Current: " + (itemData.getModelItem() != null ? "§f" + itemData.getModelItem() : "§cnone"),
        "",
        "§eClick to set material",
        "§7e.g. LEATHER_HORSE_ARMOR"));

    // Model CustomModelData (slot 23)
    gui.setItem(23, createGuiItem(Material.MAP, "§e Model Custom Model Data",
        "§7Current: " + (itemData.getModelCustomModelData() != null
            ? "§f" + itemData.getModelCustomModelData()
            : "§cnone"),
        "",
        "§eClick to set integer value",
        "§7or type 'remove' to clear"));

    // Pitch Up Limit (slot 28)
    gui.setItem(28, createGuiItem(Material.ARROW, "§e↑ Pitch Up Limit",
        "§7Current: " + formatFloat(itemData.getPitchUpLimit()),
        "",
        "§7Hide model from self when",
        "§7looking up beyond this angle",
        "§eClick to set"));

    // Pitch Down Limit (slot 30)
    gui.setItem(30, createGuiItem(Material.ARROW, "§e↓ Pitch Down Limit",
        "§7Current: " + formatFloat(itemData.getPitchDownLimit()),
        "",
        "§7Hide model from self when",
        "§7looking down beyond this angle",
        "§eClick to set"));

    // Model Item Model Component (slot 25)
    gui.setItem(25, createGuiItem(Material.NAME_TAG, "§e🏷 Item Model Component",
        "§7Current: " + (itemData.getModelItemModel() != null
            ? "§f" + itemData.getModelItemModel()
            : "§cnone"),
        "",
        "§eClick to set minecraft 1.21.4+ item model",
        "§7format: namespace:key",
        "§7or type 'remove' to clear"));

    // Back button (slot 49)
    gui.setItem(49, createGuiItem(Material.OAK_DOOR, "§e« Back to Editor",
        "§7Return to item editor"));

    // Fill empty slots with glass pane
    ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    for (int i = 0; i < 54; i++) {
      if (gui.getItem(i) == null) {
        gui.setItem(i, filler);
      }
    }

    player.openInventory(gui);
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player))
      return;
    Player player = (Player) event.getWhoClicked();

    String title = event.getView().getTitle();
    if (!TITLE.equals(title))
      return;

    int raw = event.getRawSlot();
    int topSize = event.getView().getTopInventory().getSize();
    if (raw >= topSize)
      return;

    event.setCancelled(true);

    UUID playerId = player.getUniqueId();
    String itemId = currentItemId.get(playerId);
    if (itemId == null)
      return;

    ItemData itemData = itemDataManager.getItemData(itemId);
    if (itemData == null)
      return;

    switch (raw) {
      case 19: // Toggle modelEnabled
        itemData.setModelEnabled(!itemData.isModelEnabled());
        itemDataManager.saveItemData(itemId);
        player.sendMessage(itemData.isModelEnabled()
            ? "§a 3D Model enabled!"
            : "§c 3D Model disabled.");
        open(player, itemId); // Refresh
        break;

      case 21: // Set Model Item material
        player.closeInventory();
        chatInputManager.startSingleLineSession(player,
            "Enter the material type for the model item (e.g., LEATHER_HORSE_ARMOR, PAPER):",
            single -> {
              if (single != null && !single.trim().isEmpty()) {
                String mat = single.trim().toUpperCase();
                try {
                  Material.valueOf(mat);
                  itemData.setModelItem(mat);
                  itemDataManager.saveItemData(itemId);
                  player.sendMessage("§a Model item set to: " + mat);
                } catch (IllegalArgumentException e) {
                  player.sendMessage("§c Invalid material: " + mat);
                }
              }
              Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
            },
            () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L));
        break;

      case 23: // Set Model CustomModelData
        player.closeInventory();
        chatInputManager.startSingleLineSession(player,
            "Enter Model Custom Model Data (integer) or type 'remove' to clear:",
            single -> {
              if (single != null && !single.trim().isEmpty()) {
                String input = single.trim().toLowerCase();
                if (input.equals("remove") || input.equals("clear") || input.equals("none")) {
                  itemData.setModelCustomModelData(null);
                  itemDataManager.saveItemData(itemId);
                  player.sendMessage("§a Model Custom Model Data cleared.");
                } else {
                  try {
                    int val = Integer.parseInt(input);
                    itemData.setModelCustomModelData(val);
                    itemDataManager.saveItemData(itemId);
                    player.sendMessage("§a Model Custom Model Data set to: " + val);
                  } catch (NumberFormatException e) {
                    player.sendMessage("§c Invalid number.");
                  }
                }
              }
              Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
            },
            () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L));
        break;

      case 25: // Set Model Item Model
        player.closeInventory();
        chatInputManager.startSingleLineSession(player,
            "Enter Item Model component (namespace:key) or type 'remove' to clear:",
            single -> {
              if (single != null && !single.trim().isEmpty()) {
                String input = single.trim().toLowerCase();
                if (input.equals("remove") || input.equals("clear") || input.equals("none")) {
                  itemData.setModelItemModel(null);
                  itemDataManager.saveItemData(itemId);
                  player.sendMessage("§a Item Model Component cleared.");
                } else {
                  itemData.setModelItemModel(input);
                  itemDataManager.saveItemData(itemId);
                  player.sendMessage("§a Item Model Component set to: " + input);
                }
              }
              Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
            },
            () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L));
        break;

      case 28: // Pitch Up Limit
        promptFloat(player, itemId, itemData, "Pitch Up Limit",
            itemData::setPitchUpLimit);
        break;

      case 30: // Pitch Down Limit
        promptFloat(player, itemId, itemData, "Pitch Down Limit",
            itemData::setPitchDownLimit);
        break;

      case 49: // Back to Editor
        player.closeInventory();
        currentItemId.remove(playerId);
        plugin.getEditGUI().open(player, itemId);
        break;
    }
  }

  /**
   * Prompts the player for a float value via chat input and applies it.
   */
  private void promptFloat(Player player, String itemId, ItemData itemData,
      String fieldName, java.util.function.Consumer<Float> setter) {
    player.closeInventory();
    chatInputManager.startSingleLineSession(player,
        "Enter " + fieldName + " (angle in degrees, e.g. 45.0) or 'remove' to clear:",
        single -> {
          if (single != null && !single.trim().isEmpty()) {
            String input = single.trim().toLowerCase();
            if (input.equals("remove") || input.equals("clear") || input.equals("none")) {
              setter.accept(null);
              itemDataManager.saveItemData(itemId);
              player.sendMessage("§a " + fieldName + " cleared.");
            } else {
              try {
                float val = Float.parseFloat(input);
                setter.accept(val);
                itemDataManager.saveItemData(itemId);
                player.sendMessage("§a " + fieldName + " set to: " + val + "°");
              } catch (NumberFormatException e) {
                player.sendMessage("§c Invalid number.");
              }
            }
          }
          Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
        },
        () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L));
  }

  private String formatFloat(Float value) {
    return value != null ? "§f" + value + "°" : "§cnone";
  }

  private ItemStack createGuiItem(Material material, String name, String... lore) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(name);
      if (lore.length > 0) {
        meta.setLore(Arrays.asList(lore));
      }
      item.setItemMeta(meta);
    }
    return item;
  }
}
