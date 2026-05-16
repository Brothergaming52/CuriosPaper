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

public class ItemRecipeListGUI implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final Map<UUID, String> currentlyViewing;

    public ItemRecipeListGUI(CuriosPaper plugin) {
        this.plugin = plugin;
        this.itemDataManager = plugin.getItemDataManager();
        this.currentlyViewing = new HashMap<>();
    }

    public void open(Player player, String itemId) {
        ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null) {
            player.sendMessage("§cItem not found!");
            return;
        }

        currentlyViewing.put(player.getUniqueId(), itemId);

        List<RecipeData> recipes = itemData.getRecipes();
        Inventory gui = Bukkit.createInventory(null, 27, "§8Recipes: " + itemId);

        // Display recipes
        for (int i = 0; i < Math.min(recipes.size(), 18); i++) {
            RecipeData recipe = recipes.get(i);
            ItemStack icon = createGuiItem(getIconForType(recipe.getType()), "§eRecipe #" + (i + 1),
                    "§7Type: " + recipe.getType().name(),
                    "", "§eClick to view recipe detail");
            gui.setItem(i, icon);
        }

        // Back button
        gui.setItem(18, createGuiItem(Material.ARROW, "§7Back to List"));

        // Admin Give Button
        if (player.hasPermission("curiospaper.admin")) {
            gui.setItem(26,
                    createGuiItem(Material.COMMAND_BLOCK, "§aAdmin: Give Item", "§7Click to give yourself this item"));
        }

        // Fill empty
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    private Material getIconForType(RecipeData.RecipeType type) {
        switch (type) {
            case SHAPED:
            case SHAPELESS:
                return Material.CRAFTING_TABLE;
            case FURNACE:
                return Material.FURNACE;
            case BLAST_FURNACE:
                return Material.BLAST_FURNACE;
            case SMOKER:
                return Material.SMOKER;
            case CAMPFIRE:
                return Material.CAMPFIRE;
            case ANVIL:
                return Material.ANVIL;
            case SMITHING:
                return Material.SMITHING_TABLE;
            default:
                return Material.CRAFTING_TABLE;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.startsWith("§8Recipes: "))
            return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot >= 27)
            return;

        String itemId = currentlyViewing.get(player.getUniqueId());
        if (itemId == null)
            return;

        if (slot == 18) {
            plugin.getItemListGUI().open(player);
            return;
        }

        if (slot == 26 && player.hasPermission("curiospaper.admin")) {
            ItemStack item = plugin.getCuriosPaperAPI().createItemStack(itemId);
            if (item != null) {
                player.getInventory().addItem(item);
                player.sendMessage("§aGave you 1x §e" + itemId);
            }
            return;
        }

        if (slot < 18) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
                return;

            plugin.getRecipeViewGUI().open(player, itemId, slot);
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
