package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.config.SlotConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class AccessoryGUI {
    private final CuriosPaper plugin;
    public static final String MAIN_GUI_TITLE = "§8✦ Accessory Slots ✦";
    public static final String SLOTS_GUI_PREFIX = "§8Slot: ";

    private static final Material FILLER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material BORDER_MATERIAL = Material.BLACK_STAINED_GLASS_PANE;
    private static final String FILLER_NAME = "§r";

    // Store slot positions for each slot type
    private final Map<String, int[]> slotPositionCache = new HashMap<>();

    public AccessoryGUI(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the main Tier 1 GUI with all slot type buttons
     * Always uses double chest (54 slots) with beautiful layout
     */
    public void openMainGUI(Player player) {
        Map<String, SlotConfiguration> configs = plugin.getConfigManager().getSlotConfigurations();

        // Double chest size for beautiful layout
        int size = 45;
        Inventory mainGUI = Bukkit.createInventory(null, size, MAIN_GUI_TITLE);

        // Create border
        createBorder(mainGUI);

        // Fill remaining with gray glass
        fillInventory(mainGUI, FILLER_MATERIAL);

        // Get centered positions for buttons
        int[] buttonPositions = getMainGUIButtonPositions(configs.size());

        int index = 0;
        for (SlotConfiguration config : configs.values()) {
            if (index >= buttonPositions.length)
                break;
            ItemStack button = createSlotButton(config);
            mainGUI.setItem(buttonPositions[index++], button);
        }

        player.openInventory(mainGUI);
    }

    /**
     * Opens the Tier 2 GUI for a specific slot type
     * Dynamically sized and beautifully arranged based on slot count
     */
    public void openSlotItemsGUI(Player player, String slotType) {
        SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(slotType);
        if (config == null) {
            player.sendMessage("§cInvalid slot type!");
            return;
        }

        int slotAmount = config.getAmount();

        // Determine inventory size and get slot positions
        int size = calculateSlotGUISize(slotAmount);
        int[] slotPositions = calculateSlotPositions(slotAmount, size);

        // Cache the positions for this slot type
        slotPositionCache.put(slotType, slotPositions);

        Inventory slotsGUI = Bukkit.createInventory(null, size, SLOTS_GUI_PREFIX + config.getName());

        // Create border
        createBorder(slotsGUI);

        // Fill with gray glass
        fillInventory(slotsGUI, FILLER_MATERIAL);

        // Clear the accessory slots (remove filler)
        for (int slot : slotPositions) {
            slotsGUI.setItem(slot, null);
        }

        // Load current items
        List<ItemStack> currentItems = plugin.getSlotManager().getAccessories(player.getUniqueId(), slotType);
        for (int i = 0; i < currentItems.size() && i < slotPositions.length; i++) {
            ItemStack item = currentItems.get(i);
            if (item != null && !item.getType().isAir()) {
                slotsGUI.setItem(slotPositions[i], item);
            }
        }

        player.openInventory(slotsGUI);
    }

    /**
     * Get the positions for main GUI buttons (9 slot types)
     * Beautiful arrangement in double chest
     */
    private int[] getMainGUIButtonPositions(int count) {
        // For 9 items: 3x3 grid centered in double chest
        if (count == 9) {
            return new int[] {
                    10, 13, 16,
                    21, 22, 23,
                    28, 31, 34
            };
        } else {
            // Fallback: as many as needed in grid
            List<Integer> positions = new ArrayList<>();
            int startRow = 2;
            int itemsPerRow = 3;
            int row = startRow;
            int col = 2;

            for (int i = 0; i < count; i++) {
                positions.add(row * 9 + col);
                col++;
                if (col >= 2 + itemsPerRow) {
                    col = 2;
                    row++;
                }
            }

            return positions.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    /**
     * Calculate inventory size based on slot count
     */
    private int calculateSlotGUISize(int slotAmount) {
        // Use pattern-based sizing
        if (slotAmount <= 5) {
            return 27; // 3 rows for clean single-row layout
        } else if (slotAmount <= 16) {
            return 45; // 5 rows for nice patterns
        } else {
            return 54; // Double chest for many items
        }
    }

    /**
     * Calculate beautiful slot positions based on count and size
     */
    private int[] calculateSlotPositions(int slotAmount, int inventorySize) {
        int rows = inventorySize / 9;

        // Patterns for different counts
        if (slotAmount == 1) {
            // Single centered slot
            return new int[] { 13 }; // Center of 3-row inventory
        }

        if (slotAmount == 2) {
            // Two slots side by side
            return new int[] { 12, 14 };
        }

        if (slotAmount == 3) {
            // Triangle pattern
            return new int[] {
                    11, 13, 15
            };
        }

        if (slotAmount == 4) {
            // Square pattern
            return new int[] {
                    10, 12, 14, 16
            };
        }

        if (slotAmount == 5) {
            // Dice pattern
            return new int[] {
                    9, 11, 13, 15, 17
            };
        }

        if (slotAmount == 6) {
            // Two rows of 3
            return new int[] {
                    11, 13, 15,
                    28, 30, 32
            };
        }

        if (slotAmount == 7) {
            // Flower pattern
            return new int[] {
                    12, 14,
                    20, 22, 24,
                    30, 32
            };
        }

        if (slotAmount == 8) {
            // Octagon-ish pattern
            return new int[] {
                    11, 13, 15,
                    21, 23,
                    29, 31, 33
            };
        }

        if (slotAmount == 9) {
            // 3x3 grid
            return new int[] {
                    11, 13, 15,
                    20, 22, 24,
                    29, 31, 33
            };
        }

        if (slotAmount <= 12) {
            // Rectangular pattern
            return new int[] {
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25
            };
        }

        if (slotAmount <= 16) {
            // 4x4 grid centered
            List<Integer> positions = new ArrayList<>();
            int startRow = (rows - 4) / 2;
            int startCol = 2;

            for (int row = 0; row < 4 && positions.size() < slotAmount; row++) {
                for (int col = 0; col < 4 && positions.size() < slotAmount; col++) {
                    positions.add((startRow + row) * 9 + (startCol + col));
                }
            }

            return positions.stream().mapToInt(Integer::intValue).toArray();
        }

        // For large amounts: use most of the space efficiently
        List<Integer> positions = new ArrayList<>();
        int usableRows = rows - 2; // Leave room for borders
        int startRow = 1;

        int itemsPerRow = Math.min(7, slotAmount); // Max 7 per row for aesthetics
        int neededRows = (slotAmount + itemsPerRow - 1) / itemsPerRow;

        for (int row = 0; row < neededRows && positions.size() < slotAmount; row++) {
            int itemsInThisRow = Math.min(itemsPerRow, slotAmount - positions.size());
            int startCol = (9 - itemsInThisRow) / 2;

            for (int col = 0; col < itemsInThisRow; col++) {
                positions.add((startRow + row) * 9 + (startCol + col));
            }
        }

        return positions.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Creates a decorative border around the inventory
     */
    private void createBorder(Inventory inv) {
        ItemStack border = createFillerItem(BORDER_MATERIAL);
        int size = inv.getSize();

        // Top row
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
        }

        // Bottom row
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, border);
        }

        // Sides
        for (int row = 1; row < (size / 9) - 1; row++) {
            inv.setItem(row * 9, border); // Left
            inv.setItem(row * 9 + 8, border); // Right
        }
    }

    private ItemStack createSlotButton(SlotConfiguration config) {
        // Use configured base material or fallback to config icon if resource pack is
        // disabled
        Material material = config.getIcon();
        boolean useResourcePack = plugin.getConfig().getBoolean("resource-pack.enabled", false);

        if (useResourcePack) {
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
            meta.setDisplayName(config.getName());

            List<String> lore = new ArrayList<>(config.getLore());
            lore.add("");
            lore.add("§7Slots: §f" + config.getAmount());
            lore.add("§8▶ Click to open");
            meta.setLore(lore);

            // Set CustomModelData if enabled
            if (useResourcePack) {
                meta.setItemModel(config.getItemModel());
            }

            meta.getPersistentDataContainer().set(
                    plugin.getSlotTypeKey(),
                    PersistentDataType.STRING,
                    config.getKey());

            button.setItemMeta(meta);
        }

        return button;
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
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }

    /**
     * Check if a slot in the inventory is an accessory slot (not a filler)
     */
    public boolean isAccessorySlot(Inventory inv, int slot) {
        if (slot >= inv.getSize()) {
            return false;
        }

        ItemStack item = inv.getItem(slot);

        // Null or air = accessory slot
        if (item == null || item.getType().isAir()) {
            return true;
        }

        // Check if it's a filler item
        Material type = item.getType();
        if (type == FILLER_MATERIAL || type == BORDER_MATERIAL) {
            return false;
        }

        // It's an actual item
        return true;
    }

    /**
     * Check if there's at least one empty accessory slot
     */
    public boolean hasEmptyAccessorySlot(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);

            if (item == null || item.getType().isAir()) {
                // Check if this was supposed to be an accessory slot
                if (isAccessorySlot(inv, i)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the actual accessory slot positions for a slot type
     */
    public int[] getAccessorySlots(String slotType) {
        return slotPositionCache.getOrDefault(slotType, new int[0]);
    }

    public static boolean isMainGUI(String title) {
        return MAIN_GUI_TITLE.equals(title);
    }

    public static boolean isSlotsGUI(String title) {
        return title.startsWith(SLOTS_GUI_PREFIX);
    }

    public static String extractSlotTypeFromTitle(String title) {
        if (!isSlotsGUI(title)) {
            return null;
        }

        String name = title.substring(SLOTS_GUI_PREFIX.length());

        for (Map.Entry<String, SlotConfiguration> entry : CuriosPaper.getInstance().getConfigManager()
                .getSlotConfigurations().entrySet()) {
            if (entry.getValue().getName().equals(name)) {
                return entry.getKey();
            }
        }

        return null;
    }
}
