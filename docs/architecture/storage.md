# Storage

CuriosPaper uses YAML-based flat file storage for all persistent data.

## Player Data

### Location

```
plugins/CuriosPaper/playerdata/<UUID>.yml
```

### Format

```yaml
ring:
  slot_0:
    ==: org.bukkit.inventory.ItemStack
    v: 2586
    type: GOLD_NUGGET
    meta:
      ==: ItemMeta
      meta-type: UNSPECIFIC
      display-name: §6Golden Ring
      lore:
        - §7Required Slot: §6◆ Ring Slots ◆
      PublicBukkitValues:
        curiospaper:slot_type: ring
        curiospaper:item_id: golden_ring
  slot_1: null
charm:
  slot_0: null
  slot_1: null
  slot_2: null
  slot_3: null
```

### Serialization

- Items are serialized using Bukkit's built-in `ConfigurationSerializable` system
- Empty slots are stored as `null`
- Each slot type has a section with indexed entries (`slot_0`, `slot_1`, etc.)

## Custom Item Data

### Location

```
plugins/CuriosPaper/items/<item-id>.yml
```

### Format

```yaml
item-id: speed_ring
display-name: "&6Ring of Swiftness"
material: GOLD_NUGGET
slot-type: ring
custom-model-data: 20001
item-model: "myplugin:speed_ring"
lore:
  - "&7Grants the wearer incredible speed."
abilities:
  speed:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: SPEED
    amplifier: 1
    duration: 200
recipes:
  main:
    type: SHAPED
    shape:
      - " G "
      - "GDG"
      - " G "
    ingredients:
      G: GOLD_INGOT
      D: DIAMOND
mob-drops:
  zombie:
    entity-type: ZOMBIE
    chance: 0.05
    min-amount: 1
    max-amount: 1
```

## Data Lifecycle

### On Player Join

1. Check for existing data file at `playerdata/<UUID>.yml`
2. If exists: deserialize all slot entries into `Map<String, List<ItemStack>>`
3. If not: create empty data structure with null-filled slot lists
4. Store in `SlotManager`'s memory cache

### On Player Quit

1. Save data to YAML file
2. If `unload-on-quit` is true: remove from memory cache

### Auto-Save

A repeating task runs every `save-interval` seconds (default: 300):

1. Iterate all loaded player UUIDs
2. Save each to their YAML file
3. Log the save count (if debug enabled)

### Server Shutdown

1. Cancel the auto-save task
2. Call `SlotManager.saveAllPlayerData()`
3. All loaded players' data is persisted

## File System Layout

```
plugins/CuriosPaper/
├── config.yml              # Main configuration
├── items/                  # Custom item definitions
│   ├── speed_ring.yml
│   ├── fire_amulet.yml
│   └── ...
├── playerdata/             # Per-player accessory data
│   ├── 550e8400-e29b-41d4-a716-446655440000.yml
│   └── ...
└── resourcepack/           # Generated resource pack
    ├── pack.mcmeta
    └── assets/
        └── ...
```
