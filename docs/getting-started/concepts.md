# Core Concepts

## Slot Types

A **slot type** is a category of accessory (e.g., `ring`, `necklace`, `back`). Each slot type is defined in `config.yml` under the `slots` section and has:

- **Key** — Internal identifier (e.g., `ring`, `head`)
- **Name** — Display name with color codes (e.g., `&6◆ Ring Slots ◆`)
- **Icon** — Material used as the GUI icon
- **Item Model** — NamespacedKey for the custom texture (1.21.3+)
- **CustomModelData** — Integer for the custom texture (1.14–1.21.2)
- **Amount** — Number of slots available (e.g., `2` for rings = two ring slots)
- **Lore** — Description lines shown in the GUI

## Accessories

An **accessory** is any `ItemStack` that has been **tagged** with a slot type using CuriosPaper's PersistentDataContainer (PDC) system.

When an item is tagged:

1. A PDC entry is added: `curiospaper:slot_type → "ring"` (for example)
2. Optionally, a lore line is added: `§7Required Slot: §6◆ Ring Slots ◆`
3. The item can now be placed into the corresponding slot in the accessory GUI

!!! info "Any Item Can Be an Accessory"
    CuriosPaper doesn't restrict which materials can be accessories. You can make a diamond sword a "ring" accessory if you want — it's all controlled by the PDC tag.

## Tagging Items

Items are tagged as accessories through one of three methods:

| Method | When to Use |
|---|---|
| **In-Game Editor** (`/edit`) | Server admins creating items without code |
| **API** (`tagAccessoryItem()`) | Plugin developers integrating programmatically |
| **Config** (item YAML files) | Bulk item definitions with recipes and abilities |

## Player Data

Each player's equipped accessories are stored in a per-player YAML file:

```
plugins/CuriosPaper/playerdata/<UUID>.yml
```

The file contains serialized `ItemStack` data for each slot type, organized by slot key and index.

## Custom Items

The **Item System** allows you to define fully custom items with:

- Display name, material, and lore
- Custom model data or item model
- Slot type assignment
- Crafting recipes (shaped, shapeless, furnace, anvil, smithing)
- Mob drops with configurable chances
- Villager trades
- Abilities (potion effects and attribute modifiers)

Custom items are stored as individual YAML files in:

```
plugins/CuriosPaper/items/<item-id>.yml
```

## Abilities

Abilities are effects attached to custom items that activate based on a **trigger**:

| Trigger | When It Activates |
|---|---|
| `EQUIP` | When the item is placed into an accessory slot |
| `DE_EQUIP` | When the item is removed from an accessory slot |
| `WHILE_EQUIPPED` | Continuously while the item remains equipped |

Abilities can apply:

- **Potion Effects** — Speed, strength, regeneration, etc.
- **Player Modifiers** — Attribute modifications (health, attack, etc.)
