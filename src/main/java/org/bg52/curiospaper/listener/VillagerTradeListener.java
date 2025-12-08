package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.VillagerTradeData;
import org.bg52.curiospaper.data.VillagerTradeData.TradeCost;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Handles custom villager trades injection
 * Listens for when villagers gain new trades and injects custom items
 */
public class VillagerTradeListener implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final Random random;
    private final NamespacedKey CUSTOM_TRADES_KEY;

    public VillagerTradeListener(CuriosPaper plugin, ItemDataManager itemDataManager) {
        this.plugin = plugin;
        this.itemDataManager = itemDataManager;
        this.random = new Random();
        this.CUSTOM_TRADES_KEY = new NamespacedKey(plugin, "custom_trades");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (event.isCancelled())
            return;

        // Check if entity is a Villager or WanderingTrader
        // Both extend AbstractVillager but have different types
        boolean isWanderingTrader = event.getEntity().getType() == org.bukkit.entity.EntityType.WANDERING_TRADER;
        boolean isVillager = event.getEntity() instanceof Villager;

        if (!isVillager && !isWanderingTrader)
            return;

        org.bukkit.entity.AbstractVillager abstractVillager = event.getEntity();
        String professionName;
        int level;

        if (isWanderingTrader) {
            professionName = "WANDERING_TRADER";
            level = 1; // Wandering traders don't have levels, treat as level 1
        } else {
            Villager villager = (Villager) event.getEntity();
            professionName = villager.getProfession().name();
            level = getVillagerLevel(villager);
        }

        // Get all items that can have villager trades
        for (ItemData itemData : itemDataManager.getAllItems().values()) {
            for (VillagerTradeData tradeData : itemData.getVillagerTrades()) {
                // Check if this trade applies to this villager/wandering trader
                if (!tradeData.appliesToProfession(professionName)) {
                    continue;
                }

                // Check if this trade applies to this level
                if (!tradeData.appliesToLevel(level)) {
                    continue;
                }

                // Check if villager already has this trade (duplicate prevention)
                if (hasCustomTrade(abstractVillager, itemData.getItemId())) {
                    continue;
                }

                // Roll for chance
                if (random.nextDouble() < tradeData.getChance()) {
                    // Create and add the trade
                    MerchantRecipe recipe = createTradeRecipe(itemData, tradeData);
                    if (recipe != null) {
                        // Add trade to villager/wandering trader
                        List<MerchantRecipe> recipes = new ArrayList<>(abstractVillager.getRecipes());
                        recipes.add(recipe);
                        abstractVillager.setRecipes(recipes);

                        // Mark that this villager has received this trade
                        markCustomTrade(abstractVillager, itemData.getItemId());

                        if (plugin.getConfig().getBoolean("debug.log-inventory-events", false)) {
                            plugin.getLogger().info("Added custom trade for item " + itemData.getItemId() +
                                    " to " + (isWanderingTrader ? "wandering trader" : "villager at level " + level));
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a MerchantRecipe from trade data
     */
    private MerchantRecipe createTradeRecipe(ItemData itemData, VillagerTradeData tradeData) {
        try {
            // Create the result item
            Material material = Material.valueOf(itemData.getMaterial().toUpperCase());
            ItemStack result = new ItemStack(material, 1);

            // Set display name, lore, and item model
            if (itemData.getDisplayName() != null) {
                result.editMeta(meta -> {
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
                result = plugin.getCuriosPaperAPI().tagAccessoryItem(result, itemData.getSlotType());
            }

            // Create the cost items
            List<TradeCost> costs = tradeData.getCostItems();
            if (costs.isEmpty()) {
                return null;
            }

            // First cost item (required)
            TradeCost cost1 = costs.get(0);
            int amount1 = randomAmount(cost1.getMinAmount(), cost1.getMaxAmount());
            ItemStack ingredient1 = new ItemStack(Material.valueOf(cost1.getMaterial()), amount1);

            // Create recipe with proper XP and use limits
            // uses: starts at 0, maxUses: 12 (reasonable trade limit), experienceReward:
            // true, villagerExperience: 5, priceMultiplier: 0.05f
            MerchantRecipe recipe;
            if (costs.size() > 1) {
                // Two cost items
                TradeCost cost2 = costs.get(1);
                int amount2 = randomAmount(cost2.getMinAmount(), cost2.getMaxAmount());
                ItemStack ingredient2 = new ItemStack(Material.valueOf(cost2.getMaterial()), amount2);
                recipe = new MerchantRecipe(result, 0, 12, true, 5, 0.05f);
                recipe.setIngredients(Arrays.asList(ingredient1, ingredient2));
            } else {
                // One cost item
                recipe = new MerchantRecipe(result, 0, 12, true, 5, 0.05f);
                recipe.setIngredients(Collections.singletonList(ingredient1));
            }

            return recipe;

        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material in trade configuration for item " + itemData.getItemId());
            return null;
        }
    }

    /**
     * Gets the villager's profession level
     */
    private int getVillagerLevel(Villager villager) {
        // Villager experience levels:
        // 0 = Novice (level 1)
        // 10 = Apprentice (level 2)
        // 70 = Journeyman (level 3)
        // 150 = Expert (level 4)
        // 250 = Master (level 5)
        int exp = villager.getVillagerExperience();
        if (exp >= 250)
            return 5;
        if (exp >= 150)
            return 4;
        if (exp >= 70)
            return 3;
        if (exp >= 10)
            return 2;
        return 1;
    }

    /**
     * Checks if a villager/wandering trader already has a custom trade for a
     * specific item
     */
    private boolean hasCustomTrade(org.bukkit.entity.AbstractVillager abstractVillager, String itemId) {
        PersistentDataContainer container = abstractVillager.getPersistentDataContainer();
        String[] trades = container.getOrDefault(CUSTOM_TRADES_KEY, PersistentDataType.STRING, "").split(",");
        return Arrays.asList(trades).contains(itemId);
    }

    /**
     * Marks that a villager/wandering trader has received a custom trade for a
     * specific item
     */
    private void markCustomTrade(org.bukkit.entity.AbstractVillager abstractVillager, String itemId) {
        PersistentDataContainer container = abstractVillager.getPersistentDataContainer();
        String existing = container.getOrDefault(CUSTOM_TRADES_KEY, PersistentDataType.STRING, "");

        List<String> trades = new ArrayList<>(Arrays.asList(existing.split(",")));
        trades.removeIf(String::isEmpty);

        if (!trades.contains(itemId)) {
            trades.add(itemId);
        }

        container.set(CUSTOM_TRADES_KEY, PersistentDataType.STRING, String.join(",", trades));
    }

    /**
     * Returns a random amount between min and max (inclusive)
     */
    private int randomAmount(int min, int max) {
        if (min == max)
            return min;
        return random.nextInt(max - min + 1) + min;
    }
}
