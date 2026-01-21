package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.RecipeData;
import org.bg52.curiospaper.event.CuriosRecipeTransferEvent;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Handles registration of custom recipes and ensures custom ingredients are
 * respected.
 */
public class RecipeListener implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private static final Pattern VALID_KEY_PATTERN = Pattern.compile("[a-z0-9/._-]+");

    public RecipeListener(CuriosPaper plugin, ItemDataManager itemDataManager) {
        this.plugin = plugin;
        this.itemDataManager = itemDataManager;
    }

    /**
     * Registers all recipes from loaded items
     */
    public void registerAllRecipes() {
        int registered = 0;
        int failed = 0;

        for (ItemData itemData : itemDataManager.getAllItems().values()) {
            List<RecipeData> recipes = itemData.getRecipes();
            for (int i = 0; i < recipes.size(); i++) {
                if (registerRecipe(itemData, recipes.get(i), i, recipes.size() > 1)) {
                    registered++;
                } else {
                    failed++;
                }
            }
        }

        plugin.getLogger().info("Recipe registration complete.");
        plugin.getLogger().info("  Successfully registered: " + registered);
    }

    /**
     * Registers a recipe for a specific item
     */
    public boolean registerRecipe(ItemData itemData, RecipeData recipeData) {
        // Default to variant index 0 and no suffix if not specified
        return registerRecipe(itemData, recipeData, 0, false);
    }

    public boolean registerRecipe(ItemData itemData, RecipeData recipeData, int variantIndex, boolean hasMultiple) {
        if (recipeData == null || !recipeData.isValid()) {
            return false;
        }

        try {
            ItemStack result = createResultItem(itemData);
            if (result == null)
                return false;

            // Generate a stable key: custom_<plugin>_<itemid>[_variant<N>]
            String owningPlugin = itemData.getOwningPlugin();
            if (owningPlugin == null || owningPlugin.isEmpty()) {
                owningPlugin = "curiospaper";
            }
            owningPlugin = sanitizeKey(owningPlugin.toLowerCase());
            String safeItemId = sanitizeKey(itemData.getItemId().toLowerCase());

            String keyString = "custom_" + owningPlugin + "_" + safeItemId;
            if (hasMultiple) {
                keyString += "_variant" + variantIndex;
            }

            NamespacedKey key = new NamespacedKey(plugin, keyString);

            // Remove existing if any
            plugin.getServer().removeRecipe(key);

            switch (recipeData.getType()) {
                case SHAPED:
                    return registerShapedRecipe(key, result, recipeData);
                case SHAPELESS:
                    return registerShapelessRecipe(key, result, recipeData);
                case FURNACE:
                    return registerFurnaceRecipe(key, result, recipeData, false, false);
                case BLAST_FURNACE:
                    return registerFurnaceRecipe(key, result, recipeData, true, false);
                case SMOKER:
                    return registerFurnaceRecipe(key, result, recipeData, false, true);
                case SMITHING:
                    return registerSmithingRecipe(key, result, recipeData);
                case ANVIL:
                    return true;
                default:
                    return false;
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error registering recipe for " + itemData.getItemId() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String sanitizeKey(String key) {
        String sanitized = key.toLowerCase().replace(' ', '_');
        if (!VALID_KEY_PATTERN.matcher(sanitized).matches()) {
            // Fallback cleanup if it contains other invalid chars
            sanitized = sanitized.replaceAll("[^a-z0-9/._-]", "");
        }
        return sanitized;
    }

    private boolean registerShapedRecipe(NamespacedKey key, ItemStack result, RecipeData recipeData) {
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        String[] shape = recipeData.getShape();
        if (shape == null || shape.length != 3) {
            return false;
        }

        recipe.shape(shape[0], shape[1], shape[2]);

        for (Map.Entry<Character, String> entry : recipeData.getIngredients().entrySet()) {
            RecipeChoice choice = resolveIngredient(entry.getValue());
            if (choice == null)
                return false;
            recipe.setIngredient(entry.getKey(), choice);
        }

        plugin.getServer().addRecipe(recipe);
        return true;
    }

    private boolean registerShapelessRecipe(NamespacedKey key, ItemStack result, RecipeData recipeData) {
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);

        for (String ingredient : recipeData.getIngredients().values()) {
            RecipeChoice choice = resolveIngredient(ingredient);
            if (choice == null)
                return false;
            recipe.addIngredient(choice);
        }

        plugin.getServer().addRecipe(recipe);
        return true;
    }

    private boolean registerFurnaceRecipe(NamespacedKey key, ItemStack result, RecipeData recipe, boolean blast,
                                          boolean smoker) {
        RecipeChoice input = resolveIngredient(recipe.getInputItem());
        if (input == null)
            return false;

        org.bukkit.inventory.Recipe bukkitRecipe;
        if (blast) {
            bukkitRecipe = new org.bukkit.inventory.BlastingRecipe(key, result, input, recipe.getExperience(),
                    recipe.getCookingTime());
        } else if (smoker) {
            bukkitRecipe = new org.bukkit.inventory.SmokingRecipe(key, result, input, recipe.getExperience(),
                    recipe.getCookingTime());
        } else {
            bukkitRecipe = new org.bukkit.inventory.FurnaceRecipe(key, result, input, recipe.getExperience(),
                    recipe.getCookingTime());
        }

        plugin.getServer().addRecipe(bukkitRecipe);
        return true;
    }

    private boolean registerSmithingRecipe(NamespacedKey key, ItemStack result, RecipeData recipe) {
        RecipeChoice base = resolveIngredient(recipe.getBaseItem());
        RecipeChoice addition = resolveIngredient(recipe.getAdditionItem());
        RecipeChoice template = resolveIngredient(recipe.getTemplateItem());

        if (base == null || addition == null)
            return false;

        if (template == null)
            template = new RecipeChoice.MaterialChoice(Material.AIR);

        try {
            org.bukkit.inventory.SmithingTransformRecipe str = new org.bukkit.inventory.SmithingTransformRecipe(key,
                    result, template, base, addition);
            plugin.getServer().addRecipe(str);
            return true;
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("Smithing recipes require 1.20+ API.");
            return false;
        }
    }

    private RecipeChoice resolveIngredient(String ingredient) {
        if (ingredient == null)
            return null;
        try {
            Material material = Material.valueOf(ingredient.toUpperCase());
            return new RecipeChoice.MaterialChoice(material);
        } catch (IllegalArgumentException e) {
            if (itemDataManager.hasItem(ingredient)) {
                ItemData data = itemDataManager.getItemData(ingredient);
                if (data != null) {
                    try {
                        Material mat = Material.valueOf(data.getMaterial().toUpperCase());
                        return new RecipeChoice.MaterialChoice(mat);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        return null;
    }

    private ItemStack createResultItem(ItemData itemData) {
        return plugin.getCuriosPaperAPI().createItemStack(itemData.getItemId());
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null || !(recipe instanceof Keyed))
            return;

        NamespacedKey key = ((Keyed) recipe).getKey();
        if (!key.getNamespace().equalsIgnoreCase(plugin.getName()) || !key.getKey().startsWith("custom_")) {
            // Not a CuriosPaper custom recipe
            return;
        }

        ItemStack[] matrix = event.getInventory().getMatrix();
        // Strict Validation Logic
        if (!validateCustomIngredients(key, matrix)) {
            event.getInventory().setResult(null); // Invalid craft
            return;
        }

        // Use the first valid custom item as the transfer source
        ItemStack sourceForTransfer = null;
        for (ItemStack item : matrix) {
            if (item != null && item.hasItemMeta()) {
                PersistentDataContainer itemPdc = item.getItemMeta().getPersistentDataContainer();
                if (itemPdc.has(plugin.getCuriosPaperAPI().getItemIdKey(), PersistentDataType.STRING)) {
                    if (sourceForTransfer == null) {
                        sourceForTransfer = item;
                    }
                }
            }
        }

        ItemStack result = event.getInventory().getResult();
        if (sourceForTransfer != null && result != null) {
            CuriosRecipeTransferEvent transferEvent = new CuriosRecipeTransferEvent(event.getInventory(), result,
                    sourceForTransfer);
            plugin.getServer().getPluginManager().callEvent(transferEvent);

            if (!transferEvent.isCancelled()) {
                event.getInventory().setResult(transferEvent.getResult());
            }
        }
    }

    /**
     * Strictly validates that the framing matrix matches the custom requirements
     */
    private boolean validateCustomIngredients(NamespacedKey recipeKey, ItemStack[] matrix) {
        // 1. Identify the recipe definition from the key
        RecipeDefinition definition = parseRecipeDefinition(recipeKey);
        if (definition == null || definition.recipeData == null) {
            // Should not happen if key is ours, unless data reload removed it
            return false;
        }

        RecipeData data = definition.recipeData;

        if (data.getType() == RecipeData.RecipeType.SHAPED) {
            return validateShaped(data, matrix);
        } else if (data.getType() == RecipeData.RecipeType.SHAPELESS) {
            return validateShapeless(data, matrix);
        }

        return true;
    }

    private boolean validateShapeless(RecipeData data, ItemStack[] matrix) {
        // Create a list of required ingredients
        List<String> requirements = new ArrayList<>(data.getIngredients().values());

        // Create a list of provided items
        List<ItemStack> provided = new ArrayList<>();
        for (ItemStack is : matrix) {
            if (is != null && is.getType() != Material.AIR) {
                provided.add(is);
            }
        }

        if (provided.size() != requirements.size()) {
            return false; // Mismatch in count
        }

        // Match provided items to requirements
        // We use a simple greedy match (since ingredients are interchangeable in
        // shapeless)
        // But we must respect custom IDs.

        // Create a temp copy of requirements to tick off
        List<String> remainingReqs = new ArrayList<>(requirements);

        for (ItemStack item : provided) {
            boolean matched = false;
            for (int i = 0; i < remainingReqs.size(); i++) {
                String req = remainingReqs.get(i);
                if (matchesRequirement(req, item)) {
                    remainingReqs.remove(i);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false; // Item in matrix that matches no requirement
            }
        }

        return remainingReqs.isEmpty();
    }

    private boolean validateShaped(RecipeData data, ItemStack[] matrix) {
        // Need to find the bounds of the items in the matrix to align with recipe shape
        int minRow = 2, maxRow = 0, minCol = 2, maxCol = 0;
        boolean empty = true;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (matrix[r * 3 + c] != null && matrix[r * 3 + c].getType() != Material.AIR) {
                    empty = false;
                    if (r < minRow)
                        minRow = r;
                    if (r > maxRow)
                        maxRow = r;
                    if (c < minCol)
                        minCol = c;
                    if (c > maxCol)
                        maxCol = c;
                }
            }
        }

        if (empty)
            return false;

        return checkShapedMatch(data, matrix);
    }

    // Checks if the matrix matches the shape definition strictly
    private boolean checkShapedMatch(RecipeData data, ItemStack[] matrix) {
        String[] shape = data.getShape();
        Map<Character, String> ingredients = data.getIngredients();

        // Converting shape to a character grid for easier handling
        char[][] shapeGrid = new char[3][3];
        for (int r = 0; r < 3; r++) {
            String row = (shape[r] != null) ? shape[r] : "   ";
            // Pad to 3
            while (row.length() < 3)
                row += " ";
            shapeGrid[r] = row.toCharArray();
        }

        // Locate top-left non-air item in matrix
        // We iterate offsets to align the shape on top of the matrix items.

        for (int rowOffset = -2; rowOffset <= 2; rowOffset++) {
            for (int colOffset = -2; colOffset <= 2; colOffset++) {
                if (matchesAtOffset(matrix, shapeGrid, ingredients, rowOffset, colOffset)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesAtOffset(ItemStack[] matrix, char[][] shapeGrid, Map<Character, String> ingredients,
                                    int rowOffset, int colOffset) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int shapeR = r - rowOffset;
                int shapeC = c - colOffset;

                ItemStack item = matrix[r * 3 + c];
                boolean itemIsAir = (item == null || item.getType() == Material.AIR);

                char requiredChar = ' ';
                if (shapeR >= 0 && shapeR < 3 && shapeC >= 0 && shapeC < 3) {
                    requiredChar = shapeGrid[shapeR][shapeC];
                }

                // If ingredient is defined
                if (ingredients.containsKey(requiredChar)) {
                    String reqString = ingredients.get(requiredChar);
                    if (!matchesRequirement(reqString, item)) {
                        return false;
                    }
                } else {
                    // Expecting Air
                    if (!itemIsAir) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean matchesRequirement(String reqString, ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;

        // Check if requirement is a Custom Item ID
        if (itemDataManager.hasItem(reqString)) {
            return isCustomItem(item, reqString);
        } else {
            // Requirement is a vanilla material, verify item material matches
            try {
                return item.getType().name().equalsIgnoreCase(reqString);
            } catch (Exception e) {
                return false;
            }
        }
    }

    private boolean isCustomItem(ItemStack item, String targetId) {
        if (item == null || !item.hasItemMeta())
            return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey idKey = plugin.getCuriosPaperAPI().getItemIdKey();
        if (!pdc.has(idKey, PersistentDataType.STRING))
            return false;
        String id = pdc.get(idKey, PersistentDataType.STRING);
        return id != null && id.equals(targetId);
    }

    private static class RecipeDefinition {
        RecipeData recipeData;
    }

    private RecipeDefinition parseRecipeDefinition(NamespacedKey key) {
        // Format: custom_<plugin>_<itemid>[_variantN]
        String keyStr = key.getKey();
        if (!keyStr.startsWith("custom_"))
            return null;

        String remaining = keyStr.substring(7); // <plugin>_<itemid>[_variantN]

        int firstUnder = remaining.indexOf('_');
        if (firstUnder == -1)
            return null;

        String rest = remaining.substring(firstUnder + 1); // <itemid> or <itemid>_variantN

        // Check for _variant suffix
        int variantIndex = 0;
        String itemId = rest;

        int variantMarker = rest.lastIndexOf("_variant");
        if (variantMarker != -1 && variantMarker + 8 < rest.length()) {
            try {
                String numStr = rest.substring(variantMarker + 8);
                variantIndex = Integer.parseInt(numStr);
                itemId = rest.substring(0, variantMarker);
            } catch (NumberFormatException ignored) {
            }
        }

        ItemData data = itemDataManager.getItemData(itemId);
        if (data == null) {
            // Fallback for sanitized ID matching if exact lookup fails
            for (ItemData idata : itemDataManager.getAllItems().values()) {
                if (sanitizeKey(idata.getItemId()).equals(itemId)) {
                    data = idata;
                    break;
                }
            }
        }

        if (data != null) {
            List<RecipeData> recipes = data.getRecipes();
            if (variantIndex >= 0 && variantIndex < recipes.size()) {
                RecipeDefinition def = new RecipeDefinition();
                def.recipeData = recipes.get(variantIndex);
                return def;
            }
        }

        return null;
    }

    @EventHandler
    public void onPrepareSmithing(org.bukkit.event.inventory.PrepareSmithingEvent event) {
        ItemStack result = event.getResult();
        if (result == null || !result.hasItemMeta())
            return;

        NamespacedKey itemIdKey = plugin.getCuriosPaperAPI().getItemIdKey();
        PersistentDataContainer resultPdc = result.getItemMeta().getPersistentDataContainer();
        if (!resultPdc.has(itemIdKey, PersistentDataType.STRING)) {
            return;
        }

        String resultId = resultPdc.get(itemIdKey, PersistentDataType.STRING);
        ItemData itemData = itemDataManager.getItemData(resultId);

        if (itemData == null) {
            return;
        }

        org.bukkit.inventory.SmithingInventory inv = event.getInventory();
        ItemStack template = inv.getItem(0);
        ItemStack base = inv.getItem(1);
        ItemStack addition = inv.getItem(2);

        boolean isValid = false;

        for (RecipeData recipe : itemData.getRecipes()) {
            if (recipe.getType() == RecipeData.RecipeType.SMITHING) {
                if (matchesSmithing(recipe, template, base, addition)) {
                    isValid = true;
                    break;
                }
            }
        }

        if (!isValid) {
            event.setResult(null);
            return;
        }

        if (base != null && base.hasItemMeta()) {
            CuriosRecipeTransferEvent transferEvent = new CuriosRecipeTransferEvent(inv, result, base);
            plugin.getServer().getPluginManager().callEvent(transferEvent);

            if (!transferEvent.isCancelled()) {
                event.setResult(transferEvent.getResult());
            }
        }
    }

    private boolean matchesSmithing(RecipeData recipe, ItemStack template, ItemStack base, ItemStack addition) {
        if (recipe.getTemplateItem() != null && !recipe.getTemplateItem().isEmpty()) {
            if (!matchesRequirement(recipe.getTemplateItem(), template))
                return false;
        }
        if (!matchesRequirement(recipe.getBaseItem(), base))
            return false;
        if (!matchesRequirement(recipe.getAdditionItem(), addition))
            return false;
        return true;
    }

    @EventHandler
    public void onFurnaceSmelt(org.bukkit.event.inventory.FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        ItemStack result = event.getResult();

        if (result != null && result.hasItemMeta()) {
            NamespacedKey keyIdx = plugin.getCuriosPaperAPI().getItemIdKey();
            PersistentDataContainer resultPdc = result.getItemMeta().getPersistentDataContainer();
            if (resultPdc.has(keyIdx, PersistentDataType.STRING)) {
                String resultId = resultPdc.get(keyIdx, PersistentDataType.STRING);
                ItemData itemData = itemDataManager.getItemData(resultId);

                if (itemData != null) {
                    boolean isValid = false;
                    for (RecipeData recipe : itemData.getRecipes()) {
                        if (isFurnaceType(recipe.getType())) {
                            if (matchesRequirement(recipe.getInputItem(), source)) {
                                isValid = true;
                                break;
                            }
                        }
                    }

                    if (!isValid) {
                        event.setCancelled(true);
                        return;
                    }
                }

                if (event.getBlock().getState() instanceof org.bukkit.inventory.InventoryHolder) {
                    org.bukkit.inventory.Inventory inv = ((org.bukkit.inventory.InventoryHolder) event.getBlock()
                            .getState()).getInventory();
                    CuriosRecipeTransferEvent transferEvent = new CuriosRecipeTransferEvent(inv, result, source);
                    plugin.getServer().getPluginManager().callEvent(transferEvent);

                    if (!transferEvent.isCancelled()) {
                        event.setResult(transferEvent.getResult());
                    }
                }
            }
        }
    }

    private boolean isFurnaceType(RecipeData.RecipeType type) {
        return type == RecipeData.RecipeType.FURNACE ||
                type == RecipeData.RecipeType.BLAST_FURNACE ||
                type == RecipeData.RecipeType.SMOKER;
    }

    @EventHandler
    public void onPrepareAnvil(org.bukkit.event.inventory.PrepareAnvilEvent event) {
        org.bukkit.inventory.AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        if (left == null || right == null)
            return;

        for (ItemData itemData : itemDataManager.getAllItems().values()) {
            for (RecipeData recipe : itemData.getRecipes()) {
                if (recipe.getType() == RecipeData.RecipeType.ANVIL) {
                    if (matchesAnvil(recipe, left, right)) {
                        ItemStack result = createResultItem(itemData);

                        CuriosRecipeTransferEvent transferEvent = new CuriosRecipeTransferEvent(inv, result, left);
                        plugin.getServer().getPluginManager().callEvent(transferEvent);

                        if (!transferEvent.isCancelled()) {
                            event.setResult(transferEvent.getResult());
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (event.getView() != null) {
                                    event.getInventory().setRepairCost((int) recipe.getExperience());
                                }
                            });
                        }
                        return;
                    }
                }
            }
        }
    }

    private boolean matchesAnvil(RecipeData recipe, ItemStack left, ItemStack right) {
        return matchesRequirement(recipe.getLeftInput(), left) && matchesRequirement(recipe.getRightInput(), right);
    }

    public boolean unregisterRecipe(String itemId) {
        ItemData data = itemDataManager.getItemData(itemId);
        if (data != null) {
            String owningPlugin = data.getOwningPlugin();
            if (owningPlugin == null || owningPlugin.isEmpty()) {
                owningPlugin = "curiospaper";
            }
            owningPlugin = sanitizeKey(owningPlugin);
            String safeItemId = sanitizeKey(itemId);

            List<RecipeData> recipes = data.getRecipes();
            for (int i = 0; i < recipes.size(); i++) {
                String keyString = "custom_" + owningPlugin + "_" + safeItemId;
                if (recipes.size() > 1) {
                    keyString += "_variant" + i;
                }
                NamespacedKey key = new NamespacedKey(plugin, keyString);
                plugin.getServer().removeRecipe(key);
            }
            return true;
        }
        return false;
    }

    public void unregisterAllRecipes() {
        for (String itemId : itemDataManager.getAllItemIds()) {
            unregisterRecipe(itemId);
        }
        plugin.getLogger().info("Unregistered all custom recipes");
    }
}
