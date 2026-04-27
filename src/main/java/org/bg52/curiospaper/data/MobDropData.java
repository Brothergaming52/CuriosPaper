package org.bg52.curiospaper.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

/**
 * Represents a mob drop configuration for a custom item.
 * Defines which mobs can drop this item and the drop chance.
 */
public class MobDropData {
  private String entityType; // EntityType name (e.g., "ZOMBIE", "SKELETON")
  private double chance; // 0.0 to 1.0 (0% to 100%)
  private int minAmount;
  private int maxAmount;

  // 3D Model fields
  private boolean modelEnabled = false;
  private String modelItem;
  private Integer modelCustomModelData;
  private String modelItemModel;

  public MobDropData(String entityType, double chance, int minAmount, int maxAmount) {
    this.entityType = entityType;
    this.chance = Math.max(0.0, Math.min(1.0, chance)); // Clamp between 0 and 1
    this.minAmount = Math.max(1, minAmount);
    this.maxAmount = Math.max(minAmount, maxAmount);
  }

  public MobDropData(String entityType, double chance) {
    this(entityType, chance, 1, 1);
  }

  // ========== GETTERS ==========

  public String getEntityType() {
    return entityType;
  }

  public double getChance() {
    return chance;
  }

  public int getMinAmount() {
    return minAmount;
  }

  public int getMaxAmount() {
    return maxAmount;
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

  public String getModelItemModel() {
    return modelItemModel;
  }

  // ========== SETTERS ==========

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public void setChance(double chance) {
    this.chance = Math.max(0.0, Math.min(1.0, chance));
  }

  public void setMinAmount(int minAmount) {
    this.minAmount = Math.max(1, minAmount);
    if (this.maxAmount < this.minAmount) {
      this.maxAmount = this.minAmount;
    }
  }

  public void setMaxAmount(int maxAmount) {
    this.maxAmount = Math.max(this.minAmount, maxAmount);
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

  public void setModelItemModel(String modelItemModel) {
    this.modelItemModel = modelItemModel;
  }

  // ========== SERIALIZATION ==========

  public void saveToConfig(ConfigurationSection config) {
    config.set("entity-type", entityType);
    config.set("chance", chance);
    config.set("min-amount", minAmount);
    config.set("max-amount", maxAmount);
    
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
  }

  public static MobDropData loadFromConfig(ConfigurationSection config) {
    String type = config.getString("entity-type");
    double chance = config.getDouble("chance", 0.05);
    int min = config.getInt("min-amount", 1);
    int max = config.getInt("max-amount", 1);

    if (type == null) {
      return null;
    }

    MobDropData data = new MobDropData(type, chance, min, max);
    data.setModelEnabled(config.getBoolean("model-enabled", false));
    data.setModelItem(config.getString("model-item"));
    if (config.contains("model-custom-model-data")) {
      data.setModelCustomModelData(config.getInt("model-custom-model-data"));
    }
    if (config.contains("model-item-model")) {
      data.setModelItemModel(config.getString("model-item-model"));
    }

    return data;
  }

  /**
   * Validates the mob drop configuration
   */
  public boolean isValid() {
    if (entityType == null || entityType.isEmpty()) {
      return false;
    }

    // Validate entity type
    try {
      EntityType type = EntityType.valueOf(entityType.toUpperCase());
      // Ensure it's a living entity that can drop items
      if (!type.isAlive()) {
        return false;
      }
    } catch (IllegalArgumentException e) {
      return false;
    }

    return chance >= 0.0 && chance <= 1.0
        && minAmount >= 1
        && maxAmount >= minAmount;
  }

  @Override
  public String toString() {
    return "MobDropData{" +
        "entity='" + entityType + '\'' +
        ", chance=" + (chance * 100) + "%" +
        ", amount=" + minAmount + "-" + maxAmount +
        '}';
  }
}
