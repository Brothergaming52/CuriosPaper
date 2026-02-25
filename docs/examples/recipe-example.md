# Recipe Example

Examples of each supported recipe type, shown in both YAML and Java API format.

---

## Shaped Recipe

A 3×3 crafting table recipe with a defined pattern.

### YAML

```yaml
recipes:
  diamond_ring_recipe:
    type: SHAPED
    shape:
      - " D "
      - "D D"
      - " D "
    ingredients:
      D: DIAMOND
```

### Java API

```java
import org.bg52.curiospaper.data.RecipeData;

RecipeData recipe = new RecipeData();
recipe.setType("SHAPED");
recipe.setShape(Arrays.asList(" D ", "D D", " D "));
recipe.getIngredients().put("D", "DIAMOND");

api.registerItemRecipe("my_ring", recipe);
api.saveItemData("my_ring");
```

---

## Shapeless Recipe

Ingredients in any arrangement.

### YAML

```yaml
recipes:
  lucky_charm_recipe:
    type: SHAPELESS
    ingredients:
      A: EMERALD
      B: RABBIT_FOOT
      C: GOLD_NUGGET
```

### Java API

```java
RecipeData recipe = new RecipeData();
recipe.setType("SHAPELESS");
recipe.getIngredients().put("A", "EMERALD");
recipe.getIngredients().put("B", "RABBIT_FOOT");
recipe.getIngredients().put("C", "GOLD_NUGGET");

api.registerItemRecipe("lucky_charm", recipe);
```

---

## Furnace Recipe

Smelting an item to create an accessory.

### YAML

```yaml
recipes:
  smelt_ring:
    type: FURNACE
    input-item: GOLD_INGOT
    cooking-time: 400
    experience: 2.0
```

### Java API

```java
RecipeData recipe = new RecipeData();
recipe.setType("FURNACE");
recipe.setInputItem("GOLD_INGOT");
recipe.setCookingTime(400);
recipe.setExperience(2.0f);

api.registerItemRecipe("molten_ring", recipe);
```

---

## Blast Furnace Recipe

Faster smelting via blast furnace.

### YAML

```yaml
recipes:
  blast_ring:
    type: BLAST_FURNACE
    input-item: IRON_NUGGET
    cooking-time: 100
    experience: 1.0
```

### Java API

```java
RecipeData recipe = new RecipeData();
recipe.setType("BLAST_FURNACE");
recipe.setInputItem("IRON_NUGGET");
recipe.setCookingTime(100);
recipe.setExperience(1.0f);

api.registerItemRecipe("iron_ring", recipe);
```

---

## Smoker Recipe

Cooking via smoker (food-related accessories).

### YAML

```yaml
recipes:
  cook_charm:
    type: SMOKER
    input-item: RAW_BEEF
    cooking-time: 100
    experience: 0.5
```

### Java API

```java
RecipeData recipe = new RecipeData();
recipe.setType("SMOKER");
recipe.setInputItem("RAW_BEEF");
recipe.setCookingTime(100);
recipe.setExperience(0.5f);

api.registerItemRecipe("food_charm", recipe);
```

---

## Anvil Recipe

Combining two items on an anvil.

### YAML

```yaml
recipes:
  enchant_bracelet:
    type: ANVIL
    left-input: CHAIN
    right-input: DIAMOND
```

### Java API

```java
RecipeData recipe = new RecipeData();
recipe.setType("ANVIL");
recipe.setLeftInput("CHAIN");
recipe.setRightInput("DIAMOND");

api.registerItemRecipe("enchanted_bracelet", recipe);
```

---

## Smithing Recipe

Upgrading an item at the smithing table.

### YAML

```yaml
recipes:
  upgrade_amulet:
    type: SMITHING
    base-item: NAUTILUS_SHELL
    addition-item: NETHERITE_INGOT
    template-item: NETHERITE_UPGRADE_SMITHING_TEMPLATE
```

### Java API

```java
RecipeData recipe = new RecipeData();
recipe.setType("SMITHING");
recipe.setBaseItem("NAUTILUS_SHELL");
recipe.setAdditionItem("NETHERITE_INGOT");
recipe.setTemplateItem("NETHERITE_UPGRADE_SMITHING_TEMPLATE");

api.registerItemRecipe("netherite_amulet", recipe);
```

!!! note "Pre-1.20 Servers"
    On Minecraft versions before 1.20, the `template-item` field is ignored as the old smithing table UI does not support templates.

---

## Multiple Recipes Per Item

An item obtainable through multiple crafting methods:

### YAML

```yaml
recipes:
  crafting:
    type: SHAPED
    shape:
      - " G "
      - "GDG"
      - " G "
    ingredients:
      G: GOLD_INGOT
      D: DIAMOND
  smelting:
    type: FURNACE
    input-item: RAW_GOLD_BLOCK
    cooking-time: 600
    experience: 5.0
  upgrade:
    type: SMITHING
    base-item: GOLD_NUGGET
    addition-item: DIAMOND
    template-item: NETHERITE_UPGRADE_SMITHING_TEMPLATE
```

### Java API

```java
public void registerMultipleRecipes(CuriosPaperAPI api) {
    // Shaped crafting
    RecipeData crafting = new RecipeData();
    crafting.setType("SHAPED");
    crafting.setShape(Arrays.asList(" G ", "GDG", " G "));
    crafting.getIngredients().put("G", "GOLD_INGOT");
    crafting.getIngredients().put("D", "DIAMOND");
    api.registerItemRecipe("multi_ring", crafting);

    // Furnace smelting
    RecipeData smelting = new RecipeData();
    smelting.setType("FURNACE");
    smelting.setInputItem("RAW_GOLD_BLOCK");
    smelting.setCookingTime(600);
    smelting.setExperience(5.0f);
    api.registerItemRecipe("multi_ring", smelting);

    // Smithing upgrade
    RecipeData smithing = new RecipeData();
    smithing.setType("SMITHING");
    smithing.setBaseItem("GOLD_NUGGET");
    smithing.setAdditionItem("DIAMOND");
    smithing.setTemplateItem("NETHERITE_UPGRADE_SMITHING_TEMPLATE");
    api.registerItemRecipe("multi_ring", smithing);

    api.saveItemData("multi_ring");
}
```
