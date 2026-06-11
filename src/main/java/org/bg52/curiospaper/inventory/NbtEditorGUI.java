package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.manager.ChatInputManager;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bg52.curiospaper.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class NbtEditorGUI implements Listener {
  private final CuriosPaper plugin;
  private final ItemDataManager itemDataManager;
  private final ChatInputManager chatInputManager;
  private final Map<UUID, String> currentItemId = new HashMap<>();
  private final Map<UUID, Integer> currentScreen = new HashMap<>(); // 0=Main, 1=PDC List, 2=Enchant List, 3=Select
                                                                    // Existing Key, 4=Select Enchant Type
  private final Map<UUID, Integer> currentPage = new HashMap<>();

  public NbtEditorGUI(CuriosPaper plugin) {
    this.plugin = plugin;
    this.itemDataManager = plugin.getItemDataManager();
    this.chatInputManager = plugin.getChatInputManager();
  }

  public void open(Player player, String itemId) {
    ItemData itemData = itemDataManager.getItemData(itemId);
    if (itemData == null) {
      player.sendMessage("§cItem not found!");
      return;
    }
    currentItemId.put(player.getUniqueId(), itemId);
    openMainMenu(player, itemData);
  }

  private void openMainMenu(Player player, ItemData itemData) {
    currentScreen.put(player.getUniqueId(), 0);
    Inventory gui = Bukkit.createInventory(null, 27, "§8NBT Editor: " + itemData.getItemId());

    gui.setItem(10, createGuiItem(Material.NAME_TAG, "§e§lManage NBT (PDC Keys)",
        "§7Configure custom NBT tags stored in",
        "§7the Persistent Data Container.",
        "",
        "§eClick to view/edit NBT keys"));

    gui.setItem(12, createGuiItem(Material.ENCHANTED_BOOK, "§e§lManage Enchantments",
        "§7Add, remove, or modify enchantments",
        "§7applied to this item.",
        "",
        "§eClick to edit enchants"));

    boolean unbreakable = itemData.isUnbreakable();
    gui.setItem(14, createGuiItem(Material.ANVIL, "§e§lUnbreakable Toggle",
        "§7Toggle whether this item is unbreakable.",
        "",
        "§7Status: " + (unbreakable ? "§a§lTRUE" : "§c§lFALSE"),
        "",
        "§eClick to toggle"));

    boolean placeable = itemData.isPlaceable();
    gui.setItem(16, createGuiItem(Material.GRASS_BLOCK, "§e§lPlaceable Toggle",
        "§7Toggle whether this block/head can be placed.",
        "§7If FALSE, players cannot place this item",
        "§7on the ground as a block.",
        "",
        "§7Status: " + (placeable ? "§a§lTRUE" : "§c§lFALSE"),
        "",
        "§eClick to toggle"));

    gui.setItem(22, createGuiItem(Material.OAK_DOOR, "§e« Back to Editor", "§7Return to item editor"));

    fillGlass(gui);
    player.openInventory(gui);
  }

  private void openPdcList(Player player, ItemData itemData) {
    currentScreen.put(player.getUniqueId(), 1);
    Inventory gui = Bukkit.createInventory(null, 54, "§8Custom NBT Tags");

    Map<String, String> nbt = itemData.getNbt();
    int slot = 9;
    for (Map.Entry<String, String> entry : nbt.entrySet()) {
      if (slot >= 45)
        break; // page limit
      String key = entry.getKey();
      String val = entry.getValue();
      gui.setItem(slot++, createGuiItem(Material.PAPER, "§e" + key,
          "§7Value: §b" + val,
          "",
          "§cClick to delete this tag"));
    }

    gui.setItem(45, createGuiItem(Material.LIME_DYE, "§a§l+ Add Custom NBT Tag",
        "§7Enter a new custom NBT/PDC tag",
        "§7directly via chat."));

    gui.setItem(46, createGuiItem(Material.KNOWLEDGE_BOOK, "§a§l+ Add Existing Key",
        "§7Select from NBT keys used in",
        "§7other custom items."));

    gui.setItem(49, createGuiItem(Material.OAK_DOOR, "§e« Back to Menu", "§7Return to NBT main menu"));

    fillGlass(gui);
    player.openInventory(gui);
  }

  private void openEnchantList(Player player, ItemData itemData) {
    currentScreen.put(player.getUniqueId(), 2);
    Inventory gui = Bukkit.createInventory(null, 54, "§8Item Enchantments");

    Map<String, Integer> enchants = itemData.getEnchants();
    int slot = 9;
    for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
      if (slot >= 45)
        break;
      String name = entry.getKey();
      int level = entry.getValue();
      gui.setItem(slot++, createGuiItem(Material.ENCHANTED_BOOK, "§e" + name,
          "§7Level: §b" + level,
          "",
          "§cClick to delete this enchantment"));
    }

    gui.setItem(45, createGuiItem(Material.LIME_DYE, "§a§l+ Add Enchantment",
        "§7Choose from all available",
        "§7enchantments on the server."));

    boolean hideEnchants = itemData.isHideEnchants();
    gui.setItem(46, createGuiItem(Material.FEATHER, "§e§lHide Enchantments (Glint Only)",
        "§7Status: " + (hideEnchants ? "§a§lTRUE (Glint only)" : "§c§lFALSE (Show tooltip)"),
        "",
        "§7Hides enchantment names and levels from",
        "§7the item's lore, but retains the shiny",
        "§7enchantment glint.",
        "",
        "§eClick to toggle"));

    gui.setItem(49, createGuiItem(Material.OAK_DOOR, "§e« Back to Menu", "§7Return to NBT main menu"));

    fillGlass(gui);
    player.openInventory(gui);
  }

  private void openSelectExistingKey(Player player) {
    currentScreen.put(player.getUniqueId(), 3);
    UUID uuid = player.getUniqueId();
    int page = currentPage.getOrDefault(uuid, 0);

    Inventory gui = Bukkit.createInventory(null, 54, "§8Select Existing NBT Key");

    // Pre-populate standard Minecraft NBT / Data Component keys
    Set<String> keys = new LinkedHashSet<>();
    keys.add("minecraft:damage");
    keys.add("minecraft:unbreakable");
    keys.add("minecraft:custom_model_data");
    keys.add("minecraft:repair_cost");
    keys.add("minecraft:hide_flags");
    keys.add("minecraft:dyed_color");
    keys.add("minecraft:max_stack_size");
    keys.add("minecraft:rarity");
    keys.add("minecraft:fire_resistant");
    keys.add("minecraft:potion_contents");
    keys.add("minecraft:attribute_modifiers");
    keys.add("minecraft:trim");
    keys.add("minecraft:hide_additional_tooltip");
    keys.add("minecraft:hide_tooltip");

    // Scan other custom items
    for (ItemData otherItem : itemDataManager.getAllItems().values()) {
      if (otherItem.getNbt() != null) {
        keys.addAll(otherItem.getNbt().keySet());
      }
    }
    // Scan player inventory
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.hasItemMeta()) {
        keys.addAll(org.bg52.curiospaper.util.VersionUtil.getPdcMap(item.getItemMeta()).keySet());
      }
    }

    List<String> keyList = new ArrayList<>(keys);
    int startIndex = page * 36;
    int endIndex = Math.min(startIndex + 36, keyList.size());

    int slot = 9;
    for (int i = startIndex; i < endIndex; i++) {
      String key = keyList.get(i);
      gui.setItem(slot++, createGuiItem(Material.BOOK, "§e" + key, "§7Click to select this key"));
    }

    if (page > 0) {
      gui.setItem(45, createGuiItem(Material.ARROW, "§e« Previous Page", "§7Page " + page));
    }
    if (endIndex < keyList.size()) {
      gui.setItem(53, createGuiItem(Material.ARROW, "§eNext Page »", "§7Page " + (page + 2)));
    }

    gui.setItem(49, createGuiItem(Material.OAK_DOOR, "§e« Back", "§7Go back to NBT tags"));

    fillGlass(gui);
    player.openInventory(gui);
  }

  private void openSelectEnchant(Player player) {
    currentScreen.put(player.getUniqueId(), 4);
    UUID uuid = player.getUniqueId();
    int page = currentPage.getOrDefault(uuid, 0);

    Inventory gui = Bukkit.createInventory(null, 54, "§8Select Enchantment");

    List<Enchantment> enchants = new ArrayList<>(Arrays.asList(Enchantment.values()));
    enchants.sort(Comparator.comparing(e -> e.getKey().getKey()));

    int startIndex = page * 36;
    int endIndex = Math.min(startIndex + 36, enchants.size());

    int slot = 9;
    for (int i = startIndex; i < endIndex; i++) {
      Enchantment enchant = enchants.get(i);
      String name = enchant.getKey().getKey();
      gui.setItem(slot++, createGuiItem(Material.BOOK, "§e" + name, "§7Click to select this enchantment"));
    }

    if (page > 0) {
      gui.setItem(45, createGuiItem(Material.ARROW, "§e« Previous Page", "§7Page " + page));
    }
    if (endIndex < enchants.size()) {
      gui.setItem(53, createGuiItem(Material.ARROW, "§eNext Page »", "§7Page " + (page + 2)));
    }

    gui.setItem(49, createGuiItem(Material.OAK_DOOR, "§e« Back", "§7Go back to enchantments"));

    fillGlass(gui);
    player.openInventory(gui);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player))
      return;
    Player player = (Player) event.getWhoClicked();
    UUID uuid = player.getUniqueId();

    if (!currentItemId.containsKey(uuid))
      return;
    String itemId = currentItemId.get(uuid);
    ItemData itemData = itemDataManager.getItemData(itemId);
    if (itemData == null)
      return;

    String title = event.getView().getTitle();
    if (!title.startsWith("§8NBT Editor:") && !title.equals("§8Custom NBT Tags") &&
        !title.equals("§8Item Enchantments") && !title.equals("§8Select Existing NBT Key") &&
        !title.equals("§8Select Enchantment")) {
      return;
    }

    event.setCancelled(true);

    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
      return;
    }

    int screen = currentScreen.getOrDefault(uuid, 0);

    if (screen == 0) { // Main Menu
      if (event.getSlot() == 10) {
        openPdcList(player, itemData);
      } else if (event.getSlot() == 12) {
        openEnchantList(player, itemData);
      } else if (event.getSlot() == 14) {
        itemData.setUnbreakable(!itemData.isUnbreakable());
        itemDataManager.saveItemData(itemId);
        openMainMenu(player, itemData);
      } else if (event.getSlot() == 16) {
        itemData.setPlaceable(!itemData.isPlaceable());
        itemDataManager.saveItemData(itemId);
        openMainMenu(player, itemData);
      } else if (event.getSlot() == 22) {
        currentItemId.remove(uuid);
        currentScreen.remove(uuid);
        currentPage.remove(uuid);
        plugin.getEditGUI().open(player, itemId);
      }
    } else if (screen == 1) { // PDC List
      if (event.getSlot() == 49) {
        openMainMenu(player, itemData);
      } else if (event.getSlot() == 45) { // Add Custom
        player.closeInventory();
        promptForCustomPdcKey(player, itemData);
      } else if (event.getSlot() == 46) { // Add Existing
        currentPage.put(uuid, 0);
        openSelectExistingKey(player);
      } else if (event.getSlot() >= 9 && event.getSlot() < 45) { // Delete
        String key = clicked.getItemMeta().getDisplayName().substring(2);
        Map<String, String> nbt = itemData.getNbt();
        nbt.remove(key);
        itemData.setNbt(nbt);
        itemDataManager.saveItemData(itemId);
        openPdcList(player, itemData);
      }
    } else if (screen == 2) { // Enchant List
      if (event.getSlot() == 49) {
        openMainMenu(player, itemData);
      } else if (event.getSlot() == 45) { // Add Enchant
        currentPage.put(uuid, 0);
        openSelectEnchant(player);
      } else if (event.getSlot() == 46) { // Toggle Hide Enchants
        itemData.setHideEnchants(!itemData.isHideEnchants());
        itemDataManager.saveItemData(itemId);
        openEnchantList(player, itemData);
      } else if (event.getSlot() >= 9 && event.getSlot() < 45) { // Delete
        String name = clicked.getItemMeta().getDisplayName().substring(2);
        Map<String, Integer> enchants = itemData.getEnchants();
        enchants.remove(name);
        itemData.setEnchants(enchants);
        itemDataManager.saveItemData(itemId);
        openEnchantList(player, itemData);
      }
    } else if (screen == 3) { // Select Existing Key
      if (event.getSlot() == 49) {
        openPdcList(player, itemData);
      } else if (event.getSlot() == 45) { // Previous Page
        int page = currentPage.getOrDefault(uuid, 0);
        currentPage.put(uuid, Math.max(0, page - 1));
        openSelectExistingKey(player);
      } else if (event.getSlot() == 53) { // Next Page
        int page = currentPage.getOrDefault(uuid, 0);
        currentPage.put(uuid, page + 1);
        openSelectExistingKey(player);
      } else if (event.getSlot() >= 9 && event.getSlot() < 45) {
        String key = clicked.getItemMeta().getDisplayName().substring(2);
        player.closeInventory();
        promptForExistingPdcValue(player, itemData, key);
      }
    } else if (screen == 4) { // Select Enchant
      if (event.getSlot() == 49) {
        openEnchantList(player, itemData);
      } else if (event.getSlot() == 45) { // Previous Page
        int page = currentPage.getOrDefault(uuid, 0);
        currentPage.put(uuid, Math.max(0, page - 1));
        openSelectEnchant(player);
      } else if (event.getSlot() == 53) { // Next Page
        int page = currentPage.getOrDefault(uuid, 0);
        currentPage.put(uuid, page + 1);
        openSelectEnchant(player);
      } else if (event.getSlot() >= 9 && event.getSlot() < 45) {
        String name = clicked.getItemMeta().getDisplayName().substring(2);
        player.closeInventory();
        promptForEnchantLevel(player, itemData, name);
      }
    }
  }

  private void promptForCustomPdcKey(Player player, ItemData itemData) {
    chatInputManager.startSingleLineSession(player,
        "§e§lCustom NBT Configuration\n" +
        "§eEnter NBT data using the syntax: §a§lkey = type:value\n" +
        "§7Example: §fmyplugin:power = int:42\n" +
        "§7Example: §fminecraft:custom_model_data = int:100\n" +
        "§7Supported types: string, int, double, float, byte, short, long",
        input -> {
          if (!input.contains("=")) {
            player.sendMessage("§cInvalid syntax! Format: key = type:value");
            Bukkit.getScheduler().runTaskLater(plugin, () -> openPdcList(player, itemData), 2L);
            return;
          }
          String[] parts = input.split("=", 2);
          String key = parts[0].trim().toLowerCase();
          String valPart = parts[1].trim();

          if (!key.contains(":")) {
            player.sendMessage("§cInvalid key format! Key must contain a colon (:), e.g. custom:key");
            Bukkit.getScheduler().runTaskLater(plugin, () -> openPdcList(player, itemData), 2L);
            return;
          }

          if (!valPart.contains(":")) {
            player.sendMessage("§cInvalid value format! Value must contain type:value, e.g. int:42");
            Bukkit.getScheduler().runTaskLater(plugin, () -> openPdcList(player, itemData), 2L);
            return;
          }

          String[] valSplit = valPart.split(":", 2);
          String type = valSplit[0].trim().toLowerCase();
          if (!Arrays.asList("string", "int", "integer", "double", "float", "byte", "short", "long").contains(type)) {
            player.sendMessage("§cInvalid type! Supported: string, int, double, float, byte, short, long");
            Bukkit.getScheduler().runTaskLater(plugin, () -> openPdcList(player, itemData), 2L);
            return;
          }

          Map<String, String> nbt = itemData.getNbt();
          nbt.put(key, type + ":" + valSplit[1].trim());
          itemData.setNbt(nbt);
          itemDataManager.saveItemData(itemData.getItemId());
          player.sendMessage("§a✓ Added NBT tag: §e" + key + " §7= §b" + type + ":" + valSplit[1].trim());
          Bukkit.getScheduler().runTaskLater(plugin, () -> openPdcList(player, itemData), 2L);
        },
        () -> Bukkit.getScheduler().runTaskLater(plugin, () -> openPdcList(player, itemData), 2L));
  }

  private void promptForExistingPdcValue(Player player, ItemData itemData, String key) {
    chatInputManager.startSingleLineSession(player,
        "§eEnter value with type in the format §a§ltype:value§e for key §b" + key + "§e:\n" +
        "§7Example: §fint:100\n" +
        "§7Example: §fstring:hello_world\n" +
        "§7Supported types: string, int, double, float, byte, short, long",
        input -> {
          if (!input.contains(":")) {
            player.sendMessage("§cInvalid format! Value must contain type:value, e.g. int:42");
            Bukkit.getScheduler().runTaskLater(plugin, () -> openPdcList(player, itemData), 2L);
            return;
          }
          String[] valSplit = input.split(":", 2);
          String type = valSplit[0].trim().toLowerCase();
          if (!Arrays.asList("string", "int", "integer", "double", "float", "byte", "short", "long").contains(type)) {
            player.sendMessage("§cInvalid type! Supported: string, int, double, float, byte, short, long");
            Bukkit.getScheduler().runTaskLater(plugin, () -> openPdcList(player, itemData), 2L);
            return;
          }

          Map<String, String> nbt = itemData.getNbt();
          nbt.put(key, type + ":" + valSplit[1].trim());
          itemData.setNbt(nbt);
          itemDataManager.saveItemData(itemData.getItemId());
          player.sendMessage("§a✓ Added NBT tag: §e" + key + " §7= §b" + type + ":" + valSplit[1].trim());
          Bukkit.getScheduler().runTaskLater(plugin, () -> openPdcList(player, itemData), 2L);
        },
        () -> Bukkit.getScheduler().runTaskLater(plugin, () -> openPdcList(player, itemData), 2L));
  }

  private void promptForEnchantLevel(Player player, ItemData itemData, String name) {
    chatInputManager.startSingleLineSession(player,
        "§e§lEnchantment Configuration\n§eEnter enchantment level (integer, e.g., 1-10):",
        lvlStr -> {
          try {
            int lvl = Integer.parseInt(lvlStr);
            Map<String, Integer> enchants = itemData.getEnchants();
            enchants.put(name, lvl);
            itemData.setEnchants(enchants);
            itemDataManager.saveItemData(itemData.getItemId());
            player.sendMessage("§a✓ Added enchantment: §e" + name + " §7level §b" + lvl);
          } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid level, enchantment not added.");
          }
          Bukkit.getScheduler().runTaskLater(plugin, () -> openEnchantList(player, itemData), 2L);
        },
        () -> Bukkit.getScheduler().runTaskLater(plugin, () -> openEnchantList(player, itemData), 2L));
  }

  private void fillGlass(Inventory gui) {
    ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    for (int i = 0; i < gui.getSize(); i++) {
      if (gui.getItem(i) == null) {
        gui.setItem(i, filler);
      }
    }
  }

  private ItemStack createGuiItem(Material material, String name, String... lore) {
    ItemStack item = new ItemStack(material, 1);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(name);
      meta.setLore(Arrays.asList(lore));
      item.setItemMeta(meta);
    }
    return item;
  }
}
