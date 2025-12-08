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
 * Click-to-select loot table browser with pagination and quick-config
 */
public class LootTableBrowser implements Listener {
    private final CuriosPaper plugin;
    private final ChatInputManager chat;
    private final Map<UUID, BrowserState> states = new HashMap<>();

    // How many loot tables per page (slots used 9*4=36)
    private static final int PAGE_SIZE = 36;

    public LootTableBrowser(CuriosPaper plugin) {
        this.plugin = plugin;
        this.chat = plugin.getChatInputManager();
    }

    /**
     * Opens the browser for a player to select a loot table for a specific itemId
     */
    public void open(Player player, String itemId) {
        List<NamespacedKey> all = fetchAllLootTableKeysSorted();
        BrowserState s = new BrowserState(itemId, all);
        states.put(player.getUniqueId(), s);
        openPage(player, s, 0, "");
    }

    // Fetch and sort all loot table keys (vanilla + datapacks + addons)
    private List<NamespacedKey> fetchAllLootTableKeysSorted() {
        // --- NMS Reflection approach for wider version compatibility ---
        List<NamespacedKey> keys = LootTableFetcher.fetchAllLootTableKeys(plugin); // 'this' = JavaPlugin instance

        // --- Fallback/Old API approach (Kept for reference if new util fails on a specific version) ---
        // try {
        //     Map<NamespacedKey, LootTable> reg = Bukkit.getLootTableRegistry();
        //     keys = new ArrayList<>(reg.keySet());
        // } catch (NoSuchMethodError ignored) {
        //     // continue with empty list or NMS result
        // }

        keys.sort(Comparator.comparing(NamespacedKey::toString));
        return keys;
    }

    // Build and open a page. 'filter' can be used if user typed a search term.
    private void openPage(Player p, BrowserState s, int pageIndex, String filter) {
        int totalPages;
        List<NamespacedKey> filtered = s.allKeys;
        if (filter != null && !filter.isEmpty()) {
            String f = filter.toLowerCase();
            filtered = s.allKeys.stream()
                    .filter(k -> k.getKey().toLowerCase().contains(f) || k.getNamespace().toLowerCase().contains(f))
                    .collect(Collectors.toList());
        }
        totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) PAGE_SIZE));
        pageIndex = Math.max(0, Math.min(pageIndex, totalPages - 1));
        s.currentPage = pageIndex;
        s.currentFilter = filter;

        Inventory inv = Bukkit.createInventory(null, 54, "§8Loot Browser: " + s.itemId + " ("
                + (pageIndex + 1) + "/" + totalPages + ")");

        // fill slots 0..35 with loot tables for this page
        int start = pageIndex * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());
        int slotIndex = 0;
        for (int i = start; i < end; i++) {
            NamespacedKey key = filtered.get(i);
            ItemStack it = new ItemStack(Material.PAPER);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName("§6" + key.toString());
            m.setLore(Arrays.asList("§7Click to select this loot table", "§7Vanilla/datapack/addon"));
            it.setItemMeta(m);
            inv.setItem(slotIndex++, it);
        }

        // Navigation & actions
        inv.setItem(45, makeItem(Material.ARROW, "§ePrev Page", "§7Page " + Math.max(1, pageIndex)));
        inv.setItem(46, makeItem(Material.MAP, "§eSearch", "§7Click to type filter in chat"));
        inv.setItem(47, makeItem(Material.BARRIER, "§cClear Filter", "§7Click to remove search"));
        inv.setItem(49, makeItem(Material.COMPASS, "§aAuto-detect (show list)", "§7Refresh registry"));
        inv.setItem(50, makeItem(Material.ARROW, "§eNext Page", "§7Page " + Math.min(totalPages, pageIndex + 2)));
        inv.setItem(53, makeItem(Material.REDSTONE, "§cCancel", "§7Return to editor"));

        p.openInventory(inv);
    }

    // Inventory click handler — routes interactions for browser and quick-config screens
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();

        // Browser screen
        if (title.startsWith("§8Loot Browser: ")) {
            e.setCancelled(true);
            int raw = e.getRawSlot();
            if (raw >= e.getView().getTopInventory().getSize()) return;

            BrowserState s = states.get(p.getUniqueId());
            if (s == null) {
                p.closeInventory();
                return;
            }

            // Click navigation area
            if (raw == 45) { // prev
                openPage(p, s, s.currentPage - 1, s.currentFilter);
                return;
            }
            if (raw == 50) { // next
                openPage(p, s, s.currentPage + 1, s.currentFilter);
                return;
            }
            if (raw == 46) { // search (open chat)
                p.closeInventory();
                chat.startSingleLineSession(p, "Enter filter text (loot table key or namespace):",
                        input -> {
                            if (input != null) {
                                openPage(p, s, 0, input.trim());
                            } else {
                                openPage(p, s, s.currentPage, s.currentFilter);
                            }
                        },
                        () -> openPage(p, s, s.currentPage, s.currentFilter));
                return;
            }
            if (raw == 47) { // clear filter
                openPage(p, s, 0, "");
                return;
            }
            if (raw == 49) { // refresh registry
                s.allKeys = fetchAllLootTableKeysSorted();
                openPage(p, s, 0, s.currentFilter);
                p.sendMessage("§aLoot table registry refreshed (" + s.allKeys.size() + " entries)");
                return;
            }
            if (raw == 53) { // cancel
                p.closeInventory();
                plugin.getEditGUI().open(p, s.itemId);
                states.remove(p.getUniqueId());
                return;
            }

            // If clicked on an entry slot (0..35)
            if (raw >= 0 && raw < PAGE_SIZE) {
                int index = s.currentPage * PAGE_SIZE + raw;
                List<NamespacedKey> filtered = s.getFilteredKeys();
                if (index < 0 || index >= filtered.size()) {
                    // clicked empty slot
                    return;
                }
                NamespacedKey selected = filtered.get(index);
                // open quick-config for this loot table
                openQuickConfig(p, s.itemId, selected);
            }
            return;
        }

        // Quick-config screen
        if (title.startsWith("§8Configure: ")) {
            e.setCancelled(true);
            int raw = e.getRawSlot();
            if (raw >= e.getView().getTopInventory().getSize()) return;

            QuickConfigState qs = QuickConfigState.forPlayer(p);
            if (qs == null) {
                p.closeInventory();
                return;
            }

            switch (raw) {
                case 10 -> { // Preset: common chance 10%, amount 1
                    addLootToItem(p, qs.itemId, qs.tableKey.toString(), 0.10, 1, 1);
                }
                case 12 -> { // Preset: 25%
                    addLootToItem(p, qs.itemId, qs.tableKey.toString(), 0.25, 1, 1);
                }
                case 14 -> { // Preset: 50%
                    addLootToItem(p, qs.itemId, qs.tableKey.toString(), 0.50, 1, 1);
                }
                case 16 -> { // Preset: 100%
                    addLootToItem(p, qs.itemId, qs.tableKey.toString(), 1.0, 1, 1);
                }
                case 28 -> { // Amount presets: 1-1
                    addLootToItem(p, qs.itemId, qs.tableKey.toString(), qs.defaultChance, 1, 1);
                }
                case 30 -> { // 1-3
                    addLootToItem(p, qs.itemId, qs.tableKey.toString(), qs.defaultChance, 1, 3);
                }
                case 32 -> { // 2-5
                    addLootToItem(p, qs.itemId, qs.tableKey.toString(), qs.defaultChance, 2, 5);
                }
                case 34 -> { // Custom via chat
                    p.closeInventory();
                    chat.startSingleLineSession(p, "Enter chance (0-1) then amount min-max separated by space. Example: 0.15 1-3",
                            input -> {
                                if (input == null || input.isBlank()) {
                                    // user cancelled — return to quick-config
                                    plugin.getLootTableBrowser().open(p, qs.itemId);
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
                                    p.sendMessage("§cInvalid format. Returning you to the configuration screen.");
                                    // return to quick-config so user can try again
                                    plugin.getLootTableBrowser().open(p, qs.itemId);
                                }
                            }, () -> plugin.getLootTableBrowser().open(p, qs.itemId));
                }
                case 49 -> { // Back to browser
                    plugin.getLootTableBrowser().open(p, qs.itemId);
                }
            }
            return;
        }
    }

    // Adds a LootTableData to the item and saves
    private void addLootToItem(Player p, String itemId, String tableKey, double chance, int min, int max) {
        // validate
        chance = Math.max(0.0, Math.min(1.0, chance));
        min = Math.max(1, min);
        max = Math.max(min, max);

        ItemData item = plugin.getItemDataManager().getItemData(itemId);
        if (item == null) {
            p.sendMessage("§cItem not found.");
            // close any open inventory and return to editor
            p.closeInventory();
            plugin.getEditGUI().open(p, itemId);
            return;
        }

        LootTableData lt = new LootTableData(tableKey, chance, min, max);
        // <-- THE FIX: mutate the internal list via the provided API
        item.addLootTable(lt);

        // persist and verify save succeeded
        boolean saved = plugin.getItemDataManager().saveItemData(itemId);
        if (!saved) {
            p.sendMessage("§cFailed to save item data — check console for errors.");
            plugin.getLogger().warning("Failed to save item after adding loot: " + itemId);
        } else {
            p.sendMessage("§aAdded loot entry: §6" + tableKey + " §7(" + (chance * 100) + "%, " + min + "-" + max + ")");
        }

        // Cleanup UI state: remove quick-config and browser states for this player if present
        QuickConfigState.removeForPlayer(p);
        states.remove(p.getUniqueId());

        // Close the quick-config BRUI and return to the Edit GUI for the item
        p.closeInventory();
        plugin.getEditGUI().open(p, itemId);
    }

    // Open a quick-config GUI for a selected loot table
    private void openQuickConfig(Player p, String itemId, NamespacedKey key) {
        QuickConfigState.create(p, itemId, key);

        Inventory inv = Bukkit.createInventory(null, 54, "§8Configure: " + key.toString());

        inv.setItem(10, makeItem(Material.LIME_STAINED_GLASS_PANE, "§aPreset 10%"));
        inv.setItem(12, makeItem(Material.LIME_STAINED_GLASS_PANE, "§aPreset 25%"));
        inv.setItem(14, makeItem(Material.LIME_STAINED_GLASS_PANE, "§aPreset 50%"));
        inv.setItem(16, makeItem(Material.GREEN_STAINED_GLASS_PANE, "§aPreset 100%"));

        inv.setItem(28, makeItem(Material.PAPER, "§eAmount 1-1"));
        inv.setItem(30, makeItem(Material.PAPER, "§eAmount 1-3"));
        inv.setItem(32, makeItem(Material.PAPER, "§eAmount 2-5"));
        inv.setItem(34, makeItem(Material.BOOK, "§eCustom (chat)"));

        inv.setItem(49, makeItem(Material.ARROW, "§eBack to browser"));

        p.openInventory(inv);
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name);
        if (lore != null && lore.length > 0) m.setLore(Arrays.asList(lore));
        i.setItemMeta(m);
        return i;
    }

    // Internal classes for keeping state
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
            if (currentFilter == null || currentFilter.isEmpty()) return allKeys;
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
