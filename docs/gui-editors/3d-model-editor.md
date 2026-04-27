# 3D Model Editor

The 3D Model Editor allows you to configure physical, body-mounted 3D models for custom items. These models appear on the player when the item is equipped in an accessory slot.

## Accessing the Editor

From the main Custom Item Editor (`/edit gui <item>`), click the **3D Model Settings** button located at slot 43 (Armor Stand icon).

A live preview of your configured 3D model appears directly in the very top slot (Slot 4) if you're running Minecraft 1.21.4+.

## Available Settings

The GUI provides options to tweak exactly how and when the model renders:

| Button | Function | Description |
|---|---|---|
| **Toggle Model** | Enable/Disable | Turns the 3D model feature on or off entirely for this item. |
| **Material** | Set Material | The base material of the model item (e.g., `LEATHER_HORSE_ARMOR`, `DIAMOND_HELMET`). |
| **Model Data** | Set CustomModelData | The integer value for CustomModelData matching your resource pack (Minecraft 1.21.2 and below). |
| **Item Model** | Set Item Model | Set a Minecraft 1.21.4+ `itemModel` component formatted as `namespace:key`. |
| **Pitch Down** | Set Down Limit | The downward angle threshold (e.g., `30.0`). If the player looks down past this angle, the model hides so it doesn't block their view. |
| **Pitch Up** | Set Up Limit | The upward angle threshold. Hides the model if the player looks too far up. |

## Player Toggles

While you configure the default layout here, players have the ultimate choice on visibility. By opening their Accessory GUI (`/baubles`) and **Right-Clicking** an equipped accessory, they can toggle the 3D model on and off for their specific instance of the item.
