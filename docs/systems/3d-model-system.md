# 3D Model System

The 3D Model System allows you to attach custom 3D models directly to the player's body when they equip specific Curios items.

## How It Works

Rather than only showing custom items in the hand or inventory, CuriosPaper uses synchronized Armor Stands to render custom models on the player's body.

```
┌──────────────────┐     ┌──────────────┐     ┌─────────────────┐
│ Player Equips    │────▶│ Model Stand  │────▶│ Renders Model   │
│ Curios Item      │     │ Manager      │     │ on Player       │
└──────────────────┘     └──────────────┘     └─────────────────┘
                                │
                                ▼
                         ┌──────────────┐
                         │ Rotation &   │
                         │ Visibility   │
                         │ Sync (Tick)  │
                         └──────────────┘
```

### Entity Architecture
When an item with a 3D model configuration is placed in an accessory slot, CuriosPaper dynamically spawns an invisible marker Armor Stand that rides the player as a passenger. 
- The armor stand wears the defined model item (e.g., `LEATHER_HORSE_ARMOR` combined with a specific `itemModel` component in 1.21.4+, or `CustomModelData` in older versions).
- The stand tracks the player's rotation seamlessly via a tick task.
- For items in a `head:` slot, the armor stand head strictly synchronizes to the player's independent head yaw left-to-right (specifically maintaining a 0-pitch angle so it aligns perfectly with horizontal rotations). Other slots cleanly lock into both full body yaw and overall pitch.
- When the accessory is unequipped or the player logs out, the stand is safely destroyed.

## Visibility Culling

To prevent the armor stand from obscuring the player's vision when in first-person view, CuriosPaper implements a dynamic visibility culling system.

### Pitch Limits
You can configure downward and upward pitch cutoffs. When the player looks too far down (or up), the model is hidden from their own view by temporarily unequipping the item from the Armor Stand's helmet slot. It remains visible to other players.

### Per-Item Toggles
Players have control over whether their equipped models are visible:
- By **Right-Clicking** an equipped accessory in the `/baubles` menu, players can toggle its global 3D model visibility. 
- This preference is saved directly to the item's PersistentDataContainer (`curios_model_hidden`), meaning it persists even if the item is dropped or traded.

## Configuring Models
Models can be configured in two ways:
1. **In-game GUI**: Using the [3D Model Editor](../gui-editors/3d-model-editor.md) via `/edit gui <item>`.
2. **API**: Using the `CuriosPaperAPI#setItemModelConfig` method.
