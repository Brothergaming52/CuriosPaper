package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.MobDropData;
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
 * GUI for configuring 3D model attachment settings on mob drops.
 */
public class MobDropModelConfigGUI implements Listener {
  private final CuriosPaper plugin;
  private final ItemDataManager itemDataManager;
  private final ChatInputManager chatInputManager;
  private final Map<UUID, String> currentItemId;
  private final Map<UUID, Integer> currentListIndex;

  public static final String TITLE = "§8 Mob Drop 3D Model";

  public MobDropModelConfigGUI(CuriosPaper plugin) {
    this.plugin = plugin;
    this.itemDataManager = plugin.getItemDataManager();
    this.chatInputManager = plugin.getChatInputManager();
    this.currentItemId = new HashMap<>();
    this.currentListIndex = new HashMap<>();
  }

  /**
   * Opens the 3D model configuration GUI for a given mob drop
   */
  public void open(Player player, String itemId, int listIndex) {
    ItemData itemData = itemDataManager.getItemData(itemId);
    if (itemData == null) {
      player.sendMessage("§cItem not found!");
      return;
    }

    if (listIndex < 0 || listIndex >= itemData.getMobDrops().size()) {
      player.sendMessage("§cMob drop entry not found!");
      return;
    }

    MobDropData dropData = itemData.getMobDrops().get(listIndex);

    currentItemId.put(player.getUniqueId(), itemId);
    currentListIndex.put(player.getUniqueId(), listIndex);

    Inventory gui = Bukkit.createInventory(null, 54, TITLE);

    // Header
    ItemStack headerItem = createGuiItem(Material.ARMOR_STAND, "§6§lMob Drop 3D Model Configuration",
        "§7Configure if the mob should equip this",
        "§7item visually when spawning.",
        "",
        "§7Item: §e" + itemId,
        "§7Mob: §e" + dropData.getEntityType());
    
    if (dropData.isModelEnabled() && dropData.getModelItem() != null) {
      try {
        Material mat = Material.valueOf(dropData.getModelItem().toUpperCase());
        headerItem.setType(mat);
        ItemMeta meta = headerItem.getItemMeta();
        if (meta != null) {
          org.bg52.curiospaper.util.VersionUtil.setItemModelSafe(meta, dropData.getModelItemModel(), dropData.getModelCustomModelData());
          headerItem.setItemMeta(meta);
        }
      } catch (Exception ignored) {
      }
    }
    gui.setItem(4, headerItem);

    // Toggle modelEnabled (slot 20)
    boolean enabled = dropData.isModelEnabled();
    gui.setItem(20, createGuiItem(
        enabled ? Material.LIME_DYE : Material.GRAY_DYE,
        enabled ? "§a Model Enabled" : "§c Model Disabled",
        "§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"),
        "",
        "§eClick to toggle"));

    // Model Item material (slot 22)
    gui.setItem(22, createGuiItem(Material.IRON_BLOCK, "§e Model Item Material",
        "§7Current: " + (dropData.getModelItem() != null ? "§f" + dropData.getModelItem() : "§cnone"),
        "",
        "§eClick to set material",
        "§7e.g. LEATHER_HORSE_ARMOR"));

    // Model CustomModelData (slot 24)
    gui.setItem(24, createGuiItem(Material.MAP, "§e Model Custom Model Data",
        "§7Current: " + (dropData.getModelCustomModelData() != null
            ? "§f" + dropData.getModelCustomModelData()
            : "§cnone"),
        "",
        "§eClick to set integer value",
        "§7or type 'remove' to clear"));

    // Model Item Model Component (slot 31)
    gui.setItem(31, createGuiItem(Material.NAME_TAG, "§e🏷 Item Model Component",
        "§7Current: " + (dropData.getModelItemModel() != null
            ? "§f" + dropData.getModelItemModel()
            : "§cnone"),
        "",
        "§eClick to set minecraft 1.21.4+ item model",
        "§7format: namespace:key",
        "§7or type 'remove' to clear"));

    // Back button (slot 49)
    gui.setItem(49, createGuiItem(Material.OAK_DOOR, "§e« Back to Editor",
        "§7Return to mob drop editor"));

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
    Integer listIndex = currentListIndex.get(playerId);
    if (itemId == null || listIndex == null)
      return;

    ItemData itemData = itemDataManager.getItemData(itemId);
    if (itemData == null || listIndex < 0 || listIndex >= itemData.getMobDrops().size())
      return;

    MobDropData dropData = itemData.getMobDrops().get(listIndex);

    switch (raw) {
      case 20: // Toggle modelEnabled
        dropData.setModelEnabled(!dropData.isModelEnabled());
        itemDataManager.saveItemData(itemId);
        player.sendMessage(dropData.isModelEnabled()
            ? "§a 3D Model enabled for mob drop!"
            : "§c 3D Model disabled for mob drop.");
        open(player, itemId, listIndex); // Refresh
        break;

      case 22: // Set Model Item material
        player.closeInventory();
        chatInputManager.startSingleLineSession(player,
            "Enter the material type for the model item (e.g., LEATHER_HORSE_ARMOR, PAPER):",
            single -> {
              if (single != null && !single.trim().isEmpty()) {
                String mat = single.trim().toUpperCase();
                try {
                  Material.valueOf(mat);
                  dropData.setModelItem(mat);
                  itemDataManager.saveItemData(itemId);
                  player.sendMessage("§a Model item set to: " + mat);
                } catch (IllegalArgumentException e) {
                  player.sendMessage("§c Invalid material: " + mat);
                }
              }
              Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId, listIndex), 2L);
            },
            () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId, listIndex), 2L));
        break;

      case 24: // Set Model CustomModelData
        player.closeInventory();
        chatInputManager.startSingleLineSession(player,
            "Enter Model Custom Model Data (integer) or type 'remove' to clear:",
            single -> {
              if (single != null && !single.trim().isEmpty()) {
                String input = single.trim().toLowerCase();
                if (input.equals("remove") || input.equals("clear") || input.equals("none")) {
                  dropData.setModelCustomModelData(null);
                  itemDataManager.saveItemData(itemId);
                  player.sendMessage("§a Model Custom Model Data cleared.");
                } else {
                  try {
                    int val = Integer.parseInt(input);
                    dropData.setModelCustomModelData(val);
                    itemDataManager.saveItemData(itemId);
                    player.sendMessage("§a Model Custom Model Data set to: " + val);
                  } catch (NumberFormatException e) {
                    player.sendMessage("§c Invalid number.");
                  }
                }
              }
              Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId, listIndex), 2L);
            },
            () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId, listIndex), 2L));
        break;

      case 31: // Set Model Item Model
        player.closeInventory();
        chatInputManager.startSingleLineSession(player,
            "Enter Item Model component (namespace:key) or type 'remove' to clear:",
            single -> {
              if (single != null && !single.trim().isEmpty()) {
                String input = single.trim().toLowerCase();
                if (input.equals("remove") || input.equals("clear") || input.equals("none")) {
                  dropData.setModelItemModel(null);
                  itemDataManager.saveItemData(itemId);
                  player.sendMessage("§a Item Model Component cleared.");
                } else {
                  dropData.setModelItemModel(input);
                  itemDataManager.saveItemData(itemId);
                  player.sendMessage("§a Item Model Component set to: " + input);
                }
              }
              Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId, listIndex), 2L);
            },
            () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId, listIndex), 2L));
        break;

      case 49: // Back to Editor
        player.closeInventory();
        currentItemId.remove(playerId);
        currentListIndex.remove(playerId);
        plugin.getMobDropEditor().open(player, itemId);
        break;
    }
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
