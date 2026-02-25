# Custom Model Data & Item Models

CuriosPaper supports two methods for applying custom textures to items, depending on your Minecraft version.

## Version Compatibility

| Method | Minecraft Version | Config Key | Type |
|---|---|---|---|
| CustomModelData | 1.14 – 1.21.2 | `custom-model-data` | Integer |
| Item Model | 1.21.3+ | `item-model` | NamespacedKey |

!!! tip "Auto-Detection"
    CuriosPaper automatically detects your server version and uses the appropriate method. You can define both in your configs for cross-version compatibility.

## CustomModelData (1.14 – 1.21.2)

### How It Works

1. Each slot type and custom item has a `custom-model-data` integer
2. The resource pack contains model override JSON files
3. When the item is displayed, Minecraft uses the matching custom model

### Setting CustomModelData

In `config.yml` for slot icons:

```yaml
slots:
  ring:
    custom-model-data: 10008
```

In item YAML files:

```yaml
custom-model-data: 20001
```

### Resource Pack Structure

For a `PAPER` base material with CustomModelData:

```
assets/minecraft/models/item/paper.json
```

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "item/paper"
  },
  "overrides": [
    { "predicate": { "custom_model_data": 10008 }, "model": "curiospaper/item/ring_slot" }
  ]
}
```

And the custom model at:

```
assets/curiospaper/models/item/ring_slot.json
assets/curiospaper/textures/item/ring_slot.png
```

## Item Model (1.21.3+)

### How It Works

1. Each item has an `item-model` NamespacedKey (e.g., `curiospaper:ring_slot`)
2. Minecraft resolves the model directly from the namespace
3. No override JSON needed — cleaner and more efficient

### Setting Item Model

In `config.yml` for slot icons:

```yaml
slots:
  ring:
    item-model: "curiospaper:ring_slot"
```

In item YAML files:

```yaml
item-model: "myplugin:my_ring"
```

### Resource Pack Structure

```
assets/curiospaper/models/item/ring_slot.json
assets/curiospaper/textures/item/ring_slot.png
```

Model JSON (`ring_slot.json`):

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "curiospaper:item/ring_slot"
  }
}
```

## Base Material

The `base-material` setting in `config.yml` determines which vanilla item the custom models are based on:

```yaml
resource-pack:
  base-material: "PAPER"
```

Common choices:

| Material | Pros | Cons |
|---|---|---|
| `PAPER` | Simple, stackable | May conflict with other plugins |
| `LEATHER_HORSE_ARMOR` | Unique, rarely used | Not stackable |

## Creating Custom Textures

1. Create a 16×16 or 32×32 PNG texture
2. Place it in the appropriate `textures/` directory
3. Create a model JSON that references the texture
4. Set the `custom-model-data` or `item-model` in your config
5. Run `/curios rp rebuild` to regenerate the pack
