package org.bg52.curiospaper.data;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a villager trade configuration for a custom item.
 * Defines which villager professions can offer this item as a trade,
 * the chance of the trade appearing, and the cost items required.
 */
public class VillagerTradeData {
    private List<String> professions; // List of profession names, empty = all professions
    private double chance; // 0.0 to 1.0 (0% to 100%)
    private List<TradeCost> costItems; // 1-2 items required for the trade
    private List<Integer> tradeLevels; // Which villager levels can have this trade (1-5)

    public VillagerTradeData(List<String> professions, double chance, List<TradeCost> costItems,
                             List<Integer> tradeLevels) {
        this.professions = new ArrayList<>(professions != null ? professions : new ArrayList<>());
        this.chance = Math.max(0.0, Math.min(1.0, chance));
        this.costItems = new ArrayList<>(costItems != null ? costItems : new ArrayList<>());
        this.tradeLevels = new ArrayList<>(tradeLevels != null ? tradeLevels : List.of(1, 2, 3, 4, 5));
    }

    // Simplified constructor for all professions, all levels
    public VillagerTradeData(double chance, List<TradeCost> costItems) {
        this(new ArrayList<>(), chance, costItems, List.of(1, 2, 3, 4, 5));
    }

    // ========== GETTERS ==========

    public List<String> getProfessions() {
        return new ArrayList<>(professions);
    }

    public double getChance() {
        return chance;
    }

    public List<TradeCost> getCostItems() {
        return new ArrayList<>(costItems);
    }

    public List<Integer> getTradeLevels() {
        return new ArrayList<>(tradeLevels);
    }

    // ========== SETTERS ==========

    public void setProfessions(List<String> professions) {
        this.professions = new ArrayList<>(professions != null ? professions : new ArrayList<>());
    }

    public void setChance(double chance) {
        this.chance = Math.max(0.0, Math.min(1.0, chance));
    }

    public void setCostItems(List<TradeCost> costItems) {
        this.costItems = new ArrayList<>(costItems != null ? costItems : new ArrayList<>());
    }

    public void setTradeLevels(List<Integer> tradeLevels) {
        this.tradeLevels = new ArrayList<>(tradeLevels != null ? tradeLevels : new ArrayList<>());
    }

    public void addProfession(String profession) {
        if (!this.professions.contains(profession.toUpperCase())) {
            this.professions.add(profession.toUpperCase());
        }
    }

    public void addCostItem(TradeCost cost) {
        if (this.costItems.size() < 2) {
            this.costItems.add(cost);
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Checks if this trade applies to all professions
     */
    public boolean appliesToAllProfessions() {
        return professions.isEmpty();
    }

    /**
     * Checks if this trade applies to a specific profession
     */
    public boolean appliesToProfession(String profession) {
        if (appliesToAllProfessions()) {
            return true;
        }
        return professions.contains(profession.toUpperCase());
    }

    /**
     * Checks if this trade can appear at a specific villager level
     */
    public boolean appliesToLevel(int level) {
        return tradeLevels.contains(level);
    }

    // ========== SERIALIZATION ==========

    public void saveToConfig(ConfigurationSection config) {
        config.set("professions", professions);
        config.set("chance", chance);
        config.set("trade-levels", tradeLevels);

        if (!costItems.isEmpty()) {
            ConfigurationSection costsSection = config.createSection("costs");
            for (int i = 0; i < costItems.size(); i++) {
                ConfigurationSection costSection = costsSection.createSection("cost-" + i);
                costItems.get(i).saveToConfig(costSection);
            }
        }
    }

    public static VillagerTradeData loadFromConfig(ConfigurationSection config) {
        List<String> professions = config.getStringList("professions");
        double chance = config.getDouble("chance", 0.1);
        List<Integer> tradeLevels = config.getIntegerList("trade-levels");
        if (tradeLevels.isEmpty()) {
            tradeLevels = List.of(1, 2, 3, 4, 5);
        }

        List<TradeCost> costs = new ArrayList<>();
        ConfigurationSection costsSection = config.getConfigurationSection("costs");
        if (costsSection != null) {
            for (String key : costsSection.getKeys(false)) {
                ConfigurationSection costSection = costsSection.getConfigurationSection(key);
                if (costSection != null) {
                    TradeCost cost = TradeCost.loadFromConfig(costSection);
                    if (cost != null) {
                        costs.add(cost);
                    }
                }
            }
        }

        return new VillagerTradeData(professions, chance, costs, tradeLevels);
    }

    /**
     * Validates the trade configuration
     */
    public boolean isValid() {
        // Must have at least one cost item
        if (costItems.isEmpty() || costItems.size() > 2) {
            return false;
        }

        // All cost items must be valid
        for (TradeCost cost : costItems) {
            if (!cost.isValid()) {
                return false;
            }
        }

        // Chance must be valid
        if (chance < 0.0 || chance > 1.0) {
            return false;
        }

        // Validate professions if specified
        if (!professions.isEmpty()) {
            for (String prof : professions) {
                try {
                    Villager.Profession.valueOf(prof.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }

        // Validate trade levels
        for (int level : tradeLevels) {
            if (level < 1 || level > 5) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        String profStr = appliesToAllProfessions() ? "ALL" : String.join(", ", professions);
        String costsStr = costItems.stream()
                .map(TradeCost::toString)
                .collect(Collectors.joining(" + "));
        return "VillagerTrade{" +
                "professions=" + profStr +
                ", chance=" + (chance * 100) + "%" +
                ", costs=" + costsStr +
                ", levels=" + tradeLevels +
                '}';
    }

    // ========== INNER CLASS: TradeCost ==========

    /**
     * Represents a single cost item in a trade
     */
    public static class TradeCost {
        private String material; // Material type
        private int minAmount;
        private int maxAmount;

        public TradeCost(String material, int minAmount, int maxAmount) {
            this.material = material != null ? material.toUpperCase() : "EMERALD";
            this.minAmount = Math.max(1, minAmount);
            this.maxAmount = Math.max(this.minAmount, maxAmount);
        }

        public TradeCost(String material, int amount) {
            this(material, amount, amount);
        }

        // ========== GETTERS ==========

        public String getMaterial() {
            return material;
        }

        public int getMinAmount() {
            return minAmount;
        }

        public int getMaxAmount() {
            return maxAmount;
        }

        // ========== SETTERS ==========

        public void setMaterial(String material) {
            this.material = material != null ? material.toUpperCase() : "EMERALD";
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
            config.set("material", material);
            config.set("min-amount", minAmount);
            config.set("max-amount", maxAmount);
        }

        public static TradeCost loadFromConfig(ConfigurationSection config) {
            String material = config.getString("material");
            int min = config.getInt("min-amount", 1);
            int max = config.getInt("max-amount", 1);

            if (material == null) {
                return null;
            }

            return new TradeCost(material, min, max);
        }

        /**
         * Validates the cost configuration
         */
        public boolean isValid() {
            if (material == null || material.isEmpty()) {
                return false;
            }

            try {
                Material.valueOf(material.toUpperCase());
            } catch (IllegalArgumentException e) {
                return false;
            }

            return minAmount >= 1 && maxAmount >= minAmount;
        }

        @Override
        public String toString() {
            if (minAmount == maxAmount) {
                return minAmount + "x " + material;
            }
            return minAmount + "-" + maxAmount + "x " + material;
        }
    }
}
