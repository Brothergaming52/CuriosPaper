package org.bg52.curiospaper.data;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a crafting recipe for a custom item.
 * Supports multiple recipe types including Crafting, Furnace, Anvil, etc.
 */
public class RecipeData {
    private RecipeType type;

    // Crafting Table (Shaped/Shapeless)
    private Map<Character, String> ingredients; // Character key -> Material name/ItemID
    private String[] shape; // For shaped recipes (3x3 grid)

    // Furnace / Blast Furnace / Smoker
    private String inputItem; // Material or Custom ID
    private int cookingTime; // in ticks
    private float experience; // XP yield (Furnace) or Cost (Anvil)

    // Anvil
    private String leftInput;
    private String rightInput;
    // experience is used for cost

    // Smithing
    private String baseItem;
    private String additionItem;
    private String templateItem;

    public RecipeData(RecipeType type) {
        this.type = type;
        this.ingredients = new HashMap<>();
        if (type == RecipeType.SHAPED) {
            this.shape = new String[3];
        } else if (type == RecipeType.FURNACE || type == RecipeType.BLAST_FURNACE || type == RecipeType.SMOKER) {
            this.cookingTime = 200; // Default 10 seconds
            this.experience = 0.5f;
        }
    }

    public enum RecipeType {
        SHAPED,
        SHAPELESS,
        FURNACE,
        BLAST_FURNACE,
        SMOKER,
        ANVIL,
        SMITHING
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

    public String getInputItem() {
        return inputItem;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    public float getExperience() {
        return experience;
    }

    public String getLeftInput() {
        return leftInput;
    }

    public String getRightInput() {
        return rightInput;
    }

    public String getBaseItem() {
        return baseItem;
    }

    public String getAdditionItem() {
        return additionItem;
    }

    public String getTemplateItem() {
        return templateItem;
    }

    // ========== SETTERS ==========

    public void setType(RecipeType type) {
        this.type = type;
        if (type == RecipeType.SHAPED && shape == null) {
            shape = new String[3];
        } else if (type != RecipeType.SHAPED) {
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

    public void setInputItem(String input) {
        this.inputItem = input;
    }

    public void setCookingTime(int time) {
        this.cookingTime = time;
    }

    public void setExperience(float xp) {
        this.experience = xp;
    }

    public void setLeftInput(String left) {
        this.leftInput = left;
    }

    public void setRightInput(String right) {
        this.rightInput = right;
    }

    public void setBaseItem(String base) {
        this.baseItem = base;
    }

    public void setAdditionItem(String addition) {
        this.additionItem = addition;
    }

    public void setTemplateItem(String template) {
        this.templateItem = template;
    }

    // ========== SERIALIZATION ==========

    public void saveToConfig(ConfigurationSection config) {
        config.set("type", type.name());

        if (type == RecipeType.SHAPED || type == RecipeType.SHAPELESS) {
            ConfigurationSection ingredientsSection = config.createSection("ingredients");
            for (Map.Entry<Character, String> entry : ingredients.entrySet()) {
                ingredientsSection.set(String.valueOf(entry.getKey()), entry.getValue());
            }
            if (type == RecipeType.SHAPED && shape != null) {
                config.set("shape", shape);
            }
        } else if (type == RecipeType.FURNACE || type == RecipeType.BLAST_FURNACE || type == RecipeType.SMOKER) {
            config.set("input", inputItem);
            config.set("cooking-time", cookingTime);
            config.set("experience", experience);
        } else if (type == RecipeType.ANVIL) {
            config.set("left-input", leftInput);
            config.set("right-input", rightInput);
            config.set("experience", experience);
        } else if (type == RecipeType.SMITHING) {
            config.set("base", baseItem);
            config.set("addition", additionItem);
            if (templateItem != null) {
                config.set("template", templateItem);
            }
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

        if (type == RecipeType.SHAPED || type == RecipeType.SHAPELESS) {
            ConfigurationSection ingredientsSection = config.getConfigurationSection("ingredients");
            if (ingredientsSection != null) {
                for (String key : ingredientsSection.getKeys(false)) {
                    if (key.length() == 1) {
                        data.addIngredient(key.charAt(0), ingredientsSection.getString(key));
                    }
                }
            }
            if (type == RecipeType.SHAPED && config.contains("shape")) {
                data.setShape(config.getStringList("shape").toArray(new String[0]));
            }
        } else if (type == RecipeType.FURNACE || type == RecipeType.BLAST_FURNACE || type == RecipeType.SMOKER) {
            data.setInputItem(config.getString("input"));
            data.setCookingTime(config.getInt("cooking-time", 200));
            data.setExperience((float) config.getDouble("experience", 0.0));
        } else if (type == RecipeType.ANVIL) {
            data.setLeftInput(config.getString("left-input"));
            data.setRightInput(config.getString("right-input"));
            data.setExperience((float) config.getDouble("experience", 0.0));
        } else if (type == RecipeType.SMITHING) {
            data.setBaseItem(config.getString("base"));
            data.setAdditionItem(config.getString("addition"));
            if (config.contains("template")) {
                data.setTemplateItem(config.getString("template"));
            }
        }

        return data;
    }

    /**
     * Validates the recipe configuration
     */
    public boolean isValid() {
        if (type == RecipeType.SHAPED) {
            if (ingredients.isEmpty() || shape == null || shape.length != 3)
                return false;
            for (String row : shape) {
                if (row != null) {
                    for (char c : row.toCharArray()) {
                        if (c != ' ' && !ingredients.containsKey(c))
                            return false;
                    }
                }
            }
        } else if (type == RecipeType.SHAPELESS) {
            return !ingredients.isEmpty();
        } else if (type == RecipeType.FURNACE || type == RecipeType.BLAST_FURNACE || type == RecipeType.SMOKER) {
            return inputItem != null && !inputItem.isEmpty();
        } else if (type == RecipeType.ANVIL) {
            return leftInput != null && !leftInput.isEmpty() && rightInput != null && !rightInput.isEmpty();
        } else if (type == RecipeType.SMITHING) {
            return baseItem != null && !baseItem.isEmpty() && additionItem != null && !additionItem.isEmpty();
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RecipeData that = (RecipeData) o;
        return cookingTime == that.cookingTime &&
                Float.compare(that.experience, experience) == 0 &&
                type == that.type &&
                java.util.Objects.equals(ingredients, that.ingredients) &&
                java.util.Arrays.equals(shape, that.shape) &&
                java.util.Objects.equals(inputItem, that.inputItem) &&
                java.util.Objects.equals(leftInput, that.leftInput) &&
                java.util.Objects.equals(rightInput, that.rightInput) &&
                java.util.Objects.equals(baseItem, that.baseItem) &&
                java.util.Objects.equals(additionItem, that.additionItem) &&
                java.util.Objects.equals(templateItem, that.templateItem);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(type, ingredients, inputItem, cookingTime, experience, leftInput,
                rightInput, baseItem, additionItem, templateItem);
        result = 31 * result + java.util.Arrays.hashCode(shape);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RecipeData{type=" + type);
        if (type == RecipeType.SHAPED || type == RecipeType.SHAPELESS) {
            sb.append(", ingredients=").append(ingredients.size());
        } else if (type == RecipeType.FURNACE) {
            sb.append(", input=").append(inputItem);
        }
        sb.append("}");
        return sb.toString();
    }
}
