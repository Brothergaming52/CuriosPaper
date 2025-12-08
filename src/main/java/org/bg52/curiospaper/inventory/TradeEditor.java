package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.VillagerTradeData;
import org.bg52.curiospaper.data.VillagerTradeData.TradeCost;
import org.bg52.curiospaper.manager.ChatInputManager;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TradeEditor - GUI for managing villager trades for custom items
 * Follows the same pattern as MobDropEditor
 */
public class TradeEditor implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final ChatInputManager chatInputManager;

    // player -> editing itemId
    private final Map<UUID, String> editing = new HashMap<>();
    // player -> selected entry index inside that item's villagerTrades list
    private final Map<UUID, Integer> selectedIndex = new HashMap<>();

    // Temporary storage for trade creation flow
    private final Map<UUID, List<String>> pendingProfessions = new HashMap<>();
    private final Map<UUID, Double> pendingChance = new HashMap<>();
    private final Map<UUID, List<TradeCost>> pendingCosts = new HashMap<>();
    private final Map<UUID, List<Integer>> pendingLevels = new HashMap<>();

    private static final String TITLE_PREFIX = "§8Villager Trades: ";
    private static final String PROFESSION_SELECT_TITLE = "§8Select Profession(s)";

    public TradeEditor(CuriosPaper plugin) {
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

        // Fill 0..44 with empty glass panes
        ItemStack empty = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i <= 44; i++)
            gui.setItem(i, empty);

        // Show existing trade entries in the top area (0-26)
        List<VillagerTradeData> trades = itemData.getVillagerTrades();
        for (int i = 0; i < trades.size() && i <= 26; i++) {
            VillagerTradeData trade = trades.get(i);

            String profStr = trade.appliesToAllProfessions() ? "ALL" : String.join(", ", trade.getProfessions());
            List<String> lore = new ArrayList<>();
            lore.add("§7Professions: " + profStr);
            lore.add("§7Chance: " + (trade.getChance() * 100) + "%");
            lore.add("§7Costs:");
            for (TradeCost cost : trade.getCostItems()) {
                lore.add("  §7- " + cost.toString());
            }
            lore.add("§7Levels: "
                    + trade.getTradeLevels().stream().map(String::valueOf).collect(Collectors.joining(", ")));
            lore.add("§7Click to select");

            gui.setItem(i, createGuiItem(Material.EMERALD, "§e Trade #" + (i + 1), lore.toArray(new String[0])));
        }

        // Last row (45..53) controls
        gui.setItem(45, createGuiItem(Material.LIME_CONCRETE, "§a➕ Add"));
        gui.setItem(46, createGuiItem(Material.RED_CONCRETE, "§c✖ Delete Selected"));
        gui.setItem(47, createGuiItem(Material.YELLOW_CONCRETE, "§e✎ Edit Selected"));
        gui.setItem(48, createGuiItem(Material.BOOK, "§dℹ Preview Selected"));
        gui.setItem(52, createGuiItem(Material.ARROW, "§e← Back"));
        gui.setItem(53, createGuiItem(Material.ARMOR_STAND, "§b⚙ Save"));

        // Filler for remaining last-row slots
        for (int i = 49; i <= 51; i++) {
            if (gui.getItem(i) == null)
                gui.setItem(i, createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        player.openInventory(gui);
    }

    // --- Create profession selector GUI ---
    private Inventory createProfessionSelectGui(String itemId, List<String> currentlySelected) {
        Inventory inv = Bukkit.createInventory(null, 54, PROFESSION_SELECT_TITLE + " - " + itemId);

        // Get all villager professions
        Villager.Profession[] professions = Villager.Profession.values();

        // Add "All Professions" option at slot 0
        boolean allSelected = currentlySelected.isEmpty();
        Material allMat = allSelected ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
        inv.setItem(0, createGuiItem(allMat, "§e⚡ All Professions",
                allSelected ? "§a✔ Selected" : "§7Click to select"));

        // Add individual profession options
        int slot = 9; // Start from second row
        for (Villager.Profession prof : professions) {
            if (prof.name().equals("NONE") || prof.name().equals("NITWIT"))
                continue;

            boolean selected = currentlySelected.contains(prof.name());
            Material mat = getProfessionIcon(prof);
            Material displayMat = selected ? Material.LIME_STAINED_GLASS_PANE : mat;

            inv.setItem(slot, createGuiItem(displayMat, "§e" + prettifyProfession(prof.name()),
                    selected ? "§a✔ Selected" : "§7Click to toggle",
                    "§7Shift-click to select only this"));
            slot++;
            if (slot >= 45)
                break; // Don't go past row 4
        }

        // Add Wandering Trader as a special option
        if (slot < 45) {
            boolean wanderingSelected = currentlySelected.contains("WANDERING_TRADER");
            Material wanderingMat = wanderingSelected ? Material.LIME_STAINED_GLASS_PANE : Material.LLAMA_SPAWN_EGG;
            inv.setItem(slot, createGuiItem(wanderingMat, "§eWandering Trader",
                    wanderingSelected ? "§a✔ Selected" : "§7Click to toggle",
                    "§7Shift-click to select only this"));
        }

        // Controls
        inv.setItem(49, createGuiItem(Material.EMERALD, "§a✔ Confirm", "§7Proceed with selected professions"));
        inv.setItem(45, createGuiItem(Material.BARRIER, "§cCancel", "§7Return to trade list"));

        // Fill empty slots
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 1; i < 54; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, filler);
        }

        return inv;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // MAIN TradeEditor GUI
        if (title.startsWith(TITLE_PREFIX)) {
            event.setCancelled(true);
            int raw = event.getRawSlot();
            int topSize = event.getView().getTopInventory().getSize();
            if (raw >= topSize)
                return;

            String itemId = editing.get(player.getUniqueId());
            if (itemId == null)
                return;
            ItemData itemData = itemDataManager.getItemData(itemId);
            if (itemData == null)
                return;
            List<VillagerTradeData> trades = itemData.getVillagerTrades();

            // Selection area (0..26)
            if (raw >= 0 && raw <= 26) {
                if (raw < trades.size()) {
                    selectedIndex.put(player.getUniqueId(), raw);
                    player.sendMessage("§aSelected trade #" + (raw + 1));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
                }
                return;
            }

            switch (raw) {
                case 45: // Add -> open profession selector
                    player.closeInventory();
                    // Initialize empty selection
                    pendingProfessions.put(player.getUniqueId(), new ArrayList<>());
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> player.openInventory(createProfessionSelectGui(itemId, new ArrayList<>())), 2L);
                    break;
                case 46: // Delete Selected
                    Integer sel = selectedIndex.get(player.getUniqueId());
                    if (sel == null || sel < 0 || sel >= trades.size()) {
                        player.sendMessage("§cNo entry selected!");
                    } else {
                        trades.remove((int) sel);
                        itemDataManager.saveItemData(itemId);
                        selectedIndex.remove(player.getUniqueId());
                        player.sendMessage("§aRemoved trade entry.");
                        Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
                    }
                    break;
                case 47: // Edit Selected
                    Integer s = selectedIndex.get(player.getUniqueId());
                    if (s == null || s < 0 || s >= trades.size()) {
                        player.sendMessage("§cNo entry selected!");
                    } else {
                        VillagerTradeData existing = trades.get(s);
                        // Pre-populate the pending data with existing values
                        pendingProfessions.put(player.getUniqueId(), new ArrayList<>(existing.getProfessions()));
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin,
                                () -> player
                                        .openInventory(createProfessionSelectGui(itemId, existing.getProfessions())),
                                2L);
                    }
                    break;
                case 48: // Preview Selected
                    Integer p = selectedIndex.get(player.getUniqueId());
                    if (p == null || p < 0 || p >= trades.size()) {
                        player.sendMessage("§cNo entry selected!");
                    } else {
                        VillagerTradeData trade = trades.get(p);
                        player.sendMessage("§e▬▬▬ Trade Preview ▬▬▬");
                        player.sendMessage("§7Professions: " + (trade.appliesToAllProfessions() ? "ALL"
                                : String.join(", ", trade.getProfessions())));
                        player.sendMessage("§7Chance: " + (trade.getChance() * 100) + "%");
                        player.sendMessage(" §7Costs:");
                        for (TradeCost cost : trade.getCostItems()) {
                            player.sendMessage("  §7- " + cost.toString());
                        }
                        player.sendMessage("§7Levels: " + trade.getTradeLevels().stream().map(String::valueOf)
                                .collect(Collectors.joining(", ")));
                        player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    }
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

        // PROFESSION SELECT GUI
        if (title.startsWith(PROFESSION_SELECT_TITLE)) {
            event.setCancelled(true);
            int raw = event.getRawSlot();
            int topSize = event.getView().getTopInventory().getSize();
            if (raw >= topSize)
                return;

            String itemId = extractItemIdFromTitle(title, PROFESSION_SELECT_TITLE);
            if (itemId == null)
                return;

            List<String> currentSelections = pendingProfessions.getOrDefault(player.getUniqueId(), new ArrayList<>());

            if (raw == 0) { // All Professions
                if (currentSelections.isEmpty()) {
                    // Already all selected, do nothing
                } else {
                    // Clear selections = all professions
                    currentSelections.clear();
                    pendingProfessions.put(player.getUniqueId(), currentSelections);
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> player.openInventory(createProfessionSelectGui(itemId, currentSelections)), 2L);
                }
                return;
            }

            if (raw == 49) { // Confirm
                player.closeInventory();
                startTradeConfigurationFlow(player, itemId);
                return;
            }

            if (raw == 45) { // Cancel
                pendingProfessions.remove(player.getUniqueId());
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
                return;
            }

            // Individual profession click
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir())
                return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || meta.getDisplayName() == null)
                return;

            String displayName = meta.getDisplayName();
            String profName = displayName.replace("§e", "").replace(" ", "_").toUpperCase();

            // Handle special case: Wandering Trader
            if (profName.equals("WANDERING_TRADER")) {
                boolean isShiftClick = event.isShiftClick();

                if (isShiftClick) {
                    // Shift-click: select only wandering trader
                    currentSelections.clear();
                    currentSelections.add("WANDERING_TRADER");
                } else {
                    // Normal click: toggle
                    if (currentSelections.contains("WANDERING_TRADER")) {
                        currentSelections.remove("WANDERING_TRADER");
                    } else {
                        currentSelections.add("WANDERING_TRADER");
                    }
                }

                pendingProfessions.put(player.getUniqueId(), currentSelections);
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> player.openInventory(createProfessionSelectGui(itemId, currentSelections)), 2L);
                return;
            }

            // Validate it's a real villager profession
            try {
                Villager.Profession prof = Villager.Profession.valueOf(profName);
                if (prof.name().equals("NONE"))
                    return;

                boolean isShiftClick = event.isShiftClick();

                if (isShiftClick) {
                    // Shift-click: select only this profession
                    currentSelections.clear();
                    currentSelections.add(profName);
                } else {
                    // Normal click: toggle
                    if (currentSelections.contains(profName)) {
                        currentSelections.remove(profName);
                    } else {
                        currentSelections.add(profName);
                    }
                }

                pendingProfessions.put(player.getUniqueId(), currentSelections);
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> player.openInventory(createProfessionSelectGui(itemId, currentSelections)), 2L);

            } catch (IllegalArgumentException ignored) {
                // Not a valid profession, ignore click
            }
            return;
        }
    }

    // --- Trade Configuration Flow (via chat) ---
    private void startTradeConfigurationFlow(Player player, String itemId) {
        List<String> professions = pendingProfessions.getOrDefault(player.getUniqueId(), new ArrayList<>());

        player.sendMessage("§e▬▬▬ Trade Configuration ▬▬▬");
        player.sendMessage("§7Professions: " + (professions.isEmpty() ? "ALL" : String.join(", ", professions)));
        player.sendMessage("§eEnter the chance (0.0-1.0) for this trade to appear:");

        chatInputManager.startSingleLineSession(player, "Chance:",
                chanceStr -> {
                    if (chanceStr == null) {
                        cleanupPendingData(player);
                        reopenToEditGui(player, itemId);
                        return;
                    }
                    double chance;
                    try {
                        chance = Double.parseDouble(chanceStr);
                        if (chance < 0.0 || chance > 1.0) {
                            player.sendMessage("§cChance must be between 0.0 and 1.0");
                            cleanupPendingData(player);
                            reopenToEditGui(player, itemId);
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        player.sendMessage("§cInvalid number.");
                        cleanupPendingData(player);
                        reopenToEditGui(player, itemId);
                        return;
                    }

                    pendingChance.put(player.getUniqueId(), chance);
                    pendingCosts.put(player.getUniqueId(), new ArrayList<>());

                    // Start cost configuration
                    configureCostItem(player, itemId, 1);
                },
                () -> {
                    cleanupPendingData(player);
                    reopenToEditGui(player, itemId);
                });
    }

    private void configureCostItem(Player player, String itemId, int costNumber) {
        player.sendMessage("§eEnter cost item #" + costNumber + " material (or 'skip' for cost #2):");

        chatInputManager.startSingleLineSession(player, "Cost " + costNumber + " Material:",
                matStr -> {
                    if (matStr == null) {
                        cleanupPendingData(player);
                        reopenToEditGui(player, itemId);
                        return;
                    }

                    if (costNumber == 2 && matStr.equalsIgnoreCase("skip")) {
                        // Skip second cost, move to levels
                        configureLevels(player, itemId);
                        return;
                    }

                    Material mat;
                    try {
                        mat = Material.valueOf(matStr.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        player.sendMessage("§cInvalid material: " + matStr);
                        cleanupPendingData(player);
                        reopenToEditGui(player, itemId);
                        return;
                    }

                    player.sendMessage("§eEnter min amount for " + mat.name() + ":");
                    chatInputManager.startSingleLineSession(player, "Min Amount:",
                            minStr -> {
                                if (minStr == null) {
                                    cleanupPendingData(player);
                                    reopenToEditGui(player, itemId);
                                    return;
                                }
                                final int validatedMin;
                                try {
                                    int parsedMin = Integer.parseInt(minStr);
                                    if (parsedMin < 1) {
                                        parsedMin = 1;
                                    }
                                    if (parsedMin > 64) {
                                        player.sendMessage("§cAmount cannot exceed 64. Please try again.");
                                        configureCostItem(player, itemId, costNumber);
                                        return;
                                    }
                                    validatedMin = parsedMin;
                                } catch (NumberFormatException ex) {
                                    player.sendMessage("§cInvalid integer.");
                                    cleanupPendingData(player);
                                    reopenToEditGui(player, itemId);
                                    return;
                                }

                                player.sendMessage(
                                        "§eEnter max amount for " + mat.name() + " (>= " + validatedMin + "):");
                                chatInputManager.startSingleLineSession(player, "Max Amount:",
                                        maxStr -> {
                                            if (maxStr == null) {
                                                cleanupPendingData(player);
                                                reopenToEditGui(player, itemId);
                                                return;
                                            }
                                            int max;
                                            try {
                                                max = Integer.parseInt(maxStr);
                                                if (max < validatedMin) {
                                                    max = validatedMin;
                                                }
                                                if (max > 64) {
                                                    player.sendMessage("§cAmount cannot exceed 64. Please try again.");
                                                    player.sendMessage("§eEnter max amount for " + mat.name() + " (>= "
                                                            + validatedMin + "):");
                                                    // Re-prompt for max amount only
                                                    chatInputManager.startSingleLineSession(player, "Max Amount:",
                                                            newMaxStr -> {
                                                                if (newMaxStr == null) {
                                                                    cleanupPendingData(player);
                                                                    reopenToEditGui(player, itemId);
                                                                    return;
                                                                }
                                                                int newMax;
                                                                try {
                                                                    newMax = Integer.parseInt(newMaxStr);
                                                                    if (newMax < validatedMin) {
                                                                        newMax = validatedMin;
                                                                    }
                                                                    if (newMax > 64) {
                                                                        player.sendMessage(
                                                                                "§cAmount cannot exceed 64. Using 64.");
                                                                        newMax = 64;
                                                                    }
                                                                } catch (NumberFormatException ex2) {
                                                                    player.sendMessage(
                                                                            "§cInvalid integer. Using minimum value.");
                                                                    newMax = validatedMin;
                                                                }

                                                                // Add the cost with validated values
                                                                List<TradeCost> costs = pendingCosts
                                                                        .get(player.getUniqueId());
                                                                costs.add(new TradeCost(mat.name(), validatedMin,
                                                                        newMax));
                                                                pendingCosts.put(player.getUniqueId(), costs);

                                                                // Continue to next cost or levels
                                                                if (costNumber == 1) {
                                                                    configureCostItem(player, itemId, 2);
                                                                } else {
                                                                    configureLevels(player, itemId);
                                                                }
                                                            },
                                                            () -> {
                                                                cleanupPendingData(player);
                                                                reopenToEditGui(player, itemId);
                                                            });
                                                    return;
                                                }
                                            } catch (NumberFormatException ex) {
                                                player.sendMessage("§cInvalid integer.");
                                                cleanupPendingData(player);
                                                reopenToEditGui(player, itemId);
                                                return;
                                            }

                                            // Add the cost
                                            List<TradeCost> costs = pendingCosts.get(player.getUniqueId());
                                            costs.add(new TradeCost(mat.name(), validatedMin, max));
                                            pendingCosts.put(player.getUniqueId(), costs);

                                            // If this was cost #1, ask for cost #2
                                            if (costNumber == 1) {
                                                configureCostItem(player, itemId, 2);
                                            } else {
                                                // Done with costs, move to levels
                                                configureLevels(player, itemId);
                                            }
                                        },
                                        () -> {
                                            cleanupPendingData(player);
                                            reopenToEditGui(player, itemId);
                                        });
                            },
                            () -> {
                                cleanupPendingData(player);
                                reopenToEditGui(player, itemId);
                            });
                },
                () -> {
                    cleanupPendingData(player);
                    reopenToEditGui(player, itemId);
                });
    }

    private void configureLevels(Player player, String itemId) {
        player.sendMessage("§eEnter villager levels (1-5, comma-separated, or 'all'):");
        player.sendMessage("§7Example: 1,2,3 or 3,4,5 or all");

        chatInputManager.startSingleLineSession(player, "Levels:",
                levelStr -> {
                    if (levelStr == null) {
                        cleanupPendingData(player);
                        reopenToEditGui(player, itemId);
                        return;
                    }

                    List<Integer> levels = new ArrayList<>();
                    if (levelStr.trim().equalsIgnoreCase("all")) {
                        levels = Arrays.asList(1, 2, 3, 4, 5);
                    } else {
                        String[] parts = levelStr.split(",");
                        for (String part : parts) {
                            try {
                                int level = Integer.parseInt(part.trim());
                                if (level >= 1 && level <= 5 && !levels.contains(level)) {
                                    levels.add(level);
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }

                    if (levels.isEmpty()) {
                        player.sendMessage("§cNo valid levels specified. Using all levels (1-5).");
                        levels = Arrays.asList(1, 2, 3, 4, 5);
                    }

                    pendingLevels.put(player.getUniqueId(), levels);

                    // Create the trade
                    finalizeTrade(player, itemId);
                },
                () -> {
                    cleanupPendingData(player);
                    reopenToEditGui(player, itemId);
                });
    }

    private void finalizeTrade(Player player, String itemId) {
        ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null) {
            player.sendMessage("§cItem not found.");
            cleanupPendingData(player);
            return;
        }

        List<String> professions = pendingProfessions.get(player.getUniqueId());
        double chance = pendingChance.getOrDefault(player.getUniqueId(), 0.1);
        List<TradeCost> costs = pendingCosts.get(player.getUniqueId());
        List<Integer> levels = pendingLevels.getOrDefault(player.getUniqueId(), Arrays.asList(1, 2, 3, 4, 5));

        if (costs == null || costs.isEmpty()) {
            player.sendMessage("§cNo costs configured!");
            cleanupPendingData(player);
            reopenToEditGui(player, itemId);
            return;
        }

        VillagerTradeData trade = new VillagerTradeData(professions, chance, costs, levels);

        // Check if we're editing an existing trade
        Integer editIndex = selectedIndex.get(player.getUniqueId());
        if (editIndex != null && editIndex >= 0 && editIndex < itemData.getVillagerTrades().size()) {
            // Replace existing trade
            itemData.getVillagerTrades().set(editIndex, trade);
            player.sendMessage("§aUpdated trade: " + trade);
            selectedIndex.remove(player.getUniqueId());
        } else {
            // Add new trade
            itemData.addVillagerTrade(trade);
            player.sendMessage("§aAdded trade: " + trade);
        }

        itemDataManager.saveItemData(itemId);
        cleanupPendingData(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
    }

    private void cleanupPendingData(Player player) {
        UUID uuid = player.getUniqueId();
        pendingProfessions.remove(uuid);
        pendingChance.remove(uuid);
        pendingCosts.remove(uuid);
        pendingLevels.remove(uuid);
    }

    private void reopenToEditGui(Player player, String itemId) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
    }

    // --- Helper Methods ---

    private Material getProfessionIcon(Villager.Profession profession) {
        String profName = profession.name();

        if (profName.equals("FARMER"))
            return Material.COMPOSTER;
        if (profName.equals("FISHERMAN"))
            return Material.BARREL;
        if (profName.equals("SHEPHERD"))
            return Material.LOOM;
        if (profName.equals("FLETCHER"))
            return Material.FLETCHING_TABLE;
        if (profName.equals("LIBRARIAN"))
            return Material.LECTERN;
        if (profName.equals("CARTOGRAPHER"))
            return Material.CARTOGRAPHY_TABLE;
        if (profName.equals("CLERIC"))
            return Material.BREWING_STAND;
        if (profName.equals("ARMORER"))
            return Material.BLAST_FURNACE;
        if (profName.equals("WEAPONSMITH"))
            return Material.GRINDSTONE;
        if (profName.equals("TOOLSMITH"))
            return Material.SMITHING_TABLE;
        if (profName.equals("BUTCHER"))
            return Material.SMOKER;
        if (profName.equals("LEATHERWORKER"))
            return Material.CAULDRON;
        if (profName.equals("MASON"))
            return Material.STONECUTTER;

        return Material.VILLAGER_SPAWN_EGG;
    }

    private String prettifyProfession(String profession) {
        return Arrays.stream(profession.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private String extractItemIdFromTitle(String fullTitle, String prefix) {
        if (!fullTitle.startsWith(prefix))
            return null;
        String suffix = fullTitle.substring(prefix.length()).trim();
        int idx = suffix.lastIndexOf('-');
        if (idx == -1)
            return suffix.trim();
        return suffix.substring(idx + 1).trim();
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0)
                meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
