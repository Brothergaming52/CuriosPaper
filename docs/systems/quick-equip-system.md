# Quick Equip System

The Quick Equip System allows players to instantly equip accessory items without opening the GUI.

## How It Works

When a player holds a tagged accessory item and performs **Shift + Right-Click**, CuriosPaper automatically equips the item into the first available slot matching the item's slot type tag.

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────┐
│ Player holds     │────▶│ QuickEquip       │────▶│ Item placed  │
│ tagged item +    │     │ Listener checks  │     │ in first     │
│ Shift+Right-Click│     │ slot tags        │     │ empty slot   │
└──────────────────┘     └──────────────────┘     └──────────────┘
```

## Usage

1. Hold a tagged accessory item in your main hand
2. Hold **Shift** (Sneak)
3. **Right-Click** (air or block)
4. The item is equipped into the first available slot

## Features

- **Multi-Slot Support:** Items tagged for multiple slot types (e.g., `ring, charm`) will try each slot type until an empty slot is found
- **Sound Feedback:** Plays the configured equip sound on success
- **Chat Feedback:** Sends a confirmation message showing which slot the item was equipped to
- **Interact Prevention:** Cancels the right-click event so you don't accidentally place blocks or interact with entities
- **Event Firing:** Fires an `AccessoryEquipEvent` so other plugins can react to the equip action

## Behavior

| Scenario | Result |
|---|---|
| Tagged item, empty slot available | ✅ Item equipped, one removed from hand stack |
| Tagged item, all slots full | ❌ "No empty slots available!" message |
| Untagged item | ❌ Nothing happens (event passes through) |
| Empty hand | ❌ Nothing happens |
| Not sneaking | ❌ Nothing happens |
| Off-hand click | ❌ Ignored (only main hand triggers) |

## Configuration

Quick Equip is always enabled and has no separate config toggle. It uses the standard equip sound settings:

```yaml
features:
  play-equip-sound: true
  equip-sound: "ENTITY_ITEM_PICKUP"
```

