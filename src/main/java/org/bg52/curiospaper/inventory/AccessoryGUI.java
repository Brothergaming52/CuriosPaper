package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.config.SlotConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.md_5.bungee.api.ChatColor;

import java.util.*;

public class AccessoryGUI {
  private final CuriosPaper plugin;
  // These are loaded from messages.yml at construction time
  // and used for title matching in static methods
  private static String MAIN_GUI_TITLE = "Accessory Slots";
  private static String SLOTS_GUI_PREFIX = ChatColor.GOLD + "Slot: ";

  private static final Material FILLER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;
  private static final Material BORDER_MATERIAL = Material.BLACK_STAINED_GLASS_PANE;
  private static final String FILLER_NAME = "§r";

  // Store slot positions for each slot type (tier-2 GUI)
  private final Map<String, int[]> slotPositionCache = new HashMap<>();

  // ── Custom layout override ───────────────────────────────────────────────
  // Set by editmenu on close; saved to config.yml
  private Map<String, Integer> customLayout = null; // null = use preset
  private int customGuiSize = -1; // -1 = use preset

  public AccessoryGUI(CuriosPaper plugin) {
    this.plugin = plugin;
    // Load titles from messages.yml
    if (plugin.getMessagesManager() != null) {
      MAIN_GUI_TITLE = plugin.getMessagesManager().get("gui.main-title");
      SLOTS_GUI_PREFIX = plugin.getMessagesManager().get("gui.slot-title-prefix");
    }
  }

  // =========================================================================
  // Tier-1 main GUI
  // =========================================================================

  /**
   * Opens the main GUI showing only slot types that have at least one item
   * registered. Layout comes from the session override (editmenu) or the
   * built-in preset for the number of active slots.
   */
  public void openMainGUI(Player player) {
    List<String> activeKeys = plugin.getConfigManager().getActiveSlotKeys();

    int size;
    Map<String, Integer> layout;

    if (customLayout != null) {
      // editmenu rearranged things — honour the custom layout
      size = customGuiSize;
      layout = customLayout;
    } else {
      // Compute fresh preset
      size = computeMainGUISize(activeKeys.size());
      layout = computeMainGUILayout(activeKeys, size);
    }

    Inventory mainGUI = Bukkit.createInventory(null, size, MAIN_GUI_TITLE);

    for (String key : activeKeys) {
      SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(key);
      if (config == null)
        continue;

      ItemStack button = createSlotButton(config);
      int pos = layout.getOrDefault(key.toLowerCase(), -1);

      if (pos >= 0 && pos < size && isFillerOrEmpty(mainGUI.getItem(pos))) {
        mainGUI.setItem(pos, button);
      } else {
        // Fallback: first empty slot
        boolean placed = false;
        for (int i = 0; i < size; i++) {
          if (isFillerOrEmpty(mainGUI.getItem(i))) {
            mainGUI.setItem(i, button);
            placed = true;
            break;
          }
        }
        if (!placed) {
          plugin.getLogger().warning("Could not place slot button '" + key + "' — main GUI is full!");
        }
      }
    }

    fillInventory(mainGUI, FILLER_MATERIAL);
    if (plugin.getConfig().getBoolean("features.play-gui-sound", true)) {
      try {
        org.bukkit.Sound sound = org.bukkit.Sound.valueOf(plugin.getConfig().getString("features.gui-sound", "BLOCK_CHEST_OPEN").toUpperCase());
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
      } catch (Exception ignored) {}
    }
    player.openInventory(mainGUI);
  }

  // ── Custom layout saved API ──────────────────────────────────────────────

  /**
   * Applies a custom layout override (from editmenu).
   * This is written to disk and persists on server restart.
   */
  public void saveCustomLayout(Map<String, Integer> layout, int size) {
    this.customLayout = new HashMap<>(layout);
    this.customGuiSize = size;

    plugin.getConfig().set("gui.custom-size", size);
    plugin.getConfig().set("gui.layout", null); // Clear old layout map
    for (Map.Entry<String, Integer> entry : layout.entrySet()) {
      plugin.getConfig().set("gui.layout." + entry.getKey(), entry.getValue());
    }
    plugin.saveConfig();
  }

  public void loadCustomLayout() {
    if (plugin.getConfig().contains("gui.custom-size") && plugin.getConfig().contains("gui.layout")) {
      this.customGuiSize = plugin.getConfig().getInt("gui.custom-size");
      org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("gui.layout");
      if (section != null) {
        this.customLayout = new HashMap<>();
        for (String key : section.getKeys(false)) {
          this.customLayout.put(key, section.getInt(key));
        }
      }
    }
  }

  /** Resets the custom layout back to the computed preset. */
  public void resetCustomLayout() {
    this.customLayout = null;
    this.customGuiSize = -1;
    plugin.getConfig().set("gui.custom-size", null);
    plugin.getConfig().set("gui.layout", null);
    plugin.saveConfig();
  }

  /**
   * Returns the layout currently in effect (session override or computed preset).
   * EditMenuGUI uses this to render itself consistently.
   */
  public Map<String, Integer> getCurrentLayout() {
    if (customLayout != null)
      return new HashMap<>(customLayout);
    List<String> activeKeys = plugin.getConfigManager().getActiveSlotKeys();
    int size = computeMainGUISize(activeKeys.size());
    return computeMainGUILayout(activeKeys, size);
  }

  /**
   * Returns the GUI size currently in effect.
   */
  public int getCurrentGuiSize() {
    if (customGuiSize >= 9)
      return customGuiSize;
    List<String> activeKeys = plugin.getConfigManager().getActiveSlotKeys();
    return computeMainGUISize(activeKeys.size());
  }

  // ── Layout computation ────────────────────────────────────────────────────

  /**
   * Determines the inventory size (multiple of 9) for n active slot buttons.
   */
  private int computeMainGUISize(int n) {
    if (n <= 0)
      return 27;
    if (n <= 4)
      return 27;
    if (n <= 10)
      return 45;
    return 54;
  }

  /**
   * Returns a slot-key → inventory-index map for the given active keys.
   * Presets are defined for 1-10 slots; beyond that a grid is generated.
   *
   * All positions assume the inventory is filled with filler, so these are the
   * "open window" positions where buttons will sit.
   */
  private Map<String, Integer> computeMainGUILayout(List<String> keys, int size) {
    int n = keys.size();
    int[] positions = getPresetPositions(n, size);

    Map<String, Integer> layout = new LinkedHashMap<>();
    for (int i = 0; i < keys.size() && i < positions.length; i++) {
      layout.put(keys.get(i).toLowerCase(), positions[i]);
    }
    return layout;
  }

  /**
   * Hardcoded beautiful presets for 1-10 active slots.
   * For > 10 a centred grid is generated automatically.
   */
  private int[] getPresetPositions(int n, int size) {
    switch (n) {
      case 0:
        return new int[0];

      case 1:
        // Single slot — dead centre of a 27-slot GUI (row 1, slot 4)
        return new int[] { 13 };

      case 2:
        // Two slots side-by-side, centred
        return new int[] { 12, 14 };

      case 3:
        // Row of 3 centred
        return new int[] { 11, 13, 15 };

      case 4:
        // 2 × 2 diamond
        return new int[] { 10, 12, 14, 16 };

      case 5:
        // Full middle row of 27-slot GUI
        return new int[] { 12, 14, 22, 30, 32 };

      case 6:
        // 2 rows of 3, centred in 36-slot (4 rows)
        // row 1: slots 11, 13, 15 row 2: slots 20, 22, 24
        return new int[] { 11, 13, 15, 29, 31, 33 };

      case 7:
        // 3 + 4 layout in 36-slot
        return new int[] { 12, 14, 20, 22, 24, 30, 32 };

      case 8:
        // 4 + 4 in 36-slot
        return new int[] { 11, 13, 15, 21, 23, 29, 31, 33 };

      case 9:
        // 3 × 3 grid centred in 45-slot (5 rows), occupying rows 2-4
        return new int[] {
            10, 12, 14, 16,
            20, 22, 24,
            30, 32
        };

      case 10:
        // 3 + 4 + 3 in 45-slot
        return new int[] {
            11, 13, 15,
            19, 21, 23, 25,
            29, 31, 33
        };

      default:
        // > 10: auto grid, up to 7 per row, centred
        return generateGridPositions(n, size);
    }
  }

  /**
   * Generates centred grid slot positions for more than 10 active slots.
   */
  private int[] generateGridPositions(int n, int size) {
    int rows = size / 9;
    int itemsPerRow = Math.min(7, n);
    int neededRows = (n + itemsPerRow - 1) / itemsPerRow;
    int startRow = Math.max(1, (rows - neededRows) / 2);

    List<Integer> positions = new ArrayList<>();
    for (int row = 0; row < neededRows && positions.size() < n; row++) {
      int itemsInRow = Math.min(itemsPerRow, n - positions.size());
      int startCol = (9 - itemsInRow) / 2;
      for (int col = 0; col < itemsInRow; col++) {
        positions.add((startRow + row) * 9 + startCol + col);
      }
    }
    return positions.stream().mapToInt(Integer::intValue).toArray();
  }

  private boolean isFillerOrEmpty(ItemStack item) {
    if (item == null || item.getType() == Material.AIR)
      return true;
    ItemMeta meta = item.getItemMeta();
    return meta != null && FILLER_NAME.equals(meta.getDisplayName());
  }

  // =========================================================================
  // Tier-2 slot items GUI
  // =========================================================================

  /**
   * Opens the tier-2 GUI for a specific slot type showing the player's equipped
   * items. Dynamically sized and laid out based on slot count.
   */
  public void openSlotItemsGUI(Player player, String slotType) {
    SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(slotType);
    if (config == null) {
      player.sendMessage(plugin.getMessagesManager().get("gui.invalid-slot-type"));
      return;
    }

    int slotAmount = config.getAmount();
    int size = calculateSlotGUISize(slotAmount);
    int[] slotPositions = calculateSlotPositions(slotAmount, size);

    // Cache positions for use by InventoryListener
    slotPositionCache.put(slotType.toLowerCase(), slotPositions);

    Inventory slotsGUI = Bukkit.createInventory(null, size,
        SLOTS_GUI_PREFIX + ChatColor.YELLOW + config.getName());

    createBorder(slotsGUI);
    fillInventory(slotsGUI, FILLER_MATERIAL);

    // Back button — first slot of last row
    slotsGUI.setItem(size - 9, createBackButton());

    // Load accessory slot positions
    for (int slot : slotPositions) {
      if (plugin.getConfigManager().isShowEmptySlots()) {
        slotsGUI.setItem(slot, createSlotPlaceholder(config));
      } else {
        slotsGUI.setItem(slot, null);
      }
    }

    // Load equipped items
    List<ItemStack> currentItems = plugin.getSlotManager().getAccessories(player.getUniqueId(), slotType);
    for (int i = 0; i < currentItems.size() && i < slotPositions.length; i++) {
      ItemStack item = currentItems.get(i);
      if (item != null && item.getType() != Material.AIR) {
        slotsGUI.setItem(slotPositions[i], item);
      }
    }

    player.openInventory(slotsGUI);
  }

  private int calculateSlotGUISize(int slotAmount) {
    if (slotAmount <= 5)
      return 27;
    if (slotAmount <= 16)
      return 45;
    return 54;
  }

  private int[] calculateSlotPositions(int slotAmount, int inventorySize) {
    if (slotAmount == 1)
      return new int[] { 13 };
    if (slotAmount == 2)
      return new int[] { 12, 14 };
    if (slotAmount == 3)
      return new int[] { 11, 13, 15 };
    if (slotAmount == 4)
      return new int[] { 10, 12, 14, 16 };
    if (slotAmount == 5)
      return new int[] { 9, 11, 13, 15, 17 };

    if (slotAmount == 6)
      return new int[] { 11, 13, 15, 28, 30, 32 };

    if (slotAmount == 7)
      return new int[] { 12, 14, 20, 22, 24, 30, 32 };

    if (slotAmount == 8)
      return new int[] { 11, 13, 15, 21, 23, 29, 31, 33 };

    if (slotAmount == 9)
      return new int[] { 11, 13, 15, 20, 22, 24, 29, 31, 33 };

    if (slotAmount <= 12)
      return new int[] { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23 };

    if (slotAmount <= 16) {
      // 4 × 4 grid centred
      List<Integer> positions = new ArrayList<>();
      int rows = inventorySize / 9;
      int startRow = (rows - 4) / 2;
      for (int row = 0; row < 4 && positions.size() < slotAmount; row++) {
        for (int col = 0; col < 4 && positions.size() < slotAmount; col++) {
          positions.add((startRow + row) * 9 + 2 + col);
        }
      }
      return positions.stream().mapToInt(Integer::intValue).toArray();
    }

    // Large: fill efficiently
    List<Integer> positions = new ArrayList<>();
    int rows = inventorySize / 9;
    int itemsPerRow = Math.min(7, slotAmount);
    int neededRows = (slotAmount + itemsPerRow - 1) / itemsPerRow;
    int startRow = 1;

    for (int row = 0; row < neededRows && positions.size() < slotAmount; row++) {
      int itemsInRow = Math.min(itemsPerRow, slotAmount - positions.size());
      int startCol = (9 - itemsInRow) / 2;
      for (int col = 0; col < itemsInRow; col++) {
        positions.add((startRow + row) * 9 + startCol + col);
      }
    }
    return positions.stream().mapToInt(Integer::intValue).toArray();
  }

  // =========================================================================
  // Borders, fillers, buttons
  // =========================================================================

  private void createBorder(Inventory inv) {
    ItemStack border = createFillerItem(BORDER_MATERIAL);
    int size = inv.getSize();
    for (int i = 0; i < 9; i++)
      inv.setItem(i, border);
    for (int i = size - 9; i < size; i++)
      inv.setItem(i, border);
    for (int row = 1; row < (size / 9) - 1; row++) {
      inv.setItem(row * 9, border);
      inv.setItem(row * 9 + 8, border);
    }
  }

  private ItemStack createSlotButton(SlotConfiguration config) {
    Material material = config.getIcon();

    if (!plugin.getConfigManager().isUseVanillaItems()) {
      String baseMatName = plugin.getConfig().getString("resource-pack.base-material", "PAPER");
      try {
        material = Material.valueOf(baseMatName.toUpperCase());
      } catch (IllegalArgumentException e) {
        material = Material.PAPER;
      }
    }

    ItemStack button = new ItemStack(material);
    ItemMeta meta = button.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(ChatColor.GOLD + config.getName());

      List<String> lore = new ArrayList<>(config.getLore());
      lore.add("");
      lore.add(plugin.getMessagesManager().get("gui.slot-button-slots", "amount", String.valueOf(config.getAmount())));
      lore.add(plugin.getMessagesManager().get("gui.slot-button-click"));
      meta.setLore(lore);

      if (!plugin.getConfigManager().isUseVanillaItems()) {
        org.bg52.curiospaper.util.VersionUtil.setItemModelSafe(meta, config.getItemModel(),
            config.getCustomModelData());
      }

      meta.getPersistentDataContainer().set(
          plugin.getSlotTypeKey(),
          PersistentDataType.STRING,
          config.getKey());

      button.setItemMeta(meta);
    }
    return button;
  }

  private ItemStack createSlotPlaceholder(SlotConfiguration config) {
    ItemStack placeholder = createSlotButton(config);
    ItemMeta meta = placeholder.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(ChatColor.GRAY + config.getName() + " (" + plugin.getMessagesManager().get("gui.empty-slot", "Empty") + ")");
      List<String> lore = new ArrayList<>();
      lore.add(plugin.getMessagesManager().get("gui.empty-slot-lore", "&7Equip an item here."));
      meta.setLore(lore);

      // Mark as placeholder
      meta.getPersistentDataContainer().set(
          new NamespacedKey(plugin, "curios_placeholder"),
          PersistentDataType.BYTE,
          (byte) 1);

      placeholder.setItemMeta(meta);
    }
    return placeholder;
  }

  private ItemStack createBackButton() {
    ItemStack backButton = new ItemStack(Material.ARROW);
    ItemMeta meta = backButton.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(plugin.getMessagesManager().get("gui.back-button-name"));
      List<String> lore = new ArrayList<>();
      lore.add(plugin.getMessagesManager().get("gui.back-button-lore-1"));
      lore.add(plugin.getMessagesManager().get("gui.back-button-lore-2"));
      meta.setLore(lore);
      NamespacedKey backKey = new NamespacedKey(plugin, "curios_back_button");
      meta.getPersistentDataContainer().set(backKey, PersistentDataType.BYTE, (byte) 1);
      backButton.setItemMeta(meta);
    }
    return backButton;
  }

  public boolean isBackButton(ItemStack item) {
    if (item == null || item.getType() != Material.ARROW || !item.hasItemMeta())
      return false;
    NamespacedKey backKey = new NamespacedKey(plugin, "curios_back_button");
    return item.getItemMeta().getPersistentDataContainer().has(backKey, PersistentDataType.BYTE);
  }

  public boolean isPlaceholder(ItemStack item) {
    if (item == null || !item.hasItemMeta())
      return false;
    NamespacedKey placeholderKey = new NamespacedKey(plugin, "curios_placeholder");
    return item.getItemMeta().getPersistentDataContainer().has(placeholderKey, PersistentDataType.BYTE);
  }

  private ItemStack createFillerItem(Material material) {
    ItemStack filler = new ItemStack(material);
    ItemMeta meta = filler.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(FILLER_NAME);
      filler.setItemMeta(meta);
    }
    return filler;
  }

  private void fillInventory(Inventory inv, Material material) {
    ItemStack filler = createFillerItem(material);
    for (int i = 0; i < inv.getSize(); i++) {
      if (inv.getItem(i) == null)
        inv.setItem(i, filler);
    }
  }

  // =========================================================================
  // Utility: slot checks
  // =========================================================================

  public boolean isAccessorySlot(Inventory inv, int slot) {
    if (slot >= inv.getSize())
      return false;
    ItemStack item = inv.getItem(slot);
    if (item == null || item.getType() == Material.AIR)
      return true;
    Material type = item.getType();
    return type != FILLER_MATERIAL && type != BORDER_MATERIAL;
  }

  public boolean hasEmptyAccessorySlot(Inventory inv) {
    for (int i = 0; i < inv.getSize(); i++) {
      ItemStack item = inv.getItem(i);
      if ((item == null || item.getType() == Material.AIR || isPlaceholder(item)) && isAccessorySlot(inv, i)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns cached slot positions for the tier-2 GUI (used by InventoryListener).
   */
  public int[] getAccessorySlots(String slotType) {
    return slotPositionCache.getOrDefault(slotType.toLowerCase(), new int[0]);
  }

  // =========================================================================
  // Title helpers
  // =========================================================================

  public static boolean isMainGUI(String title) {
    return MAIN_GUI_TITLE.equals(title);
  }

  public static boolean isSlotsGUI(String title) {
    return title != null && title.startsWith(SLOTS_GUI_PREFIX);
  }

  public static String extractSlotTypeFromTitle(String title) {
    if (!isSlotsGUI(title))
      return null;
    String name = title.substring(SLOTS_GUI_PREFIX.length());
    name = org.bukkit.ChatColor.stripColor(name);
    for (Map.Entry<String, SlotConfiguration> entry : CuriosPaper.getInstance()
        .getConfigManager().getSlotConfigurations().entrySet()) {
      if (org.bukkit.ChatColor.stripColor(entry.getValue().getName()).equals(name)) {
        return entry.getKey();
      }
    }
    return null;
  }
}