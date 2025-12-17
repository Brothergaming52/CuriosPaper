package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.RecipeData;
import org.bg52.curiospaper.manager.ChatInputManager;
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

public class RecipeEditorGUI implements Listener {

    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;

    // Track which item a player is editing
    private final Map<UUID, String> editingItem = new HashMap<>();

    // Track which specific recipe (by index) a player is editing. -1 implies adding
    // new.
    private final Map<UUID, Integer> editingRecipeIndex = new HashMap<>();

    private static final String TITLE_MAIN = "§8Recipe Editor";
    private static final String TITLE_TYPE_SELECT = "§8Select Recipe Type";
    private static final String TITLE_EDITOR_PREFIX = "§8Recipe Edit: ";

    // Slot definitions
    private static final int[] GRID_SLOTS_CRAFTING = { 12, 13, 14, 21, 22, 23, 30, 31, 32 };

    public RecipeEditorGUI(CuriosPaper plugin) {
        this.plugin = plugin;
        this.itemDataManager = plugin.getItemDataManager();
    }

    /**
     * Opens the main recipe list for an item
     */
    public void open(Player player, String itemId) {
        ItemData data = itemDataManager.getItemData(itemId);
        if (data == null) {
            player.sendMessage("§cItem not found!");
            return;
        }

        editingItem.put(player.getUniqueId(), itemId);
        editingRecipeIndex.remove(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MAIN + " - " + itemId);

        List<RecipeData> recipes = data.getRecipes(); // Use getRecipes()

        // List existing recipes
        for (int i = 0; i < recipes.size() && i < 45; i++) {
            RecipeData r = recipes.get(i);
            Material icon = Material.PAPER;
            String desc = "Unknown Type";

            switch (r.getType()) {
                case SHAPED:
                    icon = Material.CRAFTING_TABLE;
                    desc = "Shaped Crafting";
                    break;
                case SHAPELESS:
                    icon = Material.BOOK; // or Shapeless icon
                    desc = "Shapeless Crafting";
                    break;
                case FURNACE:
                    icon = Material.FURNACE;
                    desc = "Furnace";
                    break;
                case BLAST_FURNACE:
                    icon = Material.BLAST_FURNACE;
                    desc = "Blast Furnace";
                    break;
                case SMOKER:
                    icon = Material.SMOKER;
                    desc = "Smoker";
                    break;
                case ANVIL:
                    icon = Material.ANVIL;
                    desc = "Anvil";
                    break;
                case SMITHING:
                    icon = Material.SMITHING_TABLE;
                    desc = "Smithing";
                    break;
            }

            inv.setItem(i, createGuiItem(icon, "§eRecipe #" + (i + 1),
                    "§7Type: " + desc,
                    "§7Click to edit",
                    "§cShift-Click to Delete"));
        }

        // Bottom Row
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(45, createGuiItem(Material.LIME_CONCRETE, "§a➕ Add Recipe"));
        inv.setItem(53, createGuiItem(Material.ARROW, "§cBack to Item Editor"));

        player.openInventory(inv);
    }

    /**
     * Opens type selection for new recipe
     */
    public void openTypeSelection(Player player, String itemId) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_TYPE_SELECT);

        inv.setItem(10, createGuiItem(Material.CRAFTING_TABLE, "§eCrafting Table", "§7Standard 3x3 crafting"));
        inv.setItem(11, createGuiItem(Material.FURNACE, "§eFurnace", "§7Smelting recipe"));
        inv.setItem(12, createGuiItem(Material.BLAST_FURNACE, "§eBlast Furnace", "§7Blasting recipe"));
        inv.setItem(13, createGuiItem(Material.SMOKER, "§eSmoker", "§7Smoking recipe"));
        inv.setItem(14, createGuiItem(Material.ANVIL, "§eAnvil", "§7Repair/Combine recipe"));
        inv.setItem(15, createGuiItem(Material.SMITHING_TABLE, "§eSmithing Table", "§7Upgrade gear recipe"));

        inv.setItem(26, createGuiItem(Material.ARROW, "§cCancel"));

        // Filler
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    /**
     * Opens specific editor for a recipe
     */
    public void openRecipeEditor(Player player, String itemId, int recipeIndex) {
        ItemData data = itemDataManager.getItemData(itemId);
        if (data == null)
            return;

        List<RecipeData> recipes = data.getRecipes();
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            player.sendMessage("§cInvalid recipe index.");
            open(player, itemId);
            return;
        }

        RecipeData recipe = recipes.get(recipeIndex);
        editingRecipeIndex.put(player.getUniqueId(), recipeIndex);

        String title = TITLE_EDITOR_PREFIX + itemId + " (#" + (recipeIndex + 1) + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        fillCommonEditor(inv);

        // Populate based on type
        switch (recipe.getType()) {
            case SHAPED:
            case SHAPELESS:
                setupCraftingEditor(inv, recipe);
                break;
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
                setupFurnaceEditor(inv, recipe);
                break;
            case ANVIL:
                setupAnvilEditor(inv, recipe);
                break;
            case SMITHING:
                setupSmithingEditor(inv, recipe);
                break;
        }

        player.openInventory(inv);
    }

    private void fillCommonEditor(Inventory inv) {
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(45, createGuiItem(Material.ARROW, "§eBack", "§7Discard changes (unless saved)"));
        inv.setItem(53, createGuiItem(Material.LIME_CONCRETE, "§aSave Changes", "§7Save and Apply"));
    }

    // --- Editor Setups ---

    private void setupCraftingEditor(Inventory inv, RecipeData recipe) {
        // Clear grid slots (fillCommonEditor filled them with gray)
        for (int slot : GRID_SLOTS_CRAFTING) {
            inv.setItem(slot, null);
        }

        // Populate if existing
        if (recipe.getType() == RecipeData.RecipeType.SHAPED && recipe.getShape() != null) {
            Map<Character, String> ingredients = recipe.getIngredients();
            String[] shape = recipe.getShape();
            for (int r = 0; r < 3; r++) {
                String row = (shape[r] == null ? "   " : shape[r] + "   ").substring(0, 3);
                for (int c = 0; c < 3; c++) {
                    char ch = row.charAt(c);
                    if (ch != ' ' && ingredients.containsKey(ch)) {
                        inv.setItem(GRID_SLOTS_CRAFTING[r * 3 + c], resolveItem(ingredients.get(ch)));
                    }
                }
            }
        } else if (recipe.getType() == RecipeData.RecipeType.SHAPELESS) {
            int i = 0;
            for (String ing : recipe.getIngredients().values()) {
                if (i < GRID_SLOTS_CRAFTING.length) {
                    inv.setItem(GRID_SLOTS_CRAFTING[i++], resolveItem(ing));
                }
            }
        }

        // Toggle Type Button
        boolean isShaped = recipe.getType() == RecipeData.RecipeType.SHAPED;
        inv.setItem(49, createGuiItem(isShaped ? Material.CRAFTING_TABLE : Material.BOOK,
                isShaped ? "§eType: Shaped" : "§eType: Shapeless",
                "§7Click to toggle"));

        inv.setItem(51, createGuiItem(Material.BARRIER, "§cClear Grid"));
    }

    private void setupFurnaceEditor(Inventory inv, RecipeData recipe) {
        inv.setItem(22, resolveItem(recipe.getInputItem())); // Input

        inv.setItem(40, createGuiItem(Material.CLOCK, "§eCook Time: " + (recipe.getCookingTime() / 20) + "s",
                "§7Click to edit"));
        inv.setItem(42,
                createGuiItem(Material.EXPERIENCE_BOTTLE, "§eXP: " + recipe.getExperience(), "§7Click to edit"));

        // Visual cues
        inv.setItem(31, createGuiItem(Material.FIRE_CHARGE, "§7Fuel (Visual)"));
    }

    private void setupAnvilEditor(Inventory inv, RecipeData recipe) {
        inv.setItem(21, resolveItem(recipe.getLeftInput()));
        inv.setItem(23, resolveItem(recipe.getRightInput()));

        inv.setItem(40, createGuiItem(Material.EXPERIENCE_BOTTLE, "§eXP Cost: " + (int) recipe.getExperience(),
                "§7Click to edit"));

        inv.setItem(22, createGuiItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7+"));
    }

    private void setupSmithingEditor(Inventory inv, RecipeData recipe) {
        inv.setItem(20, resolveItem(recipe.getTemplateItem())); // Template
        inv.setItem(22, resolveItem(recipe.getBaseItem())); // Base
        inv.setItem(24, resolveItem(recipe.getAdditionItem())); // Addition

        // Labels
        inv.setItem(11, createGuiItem(Material.PAPER, "§7Template"));
        inv.setItem(13, createGuiItem(Material.PAPER, "§7Base"));
        inv.setItem(15, createGuiItem(Material.PAPER, "§7Addition"));
    }

    // --- Helpers ---

    private ItemStack resolveItem(String name) {
        if (name == null)
            return null;
        try {
            // Check custom first? Or standard?
            if (itemDataManager.hasItem(name)) {
                return plugin.getCuriosPaperAPI().createItemStack(name);
            }
            return new ItemStack(Material.valueOf(name.toUpperCase()));
        } catch (Exception e) {
            return new ItemStack(Material.BARRIER); // Error indicator
        }
    }

    private String resolveName(ItemStack item) {
        if (item == null)
            return null;
        if (item.hasItemMeta()) {
            org.bukkit.NamespacedKey key = plugin.getCuriosPaperAPI().getItemIdKey();
            String custom = item.getItemMeta().getPersistentDataContainer().get(key,
                    org.bukkit.persistence.PersistentDataType.STRING);
            if (custom != null)
                return custom;
        }
        return item.getType().name();
    }

    // --- Event Handling ---

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.startsWith(TITLE_MAIN))
            handleMainClick(event, player);
        else if (title.startsWith(TITLE_TYPE_SELECT))
            handleTypeSelectClick(event, player);
        else if (title.startsWith(TITLE_EDITOR_PREFIX))
            handleEditorClick(event, player);
    }

    private void handleMainClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        String itemId = editingItem.get(player.getUniqueId());
        if (itemId == null)
            return;

        int slot = event.getRawSlot();
        if (slot >= event.getView().getTopInventory().getSize())
            return;

        ItemData data = itemDataManager.getItemData(itemId);

        if (slot == 45) { // Add
            openTypeSelection(player, itemId);
            return;
        }
        if (slot == 53) { // Back
            // Go back to main Item Editor (assuming plugin has one accesssible via command
            // or API)
            // For now just close or maybe plugin.getEditGUI().open(player, itemId) if
            // simple edit
            // But this IS the RecipeEditor. The user likely wants 'Back to Item Menu'
            // logic.
            // Assuming `plugin.getEditGUI().open(player, itemId)` exists as per
            // MobDropEditor reference.
            plugin.getEditGUI().open(player, itemId);
            return;
        }

        if (slot >= 0 && slot < 45) {
            List<RecipeData> recipes = data.getRecipes();
            if (slot < recipes.size()) {
                if (event.isShiftClick()) {
                    // Delete
                    recipes.remove(slot);
                    data.setRecipes(recipes); // Update list
                    itemDataManager.saveItemData(itemId);
                    plugin.getRecipeListener().registerAllRecipes(); // Re-register all to clear
                    player.sendMessage("§cDeleted recipe #" + (slot + 1));
                    open(player, itemId);
                } else {
                    // Edit
                    openRecipeEditor(player, itemId, slot);
                }
            }
        }
    }

    private void handleTypeSelectClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        String itemId = editingItem.get(player.getUniqueId());
        if (itemId == null)
            return;

        int slot = event.getRawSlot();
        RecipeData.RecipeType type = null;

        switch (slot) {
            case 10:
                type = RecipeData.RecipeType.SHAPED;
                break; // Default to shaped
            case 11:
                type = RecipeData.RecipeType.FURNACE;
                break;
            case 12:
                type = RecipeData.RecipeType.BLAST_FURNACE;
                break;
            case 13:
                type = RecipeData.RecipeType.SMOKER;
                break;
            case 14:
                type = RecipeData.RecipeType.ANVIL;
                break;
            case 15:
                type = RecipeData.RecipeType.SMITHING;
                break;
            case 26:
                open(player, itemId);
                return; // Cancel
        }

        if (type != null) {
            ItemData data = itemDataManager.getItemData(itemId);
            RecipeData newRecipe = new RecipeData(type);
            data.addRecipe(newRecipe); // Add new empty recipe
            itemDataManager.saveItemData(itemId);
            // Open editor for the new last recipe
            openRecipeEditor(player, itemId, data.getRecipes().size() - 1);
        }
    }

    private void handleEditorClick(InventoryClickEvent event, Player player) {
        String itemId = editingItem.get(player.getUniqueId());
        Integer index = editingRecipeIndex.get(player.getUniqueId());
        if (itemId == null || index == null)
            return;

        boolean cancel = true;

        Inventory top = event.getView().getTopInventory();
        Inventory bottom = event.getView().getBottomInventory();
        int slot = event.getRawSlot();

        // Allow player inventory logic
        if (event.getClickedInventory() == bottom) {
            event.setCancelled(false);
            return;
        }

        ItemData data = itemDataManager.getItemData(itemId);
        if (data == null || index >= data.getRecipes().size()) {
            player.closeInventory();
            return;
        }
        RecipeData recipe = data.getRecipes().get(index);

        // Logic based on type
        if (recipe.getType() == RecipeData.RecipeType.SHAPED || recipe.getType() == RecipeData.RecipeType.SHAPELESS) {
            // Crafting grid interaction
            for (int gridSlot : GRID_SLOTS_CRAFTING) {
                if (slot == gridSlot) {
                    cancel = false; // Allow interaction
                }
            }
            if (slot == 49) { // Toggle Type
                if (recipe.getType() == RecipeData.RecipeType.SHAPED)
                    recipe.setType(RecipeData.RecipeType.SHAPELESS);
                else
                    recipe.setType(RecipeData.RecipeType.SHAPED);
                // Redraw
                setupCraftingEditor(top, recipe);
            }
            if (slot == 51) { // Clear
                for (int s : GRID_SLOTS_CRAFTING)
                    top.setItem(s, null);
            }
        } else if (isFurnaceType(recipe.getType())) {
            if (slot == 22)
                cancel = false; // Input slot
            if (slot == 40) { // Time
                player.closeInventory();
                plugin.getChatInputManager().startSingleLineSession(player, "Enter cooking time in seconds:",
                        (input) -> {
                            try {
                                int seconds = Integer.parseInt(input);
                                recipe.setCookingTime(seconds * 20);
                                player.sendMessage("§aSet cooking time to " + seconds + "s.");
                            } catch (NumberFormatException e) {
                                player.sendMessage("§cInvalid number.");
                            }
                            Bukkit.getScheduler().runTask(plugin, () -> openRecipeEditor(player, itemId, index));
                        }, () -> Bukkit.getScheduler().runTask(plugin, () -> openRecipeEditor(player, itemId, index)));
            }
            if (slot == 42) { // XP
                player.closeInventory();
                plugin.getChatInputManager().startSingleLineSession(player, "Enter Experience yield:", (input) -> {
                    try {
                        float xp = Float.parseFloat(input);
                        recipe.setExperience(xp);
                        player.sendMessage("§aSet XP to " + xp);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid number.");
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> openRecipeEditor(player, itemId, index));
                }, () -> Bukkit.getScheduler().runTask(plugin, () -> openRecipeEditor(player, itemId, index)));
            }
        } else if (recipe.getType() == RecipeData.RecipeType.ANVIL) {
            if (slot == 21 || slot == 23)
                cancel = false;
            if (slot == 40) { // XP Cost
                player.closeInventory();
                plugin.getChatInputManager().startSingleLineSession(player, "Enter Cost (Levels):", (input) -> {
                    try {
                        float xp = Float.parseFloat(input);
                        recipe.setExperience(xp);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid number.");
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> openRecipeEditor(player, itemId, index));
                }, () -> Bukkit.getScheduler().runTask(plugin, () -> openRecipeEditor(player, itemId, index)));
            }
        } else if (recipe.getType() == RecipeData.RecipeType.SMITHING) {
            if (slot == 20 || slot == 22 || slot == 24)
                cancel = false;
        }

        event.setCancelled(cancel);

        // Save Button
        if (slot == 53) {
            saveRecipeFromView(top, recipe);
            itemDataManager.saveItemData(itemId);
            plugin.getRecipeListener().registerAllRecipes(); // Re-register
            player.sendMessage("§aRecipe saved.");
            open(player, itemId); // Return to list
            return;
        }

        // Back Button
        if (slot == 45) {
            open(player, itemId);
            return;
        }
    }

    private void saveRecipeFromView(Inventory inv, RecipeData recipe) {
        if (recipe.getType() == RecipeData.RecipeType.SHAPED) {
            saveShaped(inv, recipe);
        } else if (recipe.getType() == RecipeData.RecipeType.SHAPELESS) {
            saveShapeless(inv, recipe);
        } else if (isFurnaceType(recipe.getType())) {
            recipe.setInputItem(resolveName(inv.getItem(22)));
        } else if (recipe.getType() == RecipeData.RecipeType.ANVIL) {
            recipe.setLeftInput(resolveName(inv.getItem(21)));
            recipe.setRightInput(resolveName(inv.getItem(23)));
        } else if (recipe.getType() == RecipeData.RecipeType.SMITHING) {
            recipe.setTemplateItem(resolveName(inv.getItem(20)));
            recipe.setBaseItem(resolveName(inv.getItem(22)));
            recipe.setAdditionItem(resolveName(inv.getItem(24)));
        }
    }

    private void saveShaped(Inventory inv, RecipeData recipe) {
        Map<Character, String> ingredients = new HashMap<>();
        String[] shape = new String[3];
        char charCode = 'A';

        for (int r = 0; r < 3; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < 3; c++) {
                ItemStack item = inv.getItem(GRID_SLOTS_CRAFTING[r * 3 + c]);
                if (item == null) {
                    row.append(' ');
                } else {
                    String name = resolveName(item);
                    // Check existing char
                    Character existingChar = null;
                    for (Map.Entry<Character, String> e : ingredients.entrySet()) {
                        if (e.getValue().equals(name)) {
                            existingChar = e.getKey();
                            break;
                        }
                    }
                    if (existingChar != null) {
                        row.append(existingChar);
                    } else {
                        ingredients.put(charCode, name);
                        row.append(charCode);
                        charCode++;
                    }
                }
            }
            shape[r] = row.toString();
        }
        recipe.setIngredients(ingredients);
        recipe.setShape(shape);
    }

    private void saveShapeless(Inventory inv, RecipeData recipe) {
        Map<Character, String> ingredients = new HashMap<>();
        char charCode = 'A';
        for (int slot : GRID_SLOTS_CRAFTING) {
            ItemStack item = inv.getItem(slot);
            if (item != null) {
                ingredients.put(charCode++, resolveName(item));
            }
        }
        recipe.setIngredients(ingredients);
    }

    private boolean isFurnaceType(RecipeData.RecipeType type) {
        return type == RecipeData.RecipeType.FURNACE || type == RecipeData.RecipeType.BLAST_FURNACE
                || type == RecipeData.RecipeType.SMOKER;
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

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // Cleanup if needed, but we used maps, so maybe clear if completely done?
        // But player might be switching between chat/gui.
    }
}
