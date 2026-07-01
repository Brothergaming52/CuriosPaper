# Elytra Back Slot

CuriosPaper allows players to equip an **Elytra in the back accessory slot**, freeing up the chestplate armor slot for full-time wear.

<!-- TODO: Add image - In-game screenshot showing an Elytra equipped in the back slot of the accessory GUI, with the player wearing a chestplate in their normal armor slot -->
![Elytra in back accessory slot with chestplate in armor slot](../images/elytra-back-slot.png)

CuriosPaper allows players to equip an Elytra in the **back** accessory slot, enabling flight while wearing a chestplate. This feature requires **Minecraft 1.21+** with **Paper** (uses DataComponents API). The `GLIDER` component is a `NonValued` data component type, meaning it acts as a marker — its presence on an item enables gliding.

## Enabling

In `config.yml`:

```yaml
features:
  allow-elytra-on-back-slot: true
```

!!! warning "Version Requirement"
    This feature only works on Minecraft 1.21.3+ with Paper. On older versions, a warning is logged and the feature is disabled automatically:
    ```
    Elytra back slot feature requires Minecraft 1.21.3+ with Paper.
    ```

## How It Works

When an Elytra is placed in the back accessory slot:

### With Chestplate Equipped

1. The **GLIDER** component is added to the player's chestplate
2. The player can fly using the chestplate (it gains elytra gliding)
3. Durability damage is redirected from the chestplate to the back-slot Elytra
4. Custom wing textures are applied based on chestplate type

### Without Chestplate

1. A **secret invisible Elytra** is equipped in the chest armor slot
2. The Elytra has an invisible item model (not visible in inventory)
3. Wings are still visible in third-person view
4. The secret Elytra is protected from being picked up or moved

### Custom Elytras & Flight Assets

CuriosPaper supports custom Elytra items equipped in the back slot that possess a custom equippable asset ID.

1. **Asset Mapping:** If the back-slot Elytra has a custom equippable asset ID (i.e. not vanilla `minecraft:elytra`), CuriosPaper resolves a combined asset key: `elytra_<chestplate_material>_<sanitized_wings_asset_key>`.
2. **Resource Pack Integration:** During the resource pack build, CuriosPaper scans registered equipment models. For any custom wings texture detected, it automatically compiles a combined humanoid JSON model template for each chestplate material (leather, chainmail, iron, gold, diamond, netherite) merged with the custom wings layer.
3. **Dynamic Flight Application:** If a player is wearing a chestplate and a custom Elytra, CuriosPaper checks if the combined asset model JSON exists on disk. If so, it dynamically applies the custom wings flight metadata using the combined asset ID. If no combined model file is found, it falls back to the vanilla wings asset for that chestplate material.
4. **Secret Elytra Visuals:** If the player is not wearing a chestplate, the secret invisible Elytra equipped in their chest armor slot has the custom flight wings asset applied directly, displaying the correct custom model in third-person.

## Automatic Tagging

All Elytra items are automatically tagged for the back slot:

- When a player picks up an Elytra (`EntityPickupItemEvent`)
- When scanning player inventories on join

The tag adds:

- PDC: `curiospaper:slot_type → "back"`
- Lore: `§7Required Slot: <Back Slot Name>`

## Chestplate Integration

The handler dynamically manages the chestplate's `GLIDER` component:

| Event | Action |
|---|---|
| Elytra equipped in back slot | Add GLIDER to chestplate |
| Elytra removed from back slot | Remove GLIDER from chestplate |
| Chestplate equipped (with elytra in back) | Add GLIDER to new chestplate |
| Chestplate removed (with elytra in back) | Equip secret invisible elytra |

## Durability Management

Durability damage from flight is redirected to the actual Elytra in the back slot:

- Chestplate/secret Elytra damage events are intercepted
- Damage is applied to the back-slot Elytra instead
- The Elytra uses standard vanilla durability rules

## Death Handling

On player death:

- Secret invisible Elytra items are removed from death drops
- The GLIDER component is cleaned from chestplate drops
- The back-slot Elytra is handled normally as an accessory
