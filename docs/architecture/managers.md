# Managers

CuriosPaper uses a manager pattern to organize its core functionality.

## ConfigManager

**Package:** `org.bg52.curiospaper.config`

Manages plugin configuration and slot type definitions.

| Responsibility | Details |
|---|---|
| Load `config.yml` | Parses YAML and creates `SlotConfiguration` objects |
| Slot validation | Reports errors and warnings for invalid configurations |
| Configuration access | Provides typed access to configuration values |

### Key Methods

| Method | Description |
|---|---|
| `getSlotConfigurations()` | Get all loaded slot configurations |
| `getSlotConfiguration(key)` | Get a specific slot's configuration |
| `addSlotConfiguration(key, config)` | Register a new slot configuration |
| `removeSlotConfiguration(key)` | Remove a slot configuration |
| `hasSlotConfiguration(key)` | Check if a slot exists |
| `getValidationReport()` | Get configuration validation results |

## SlotManager

**Package:** `org.bg52.curiospaper.manager`

Manages per-player accessory data in memory and on disk.

| Responsibility | Details |
|---|---|
| Player data I/O | Load/save YAML files per player UUID |
| Memory cache | `Map<UUID, Map<String, List<ItemStack>>>` |
| Slot operations | Get, set, and clear items by slot type and index |
| Bulk operations | Save all, cleanup orphaned data |

### Data Structure

```
playerAccessories: Map<UUID, Map<String, List<ItemStack>>>
                        │          │           │
                     Player    Slot Type    Items in slots
                      UUID    (e.g. "ring") (indexed list)
```

### Key Methods

| Method | Description |
|---|---|
| `loadPlayerData(Player)` | Load from YAML or create new |
| `savePlayerData(UUID)` | Persist to YAML |
| `saveAllPlayerData()` | Save all loaded players |
| `getAccessories(UUID, String)` | Get items for a slot type |
| `setAccessoryItem(UUID, String, int, ItemStack)` | Set a specific slot |
| `unloadPlayerData(UUID)` | Remove from memory |
| `cleanupOrphanedData(Set<UUID>)` | Delete orphaned files |

## ItemDataManager

**Package:** `org.bg52.curiospaper.manager`

Manages custom item definitions stored as individual YAML files.

| Responsibility | Details |
|---|---|
| Item loading | Scan `items/` directory for `.yml` files |
| Item persistence | Save/delete individual item files |
| External items | Track items owned by other plugins |
| Cleanup | Remove items from unloaded plugins |

## ResourcePackManager

**Package:** `org.bg52.curiospaper.resourcepack`

Handles resource pack generation and HTTP hosting.

| Responsibility | Details |
|---|---|
| Asset collection | Merge CuriosPaper and external plugin assets |
| Pack generation | Create ZIP file with `pack.mcmeta` |
| Conflict detection | Detect namespace and file conflicts |
| HTTP hosting | Serve pack via embedded Netty server |
| Hash calculation | SHA-1 hash for client verification |
| Distribution | Send pack URL to players on join |

## ChatInputManager

**Package:** `org.bg52.curiospaper.manager`

Captures chat messages for use as GUI input values.

| Responsibility | Details |
|---|---|
| Input capture | Intercept chat messages from players in "input mode" |
| Callbacks | Execute registered callbacks with the captured input |
| Timeout | Auto-cancel after configurable timeout |
