package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.RecipeData;
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

public class RecipeViewGUI implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final Map<UUID, String> playerItem;

    public RecipeViewGUI(CuriosPaper plugin) {
        this.plugin = plugin;
        this.itemDataManager = plugin.getItemDataManager();
        this.playerItem = new HashMap<>();
    }

    public void open(Player player, String itemId, int recipeIndex) {
        ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null || recipeIndex >= itemData.getRecipes().size()) {
            player.sendMessage("§cRecipe not found!");
            return;
        }

        playerItem.put(player.getUniqueId(), itemId);
        RecipeData recipe = itemData.getRecipes().get(recipeIndex);

        Inventory gui = Bukkit.createInventory(null, 54, "§8View Recipe: " + itemId);

        // Display the result item at slot 25 (right side of crafting grid area)
        ItemStack result = plugin.getCuriosPaperAPI().createItemStack(itemId);
        gui.setItem(25, result);

        // Crafting Grid slots: 10, 11, 12, 19, 20, 21, 28, 29, 30
        int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};

        if (recipe.getType() == RecipeData.RecipeType.SHAPED) {
            String[] shape = recipe.getShape();
            Map<Character, String> ingredients = recipe.getIngredients();
            for (int r = 0; r < 3; r++) {
                if (shape[r] == null) continue;
                for (int c = 0; c < Math.min(shape[r].length(), 3); c++) {
                    char key = shape[r].charAt(c);
                    if (key != ' ') {
                        String ing = ingredients.get(key);
                        gui.setItem(gridSlots[r * 3 + c], buildIngredientItem(ing));
                    }
                }
            }
        } else if (recipe.getType() == RecipeData.RecipeType.SHAPELESS) {
            int slotIdx = 0;
            for (String ing : recipe.getIngredients().values()) {
                if (slotIdx < gridSlots.length) {
                    gui.setItem(gridSlots[slotIdx++], buildIngredientItem(ing));
                }
            }
        } else if (recipe.getType() == RecipeData.RecipeType.FURNACE || 
                   recipe.getType() == RecipeData.RecipeType.BLAST_FURNACE || 
                   recipe.getType() == RecipeData.RecipeType.SMOKER || 
                   recipe.getType() == RecipeData.RecipeType.CAMPFIRE) {
            gui.setItem(20, buildIngredientItem(recipe.getInputItem()));
            gui.setItem(23, createGuiItem(Material.FURNACE, "§eCooking..."));
        } else if (recipe.getType() == RecipeData.RecipeType.SMITHING) {
            gui.setItem(19, buildIngredientItem(recipe.getTemplateItem()));
            gui.setItem(20, buildIngredientItem(recipe.getBaseItem()));
            gui.setItem(21, buildIngredientItem(recipe.getAdditionItem()));
        } else if (recipe.getType() == RecipeData.RecipeType.ANVIL) {
            gui.setItem(19, buildIngredientItem(recipe.getLeftInput()));
            gui.setItem(21, buildIngredientItem(recipe.getRightInput()));
            gui.setItem(20, createGuiItem(Material.ANVIL, "§eAnvil Recipe"));
        }

        // Back button
        gui.setItem(49, createGuiItem(Material.ARROW, "§7Back to Recipes"));

        // Fill background
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    private ItemStack buildIngredientItem(String input) {
        if (input == null) return null;
        // Try as custom item
        ItemStack custom = plugin.getCuriosPaperAPI().createItemStack(input);
        if (custom != null) return custom;

        // Try as material
        try {
            return new ItemStack(Material.valueOf(input.toUpperCase()));
        } catch (Exception e) {
            return createGuiItem(Material.BARRIER, "§cUnknown: " + input);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.startsWith("§8View Recipe: ")) return;

        event.setCancelled(true);

        if (event.getRawSlot() == 49) {
            String itemId = playerItem.get(player.getUniqueId());
            if (itemId != null) {
                plugin.getItemRecipeListGUI().open(player, itemId);
            }
        }
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
