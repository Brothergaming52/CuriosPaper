# Accessory System

The accessory system is the core of CuriosPaper, managing slot types, item validation, equip/unequip mechanics, and data persistence.

The Accessory System is the core of CuriosPaper. It manages custom equipment slots, item validation, and the GUI interface for equipping and unequipping accessories.

## How It Works

```
┌──────────────────┐     ┌──────────────┐     ┌─────────────────┐
│ Config Manager  │────▶│Slot Manager│────▶│  Player Data   │
│  (slot types)   │     │ (runtime)  │     │  (YAML files)  │
└──────────────────┘     └──────────────┘     └─────────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐     ┌──────────────┐
│ Accessory GUI  │◀────│  Inventory  │
│ (2-tier menu)  │     │  Listener   │
└─────────────────┘     └──────────────┘
```

## Slot Types

Slot types are loaded from `config.yml` on startup. Each slot type defines:

- A unique key (e.g., `ring`, `head`)
- Display properties (name, icon, lore)
- Number of available slots
- Custom texture data (item model or custom model data)

## Item Validation

When a player tries to place an item in an accessory slot, CuriosPaper validates:

1. **PDC tag** — The item must have a `curiospaper:slot_type` tag
2. **Slot match** — The tag value must match the target slot type
3. **Slot type exists** — The tagged slot type must be registered

```java
// How CuriosPaper checks if an item is valid for a slot
boolean isValid = api.isValidAccessory(itemStack, "ring");
```

## Equip/Unequip Flow

### Equipping

1. Player opens the GUI with `/baubles`
2. Player clicks a slot type → opens Tier 2 view
3. Player clicks an empty slot with a tagged item on cursor
4. `InventoryListener` validates the item
5. `AccessoryEquipEvent` is fired (cancellable)
6. Item is stored in `SlotManager` → player data
7. Abilities are evaluated by `AbilityListener`

### Unequipping

1. Player clicks an occupied slot
2. Item is returned to cursor
3. `AccessoryEquipEvent` is fired with `Action.UNEQUIP`
4. Running abilities are removed by `AbilityListener`
5. Player data is updated

## Data Persistence

Player accessories are saved:

- **Automatically** — At the configured `save-interval` (default: 5 minutes)
- **On GUI close** — If `save-on-close` is enabled
- **On disconnect** — Player data is always saved when quitting
- **On server stop** — All loaded player data is saved

## Slot Manager

The `SlotManager` is the central data store for player accessories:

| Method | Description |
|---|---|
| `loadPlayerData(player)` | Load or create player data from YAML |
| `savePlayerData(playerId)` | Save player data to YAML |
| `getAccessories(playerId, slotType)` | Get items in a slot type |
| `setAccessories(playerId, slotType, items)` | Set items in a slot type |
| `setAccessoryItem(playerId, slotType, index, item)` | Set a single item |
| `getAccessoryItem(playerId, slotType, index)` | Get a single item |
| `unloadPlayerData(playerId)` | Remove from memory |
