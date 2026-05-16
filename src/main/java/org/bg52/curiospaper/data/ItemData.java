package org.bg52.curiospaper.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;

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
  private Integer customModelData;
  private List<String> lore;
  private List<RecipeData> recipes;
  private List<LootTableData> lootTables;
  private List<MobDropData> mobDrops;
  private List<VillagerTradeData> villagerTrades;
  private List<AbilityData> abilities;

  // 3D Model attachment fields
  private boolean modelEnabled = false;
  private String modelItem;
  private Integer modelCustomModelData;
  private String modelItemModel;
  private Float pitchUpLimit;
  private Float pitchDownLimit;

  private boolean hidden = false;

  public ItemData(String itemId) {
    this.itemId = itemId;
    this.recipes = new ArrayList<>();
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

  public Integer getCustomModelData() {
    return customModelData;
  }

  public List<String> getLore() {
    return new ArrayList<>(lore);
  }

  public List<RecipeData> getRecipes() {
    return new ArrayList<>(recipes);
  }

  /**
   * @deprecated Use getRecipes() instead.
   */
  @Deprecated
  public RecipeData getRecipe() {
    return recipes.isEmpty() ? null : recipes.get(0);
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

  // ========== 3D MODEL GETTERS ==========

  public boolean isModelEnabled() {
    return modelEnabled;
  }

  public String getModelItem() {
    return modelItem;
  }

  public Integer getModelCustomModelData() {
    return modelCustomModelData;
  }

  public Float getPitchUpLimit() {
    return pitchUpLimit;
  }

  public Float getPitchDownLimit() {
    return pitchDownLimit;
  }

  public String getModelItemModel() {
    return modelItemModel;
  }

  public boolean isHidden() {
    return hidden;
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

  public void setCustomModelData(Integer customModelData) {
    this.customModelData = customModelData;
  }

  public void setLore(List<String> lore) {
    this.lore = new ArrayList<>(lore);
  }

  public void addLoreLine(String line) {
    this.lore.add(line);
  }

  public void setRecipes(List<RecipeData> recipes) {
    this.recipes = new ArrayList<>(recipes);
  }

  public void addRecipe(RecipeData recipe) {
    this.recipes.add(recipe);
  }

  public void removeRecipe(RecipeData recipe) {
    this.recipes.remove(recipe);
  }

  /**
   * @deprecated Use setRecipes() or addRecipe() instead. Replaces all recipes
   *             with this single one.
   */
  @Deprecated
  public void setRecipe(RecipeData recipe) {
    this.recipes.clear();
    if (recipe != null) {
      this.recipes.add(recipe);
    }
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

  // ========== 3D MODEL SETTERS ==========

  public void setModelEnabled(boolean modelEnabled) {
    this.modelEnabled = modelEnabled;
  }

  public void setModelItem(String modelItem) {
    this.modelItem = modelItem;
  }

  public void setModelCustomModelData(Integer modelCustomModelData) {
    this.modelCustomModelData = modelCustomModelData;
  }

  public void setPitchUpLimit(Float pitchUpLimit) {
    this.pitchUpLimit = pitchUpLimit;
  }

  public void setPitchDownLimit(Float pitchDownLimit) {
    this.pitchDownLimit = pitchDownLimit;
  }

  public void setModelItemModel(String modelItemModel) {
    this.modelItemModel = modelItemModel;
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  // ========== SERIALIZATION ==========

  /**
   * Saves this ItemData to a YamlConfiguration
   */
  private String owningPlugin;

  // ... existing constructors ...

  // ========== GETTERS ==========

  public String getOwningPlugin() {
    return owningPlugin;
  }

  // ... existing getters ...

  // ========== SETTERS ==========

  public void setOwningPlugin(String owningPlugin) {
    this.owningPlugin = owningPlugin;
  }

  // ... existing setters ...

  // ========== SERIALIZATION ==========

  /**
   * Saves this ItemData to a YamlConfiguration
   */
  public void saveToConfig(YamlConfiguration config) {
    config.set("item-id", itemId);
    // Only save owning plugin if it is set
    if (owningPlugin != null) {
      config.set("owning-plugin", owningPlugin);
    }
    config.set("display-name", displayName);
    config.set("material", material);
    config.set("item-model", itemModel);
    if (customModelData != null) {
      config.set("custom-model-data", customModelData);
    }
    config.set("slot-type", slotType);
    config.set("lore", lore);

    // 3D Model attachment data
    config.set("model-enabled", modelEnabled);
    if (modelItem != null) {
      config.set("model-item", modelItem);
    }
    if (modelCustomModelData != null) {
      config.set("model-custom-model-data", modelCustomModelData);
    }
    if (modelItemModel != null) {
      config.set("model-item-model", modelItemModel);
    }
    if (pitchUpLimit != null) {
      config.set("pitch-up-limit", pitchUpLimit);
    }
    if (pitchDownLimit != null) {
      config.set("pitch-down-limit", pitchDownLimit);
    }
    config.set("hidden", hidden);

    if (!recipes.isEmpty()) {
      ConfigurationSection recipesSection = config.createSection("recipes");
      for (int i = 0; i < recipes.size(); i++) {
        ConfigurationSection entrySection = recipesSection.createSection("entry-" + i);
        recipes.get(i).saveToConfig(entrySection);
      }
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
    data.setOwningPlugin(config.getString("owning-plugin"));
    data.setDisplayName(config.getString("display-name"));
    data.setMaterial(config.getString("material", "PAPER"));
    data.setItemModel(config.getString("item-model"));
    // Load customModelData - try to parse from item-model if it's an integer
    if (config.contains("custom-model-data")) {
      data.setCustomModelData(config.getInt("custom-model-data"));
    } else if (data.getItemModel() != null) {
      try {
        data.setCustomModelData(Integer.parseInt(data.getItemModel()));
      } catch (NumberFormatException e) {
        // Not an integer, leave as null
      }
    }
    data.setSlotType(config.getString("slot-type"));
    data.setLore(config.getStringList("lore"));

    // Load 3D model data (backwards-compatible — missing keys default safely)
    data.setModelEnabled(config.getBoolean("model-enabled", false));
    data.setModelItem(config.getString("model-item"));
    if (config.contains("model-custom-model-data")) {
      data.setModelCustomModelData(config.getInt("model-custom-model-data"));
    }
    if (config.contains("model-item-model")) {
      data.setModelItemModel(config.getString("model-item-model"));
    }
    if (config.contains("pitch-up-limit")) {
      data.setPitchUpLimit((float) config.getDouble("pitch-up-limit"));
    }
    if (config.contains("pitch-down-limit")) {
      data.setPitchDownLimit((float) config.getDouble("pitch-down-limit"));
    }
    data.setHidden(config.getBoolean("hidden", false));

    // Load recipes
    ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
    if (recipesSection != null) {
      for (String key : recipesSection.getKeys(false)) {
        ConfigurationSection entrySection = recipesSection.getConfigurationSection(key);
        if (entrySection != null) {
          data.addRecipe(RecipeData.loadFromConfig(entrySection));
        }
      }
    } else {
      // Legacy support
      ConfigurationSection recipeSection = config.getConfigurationSection("recipe");
      if (recipeSection != null) {
        data.addRecipe(RecipeData.loadFromConfig(recipeSection));
      }
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
        ", recipes=" + recipes.size() +
        ", lootTables=" + lootTables.size() +
        ", mobDrops=" + mobDrops.size() +
        ", villagerTrades=" + villagerTrades.size() +
        ", abilities=" + abilities.size() +
        '}';
  }
}
