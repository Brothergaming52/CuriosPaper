# Loot Tables & Mob Drops API

The CuriosPaper API provides methods for programmatically registering loot table entries and mob drops for custom items.

## Loot Tables

### Registering a Loot Table Entry

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.data.LootTableData;

CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

// Create a loot table entry
LootTableData lootEntry = new LootTableData(
    "minecraft:chests/simple_dungeon",  // loot table key
    0.15,                                // 15% chance
    1,                                   // min amount
    2                                    // max amount
);

// Register it on an existing item
boolean success = api.registerItemLootTable("magic_ring", lootEntry);
if (success) {
    api.saveItemData("magic_ring");
}
```

### Creating an Item with Loot Tables

```java
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.LootTableData;

// Create the item
ItemData item = api.createItem(myPlugin, "dungeon_amulet");
item.setDisplayName("§5Dungeon Amulet");
item.setMaterial("NAUTILUS_SHELL");
item.setSlotType("necklace");

// Add multiple loot table entries
item.addLootTable(new LootTableData("minecraft:chests/simple_dungeon", 0.20, 1, 1));
item.addLootTable(new LootTableData("minecraft:chests/abandoned_mineshaft", 0.10, 1, 1));
item.addLootTable(new LootTableData("minecraft:chests/stronghold_corridor", 0.30, 1, 1));

// Save
api.saveItemData("dungeon_amulet");
```

### LootTableData Reference

| Constructor | Description |
|---|---|
| `LootTableData(String type, double chance, int min, int max)` | Full constructor |
| `LootTableData(String type, double chance)` | Simple constructor (amount 1–1) |

| Method | Type | Description |
|---|---|---|
| `getLootTableType()` / `setLootTableType(String)` | `String` | The loot table namespaced key |
| `getChance()` / `setChance(double)` | `double` | Drop probability (0.0–1.0) |
| `getMinAmount()` / `setMinAmount(int)` | `int` | Minimum item count (≥ 1) |
| `getMaxAmount()` / `setMaxAmount(int)` | `int` | Maximum item count (≥ min) |
| `isValid()` | `boolean` | Validates the configuration |

---

## Mob Drops

### Registering a Mob Drop

```java
import org.bg52.curiospaper.data.MobDropData;

// Create a mob drop entry
MobDropData mobDrop = new MobDropData();
mobDrop.setEntityType("ZOMBIE");
mobDrop.setChance(0.05);      // 5% chance
mobDrop.setMinAmount(1);
mobDrop.setMaxAmount(1);

// Register it
boolean success = api.registerItemMobDrop("cursed_ring", mobDrop);
if (success) {
    api.saveItemData("cursed_ring");
}
```

### Creating an Item with Mob Drops

```java
ItemData item = api.createItem(myPlugin, "skeleton_charm");
item.setDisplayName("§7Bone Charm");
item.setMaterial("BONE");
item.setSlotType("charm");

// Add mob drop
MobDropData drop = new MobDropData();
drop.setEntityType("SKELETON");
drop.setChance(0.08);
drop.setMinAmount(1);
drop.setMaxAmount(1);
item.getMobDrops().add(drop);

api.saveItemData("skeleton_charm");
```

### MobDropData Reference

| Method | Type | Description |
|---|---|---|
| `getEntityType()` / `setEntityType(String)` | `String` | The entity type name (e.g., `ZOMBIE`) |
| `getChance()` / `setChance(double)` | `double` | Drop probability (0.0–1.0) |
| `getMinAmount()` / `setMinAmount(int)` | `int` | Minimum drop count |
| `getMaxAmount()` / `setMaxAmount(int)` | `int` | Maximum drop count |
| `isModelEnabled()` / `setModelEnabled(boolean)` | `boolean` | Whether the mob wears a 3D model |
| `getModelItem()` / `setModelItem(String)` | `String` | Model material name |
| `getModelCustomModelData()` / `setModelCustomModelData(Integer)` | `Integer` | Model CMD value |
| `getModelItemModel()` / `setModelItemModel(String)` | `String` | Model item model component |

---

## Villager Trades

### Registering a Villager Trade

```java
import org.bg52.curiospaper.data.VillagerTradeData;

VillagerTradeData trade = new VillagerTradeData();
trade.setProfessions(Arrays.asList("CLERIC", "LIBRARIAN"));
trade.setChance(0.3);           // 30% chance the villager has this trade
trade.setTradeLevels(Arrays.asList(3, 4, 5));
trade.setCostMaterial("EMERALD");
trade.setCostMinAmount(15);
trade.setCostMaxAmount(30);

boolean success = api.registerItemVillagerTrade("holy_ring", trade);
if (success) {
    api.saveItemData("holy_ring");
}
```

---

## Listening for Events

Both loot generation and mob drops fire custom events:

```java
import org.bg52.curiospaper.event.CuriosLootGenerateEvent;
import org.bg52.curiospaper.event.CuriosMobDropEvent;

// Listen for loot table generation
@EventHandler
public void onLootGenerate(CuriosLootGenerateEvent event) {
    String tableKey = event.getLootTableKey();
    String itemId = event.getCustomItemId();
    // Cancel, modify item, etc.
}

// Listen for mob drops
@EventHandler
public void onMobDrop(CuriosMobDropEvent event) {
    LivingEntity mob = event.getEntity();
    String itemId = event.getCustomItemId();
    // Cancel, modify item, etc.
}
```

See the [Events](events.md) page for detailed event documentation and examples.
