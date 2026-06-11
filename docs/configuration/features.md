# Features Configuration

CuriosPaper provides feature toggles in `config.yml` to enable or disable specific functionality.

## Basic Feature Toggles

```yaml
features:
  add-slot-lore-to-items: true
  item-editor:
    enabled: true
  allow-elytra-on-back-slot: true
  show-empty-slots: true
```

| Feature | Default | Description |
|---|---|---|
| `add-slot-lore-to-items` | `true` | Adds "Required Slot: ..." lore to tagged items |
| `item-editor.enabled` | `true` | Enables `/edit` command and custom item system |
| `allow-elytra-on-back-slot` | `true` | Allows elytra in back slots (requires 1.21.3+) |
| `show-empty-slots` | `true` | Show placeholder icons for empty slots in GUI |

## Sound Effects

```yaml
features:
  play-gui-sound: true
  gui-sound: "BLOCK_CHEST_OPEN"

  play-equip-sound: true
  equip-sound: "ENTITY_ITEM_PICKUP"

  play-unequip-sound: true
  unequip-sound: "ENTITY_ITEM_PICKUP"
```

| Feature | Default | Description |
|---|---|---|
| `play-gui-sound` | `true` | Play sound when opening the accessory GUI |
| `gui-sound` | `BLOCK_CHEST_OPEN` | Sound to play when the GUI opens |
| `play-equip-sound` | `true` | Play sound when equipping an accessory |
| `equip-sound` | `ENTITY_ITEM_PICKUP` | Sound to play on equip |
| `play-unequip-sound` | `true` | Play sound when unequipping an accessory |
| `unequip-sound` | `ENTITY_ITEM_PICKUP` | Sound to play on unequip |

## Hotkey System

Allows players to open the accessory GUI without commands using a sneak-based keybind.

```yaml
features:
  hotkey:
    enabled: true
    slot: 8
    sneak-type: "double"
    sneak-hold-duration: 3
```

| Setting | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable/disable the hotkey system |
| `slot` | `8` | Hotbar slot (1–9) that must be selected |
| `sneak-type` | `double` | Sneak detection mode: `single`, `double`, or `hold` |
| `sneak-hold-duration` | `3` | Seconds to hold sneak (only for `hold` mode) |

### Sneak Detection Modes

| Mode | Description |
|---|---|
| `single` | Opens GUI on a single sneak press |
| `double` | Opens GUI when sneaking twice within 500ms |
| `hold` | Opens GUI when sneaking is held for the configured duration |

See [Hotkey System](../systems/hotkey-system.md) for detailed documentation.

## Keep Curio Inventory on Death

Controls what happens to a player's equipped accessories when they die.

```yaml
features:
  keep-curio-inventory:
    type: "Auto"
```

| Mode | Behavior |
|---|---|
| `Always` | Always keep curio inventory on death, even without `keepInventory` gamerule |
| `Auto` | Follow the vanilla `keepInventory` gamerule (default) |
| `Never` | Always drop curio inventory on death, even with `keepInventory` enabled |

See [Death Behavior](../systems/death-behavior.md) for detailed documentation.

## Random Teleport (RTP) Compatibility

CuriosPaper includes a temporary dismount and recording system to prevent passenger armor stand glitches during teleports, portals, or random teleports.

```yaml
features:
  rtp:
    enabled: false
    commands:
      - "rtp"
      - "wild"
    blocks: []
    entities: []
    guis: []
```

| Setting | Default | Description |
|---|---|---|
| `enabled` | `false` | Enable/disable RTP dismount checks |
| `commands` | `["rtp", "wild"]` | Commands that trigger temporary model dismounting |
| `blocks` | `[]` | Interacted block types (e.g., `STONE_BUTTON`) or locations (`world,x,y,z`) |
| `entities` | `[]` | Clicked entities or NPC names |
| `guis` | `[]` | Opened inventory/GUI titles or `title:slot` |

See [3D Model System](../systems/3d-model-system.md#rtp-compatibility) for detailed documentation.

## Resource Pack Settings

```yaml
resource-pack:
  mode: "SELF"
  url: "https://example.com/your-resource-pack.zip"
  port: 8080
  host-ip: "localhost"
  base-material: "PAPER"
  combine-external-rp: false
  allow-minecraft-namespace: true
  allow-namespace-conflicts: false
```

| Setting | Default | Description |
|---|---|---|
| `mode` | `SELF` | Hosting mode: `SELF` (embedded Netty server), `LINK` (external download link), `NONE` (disabled) |
| `url` | `""` | Direct download link for the resource pack (only used if `mode` is `LINK`) |
| `port` | `8080` | Port for the embedded HTTP server (only used if `mode` is `SELF`) |
| `host-ip` | `localhost` | Public IP or Hostname of the server (only used if `mode` is `SELF`) |
| `base-material` | `PAPER` | Base material for custom model data icons |
| `combine-external-rp` | `false` | Merge external .zip resource packs from the `external-resource-packs/` folder |
| `allow-minecraft-namespace` | `true` | Allow assets using the `minecraft` namespace |
| `allow-namespace-conflicts` | `false` | Allow multiple sources to provide assets in the same namespace |

## GUI Customization

```yaml
gui:
  main-title: "§8Accessory Slots"
  slot-title-prefix: "§8Slots: "
  filler-material: "GRAY_STAINED_GLASS_PANE"
  border-material: "BLACK_STAINED_GLASS_PANE"
  use-patterns: true
  filler-name: "&r"
```

| Setting | Default | Description |
|---|---|---|
| `main-title` | `§8Accessory Slots` | Title of the main (Tier 1) GUI |
| `slot-title-prefix` | `§8Slots: ` | Prefix for Tier 2 slot inventory titles |
| `filler-material` | `GRAY_STAINED_GLASS_PANE` | Material for filler items |
| `border-material` | `BLACK_STAINED_GLASS_PANE` | Material for border items |
| `use-patterns` | `true` | Use decorative border patterns |
| `filler-name` | `&r` | Display name for filler items |

!!! note "Layout Changes in v1.3.0"
    The `gui.main-slots` and `gui.main-layout` settings have been removed. The GUI size is now automatically calculated, and layout is managed via the `/curios editmenu` command or automatic placement.
