package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.LootTableData;
import org.bg52.curiospaper.manager.ChatInputManager;
import org.bg52.curiospaper.util.LootTableFetcher;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Loot Table Editor — full editor GUI for managing loot table entries on custom items.
 *
 * Flow:
 * 1. Main screen: lists existing loot table entries with Add/Edit/Delete/Back
 * 2. Browser screen: paginated loot table browser for selecting a table to add
 * 3. Quick-config screen: preset chance/amount selection
 *
 * Style follows the EditGUI reference (§8 titles, gray glass filler, consistent bottom bar).
 */
public class LootTableBrowser implements Listener {
  private final CuriosPaper plugin;
  private final ChatInputManager chat;

  // Player state tracking
  private final Map<UUID, String> editingItem = new HashMap<>();
  private final Map<UUID, Integer> selectedIndex = new HashMap<>();
  private final Map<UUID, BrowserState> browserStates = new HashMap<>();

  // How many loot tables per page in the browser (slots 0..35)
  private static final int PAGE_SIZE = 36;

  // Title prefixes
  private static final String TITLE_PREFIX = "§8Loot Tables: ";
  private static final String BROWSER_PREFIX = "§8Loot Browser: ";
  private static final String CONFIG_PREFIX = "§8Configure: ";

  public LootTableBrowser(CuriosPaper plugin) {
    this.plugin = plugin;
    this.chat = plugin.getChatInputManager();
  }

  // =========================================================================
  // Screen 1: Main Editor — list existing loot table entries
  // =========================================================================

  /**
   * Opens the main loot table editor for a player
   */
  public void open(Player player, String itemId) {
    ItemData itemData = plugin.getItemDataManager().getItemData(itemId);
    if (itemData == null) {
      player.sendMessage("§cItem not found!");
      return;
    }

    editingItem.put(player.getUniqueId(), itemId);

    Inventory gui = Bukkit.createInventory(null, 54, TITLE_PREFIX + itemId);

    // Fill slots 0..44 with gray glass
    ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    for (int i = 0; i <= 44; i++)
      gui.setItem(i, filler);

    // Show existing loot table entries in the top area (0..26)
    List<LootTableData> entries = itemData.getLootTables();
    for (int i = 0; i < entries.size() && i <= 26; i++) {
      LootTableData lt = entries.get(i);
      gui.setItem(i, createGuiItem(Material.CHEST,
          "§6" + lt.getLootTableType(),
          "§7Chance: §f" + (lt.getChance() * 100) + "%",
          "§7Amount: §f" + lt.getMinAmount() + " - " + lt.getMaxAmount(),
          "",
          "§eClick to select"));
    }

    // Bottom bar (45..53) — consistent with MobDropEditor/TradeEditor style
    gui.setItem(45, createGuiItem(Material.LIME_CONCRETE, "§a➕ Add"));
    gui.setItem(46, createGuiItem(Material.RED_CONCRETE, "§c✖ Delete Selected"));
    gui.setItem(47, createGuiItem(Material.YELLOW_CONCRETE, "§e✎ Edit Selected"));
    gui.setItem(49, createGuiItem(Material.OAK_DOOR, "§e« Back to Editor"));
    gui.setItem(53, createGuiItem(Material.ARMOR_STAND, "§b💾 Save"));

    // Fill remaining bottom bar slots
    ItemStack barFiller = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
    for (int i = 48; i <= 52; i++) {
      if (gui.getItem(i) == null)
        gui.setItem(i, barFiller);
    }

    player.openInventory(gui);
  }

  // =========================================================================
  // Screen 2: Browser — paginated loot table selection
  // =========================================================================

  private void openBrowser(Player player, String itemId) {
    List<NamespacedKey> all = fetchAllLootTableKeysSorted();
    BrowserState s = new BrowserState(itemId, all);
    browserStates.put(player.getUniqueId(), s);
    openBrowserPage(player, s, 0, "");
  }

  private List<NamespacedKey> fetchAllLootTableKeysSorted() {
    List<NamespacedKey> keys = LootTableFetcher.fetchAllLootTableKeys(plugin);
    keys.sort(Comparator.comparing(NamespacedKey::toString));
    return keys;
  }

  private void openBrowserPage(Player p, BrowserState s, int pageIndex, String filter) {
    List<NamespacedKey> filtered = s.allKeys;
    if (filter != null && !filter.isEmpty()) {
      String f = filter.toLowerCase();
      filtered = s.allKeys.stream()
          .filter(k -> k.getKey().toLowerCase().contains(f) || k.getNamespace().toLowerCase().contains(f))
          .collect(Collectors.toList());
    }
    int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) PAGE_SIZE));
    pageIndex = Math.max(0, Math.min(pageIndex, totalPages - 1));
    s.currentPage = pageIndex;
    s.currentFilter = filter;

    Inventory inv = Bukkit.createInventory(null, 54, BROWSER_PREFIX + s.itemId + " ("
        + (pageIndex + 1) + "/" + totalPages + ")");

    // Fill slots 0..35 with loot tables for this page
    int start = pageIndex * PAGE_SIZE;
    int end = Math.min(start + PAGE_SIZE, filtered.size());
    int slotIndex = 0;
    for (int i = start; i < end; i++) {
      NamespacedKey key = filtered.get(i);
      inv.setItem(slotIndex++, createGuiItem(Material.PAPER,
          "§6" + key.toString(),
          "§7Click to select this loot table"));
    }

    // Fill empty content slots with filler
    ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    for (int i = slotIndex; i < 36; i++)
      inv.setItem(i, filler);

    // Fill row 4 (36..44) with filler
    for (int i = 36; i < 45; i++)
      inv.setItem(i, filler);

    // Bottom bar (45..53)
    inv.setItem(45, createGuiItem(Material.ARROW, "§e« Prev Page", "§7Page " + Math.max(1, pageIndex)));
    inv.setItem(46, createGuiItem(Material.MAP, "§eSearch", "§7Click to type filter in chat"));
    inv.setItem(47, createGuiItem(Material.BARRIER, "§cClear Filter", "§7Click to remove search"));
    inv.setItem(49, createGuiItem(Material.COMPASS, "§aRefresh Registry", "§7Refresh loot table list"));
    inv.setItem(50, createGuiItem(Material.ARROW, "§eNext Page »", "§7Page " + Math.min(totalPages, pageIndex + 2)));
    inv.setItem(53, createGuiItem(Material.OAK_DOOR, "§e« Back to Loot Tables", "§7Return to editor"));

    // Fill remaining bottom bar slots
    ItemStack barFiller = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
    for (int i = 48; i <= 52; i++) {
      if (inv.getItem(i) == null)
        inv.setItem(i, barFiller);
    }

    p.openInventory(inv);
  }

  // =========================================================================
  // Screen 3: Quick Config — preset chance/amount selection
  // =========================================================================

  private void openQuickConfig(Player p, String itemId, NamespacedKey key) {
    QuickConfigState.create(p, itemId, key);

    Inventory inv = Bukkit.createInventory(null, 54, CONFIG_PREFIX + key.toString());

    // Header
    inv.setItem(4, createGuiItem(Material.CHEST, "§6§lConfigure Loot Entry",
        "§7Table: §f" + key.toString(),
        "",
        "§7Choose a preset or use custom"));

    // Row 2: Chance presets
    inv.setItem(19, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§aPreset 10%",
        "§7Chance: 10%", "§7Amount: 1", "", "§eClick to apply"));
    inv.setItem(21, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§aPreset 25%",
        "§7Chance: 25%", "§7Amount: 1", "", "§eClick to apply"));
    inv.setItem(23, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§aPreset 50%",
        "§7Chance: 50%", "§7Amount: 1", "", "§eClick to apply"));
    inv.setItem(25, createGuiItem(Material.GREEN_STAINED_GLASS_PANE, "§aPreset 100%",
        "§7Chance: 100%", "§7Amount: 1", "", "§eClick to apply"));

    // Row 3: Amount presets
    inv.setItem(28, createGuiItem(Material.PAPER, "§eAmount 1-1",
        "§7Uses default 10% chance", "", "§eClick to apply"));
    inv.setItem(30, createGuiItem(Material.PAPER, "§eAmount 1-3",
        "§7Uses default 10% chance", "", "§eClick to apply"));
    inv.setItem(32, createGuiItem(Material.PAPER, "§eAmount 2-5",
        "§7Uses default 10% chance", "", "§eClick to apply"));
    inv.setItem(34, createGuiItem(Material.BOOK, "§eCustom (chat)",
        "§7Enter exact chance & amount", "", "§eClick to configure via chat"));

    // Bottom bar
    inv.setItem(49, createGuiItem(Material.OAK_DOOR, "§e« Back to Browser"));

    // Fill with glass
    ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    for (int i = 0; i < 54; i++) {
      if (inv.getItem(i) == null)
        inv.setItem(i, filler);
    }

    p.openInventory(inv);
  }

  // =========================================================================
  // Event Handling
  // =========================================================================

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player))
      return;
    Player p = (Player) e.getWhoClicked();
    String title = e.getView().getTitle();

    // Main Editor screen
    if (title.startsWith(TITLE_PREFIX)) {
      handleMainClick(e, p, title);
      return;
    }

    // Browser screen
    if (title.startsWith(BROWSER_PREFIX)) {
      handleBrowserClick(e, p);
      return;
    }

    // Quick-config screen
    if (title.startsWith(CONFIG_PREFIX)) {
      handleQuickConfigClick(e, p);
      return;
    }
  }

  // ── Main Editor Click ──────────────────────────────────────────────────

  private void handleMainClick(InventoryClickEvent e, Player p, String title) {
    e.setCancelled(true);
    int raw = e.getRawSlot();
    if (raw >= e.getView().getTopInventory().getSize()) return;

    String itemId = editingItem.get(p.getUniqueId());
    if (itemId == null) return;

    ItemData itemData = plugin.getItemDataManager().getItemData(itemId);
    if (itemData == null) return;

    List<LootTableData> entries = itemData.getLootTables();

    // Selection area (0..26)
    if (raw >= 0 && raw <= 26) {
      if (raw < entries.size()) {
        selectedIndex.put(p.getUniqueId(), raw);
        p.sendMessage("§aSelected loot entry #" + (raw + 1) + " (" + entries.get(raw).getLootTableType() + ")");
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(p, itemId), 2L);
      }
      return;
    }

    switch (raw) {
      case 45: // Add → open browser
        p.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> openBrowser(p, itemId), 2L);
        break;

      case 46: // Delete Selected
        Integer sel = selectedIndex.get(p.getUniqueId());
        if (sel == null || sel < 0 || sel >= entries.size()) {
          p.sendMessage("§cNo entry selected!");
        } else {
          entries.remove((int) sel);
          plugin.getItemDataManager().saveItemData(itemId);
          selectedIndex.remove(p.getUniqueId());
          p.sendMessage("§aRemoved loot table entry.");
          Bukkit.getScheduler().runTaskLater(plugin, () -> open(p, itemId), 2L);
        }
        break;

      case 47: // Edit Selected → open quick-config with existing values
        Integer s = selectedIndex.get(p.getUniqueId());
        if (s == null || s < 0 || s >= entries.size()) {
          p.sendMessage("§cNo entry selected!");
        } else {
          LootTableData existing = entries.get(s);
          // Remove the old entry, re-add via quick-config flow
          try {
            NamespacedKey key = NamespacedKey.minecraft(existing.getLootTableType()
                .replaceFirst("^minecraft:", ""));
            // Delete old, let quick-config add new
            entries.remove((int) s);
            plugin.getItemDataManager().saveItemData(itemId);
            selectedIndex.remove(p.getUniqueId());
            p.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> openQuickConfig(p, itemId, key), 2L);
          } catch (Exception ex) {
            // Fallback: try as-is
            p.sendMessage("§cCould not parse loot table key. Delete and re-add manually.");
          }
        }
        break;

      case 49: // Back to Edit GUI
        p.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getEditGUI().open(p, itemId), 2L);
        break;

      case 53: // Save
        plugin.getItemDataManager().saveItemData(itemId);
        p.sendMessage("§aSaved loot table data for " + itemId);
        break;
    }
  }

  // ── Browser Click ──────────────────────────────────────────────────────

  private void handleBrowserClick(InventoryClickEvent e, Player p) {
    e.setCancelled(true);
    int raw = e.getRawSlot();
    if (raw >= e.getView().getTopInventory().getSize()) return;

    BrowserState s = browserStates.get(p.getUniqueId());
    if (s == null) {
      p.closeInventory();
      return;
    }

    // Navigation
    if (raw == 45) { // prev
      openBrowserPage(p, s, s.currentPage - 1, s.currentFilter);
      return;
    }
    if (raw == 50) { // next
      openBrowserPage(p, s, s.currentPage + 1, s.currentFilter);
      return;
    }
    if (raw == 46) { // search
      p.closeInventory();
      chat.startSingleLineSession(p, "Enter filter text (loot table key or namespace):",
          input -> {
            if (input != null) {
              openBrowserPage(p, s, 0, input.trim());
            } else {
              openBrowserPage(p, s, s.currentPage, s.currentFilter);
            }
          },
          () -> openBrowserPage(p, s, s.currentPage, s.currentFilter));
      return;
    }
    if (raw == 47) { // clear filter
      openBrowserPage(p, s, 0, "");
      return;
    }
    if (raw == 49) { // refresh
      s.allKeys = fetchAllLootTableKeysSorted();
      openBrowserPage(p, s, 0, s.currentFilter);
      p.sendMessage("§aLoot table registry refreshed (" + s.allKeys.size() + " entries)");
      return;
    }
    if (raw == 53) { // back to main editor
      browserStates.remove(p.getUniqueId());
      p.closeInventory();
      Bukkit.getScheduler().runTaskLater(plugin, () -> open(p, s.itemId), 2L);
      return;
    }

    // Click on loot table entry (0..35)
    if (raw >= 0 && raw < PAGE_SIZE) {
      int index = s.currentPage * PAGE_SIZE + raw;
      List<NamespacedKey> filtered = s.getFilteredKeys();
      if (index < 0 || index >= filtered.size()) return;

      NamespacedKey selected = filtered.get(index);
      p.closeInventory();
      Bukkit.getScheduler().runTaskLater(plugin, () -> openQuickConfig(p, s.itemId, selected), 2L);
    }
  }

  // ── Quick Config Click ─────────────────────────────────────────────────

  private void handleQuickConfigClick(InventoryClickEvent e, Player p) {
    e.setCancelled(true);
    int raw = e.getRawSlot();
    if (raw >= e.getView().getTopInventory().getSize()) return;

    QuickConfigState qs = QuickConfigState.forPlayer(p);
    if (qs == null) {
      p.closeInventory();
      return;
    }

    switch (raw) {
      case 19: // 10%
        addLootToItem(p, qs.itemId, qs.tableKey.toString(), 0.10, 1, 1);
        break;
      case 21: // 25%
        addLootToItem(p, qs.itemId, qs.tableKey.toString(), 0.25, 1, 1);
        break;
      case 23: // 50%
        addLootToItem(p, qs.itemId, qs.tableKey.toString(), 0.50, 1, 1);
        break;
      case 25: // 100%
        addLootToItem(p, qs.itemId, qs.tableKey.toString(), 1.0, 1, 1);
        break;
      case 28: // 1-1
        addLootToItem(p, qs.itemId, qs.tableKey.toString(), qs.defaultChance, 1, 1);
        break;
      case 30: // 1-3
        addLootToItem(p, qs.itemId, qs.tableKey.toString(), qs.defaultChance, 1, 3);
        break;
      case 32: // 2-5
        addLootToItem(p, qs.itemId, qs.tableKey.toString(), qs.defaultChance, 2, 5);
        break;
      case 34: // Custom via chat
        p.closeInventory();
        chat.startSingleLineSession(p,
            "Enter chance (0-1) then amount min-max separated by space. Example: 0.15 1-3",
            input -> {
              if (input == null || input.trim().isEmpty()) {
                open(p, qs.itemId);
                return;
              }
              try {
                String[] parts = input.trim().split("\\s+");
                double chance = Double.parseDouble(parts[0]);
                String[] rng = parts[1].split("-");
                int min = Integer.parseInt(rng[0]);
                int max = Integer.parseInt(rng[1]);
                addLootToItem(p, qs.itemId, qs.tableKey.toString(), chance, min, max);
              } catch (Exception ex) {
                p.sendMessage("§cInvalid format. Returning to loot table editor.");
                open(p, qs.itemId);
              }
            }, () -> open(p, qs.itemId));
        break;
      case 49: // Back to browser
        QuickConfigState.removeForPlayer(p);
        String itemId = qs.itemId;
        p.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> openBrowser(p, itemId), 2L);
        break;
    }
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private void addLootToItem(Player p, String itemId, String tableKey, double chance, int min, int max) {
    chance = Math.max(0.0, Math.min(1.0, chance));
    min = Math.max(1, min);
    max = Math.max(min, max);

    ItemData item = plugin.getItemDataManager().getItemData(itemId);
    if (item == null) {
      p.sendMessage("§cItem not found.");
      p.closeInventory();
      plugin.getEditGUI().open(p, itemId);
      return;
    }

    LootTableData lt = new LootTableData(tableKey, chance, min, max);
    item.addLootTable(lt);

    boolean saved = plugin.getItemDataManager().saveItemData(itemId);
    if (!saved) {
      p.sendMessage("§cFailed to save — check console for errors.");
      plugin.getLogger().warning("Failed to save item after adding loot: " + itemId);
    } else {
      p.sendMessage("§aAdded loot entry: §6" + tableKey + " §7(" + (chance * 100) + "%, " + min + "-" + max + ")");
    }

    // Cleanup and return to main editor
    QuickConfigState.removeForPlayer(p);
    browserStates.remove(p.getUniqueId());
    p.closeInventory();
    Bukkit.getScheduler().runTaskLater(plugin, () -> open(p, itemId), 2L);
  }

  private ItemStack createGuiItem(Material mat, String name, String... lore) {
    ItemStack i = new ItemStack(mat);
    ItemMeta m = i.getItemMeta();
    if (m != null) {
      m.setDisplayName(name);
      if (lore != null && lore.length > 0)
        m.setLore(Arrays.asList(lore));
      i.setItemMeta(m);
    }
    return i;
  }

  // =========================================================================
  // State classes
  // =========================================================================

  private static class BrowserState {
    String itemId;
    List<NamespacedKey> allKeys;
    int currentPage = 0;
    String currentFilter = "";

    BrowserState(String itemId, List<NamespacedKey> allKeys) {
      this.itemId = itemId;
      this.allKeys = allKeys;
    }

    List<NamespacedKey> getFilteredKeys() {
      if (currentFilter == null || currentFilter.isEmpty())
        return allKeys;
      String f = currentFilter.toLowerCase();
      return allKeys.stream()
          .filter(k -> k.getKey().toLowerCase().contains(f) || k.getNamespace().toLowerCase().contains(f))
          .collect(Collectors.toList());
    }
  }

  private static class QuickConfigState {
    final String itemId;
    final NamespacedKey tableKey;
    final double defaultChance = 0.10;

    private static final Map<UUID, QuickConfigState> MAP = new HashMap<>();

    private QuickConfigState(UUID player, String itemId, NamespacedKey key) {
      this.itemId = itemId;
      this.tableKey = key;
      MAP.put(player, this);
    }

    static void create(Player p, String itemId, NamespacedKey key) {
      new QuickConfigState(p.getUniqueId(), itemId, key);
    }

    static QuickConfigState forPlayer(Player p) {
      return MAP.get(p.getUniqueId());
    }

    static void removeForPlayer(Player p) {
      MAP.remove(p.getUniqueId());
    }
  }
}
