package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.RecipeData;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.Map;

/**
 * Handles registration of custom recipes from ItemDataManager
 */
public class RecipeListener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;

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
            if (itemData.getRecipe() != null) {
                if (registerRecipe(itemData)) {
                    registered++;
                } else {
                    failed++;
                }
            }
        }

        plugin.getLogger().info("Recipe registration complete:");
        plugin.getLogger().info("  Successfully registered: " + registered);
        if (failed > 0) {
            plugin.getLogger().warning("  Failed to register: " + failed);
        }
    }

    /**
     * Registers a recipe for a specific item
     */
    public boolean registerRecipe(ItemData itemData) {
        RecipeData recipeData = itemData.getRecipe();
        if (recipeData == null || !recipeData.isValid()) {
            return false;
        }

        try {
            ItemStack result = createResultItem(itemData);
            if (result == null)
                return false;

            // Ensure key is lowercase and valid
            NamespacedKey key = new NamespacedKey(plugin, "custom_" + itemData.getItemId().toLowerCase());

            // FIX: Always remove the recipe first to ensure no conflicts
            plugin.getServer().removeRecipe(key);

            if (recipeData.getType() == RecipeData.RecipeType.SHAPED) {
                return registerShapedRecipe(key, result, recipeData);
            } else {
                return registerShapelessRecipe(key, result, recipeData);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error registering recipe for " + itemData.getItemId() + ": " + e.getMessage());
            return false;
        }
    }

    private boolean registerShapedRecipe(NamespacedKey key, ItemStack result, RecipeData recipeData) {
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        String[] shape = recipeData.getShape();
        if (shape == null || shape.length != 3) {
            plugin.getLogger().warning("Invalid shaped recipe shape for " + key.getKey());
            return false;
        }

        recipe.shape(shape[0], shape[1], shape[2]);

        // Set ingredients
        for (Map.Entry<Character, String> entry : recipeData.getIngredients().entrySet()) {
            try {
                Material material = Material.valueOf(entry.getValue().toUpperCase());
                // Use exact choice to ensure stability
                recipe.setIngredient(entry.getKey(), new RecipeChoice.MaterialChoice(material));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material '" + entry.getValue() + "' in recipe for " + key.getKey());
                return false;
            }
        }

        plugin.getServer().addRecipe(recipe);
        plugin.getLogger().info("✓ Registered shaped recipe: " + key.getKey());
        return true;
    }

    private boolean registerShapelessRecipe(NamespacedKey key, ItemStack result, RecipeData recipeData) {
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);

        // Add ingredients
        for (String materialName : recipeData.getIngredients().values()) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                recipe.addIngredient(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material '" + materialName + "' in recipe for " + key.getKey());
                return false;
            }
        }

        plugin.getServer().addRecipe(recipe);
        plugin.getLogger().info("✓ Registered shapeless recipe: " + key.getKey());
        return true;
    }

    /**
     * Creates the result ItemStack for a recipe
     */
    private ItemStack createResultItem(ItemData itemData) {
        try {
            Material material = Material.valueOf(itemData.getMaterial().toUpperCase());
            ItemStack item = new ItemStack(material);

            // Set display name, lore, and item model
            if (itemData.getDisplayName() != null) {
                item.editMeta(meta -> {
                    meta.setDisplayName(itemData.getDisplayName());
                    if (!itemData.getLore().isEmpty()) {
                        meta.setLore(itemData.getLore());
                    }
                    // Apply item model if specified
                    if (itemData.getItemModel() != null && !itemData.getItemModel().isEmpty()) {
                        meta.setItemModel(org.bukkit.NamespacedKey.fromString(itemData.getItemModel()));
                    }
                });
            }

            // Tag the item for the appropriate slot if specified
            if (itemData.getSlotType() != null && !itemData.getSlotType().isEmpty()) {
                item = plugin.getCuriosPaperAPI().tagAccessoryItem(item, itemData.getSlotType());
            }

            return item;
        } catch (IllegalArgumentException e) {
            plugin.getLogger()
                    .warning("Invalid material '" + itemData.getMaterial() + "' for item " + itemData.getItemId());
            return null;
        }
    }

    /**
     * Unregisters a recipe for a specific item
     */
    public boolean unregisterRecipe(String itemId) {
        NamespacedKey key = new NamespacedKey(plugin, "custom_" + itemId);
        return plugin.getServer().removeRecipe(key);
    }

    /**
     * Unregisters all custom recipes
     */
    public void unregisterAllRecipes() {
        for (String itemId : itemDataManager.getAllItemIds()) {
            unregisterRecipe(itemId);
        }
        plugin.getLogger().info("Unregistered all custom recipes");
    }
}
