# NBT & Enchants Editor

The **NBT & Enchants Editor** allows you to attach custom PersistentDataContainer (PDC/NBT) tags, manage enchantments, toggle the item's Unbreakable flag, and control whether the custom item is Placeable as a block.

## Accessing the Editor

From the main Custom Item Editor (`/curios edit <item>`), click the **NBT & Enchants Editor** button located at slot 21 (Command Block icon).

---

## 1. Main Menu

The main screen of the editor offers four main controls:

| Control | Icon | Slot | Description |
|---|---|---|---|
| **Manage NBT (PDC Keys)** | Name Tag | 10 | View, add, or delete custom NBT keys stored on the item. |
| **Manage Enchantments** | Enchanted Book | 12 | Add or remove enchantments and toggle tooltips. |
| **Unbreakable Toggle** | Anvil | 14 | Toggle whether the accessory has infinite durability (`true`/`false`). |
| **Placeable Toggle** | Grass Block | 16 | If set to `FALSE`, players are prevented from placing the custom block or head on the ground. |
| **Back to Editor** | Oak Door | 22 | Return to the main custom item editor. |

---

## 2. NBT Tags (PDC) Manager

PDC (Persistent Data Container) keys allow plugins to store persistent data on items. This screen lists all current custom NBT tags.

### Modifying Tags
- **Delete Tag:** Click on any existing tag paper icon to instantly remove it.
- **Add Custom Tag:** Click the **Add Custom NBT Tag** button (Lime Dye, slot 45) to input a tag via chat.
- **Add Existing Tag:** Click the **Add Existing Key** button (Knowledge Book, slot 46) to pick from a list of suggested keys.

### Syntax for Chat Input
When adding a custom tag, type the tag in chat using the syntax:
```text
key = type:value
```
- **Key:** Must contain a namespace prefix, e.g., `myplugin:power` or `custom:rarity`.
- **Supported Types:** `string`, `int`, `double`, `float`, `byte`, `short`, `long`.

**Examples:**
- `myplugin:power = int:42`
- `minecraft:custom_model_data = int:100`
- `curios:attribute_modifier = string:speed`

### NBT Key Suggester
If you choose **Add Existing Key**, a paginated selector screen displays:
- Standard Minecraft keys (e.g. `minecraft:damage`, `minecraft:repair_cost`, `minecraft:dyed_color`).
- Keys scanned from all other registered custom items on the server.
- Keys scanned from items currently in your own player inventory.

After selecting a suggested key, type the value in chat in the format `type:value` (e.g. `string:rare` or `int:1`).

---

## 3. Enchantments Manager

The Enchantments screen displays a list of all active enchantments on the item.

### Modifying Enchantments
- **Delete Enchantment:** Click on any active enchanted book icon to remove it from the item.
- **Add Enchantment:** Click the **Add Enchantment** button (Lime Dye, slot 45) to open a paginated list of all enchantments registered on the server. Select one and enter the desired level in chat (e.g. `5` for Level V).
- **Toggle Glint Only:** Click the **Hide Enchantments** button (Feather, slot 46).
  - **TRUE (Glint only):** Hides the enchantment names and levels from the item's tooltips/lore, but retains the shiny enchantment glint.
  - **FALSE:** Displays the standard enchantment tooltips.
