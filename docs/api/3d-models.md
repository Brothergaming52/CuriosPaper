# 3D Models API

The CuriosPaper API provides methods for configuring 3D model attachments on custom items and mob drops.

## Item Model Configuration

Configure how a 3D model appears on the player's body when an accessory is equipped.

### setItemModelConfig

```java
boolean setItemModelConfig(
    String itemId,          // Item to configure
    boolean modelEnabled,   // Enable/disable the 3D model
    String modelItem,       // Material name (e.g., "LEATHER_HORSE_ARMOR")
    Integer customModelData,// CustomModelData for older versions (nullable)
    String itemModel,       // Item model component for 1.21.4+ (nullable)
    Float pitchUpLimit,     // Hide when looking up beyond this angle (nullable)
    Float pitchDownLimit    // Hide when looking down beyond this angle (nullable)
);
```

### Example: Enable 3D Model on an Item

```java
CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

// Enable a 3D model for the "royal_cape" item
api.setItemModelConfig(
    "royal_cape",
    true,                      // enabled
    "LEATHER_HORSE_ARMOR",     // model material
    null,                      // no CustomModelData
    "myplugin:royal_cape_3d",  // item model component (1.21.4+)
    45.0f,                     // hide when looking up >45°
    30.0f                      // hide when looking down >30°
);
```

### Example: Disable 3D Model

```java
// Disable 3D model for an item
api.setItemModelConfig(
    "royal_cape",
    false,  // disabled
    null, null, null, null, null
);
```

### Example: Model with CustomModelData (Pre-1.21.4)

```java
api.setItemModelConfig(
    "spirit_crown",
    true,
    "DIAMOND_HELMET",
    50001,  // CustomModelData integer
    null,   // no item model component
    60.0f,  // pitch up limit
    40.0f   // pitch down limit
);
```

---

## Mob Drop Model Configuration

Configure 3D models that mobs wear when they spawn with a custom drop item.

### setMobDropModelConfig

```java
boolean setMobDropModelConfig(
    String itemId,          // Item ID
    String entityType,      // Entity type string (e.g., "ZOMBIE")
    boolean modelEnabled,   // Enable/disable the model
    String modelItem,       // Material name for the model
    Integer customModelData,// CustomModelData (nullable)
    String itemModel        // Item model component (nullable)
);
```

### Example: Zombie Wearing a Crown

```java
// Make zombies that can drop "cursed_crown" wear it visually
api.setMobDropModelConfig(
    "cursed_crown",
    "ZOMBIE",
    true,
    "GOLDEN_HELMET",
    null,
    "myplugin:cursed_crown_3d"
);
```

### Example: Skeleton with Custom Necklace

```java
api.setMobDropModelConfig(
    "bone_amulet",
    "SKELETON",
    true,
    "LEATHER_HORSE_ARMOR",
    30005,  // CustomModelData
    null
);
```

---

## ItemData Model Fields

When working with `ItemData` directly, you can access the model fields:

```java
ItemData item = api.getItemData("my_item");

// Get/set model properties
item.setModelEnabled(true);
item.setModelItem("LEATHER_HORSE_ARMOR");
item.setModelCustomModelData(50001);
item.setModelItemModel("myplugin:my_model");
item.setPitchUpLimit(45.0f);
item.setPitchDownLimit(30.0f);

// Save changes
api.saveItemData("my_item");
```

### Model Properties

| Property | Type | Description |
|---|---|---|
| `modelEnabled` | `boolean` | Whether the 3D model is active |
| `modelItem` | `String` | Material name for the model entity's helmet |
| `modelCustomModelData` | `Integer` | CustomModelData value (nullable, for pre-1.21.4) |
| `modelItemModel` | `String` | Item model component key (nullable, for 1.21.4+) |
| `pitchUpLimit` | `Float` | Upward pitch angle to hide model from self (nullable) |
| `pitchDownLimit` | `Float` | Downward pitch angle to hide model from self (nullable) |

---

## YAML Configuration

Model settings are also stored in item YAML files:

```yaml
# plugins/CuriosPaper/items/royal_cape.yml
item-id: royal_cape
display-name: "&5Royal Cape"
material: PAPER
slot-type: back

# 3D Model Settings
model-enabled: true
model-item: "LEATHER_HORSE_ARMOR"
model-custom-model-data: null
model-item-model: "myplugin:royal_cape_3d"
pitch-up-limit: 45.0
pitch-down-limit: 30.0
```

See the [3D Model System](../systems/3d-model-system.md) for more details on how models work, and the [3D Model Editor](../gui-editors/3d-model-editor.md) for the in-game GUI.
