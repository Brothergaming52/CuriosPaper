# Item System

The Item System provides a framework for creating and managing custom items with all their associated data — materials, textures, recipes, abilities, mob drops, and villager trades.

## Overview

Custom items in CuriosPaper are defined as `ItemData` objects and stored as individual YAML files in:

```
plugins/CuriosPaper/items/<item-id>.yml
```

## Item Properties

| Property | Type | Description |
|---|---|---|
| `itemId` | String | Unique identifier (e.g., `speed_ring`) |
| `displayName` | String | Display name with color codes |
| `material` | String | Base Minecraft material |
| `itemModel` | String | NamespacedKey for item model (1.21.3+) |
| `customModelData` | Integer | CustomModelData value (1.14–1.21.2) |
| `slotType` | String | Which accessory slot this item fits in |
| `lore` | String List | Item description lines |
| `recipes` | List | Crafting recipes for this item |
| `abilities` | Map | Abilities granted when equipped |
| `lootTables` | List | Loot table entries |
| `mobDrops` | List | Mob drop configurations |
| `villagerTrades` | List | Villager trade configurations |

## Creating Items

### In-Game Editor

Use `/edit create <item-id>` to create a new item and open the visual editor.

### Via API

```java
CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

ItemData itemData = api.createItem("my_ring");
itemData.setDisplayName("&6Golden Ring");
itemData.setMaterial("GOLD_NUGGET");
itemData.setSlotType("ring");
api.saveItemData("my_ring");
```

### Direct YAML

Create a file at `plugins/CuriosPaper/items/my_ring.yml`:

```yaml
item-id: my_ring
display-name: "&6Golden Ring"
material: GOLD_NUGGET
slot-type: ring
custom-model-data: 20001
lore:
  - "&7A simple golden ring."
  - "&7Required Slot: Ring"
```

## Item Data Manager

The `ItemDataManager` handles loading, saving, and managing custom items:

| Method | Description |
|---|---|
| `loadAllItems()` | Load all items from the `items/` directory |
| `saveItem(itemId)` | Save a single item to YAML |
| `getItem(itemId)` | Get an `ItemData` object |
| `getAllItems()` | Get all loaded items |
| `deleteItem(itemId)` | Delete an item's YAML file |
| `cleanupExternalItems()` | Remove items from plugins that are no longer loaded |

## External Items

Other plugins can register custom items through the API. External items track which plugin created them, allowing automatic cleanup if the plugin is removed:

```java
ItemData item = api.createItem(myPlugin, "plugin_ring");
```

## Item Tagging

When an `ItemStack` is created from an `ItemData`, it receives:

1. A PDC tag: `curiospaper:slot_type → "<slot-type>"`
2. A PDC tag: `curiospaper:item_id → "<item-id>"`
3. The configured display name, lore, and custom model data
4. Optional slot lore line (if `add-slot-lore-to-items` is enabled)
