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
import java.util.stream.Collectors;

/**
 * MobDropEditor updated:
 * - auto-discovers spawn eggs
 * - paginated (0..44 slots per page)
 * - search/filter via chat
 * - derive mob by removing _SPAWN_EGG from material name
 */
public class MobDropEditor implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final ChatInputManager chatInputManager;

    // player -> editing itemId
    private final Map<UUID, String> editing = new HashMap<>();
    // player -> selected entry index inside that item's mobDrops list
    private final Map<UUID, Integer> selectedIndex = new HashMap<>();

    // paging/filter state per player while in mob select GUI
    private final Map<UUID, Integer> mobSelectPage = new HashMap<>();
    private final Map<UUID, String> mobSelectFilter = new HashMap<>();

    // pending mob chosen for player (used by preset/custom flows)
    private final Map<UUID, String> pendingMobForPlayer = new HashMap<>();

    private static final String TITLE_PREFIX = "§8Mob Drops: ";
    private static final String MOB_SELECT_TITLE_BASE = "§8Select Mob";
    private static final String PRESET_TITLE = "§8Preset: ";

    private static final int ITEMS_PER_PAGE = 45; // slots 0..44

    public MobDropEditor(CuriosPaper plugin) {
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

        editing.put(player.getUniqueId(), itemId);

        Inventory gui = Bukkit.createInventory(null, 54, TITLE_PREFIX + itemId);

        // Fill 0..44 with empty glass panes to create "space"
        ItemStack empty = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i <= 44; i++) gui.setItem(i, empty);

        // show existing mob drop entries visually in the top-left area if any (optional)
        List<MobDropData> drops = itemData.getMobDrops();
        for (int i = 0; i < drops.size() && i <= 26; i++) { // show up to 27 visually
            MobDropData d = drops.get(i);
            gui.setItem(i, createGuiItem(Material.PAPER,
                    "§e" + d.getEntityType(),
                    "§7Chance: " + (d.getChance() * 100) + "%",
                    "§7Amount: " + d.getMinAmount() + " - " + d.getMaxAmount(),
                    "§7Click to select"));
        }

        // Last row (45..53) controls
        gui.setItem(45, createGuiItem(Material.LIME_CONCRETE, "§a➕ Add"));             // Add
        gui.setItem(46, createGuiItem(Material.RED_CONCRETE, "§c✖ Delete Selected")); // Delete
        gui.setItem(47, createGuiItem(Material.YELLOW_CONCRETE, "§e✎ Edit Selected"));// Edit
        gui.setItem(48, createGuiItem(Material.BOOK, "§dℹ Preview Selected"));       // Preview (optional)
        gui.setItem(49, createGuiItem(Material.COMPASS, "§eSearch"));                // Search (opens chat)
        gui.setItem(52, createGuiItem(Material.ARROW, "§e← Back"));                  // Back
        gui.setItem(53, createGuiItem(Material.ARMOR_STAND, "§b⚙ Save"));            // Save

        // filler for remaining last-row slots
        for (int i = 50; i <= 51; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        player.openInventory(gui);

        // reset paging/filter for this player when opening main editor
        mobSelectPage.remove(player.getUniqueId());
        mobSelectFilter.remove(player.getUniqueId());
    }

    // --- Create mob-select GUI for given page and filter ---
    private Inventory createMobSelectGui(String itemId, int page, String filter) {
        // Title will contain page and filter for clarity
        String title = MOB_SELECT_TITLE_BASE + " - p" + (page + 1);
        if (filter != null && !filter.isEmpty()) title += " (" + filter + ")";
        title += " - " + itemId;

        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Build list of all spawn egg materials + special-case boss icons
        List<Material> allEggs = Arrays.stream(Material.values())
                .filter(m -> m.name().endsWith("_SPAWN_EGG"))
                .sorted(Comparator.comparing(Enum::name))
                .collect(Collectors.toList());

        // include special boss icons at the end (if they exist on server version)
        if (hasMaterial(Material.DRAGON_EGG)) allEggs.add(Material.DRAGON_EGG);
        if (hasMaterial(Material.WITHER_SKELETON_SKULL)) allEggs.add(Material.WITHER_SKELETON_SKULL);

        // apply filter if present (case-insensitive substring match against material name)
        List<Material> filtered = allEggs;
        if (filter != null && !filter.isBlank()) {
            String q = filter.trim().toUpperCase();
            filtered = allEggs.stream()
                    .filter(m -> m.name().contains(q))
                    .collect(Collectors.toList());
        }

        int total = filtered.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));
        int safePage = Math.min(Math.max(0, page), pages - 1);

        int start = safePage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, total);

        // fill 0..44 with selected page materials
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < ITEMS_PER_PAGE; slot++) {
            int idx = start + slot;
            if (idx < end) {
                Material m = filtered.get(idx);
                inv.setItem(slot, createGuiItem(m, "§e" + prettifyEggName(m), "§7Click to choose"));
            } else {
                inv.setItem(slot, filler);
            }
        }

        // controls
        inv.setItem(45, createGuiItem(Material.ARROW, "§ePrev")); // prev
        inv.setItem(49, createGuiItem(Material.COMPASS, "§eSearch")); // search
        inv.setItem(53, createGuiItem(Material.BARRIER, "§cClose")); // close back to editor

        // show page indicator in center bottom
        inv.setItem(52, createGuiItem(Material.PAPER, "§7Page " + (safePage + 1) + " / " + pages,
                "§7Results: " + total,
                filter != null && !filter.isBlank() ? "§7Filter: " + filter : "§7No filter"));

        // next button placed at slot 51 to avoid conflicting with center indicator
        inv.setItem(51, createGuiItem(Material.ARROW, "§eNext"));

        // filler for any other empty bottom slots
        for (int i = 46; i <= 50; i++) if (inv.getItem(i) == null) inv.setItem(i, createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));

        return inv;
    }

    // helper to check material existence on server
    private boolean hasMaterial(Material m) {
        try {
            return m != null;
        } catch (NoSuchFieldError | Exception e) {
            return false;
        }
    }

    // --- Event handling ---
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // MAIN MobDropEditor GUI
        if (title.startsWith(TITLE_PREFIX)) {
            event.setCancelled(true);
            int raw = event.getRawSlot();
            int topSize = event.getView().getTopInventory().getSize();
            if (raw >= topSize) return;
            String itemId = editing.get(player.getUniqueId());
            if (itemId == null) return;
            ItemData itemData = itemDataManager.getItemData(itemId);
            if (itemData == null) return;
            List<MobDropData> drops = itemData.getMobDrops();

            // selection area (0..26 shown)
            if (raw >= 0 && raw <= 26) {
                if (raw < drops.size()) {
                    selectedIndex.put(player.getUniqueId(), raw);
                    player.sendMessage("§aSelected mob drop #" + (raw + 1) + " (" + drops.get(raw).getEntityType() + ")");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
                }
                return;
            }

            switch (raw) {
                case 45: // Add -> open mob select (page 0, no filter)
                    player.closeInventory();
                    mobSelectPage.put(player.getUniqueId(), 0);
                    mobSelectFilter.remove(player.getUniqueId());
                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(
                            createMobSelectGui(itemId, 0, "")), 2L);
                    break;
                case 46: // Delete Selected
                    Integer sel = selectedIndex.get(player.getUniqueId());
                    if (sel == null) {
                        player.sendMessage("§cNo entry selected!");
                    } else if (sel < 0 || sel >= drops.size()) {
                        player.sendMessage("§cSelected index out of range!");
                    } else {
                        drops.remove((int) sel);
                        itemDataManager.saveItemData(itemId);
                        selectedIndex.remove(player.getUniqueId());
                        player.sendMessage("§aRemoved drop entry.");
                        Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
                    }
                    break;
                case 47: // Edit Selected -> open preset for selected
                    Integer s = selectedIndex.get(player.getUniqueId());
                    if (s == null || s < 0 || s >= drops.size()) {
                        player.sendMessage("§cNo entry selected!");
                    } else {
                        MobDropData d = drops.get(s);
                        pendingMobForPlayer.put(player.getUniqueId(), d.getEntityType());
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(
                                createPresetGui(itemId, d.getEntityType())), 2L);
                    }
                    break;
                case 49: // Search (open chat)
                    player.closeInventory();
                    player.sendMessage("§eEnter search query to filter spawn eggs (empty = clear):");
                    chatInputManager.startSingleLineSession(player, "Search:",
                            query -> {
                                if (query == null) {
                                    // reopen mob select with old values
                                    int p = mobSelectPage.getOrDefault(player.getUniqueId(), 0);
                                    String f = mobSelectFilter.getOrDefault(player.getUniqueId(), "");
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createMobSelectGui(itemId, p, f)), 2L);
                                    return;
                                }
                                String q = query.trim();
                                mobSelectFilter.put(player.getUniqueId(), q);
                                mobSelectPage.put(player.getUniqueId(), 0);
                                Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createMobSelectGui(itemId, 0, q)), 2L);
                            }, () -> Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createMobSelectGui(itemId,
                                    mobSelectPage.getOrDefault(player.getUniqueId(), 0),
                                    mobSelectFilter.getOrDefault(player.getUniqueId(), ""))), 2L));
                    break;
                case 52: // Back -> main edit GUI
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getEditGUI().open(player, itemId), 2L);
                    break;
                case 53: // Save
                    itemDataManager.saveItemData(itemId);
                    player.sendMessage("§aSaved item data for " + itemId);
                    break;
            }
            return;
        }

        // MOB SELECT GUI
        if (title.startsWith(MOB_SELECT_TITLE_BASE)) {
            event.setCancelled(true);
            int raw = event.getRawSlot();
            int topSize = event.getView().getTopInventory().getSize();
            if (raw >= topSize) return;

            // Extract itemId and also detect page/filter from title
            String titleSuffix = title.substring(MOB_SELECT_TITLE_BASE.length()).trim();
            // title format: " - pN (filter) - itemId" or " - pN - itemId"
            String itemId = extractItemIdFromMobTitle(title);
            if (itemId == null) {
                player.sendMessage("§cMissing item id.");
                return;
            }

            // compute current page/filter state from stored maps (fallbacks)
            int page = mobSelectPage.getOrDefault(player.getUniqueId(), 0);
            String filter = mobSelectFilter.getOrDefault(player.getUniqueId(), "");

            // Prev button
            if (raw == 45) {
                int current = mobSelectPage.getOrDefault(player.getUniqueId(), page);
                int newPage = Math.max(0, current - 1);
                mobSelectPage.put(player.getUniqueId(), newPage);
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createMobSelectGui(itemId, newPage, filter)), 2L);
                return;
            }

            // Search button (open chat)
            if (raw == 49) {
                player.closeInventory();
                player.sendMessage("§eEnter search query to filter spawn eggs (empty = clear):");
                chatInputManager.startSingleLineSession(player, "Search:", query -> {
                    String q = query == null ? "" : query.trim();
                    mobSelectFilter.put(player.getUniqueId(), q);
                    mobSelectPage.put(player.getUniqueId(), 0);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createMobSelectGui(itemId, 0, q)), 2L);
                }, () -> Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createMobSelectGui(itemId,
                        mobSelectPage.getOrDefault(player.getUniqueId(), 0),
                        mobSelectFilter.getOrDefault(player.getUniqueId(), ""))), 2L));
                return;
            }

            // Next button
            if (raw == 51) {
                int current = mobSelectPage.getOrDefault(player.getUniqueId(), page);
                // we need to calculate pages based on current filter
                List<Material> list = getFilteredEggs(filter);
                int pages = Math.max(1, (int) Math.ceil((double) list.size() / ITEMS_PER_PAGE));
                int newPage = Math.min(pages - 1, current + 1);
                mobSelectPage.put(player.getUniqueId(), newPage);
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createMobSelectGui(itemId, newPage, filter)), 2L);
                return;
            }

            // Close / Back button
            if (raw == 53) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getEditGUI().open(player, itemId), 2L);
                return;
            }

            // click on an item slot 0..44
            if (raw >= 0 && raw < ITEMS_PER_PAGE) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType().isAir()) return;

                Material mat = clicked.getType();
                String mobName;
                if (mat.name().endsWith("_SPAWN_EGG")) {
                    mobName = mat.name().replace("_SPAWN_EGG", "");
                } else if (mat == Material.DRAGON_EGG) {
                    mobName = "ENDER_DRAGON";
                } else if (mat == Material.WITHER_SKELETON_SKULL) {
                    mobName = "WITHER";
                } else {
                    // fallback to material name
                    mobName = mat.name();
                }

                pendingMobForPlayer.put(player.getUniqueId(), mobName);
                // open preset GUI for chosen mob
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createPresetGui(itemId, mobName)), 2L);
            }

            return;
        }

        // PRESET GUI (unchanged)
        if (title.startsWith(PRESET_TITLE)) {
            event.setCancelled(true);
            int raw = event.getRawSlot();
            int topSize = event.getView().getTopInventory().getSize();
            if (raw >= topSize) return;
            String titleSuffix = title.substring(PRESET_TITLE.length()).trim();
            // title format: "<mobName> - <itemId>"
            String[] parts = titleSuffix.split("-", 2);
            if (parts.length < 2) return;
            String mobName = parts[0].trim();
            String itemId = parts[1].trim();
            ItemData itemData = itemDataManager.getItemData(itemId);
            if (itemData == null) return;

            if (raw == 8) { // Custom (chat)
                player.closeInventory();
                startCustomChatFlow(player, itemId, mobName);
                return;
            }

            // Preset mapping
            MobDropData created;
            switch (raw) {
                case 0: // Common
                    created = new MobDropData(mobName.toUpperCase(), 0.05, 1, 1);
                    break;
                case 1: // Uncommon
                    created = new MobDropData(mobName.toUpperCase(), 0.20, 1, 2);
                    break;
                case 2: // Rare
                    created = new MobDropData(mobName.toUpperCase(), 0.01, 2, 4);
                    break;
                case 3: // Boss
                    created = new MobDropData(mobName.toUpperCase(), 1.0, 8, 16);
                    break;
                default:
                    player.sendMessage("§cNo preset selected.");
                    return;
            }

            // store
            itemData.addMobDrop(created);
            itemDataManager.saveItemData(itemId);

            player.sendMessage("§aAdded mob drop: " + created);
            // close and reopen main edit GUI
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getEditGUI().open(player, itemId), 2L);
            return;
        }
    }

    // build preset GUI (same as before)
    private Inventory createPresetGui(String itemId, String mobName) {
        Inventory inv = Bukkit.createInventory(null, 9, PRESET_TITLE + mobName + " - " + itemId);
        inv.setItem(0, createGuiItem(Material.PAPER, "§aCommon", "§7Chance: 0.05 (5%)", "§71 - 1"));
        inv.setItem(1, createGuiItem(Material.PAPER, "§eUncommon", "§7Chance: 0.20 (20%)", "§71 - 2"));
        inv.setItem(2, createGuiItem(Material.PAPER, "§cRare", "§7Chance: 0.01 (1%)", "§72 - 4"));
        inv.setItem(3, createGuiItem(Material.END_CRYSTAL, "§5Boss", "§7Chance: 1.0 (100%)", "§78 - 16"));
        inv.setItem(8, createGuiItem(Material.PAPER, "§6Custom (chat)", "§7Open chat to enter chance/min/max"));

        // filler
        ItemStack f = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 4; i < 8; i++) if (inv.getItem(i) == null) inv.setItem(i, f);
        return inv;
    }

    // chat flow for custom values (same as before)
    private void startCustomChatFlow(Player player, String itemId, String mobName) {
        player.sendMessage("§eEnter chance (0.0 - 1.0) for " + mobName + " (e.g. 0.05):");
        chatInputManager.startSingleLineSession(player, "Chance:",
                chanceStr -> {
                    if (chanceStr == null) {
                        reopenToEditGui(player, itemId); return;
                    }
                    double chance;
                    try {
                        chance = Double.parseDouble(chanceStr);
                    } catch (NumberFormatException ex) {
                        player.sendMessage("§cInvalid number.");
                        reopenToEditGui(player, itemId); return;
                    }
                    chatInputManager.startSingleLineSession(player, "Min amount (>=1):",
                            minStr -> {
                                if (minStr == null) { reopenToEditGui(player, itemId); return; }
                                int min;
                                try { min = Math.max(1, Integer.parseInt(minStr)); } catch (NumberFormatException ex) {
                                    player.sendMessage("§cInvalid integer."); reopenToEditGui(player, itemId); return;
                                }
                                chatInputManager.startSingleLineSession(player, "Max amount (>=min):",
                                        maxStr -> {
                                            if (maxStr == null) { reopenToEditGui(player, itemId); return; }
                                            int max;
                                            try { max = Math.max(min, Integer.parseInt(maxStr)); } catch (NumberFormatException ex) {
                                                player.sendMessage("§cInvalid integer."); reopenToEditGui(player, itemId); return;
                                            }

                                            ItemData itemData = itemDataManager.getItemData(itemId);
                                            if (itemData == null) { player.sendMessage("§cItem not found."); return; }

                                            String mobUpper = mobName.toUpperCase();
                                            MobDropData d = new MobDropData(mobUpper, chance, min, max);
                                            itemData.addMobDrop(d);
                                            itemDataManager.saveItemData(itemId);
                                            player.sendMessage("§aAdded mob drop: " + d);

                                            // close and reopen main edit GUI
                                            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getEditGUI().open(player, itemId), 2L);
                                        },
                                        () -> reopenToEditGui(player, itemId));
                            },
                            () -> reopenToEditGui(player, itemId));
                },
                () -> reopenToEditGui(player, itemId));
    }

    private void reopenToEditGui(Player player, String itemId) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getEditGUI().open(player, itemId), 2L);
    }

    // helper to get all filtered eggs (used for page counts)
    private List<Material> getFilteredEggs(String filter) {
        List<Material> all = Arrays.stream(Material.values())
                .filter(m -> m.name().endsWith("_SPAWN_EGG"))
                .sorted(Comparator.comparing(Enum::name))
                .collect(Collectors.toList());
        if (hasMaterial(Material.DRAGON_EGG)) all.add(Material.DRAGON_EGG);
        if (hasMaterial(Material.WITHER_SKELETON_SKULL)) all.add(Material.WITHER_SKELETON_SKULL);

        if (filter == null || filter.isBlank()) return all;
        String q = filter.trim().toUpperCase();
        return all.stream().filter(m -> m.name().contains(q)).collect(Collectors.toList());
    }

    // pretty display name for an egg
    private String prettifyEggName(Material m) {
        if (m == Material.DRAGON_EGG) return "Ender Dragon";
        if (m == Material.WITHER_SKELETON_SKULL) return "Wither";
        String s = m.name();
        if (s.endsWith("_SPAWN_EGG")) s = s.replace("_SPAWN_EGG", "");
        return s.replace('_', ' ');
    }

    private String extractItemIdFromMobTitle(String fullTitle) {
        // fullTitle minus base: e.g. " - p1 (filter) - <itemId>" or " - p1 - <itemId>"
        // We'll take substring after the last '-' char
        int idx = fullTitle.lastIndexOf('-');
        if (idx == -1) return null;
        String tail = fullTitle.substring(idx + 1).trim();
        return tail.isEmpty() ? null : tail;
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
