# Recipe System

CuriosPaper supports multiple recipe types for custom items, all managed through the `RecipeData` class and `RecipeListener`.

## Supported Recipe Types

| Type | Description |
|---|---|
| `SHAPED` | 3×3 crafting table recipe with a defined shape |
| `SHAPELESS` | Crafting recipe without a specific pattern |
| `FURNACE` | Furnace smelting recipe |
| `ANVIL` | Anvil combining recipe |
| `SMITHING` | Smithing table recipe |

## Shaped Recipes

Shaped recipes define a 3×3 grid pattern with ingredient mappings.

```yaml
recipes:
  recipe_1:
    type: SHAPED
    shape:
      - " G "
      - "GDG"
      - " G "
    ingredients:
      G: GOLD_INGOT
      D: DIAMOND
```

### Shape Rules

- Each row is a 3-character string
- Spaces represent empty slots
- Characters map to ingredients
- The `shape` must have exactly 3 rows

## Shapeless Recipes

Shapeless recipes require specific ingredients in any arrangement.

```yaml
recipes:
  recipe_1:
    type: SHAPELESS
    ingredients:
      A: GOLD_INGOT
      B: DIAMOND
      C: EMERALD
```

## Furnace Recipes

Furnace recipes define a single input item to be smelted.

```yaml
recipes:
  recipe_1:
    type: FURNACE
    input-item: GOLD_ORE
    cooking-time: 200
    experience: 1.0
```

| Property | Default | Description |
|---|---|---|
| `input-item` | — | Material to smelt |
| `cooking-time` | `200` | Time in ticks (200 = 10 seconds) |
| `experience` | `0.0` | XP reward |

## Anvil Recipes

Anvil recipes combine two items.

```yaml
recipes:
  recipe_1:
    type: ANVIL
    left-input: GOLD_NUGGET
    right-input: DIAMOND
```

| Property | Description |
|---|---|
| `left-input` | First item (left slot) |
| `right-input` | Second item (right slot) |

## Smithing Recipes

Smithing table recipes use base, addition, and template items.

```yaml
recipes:
  recipe_1:
    type: SMITHING
    base-item: IRON_SWORD
    addition-item: NETHERITE_INGOT
    template-item: NETHERITE_UPGRADE_SMITHING_TEMPLATE
```

| Property | Description |
|---|---|
| `base-item` | The base item to upgrade |
| `addition-item` | The material to apply |
| `template-item` | The smithing template (1.20+) |

!!! note "Version Compatibility"
    Smithing recipes use reflection to work across Minecraft versions. On versions before 1.20, the template item is not required.

## Multiple Recipes

Items can have **multiple recipes**. Each recipe is a separate entry:

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
    input-item: RAW_GOLD
    cooking-time: 100
    experience: 0.5
```

## Custom Item Ingredients

Recipes can reference other custom CuriosPaper items as ingredients by prefixing the item ID with `curiospaper:`:

```yaml
ingredients:
  A: "curiospaper:magic_gem"
  B: GOLD_INGOT
```

## Recipe Registration

All recipes are registered on server startup by the `RecipeListener` and unregistered on shutdown to prevent stale entries.

## Recipe Data Transfer

When a recipe produces a custom item, the `CuriosRecipeTransferEvent` is fired to transfer PDC data from ingredients to the result, preserving custom item identity across crafting operations.
