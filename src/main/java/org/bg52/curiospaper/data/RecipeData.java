package org.bg52.curiospaper.data;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a crafting recipe for a custom item.
 * Supports both shaped and shapeless recipes.
 */
public class RecipeData {
    private RecipeType type;
    private Map<Character, String> ingredients; // Character key -> Material name
    private String[] shape; // For shaped recipes (3x3 grid)

    public RecipeData(RecipeType type) {
        this.type = type;
        this.ingredients = new HashMap<>();
        if (type == RecipeType.SHAPED) {
            this.shape = new String[3];
        }
    }

    public enum RecipeType {
        SHAPED,
        SHAPELESS
    }

    // ========== GETTERS ==========

    public RecipeType getType() {
        return type;
    }

    public Map<Character, String> getIngredients() {
        return new HashMap<>(ingredients);
    }

    public String[] getShape() {
        if (shape == null) {
            return null;
        }
        return shape.clone();
    }

    // ========== SETTERS ==========

    public void setType(RecipeType type) {
        this.type = type;
        if (type == RecipeType.SHAPED && shape == null) {
            shape = new String[3];
        } else if (type == RecipeType.SHAPELESS) {
            shape = null;
        }
    }

    public void setIngredients(Map<Character, String> ingredients) {
        this.ingredients = new HashMap<>(ingredients);
    }

    public void addIngredient(char key, String material) {
        this.ingredients.put(key, material);
    }

    public void setShape(String[] shape) {
        if (type != RecipeType.SHAPED) {
            throw new IllegalStateException("Cannot set shape on non-shaped recipe");
        }
        if (shape.length != 3) {
            throw new IllegalArgumentException("Shape must have exactly 3 rows");
        }
        this.shape = shape.clone();
    }

    public void setShapeRow(int row, String pattern) {
        if (type != RecipeType.SHAPED) {
            throw new IllegalStateException("Cannot set shape on non-shaped recipe");
        }
        if (row < 0 || row > 2) {
            throw new IllegalArgumentException("Row must be 0, 1, or 2");
        }
        this.shape[row] = pattern;
    }

    // ========== SERIALIZATION ==========

    public void saveToConfig(ConfigurationSection config) {
        config.set("type", type.name());

        // Save ingredients
        ConfigurationSection ingredientsSection = config.createSection("ingredients");
        for (Map.Entry<Character, String> entry : ingredients.entrySet()) {
            ingredientsSection.set(String.valueOf(entry.getKey()), entry.getValue());
        }

        // Save shape for shaped recipes
        if (type == RecipeType.SHAPED && shape != null) {
            config.set("shape", shape);
        }
    }

    public static RecipeData loadFromConfig(ConfigurationSection config) {
        String typeStr = config.getString("type");
        if (typeStr == null) {
            return null;
        }

        RecipeType type;
        try {
            type = RecipeType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        RecipeData data = new RecipeData(type);

        // Load ingredients
        ConfigurationSection ingredientsSection = config.getConfigurationSection("ingredients");
        if (ingredientsSection != null) {
            for (String key : ingredientsSection.getKeys(false)) {
                if (key.length() == 1) {
                    data.addIngredient(key.charAt(0), ingredientsSection.getString(key));
                }
            }
        }

        // Load shape for shaped recipes
        if (type == RecipeType.SHAPED) {
            if (config.contains("shape")) {
                data.setShape(config.getStringList("shape").toArray(new String[0]));
            }
        }

        return data;
    }

    /**
     * Validates the recipe configuration
     */
    public boolean isValid() {
        if (ingredients.isEmpty()) {
            return false;
        }

        if (type == RecipeType.SHAPED) {
            if (shape == null || shape.length != 3) {
                return false;
            }
            // Verify all characters in shape have corresponding ingredients
            for (String row : shape) {
                if (row != null) {
                    for (char c : row.toCharArray()) {
                        if (c != ' ' && !ingredients.containsKey(c)) {
                            return false;
                        }
                    }
                }
            }
        }

        // Verify all material names are valid
        for (String materialName : ingredients.values()) {
            try {
                Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "RecipeData{" +
                "type=" + type +
                ", ingredients=" + ingredients.size() +
                (type == RecipeType.SHAPED ? ", hasShape=" + (shape != null) : "") +
                '}';
    }
}
