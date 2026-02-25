# Slot Configuration

Customize the accessory slot types available to players by editing the `slots` section of `config.yml`.

Accessory slots are defined under the `slots` section of `config.yml`. Each slot type represents a category of wearable accessory.

## Slot Definition

```yaml
slots:
  ring:
    name: "&6◆ Ring Slots ◆"
    icon: "GOLD_NUGGET"
    item-model: "curiospaper:ring_slot"
    custom-model-data: 10008
    amount: 2
    lore:
      - "&7Holds powerful magical rings."
      - "&7Ancient bands of arcane energy."
```

## Properties

| Property | Type | Required | Description |
|---|---|---|---|
| `name` | String | ✅ | Display name with `&` color codes |
| `icon` | String | ✅ | Material name for the GUI icon (e.g., `GOLD_NUGGET`) |
| `item-model` | String | ❌ | NamespacedKey for custom texture (MC 1.21.3+) |
| `custom-model-data` | Integer | ❌ | CustomModelData integer for custom texture (MC 1.14–1.21.2) |
| `amount` | Integer | ✅ | Number of slots of this type per player (minimum: 1) |
| `lore` | String List | ❌ | Description lines shown in the GUI |

!!! info "Slot Key"
    The YAML key (e.g., `ring`, `head`) is the internal identifier used in commands, API calls, and item tagging. Keep it lowercase with no spaces.

## Default Slots

CuriosPaper ships with 9 pre-configured slot types:

| Key | Name | Icon | Model | CMD | Amount |
|---|---|---|---|---|---|
| `head` | ⚜ Head Slot ⚜ | `GOLDEN_HELMET` | `curiospaper:head_slot` | 10001 | 1 |
| `necklace` | ✦ Necklace Slot ✦ | `NAUTILUS_SHELL` | `curiospaper:necklace_slot` | 10002 | 1 |
| `back` | ☾ Back Slot ☾ | `ELYTRA` | `curiospaper:back_slot` | 10003 | 1 |
| `body` | ❖ Body Slot ❖ | `DIAMOND_CHESTPLATE` | `curiospaper:body_slot` | 10004 | 1 |
| `belt` | ⚔ Belt Slot ⚔ | `LEATHER` | `curiospaper:belt_slot` | 10005 | 1 |
| `hands` | ✋ Hand Slots ✋ | `LEATHER_CHESTPLATE` | `curiospaper:hands_slot` | 10006 | 2 |
| `bracelet` | ◈ Bracelet Slots ◈ | `CHAIN` | `curiospaper:bracelet_slot` | 10007 | 2 |
| `ring` | ◆ Ring Slots ◆ | `GOLD_NUGGET` | `curiospaper:ring_slot` | 10008 | 2 |
| `charm` | ✧ Charm Slots ✧ | `EMERALD` | `curiospaper:charm_slot` | 10009 | 4 |

## Adding Custom Slots

To add a new slot type, add a new entry under `slots`:

```yaml
slots:
  # ... existing slots ...

  earring:
    name: "&a✦ Earring Slots ✦"
    icon: "DIAMOND"
    item-model: "myplugin:earring_slot"
    custom-model-data: 10010
    amount: 2
    lore:
      - "&7Sparkling earrings of power."
```

## Removing Slots

To remove a slot type:

1. Delete its entry from `config.yml`
2. Restart the server

!!! warning "Data Preservation"
    Removing a slot type **does not** delete player data for that slot. Items in removed slots remain in the YAML files but are inaccessible. If you re-add the slot later, the data is restored.

## Color Codes

Use `&` color codes in slot names and lore:

| Code | Color | Code | Color |
|---|---|---|---|
| `&0` | Black | `&8` | Dark Gray |
| `&1` | Dark Blue | `&9` | Blue |
| `&2` | Dark Green | `&a` | Green |
| `&3` | Dark Aqua | `&b` | Aqua |
| `&4` | Dark Red | `&c` | Red |
| `&5` | Dark Purple | `&d` | Light Purple |
| `&6` | Gold | `&e` | Yellow |
| `&7` | Gray | `&f` | White |
| `&l` | **Bold** | `&o` | *Italic* |
| `&n` | <u>Underline</u> | `&r` | Reset |

## GUI Layout Customization

You can customize the layout of the main (Tier 1) accessory GUI by editing the `gui` section of `config.yml`.

```yaml
gui:
  # Main GUI size (multiple of 9, e.g., 27, 45, 54)
  main-slots: 54

  # Main GUI layout (slot-key: slot-index)
  # Slot index is 0-indexed (0 to main-slots - 1)
  main-layout:
    head: 10
    necklace: 13
    back: 16
    body: 21
    belt: 22
    hands: 23
    bracelet: 28
    ring: 31
    charm: 34
```

### In-Game Editor

Administrators can rearrange the main GUI layout in-game using the following command:

`/curios editmenu`

1. Open the edit GUI.
2. Drag and drop the slot icons to your desired positions.
3. Close the GUI to save the new layout to `config.yml`.

### Fail-Safe Logic

If you change the `main-slots` size or add new slot types without updating the `main-layout`, CuriosPaper will automatically place any missing or out-of-bounds slots into the first available empty spaces in the GUI.
