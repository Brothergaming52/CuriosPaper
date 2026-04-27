# Hotkey System

The Hotkey System allows players to open the accessory GUI without typing a command, using a configurable sneak-based keybind.

## How It Works

When the hotkey feature is enabled, players can open the accessory GUI by sneaking while having a specific hotbar slot selected. The sneak detection mode is configurable.

## Configuration

```yaml
features:
  hotkey:
    enabled: true
    slot: 8                    # Which hotbar slot must be selected (1-9)
    sneak-type: "double"       # Detection mode: single, double, or hold
    sneak-hold-duration: 3     # Seconds to hold sneak (only for 'hold' type)
```

### Settings

| Setting | Type | Default | Description |
|---|---|---|---|
| `enabled` | Boolean | `true` | Enable/disable the hotkey system |
| `slot` | Integer | `8` | Which hotbar slot (1–9) must be selected for the hotkey to trigger |
| `sneak-type` | String | `double` | Sneak detection mode (see below) |
| `sneak-hold-duration` | Integer | `3` | Seconds the player must hold sneak for `hold` mode |

## Sneak Detection Modes

### `single` — Single Sneak

The accessory GUI opens immediately the moment the player presses sneak.

- **Pros:** Fastest to trigger
- **Cons:** May conflict with normal sneaking gameplay

### `double` — Double Sneak

The accessory GUI opens when the player presses sneak twice within 500 milliseconds.

- **Pros:** Low chance of accidental triggers
- **Cons:** Slight delay before opening

### `hold` — Hold Sneak

The accessory GUI opens when the player holds sneak for the configured duration (e.g., 3 seconds).

- **Pros:** No accidental triggers
- **Cons:** Slowest to trigger

## Requirements

For the hotkey to work, **all** of these conditions must be met:

1. `features.hotkey.enabled` is `true`
2. The player has the configured hotbar slot selected
3. The player performs the configured sneak action

## Examples

### Default Setup (Double Sneak on Slot 8)

```yaml
features:
  hotkey:
    enabled: true
    slot: 8
    sneak-type: "double"
```

**Usage:** Select hotbar slot 8, then quickly double-tap sneak.

### RPG-Style Setup (Hold Sneak on Slot 9)

```yaml
features:
  hotkey:
    enabled: true
    slot: 9
    sneak-type: "hold"
    sneak-hold-duration: 2
```

**Usage:** Select hotbar slot 9, then hold sneak for 2 seconds.

### Quick Access (Single Sneak on Slot 1)

```yaml
features:
  hotkey:
    enabled: true
    slot: 1
    sneak-type: "single"
```

**Usage:** Select hotbar slot 1, then tap sneak once.

