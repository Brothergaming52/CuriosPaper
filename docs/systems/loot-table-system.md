# Loot Table System

The Loot Table System allows custom accessories to spawn naturally inside vanilla loot containers (chests, barrels, brushable blocks) throughout the world.

## Overview

When a vanilla loot table is populated (e.g., a dungeon chest being opened for the first time), CuriosPaper checks if any custom items are configured to appear in that loot table. If a chance roll succeeds, the custom item is injected into the container.

```
┌─────────────────────┐     ┌──────────────────┐     ┌──────────────┐
│ Vanilla Loot Event  │────▶│ LootTableListener │────▶│ Container    │
│ (chest generated)   │     │ (chance roll)     │     │ (item added) │
└─────────────────────┘     └──────────────────┘     └──────────────┘
```

## How It Works

1. CuriosPaper listens for `LootGenerateEvent` (Paper) or equivalent events
2. For each custom item with loot table entries, it checks if the current loot table key matches
3. If matched, a random chance roll is performed against the configured probability
4. On success, the custom item is created and added to the loot
5. A `CuriosLootGenerateEvent` is fired, allowing other plugins to intercept or modify the item

## Configuration

Loot table entries are stored in each item's YAML file:

```yaml
# plugins/CuriosPaper/items/magic_amulet.yml
item-id: magic_amulet
display-name: "&5Magic Amulet"
material: NAUTILUS_SHELL
slot-type: necklace

loot-tables:
  "0":
    loot-table-type: "minecraft:chests/simple_dungeon"
    chance: 0.25
    min-amount: 1
    max-amount: 1
  "1":
    loot-table-type: "minecraft:chests/buried_treasure"
    chance: 0.5
    min-amount: 1
    max-amount: 2
```

### Properties

| Property | Type | Description |
|---|---|---|
| `loot-table-type` | String | The namespaced key of the vanilla loot table (e.g., `minecraft:chests/simple_dungeon`) |
| `chance` | Double | Drop probability from 0.0 (0%) to 1.0 (100%) |
| `min-amount` | Integer | Minimum number of items to generate (≥ 1) |
| `max-amount` | Integer | Maximum number of items to generate (≥ min-amount) |

## Supported Loot Table Keys

CuriosPaper supports any vanilla loot table key. Common ones include:

| Key | Location |
|---|---|
| `minecraft:chests/simple_dungeon` | Dungeon chests |
| `minecraft:chests/abandoned_mineshaft` | Mineshaft chests |
| `minecraft:chests/buried_treasure` | Buried treasure |
| `minecraft:chests/desert_pyramid` | Desert temple |
| `minecraft:chests/jungle_temple` | Jungle temple |
| `minecraft:chests/stronghold_corridor` | Stronghold corridor |
| `minecraft:chests/stronghold_crossing` | Stronghold crossing |
| `minecraft:chests/stronghold_library` | Stronghold library |
| `minecraft:chests/end_city_treasure` | End city |
| `minecraft:chests/woodland_mansion` | Woodland mansion |
| `minecraft:chests/nether_bridge` | Nether fortress |
| `minecraft:chests/bastion_treasure` | Bastion remnant |
| `minecraft:chests/ancient_city` | Ancient city |
| `minecraft:chests/trial_chambers_reward` | Trial chambers |
| `minecraft:chests/village/village_weaponsmith` | Village weaponsmith |
| `minecraft:chests/village/village_toolsmith` | Village toolsmith |
| `minecraft:chests/shipwreck_treasure` | Shipwreck treasure |
| `minecraft:archaeology/desert_well` | Brushable sand (desert well) |
| `minecraft:archaeology/ocean_ruin_warm` | Brushable sand (ocean ruin) |

!!! tip "Finding Loot Table Keys"
    Use the in-game **Loot Table Browser** (`/edit gui <item>` → Loot Tables → Add) to browse all registered server loot table keys with search and filtering.

## Setting Up via GUI

1. Create or edit an item: `/edit gui <itemId>`
2. Click the **Loot Tables** button (chest icon)
3. Click **➕ Add** to open the Loot Table Browser
4. Browse or search for the desired loot table
5. Click a loot table to open the Quick Config screen
6. Choose a preset (10%, 25%, 50%, 100%) or enter custom values

See [Loot Table Editor](../gui-editors/loot-table-editor.md) for the full GUI guide.

## Setting Up via API

```java
CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

// Create or get the item
ItemData item = api.getItemData("magic_amulet");

// Create a loot table entry
LootTableData lootEntry = new LootTableData(
    "minecraft:chests/simple_dungeon",  // loot table key
    0.25,                                // 25% chance
    1,                                   // min amount
    1                                    // max amount
);

// Register it
api.registerItemLootTable("magic_amulet", lootEntry);

// Save to disk
api.saveItemData("magic_amulet");
```

## LootTableData Class Reference

### Constructors

```java
// Full constructor
LootTableData(String lootTableType, double chance, int minAmount, int maxAmount)

// Simple constructor (amount defaults to 1-1)
LootTableData(String lootTableType, double chance)
```

### Methods

| Method | Type | Description |
|---|---|---|
| `getLootTableType()` / `setLootTableType(String)` | `String` | The loot table key |
| `getChance()` / `setChance(double)` | `double` | Drop chance (0.0–1.0) |
| `getMinAmount()` / `setMinAmount(int)` | `int` | Minimum drop count |
| `getMaxAmount()` / `setMaxAmount(int)` | `int` | Maximum drop count |
| `isValid()` | `boolean` | Validates the entry |

### Validation

An entry is valid when:

- `lootTableType` is not null and not empty
- The type is either a valid `LootTables` enum name or a valid namespaced key format
- `chance` is between 0.0 and 1.0
- `minAmount` ≥ 1
- `maxAmount` ≥ `minAmount`

## Events

### CuriosLootGenerateEvent

Fired when a custom item is about to be placed in a loot container. Other plugins can listen to this event to modify, replace, or cancel the item generation.

```java
@EventHandler
public void onLootGenerate(CuriosLootGenerateEvent event) {
    // Get the loot table key
    String tableKey = event.getLootTableKey();

    // Get the custom item ID
    String itemId = event.getCustomItemId();

    // Cancel to prevent item from generating
    if (tableKey.contains("buried_treasure")) {
        event.setCancelled(true);
        return;
    }

    // Modify the generated item
    ItemStack item = event.getItem();
    item.setAmount(3);
    event.setItem(item);
}
```

| Method | Returns | Description |
|---|---|---|
| `getLootTableKey()` | `String` | The namespaced key of the loot table |
| `getCustomItemId()` | `String` | The internal ID of the custom item |
| `getItem()` / `setItem(ItemStack)` | `ItemStack` | The generated item |
| `isCancelled()` / `setCancelled(boolean)` | `boolean` | Cancel the item generation |
