package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.RecipeData;
import org.bg52.curiospaper.listener.RecipeListener;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * RecipeEditorGUI — 6x9 (54) inventory layout with fixes for shaped rows and immediate save on buttons.
 */
public class RecipeEditorGUI implements Listener {

    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;

    private final Map<UUID, String> editingItem = new HashMap<>();

    private static final int[] GRID_SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32};

    private static final int SLOT_SHAPED = 47;
    private static final int SLOT_SHAPELESS = 49;
    private static final int SLOT_CLEAR = 51;

    public RecipeEditorGUI(CuriosPaper plugin) {
        this.plugin = plugin;
        this.itemDataManager = plugin.getItemDataManager();
    }

    public void open(Player player, String itemId) {
        ItemData data = itemDataManager.getItemData(itemId);
        if (data == null) {
            player.sendMessage("§cItem not found!");
            return;
        }

        editingItem.put(player.getUniqueId(), itemId);

        Inventory inv = Bukkit.createInventory(null, 54, "§6Recipe Editor: " + itemId);

        RecipeData recipe = data.getRecipe();
        if (recipe != null) {
            if (recipe.getType() == RecipeData.RecipeType.SHAPED && recipe.getShape() != null) {
                Map<Character, String> ingredients = recipe.getIngredients();
                String[] shape = recipe.getShape();
                for (int r = 0; r < Math.min(3, shape.length); r++) {
                    String row = shape[r] == null ? "   " : padRow(shape[r]);
                    for (int c = 0; c < 3; c++) {
                        char ch = row.charAt(c);
                        int gridIndex = r * 3 + c;
                        int slot = GRID_SLOTS[gridIndex];
                        if (ch != ' ' && ingredients.containsKey(ch)) {
                            String matName = ingredients.get(ch);
                            try {
                                Material m = Material.valueOf(matName.toUpperCase());
                                inv.setItem(slot, new ItemStack(m));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            } else if (recipe.getType() == RecipeData.RecipeType.SHAPELESS) {
                int pos = 0;
                for (String mat : recipe.getIngredients().values()) {
                    if (pos >= GRID_SLOTS.length) break;
                    try {
                        Material m = Material.valueOf(mat.toUpperCase());
                        inv.setItem(GRID_SLOTS[pos], new ItemStack(m));
                        pos++;
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        fillDecoration(inv);
        player.openInventory(inv);
    }

    private static String padRow(String row) {
        if (row == null) return "   ";
        if (row.length() >= 3) return row.substring(0, 3);
        return (row + "   ").substring(0, 3);
    }

    private void fillDecoration(Inventory inv) {
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        // Row 0 fill
        for (int i = 0; i <= 8; i++) inv.setItem(i, filler.clone());

        // Rows 1..3: fill cols 0..2 and 6..8, leave grid cols 3..5
        for (int row = 1; row <= 3; row++) {
            int base = row * 9;
            for (int c = 0; c <= 2; c++) {
                int slot = base + c;
                if (inv.getItem(slot) == null) inv.setItem(slot, filler.clone());
            }
            for (int c = 6; c <= 8; c++) {
                int slot = base + c;
                if (inv.getItem(slot) == null) inv.setItem(slot, filler.clone());
            }
        }

        // Row 4 fill
        for (int i = 36; i <= 44; i++) inv.setItem(i, filler.clone());

        // Row 5 fill except buttons
        for (int i = 45; i <= 53; i++) {
            if (i == SLOT_SHAPED || i == SLOT_SHAPELESS || i == SLOT_CLEAR ) continue;
            inv.setItem(i, filler.clone());
        }

        // Buttons
        inv.setItem(SLOT_SHAPED, createGuiItem(Material.OAK_SIGN, "§eSet Shaped", "§7Save current grid as a shaped recipe"));
        inv.setItem(SLOT_SHAPELESS, createGuiItem(Material.BOOK, "§eSet Shapeless", "§7Save current grid as a shapeless recipe"));
        inv.setItem(SLOT_CLEAR, createGuiItem(Material.BARRIER, "§cClear Grid", "§7Removes all items from the 3x3 area"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) return;
        Player player = (Player) clicker;

        String title = event.getView().getTitle();
        if (!title.startsWith("§6Recipe Editor: ")) return;

        UUID uid = player.getUniqueId();
        String itemId = editingItem.get(uid);
        if (itemId == null) {
            player.closeInventory();
            return;
        }

        Inventory top = event.getView().getTopInventory();
        Inventory bottom = event.getView().getBottomInventory();
        int raw = event.getRawSlot();
        Inventory clicked = event.getClickedInventory();

        if (clicked == null) {
            event.setCancelled(true);
            return;
        }

        // Allow player inventory interaction
        if (clicked.equals(bottom)) {
            event.setCancelled(false);
            return;
        }

        // Allow manipulation inside grid
        if (Arrays.stream(GRID_SLOTS).anyMatch(s -> s == raw)) {
            event.setCancelled(false);
            return;
        }

        // Otherwise handle buttons and cancel
        event.setCancelled(true);

        if (raw == SLOT_CLEAR) {
            for (int slot : GRID_SLOTS) top.setItem(slot, null);
            player.sendMessage("§eGrid cleared.");
            return;
        }

        if (raw == SLOT_SHAPED) {
            RecipeData rd = buildShapedFromGrid(top);
            if (rd == null || !rd.isValid()) {
                player.sendMessage("§cCannot create shaped recipe: no ingredients or invalid materials.");
                return;
            }
            boolean saved = persistRecipeFor(player, itemId, rd);
            if (saved) player.sendMessage("§a✔ Shaped recipe saved.");
            return;
        }

        if (raw == SLOT_SHAPELESS) {
            RecipeData rd = buildShapelessFromGrid(top);
            if (rd == null || !rd.isValid()) {
                player.sendMessage("§cCannot create shapeless recipe: no ingredients or invalid materials.");
                return;
            }
            boolean saved = persistRecipeFor(player, itemId, rd);
            if (saved) player.sendMessage("§a✔ Shapeless recipe saved.");
            return;
        }
    }

    private boolean persistRecipeFor(Player player, String itemId, RecipeData toSave) {
        ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null) {
            player.sendMessage("§cFailed to load item data.");
            editingItem.remove(player.getUniqueId());
            player.closeInventory();
            return false;
        }

        // Save to disk
        itemData.setRecipe(toSave);
        boolean saved = itemDataManager.saveItemData(itemId);
        if (!saved) {
            player.sendMessage("§cFailed to save recipe to disk.");
            return false;
        }

        // Immediately register with Bukkit by asking the RecipeListener to register this one recipe.
        try {
            // Use the recipe listener instance from the main plugin
            boolean registered = plugin.getRecipeListener().registerRecipe(itemData);
            if (!registered) {
                player.sendMessage("§cRecipe saved but failed to register with the server (check server logs).");
            } else {
                player.sendMessage("§a✔ Recipe saved and registered.");
            }
        } catch (Exception e) {
            player.sendMessage("§cRecipe saved but an error occurred during registration. See console.");
            plugin.getLogger().severe("Error registering recipe for " + itemId + ": " + e.getMessage());
            e.printStackTrace();
        }

        // close and return to edit GUI
        editingItem.remove(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getEditGUI().open(player, itemId), 1L);
        return true;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§6Recipe Editor: ")) return;
        HumanEntity he = event.getPlayer();
        if (he == null) return;
        editingItem.remove(he.getUniqueId());
    }

    // -> FIXED: This now returns a shape[] of length 3 (padded) so setShape won't throw.
    private RecipeData buildShapedFromGrid(Inventory inv) {
        ItemStack[][] matrix = new ItemStack[3][3];
        boolean any = false;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                ItemStack is = inv.getItem(GRID_SLOTS[r * 3 + c]);
                matrix[r][c] = is;
                if (is != null) any = true;
            }
        }
        if (!any) return null;

        int top = 0, bottom = 2, left = 0, right = 2;
        while (top <= bottom && rowEmpty(matrix[top])) top++;
        while (bottom >= top && rowEmpty(matrix[bottom])) bottom--;
        while (left <= right && colEmpty(matrix, left)) left++;
        while (right >= left && colEmpty(matrix, right)) right--;

        if (top > bottom || left > right) return null;

        int rows = bottom - top + 1;
        int cols = right - left + 1;

        Map<Character, String> ingredients = new LinkedHashMap<>();
        String[] compactShape = new String[rows];
        char nextKey = 'A';

        for (int r = 0; r < rows; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                ItemStack is = matrix[top + r][left + c];
                if (is == null) {
                    sb.append(' ');
                } else {
                    String matName = is.getType().name();
                    Character found = null;
                    for (Map.Entry<Character, String> e : ingredients.entrySet()) {
                        if (e.getValue().equals(matName)) {
                            found = e.getKey();
                            break;
                        }
                    }
                    if (found != null) {
                        sb.append(found);
                    } else {
                        ingredients.put(nextKey, matName);
                        sb.append(nextKey);
                        nextKey++;
                    }
                }
            }
            // compact row built (length == cols)
            compactShape[r] = sb.toString();
        }

        // Build a full 3-row shape (pad rows to length 3 and pad missing rows with spaces)
        String[] fullShape = new String[3];
        for (int i = 0; i < 3; i++) {
            if (i < compactShape.length) {
                fullShape[i] = padRow(compactShape[i]); // ensures length 3
            } else {
                fullShape[i] = "   ";
            }
        }

        RecipeData rd = new RecipeData(RecipeData.RecipeType.SHAPED);
        rd.setIngredients(ingredients);
        rd.setShape(fullShape); // safe: length == 3
        return rd;
    }

    private boolean rowEmpty(ItemStack[] row) {
        for (ItemStack is : row) if (is != null) return false;
        return true;
    }

    private boolean colEmpty(ItemStack[][] m, int col) {
        for (int r = 0; r < 3; r++) if (m[r][col] != null) return false;
        return true;
    }

    private RecipeData buildShapelessFromGrid(Inventory inv) {
        Map<Character, String> ingredients = new LinkedHashMap<>();
        char nextKey = 'A';
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            ItemStack is = inv.getItem(GRID_SLOTS[i]);
            if (is == null) continue;
            String mat = is.getType().name();
            boolean exists = ingredients.values().stream().anyMatch(v -> v.equals(mat));
            if (!exists) {
                ingredients.put(nextKey, mat);
                nextKey++;
            }
        }
        if (ingredients.isEmpty()) return null;
        RecipeData rd = new RecipeData(RecipeData.RecipeType.SHAPELESS);
        rd.setIngredients(ingredients);
        return rd;
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null && lore.length > 0) m.setLore(Arrays.asList(lore));
            item.setItemMeta(m);
        }
        return item;
    }
}
