package org.bg52.curiospaper.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents all data associated with a custom item, including its properties,
 * recipes, loot tables, mob drops, and villager trades.
 */
public class ItemData {
    private final String itemId;
    private String displayName;
    private String material;
    private String itemModel;
    private String slotType;
    private List<String> lore;
    private RecipeData recipe;
    private List<LootTableData> lootTables;
    private List<MobDropData> mobDrops;
    private List<VillagerTradeData> villagerTrades;
    private List<AbilityData> abilities;

    public ItemData(String itemId) {
        this.itemId = itemId;
        this.lore = new ArrayList<>();
        this.lootTables = new ArrayList<>();
        this.mobDrops = new ArrayList<>();
        this.villagerTrades = new ArrayList<>();
        this.abilities = new ArrayList<>();
    }

    // ========== GETTERS ==========

    public String getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMaterial() {
        return material;
    }

    public String getItemModel() {
        return itemModel;
    }

    public String getSlotType() {
        return slotType;
    }

    public List<String> getLore() {
        return new ArrayList<>(lore);
    }

    public RecipeData getRecipe() {
        return recipe;
    }

    public List<LootTableData> getLootTables() {
        return new ArrayList<>(lootTables);
    }

    public List<MobDropData> getMobDrops() {
        return mobDrops;
    }

    public List<VillagerTradeData> getVillagerTrades() {
        return villagerTrades;
    }

    public List<AbilityData> getAbilities() {
        return new ArrayList<>(abilities);
    }

    // ========== SETTERS ==========

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public void setItemModel(String itemModel) {
        this.itemModel = itemModel;
    }

    public void setSlotType(String slotType) {
        this.slotType = slotType;
    }

    public void setLore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
    }

    public void addLoreLine(String line) {
        this.lore.add(line);
    }

    public void setRecipe(RecipeData recipe) {
        this.recipe = recipe;
    }

    public void setLootTables(List<LootTableData> lootTables) {
        this.lootTables = new ArrayList<>(lootTables);
    }

    public void addLootTable(LootTableData lootTable) {
        this.lootTables.add(lootTable);
    }

    public void setMobDrops(List<MobDropData> mobDrops) {
        this.mobDrops = new ArrayList<>(mobDrops);
    }

    public void addMobDrop(MobDropData mobDrop) {
        this.mobDrops.add(mobDrop);
    }

    public void setVillagerTrades(List<VillagerTradeData> villagerTrades) {
        this.villagerTrades = new ArrayList<>(villagerTrades);
    }

    public void addVillagerTrade(VillagerTradeData villagerTrade) {
        this.villagerTrades.add(villagerTrade);
    }

    public void setAbilities(List<AbilityData> abilities) {
        this.abilities = new ArrayList<>(abilities);
    }

    public void addAbility(AbilityData ability) {
        this.abilities.add(ability);
    }

    // ========== SERIALIZATION ==========

    /**
     * Saves this ItemData to a YamlConfiguration
     */
    public void saveToConfig(YamlConfiguration config) {
        config.set("item-id", itemId);
        config.set("display-name", displayName);
        config.set("material", material);
        config.set("item-model", itemModel);
        config.set("slot-type", slotType);
        config.set("lore", lore);

        if (recipe != null) {
            ConfigurationSection recipeSection = config.createSection("recipe");
            recipe.saveToConfig(recipeSection);
        }

        if (!lootTables.isEmpty()) {
            ConfigurationSection lootSection = config.createSection("loot-tables");
            for (int i = 0; i < lootTables.size(); i++) {
                ConfigurationSection entrySection = lootSection.createSection("entry-" + i);
                lootTables.get(i).saveToConfig(entrySection);
            }
        }

        if (!mobDrops.isEmpty()) {
            ConfigurationSection mobSection = config.createSection("mob-drops");
            for (int i = 0; i < mobDrops.size(); i++) {
                ConfigurationSection entrySection = mobSection.createSection("entry-" + i);
                mobDrops.get(i).saveToConfig(entrySection);
            }
        }

        if (!villagerTrades.isEmpty()) {
            ConfigurationSection tradesSection = config.createSection("villager-trades");
            for (int i = 0; i < villagerTrades.size(); i++) {
                ConfigurationSection entrySection = tradesSection.createSection("entry-" + i);
                villagerTrades.get(i).saveToConfig(entrySection);
            }
        }

        if (!abilities.isEmpty()) {
            ConfigurationSection abilitiesSection = config.createSection("abilities");
            for (int i = 0; i < abilities.size(); i++) {
                ConfigurationSection entrySection = abilitiesSection.createSection("entry-" + i);
                abilities.get(i).saveToConfig(entrySection);
            }
        }
    }

    /**
     * Loads ItemData from a YamlConfiguration
     */
    public static ItemData loadFromConfig(YamlConfiguration config) {
        String itemId = config.getString("item-id");
        if (itemId == null) {
            return null;
        }

        ItemData data = new ItemData(itemId);
        data.setDisplayName(config.getString("display-name"));
        data.setMaterial(config.getString("material", "PAPER"));
        data.setItemModel(config.getString("item-model"));
        data.setSlotType(config.getString("slot-type"));
        data.setLore(config.getStringList("lore"));

        // Load recipe
        ConfigurationSection recipeSection = config.getConfigurationSection("recipe");
        if (recipeSection != null) {
            data.setRecipe(RecipeData.loadFromConfig(recipeSection));
        }

        // Load loot tables
        ConfigurationSection lootSection = config.getConfigurationSection("loot-tables");
        if (lootSection != null) {
            for (String key : lootSection.getKeys(false)) {
                ConfigurationSection entrySection = lootSection.getConfigurationSection(key);
                if (entrySection != null) {
                    data.addLootTable(LootTableData.loadFromConfig(entrySection));
                }
            }
        }

        // Load mob drops
        ConfigurationSection mobSection = config.getConfigurationSection("mob-drops");
        if (mobSection != null) {
            for (String key : mobSection.getKeys(false)) {
                ConfigurationSection entrySection = mobSection.getConfigurationSection(key);
                if (entrySection != null) {
                    data.addMobDrop(MobDropData.loadFromConfig(entrySection));
                }
            }
        }

        // Load villager trades
        ConfigurationSection tradesSection = config.getConfigurationSection("villager-trades");
        if (tradesSection != null) {
            for (String key : tradesSection.getKeys(false)) {
                ConfigurationSection entrySection = tradesSection.getConfigurationSection(key);
                if (entrySection != null) {
                    data.addVillagerTrade(VillagerTradeData.loadFromConfig(entrySection));
                }
            }
        }

        // Load abilities
        ConfigurationSection abilitiesSection = config.getConfigurationSection("abilities");
        if (abilitiesSection != null) {
            for (String key : abilitiesSection.getKeys(false)) {
                ConfigurationSection entrySection = abilitiesSection.getConfigurationSection(key);
                if (entrySection != null) {
                    AbilityData ability = AbilityData.loadFromConfig(entrySection);
                    if (ability != null && ability.isValid()) {
                        data.addAbility(ability);
                    }
                }
            }
        }

        return data;
    }

    /**
     * Checks if this item has a valid configuration
     */
    public boolean isValid() {
        return itemId != null && !itemId.isEmpty()
                && displayName != null && !displayName.isEmpty()
                && material != null && !material.isEmpty();
    }

    @Override
    public String toString() {
        return "ItemData{" +
                "itemId='" + itemId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", slotType='" + slotType + '\'' +
                ", hasRecipe=" + (recipe != null) +
                ", lootTables=" + lootTables.size() +
                ", mobDrops=" + mobDrops.size() +
                ", villagerTrades=" + villagerTrades.size() +
                ", abilities=" + abilities.size() +
                '}';
    }
}
