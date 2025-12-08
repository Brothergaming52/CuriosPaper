package org.bg52.curiospaper.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.loot.LootTables;

/**
 * Represents a loot table entry for a custom item.
 * Defines which chest types can contain this item and the drop chance.
 */
public class LootTableData {
    private String lootTableType; // e.g., "CHEST", "MINESHAFT", "DUNGEON", etc.
    private double chance; // 0.0 to 1.0 (0% to 100%)
    private int minAmount;
    private int maxAmount;

    public LootTableData(String lootTableType, double chance, int minAmount, int maxAmount) {
        this.lootTableType = lootTableType;
        this.chance = Math.max(0.0, Math.min(1.0, chance)); // Clamp between 0 and 1
        this.minAmount = Math.max(1, minAmount);
        this.maxAmount = Math.max(minAmount, maxAmount);
    }

    public LootTableData(String lootTableType, double chance) {
        this(lootTableType, chance, 1, 1);
    }

    // ========== GETTERS ==========

    public String getLootTableType() {
        return lootTableType;
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

    // ========== SETTERS ==========

    public void setLootTableType(String lootTableType) {
        this.lootTableType = lootTableType;
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

    // ========== SERIALIZATION ==========

    public void saveToConfig(ConfigurationSection config) {
        config.set("loot-table-type", lootTableType);
        config.set("chance", chance);
        config.set("min-amount", minAmount);
        config.set("max-amount", maxAmount);
    }

    public static LootTableData loadFromConfig(ConfigurationSection config) {
        String type = config.getString("loot-table-type");
        double chance = config.getDouble("chance", 0.1);
        int min = config.getInt("min-amount", 1);
        int max = config.getInt("max-amount", 1);

        if (type == null) {
            return null;
        }

        return new LootTableData(type, chance, min, max);
    }

    /**
     * Validates the loot table configuration
     */
    public boolean isValid() {
        if (lootTableType == null || lootTableType.isEmpty()) {
            return false;
        }

        // Try to validate against known loot table types
        try {
            // Check if it's a valid vanilla loot table
            LootTables.valueOf(lootTableType.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Could be a custom loot table type, which is also valid
            // Just ensure it's a reasonable string
            if (lootTableType.length() > 100) {
                return false;
            }
        }

        return chance >= 0.0 && chance <= 1.0
                && minAmount >= 1
                && maxAmount >= minAmount;
    }

    @Override
    public String toString() {
        return "LootTableData{" +
                "type='" + lootTableType + '\'' +
                ", chance=" + (chance * 100) + "%" +
                ", amount=" + minAmount + "-" + maxAmount +
                '}';
    }
}
