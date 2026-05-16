# Accessory Example

Complete examples of creating accessories using both YAML config and the Java API.

---

## Ring of Swiftness

A gold ring that grants Speed I while worn.

### YAML Config

Create `plugins/CuriosPaper/items/speed_ring.yml`:

```yaml
item-id: speed_ring
display-name: "&6&lRing of Swiftness"
material: GOLD_NUGGET
slot-type: ring
custom-model-data: 20001
item-model: "curiospaper:speed_ring"
lore:
  - "&7An ancient ring imbued with wind magic."
  - "&7Grants swiftness to its wearer."
  - ""
  - "&e&oBound to the Ring Slot"

abilities:
  speed_boost:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: SPEED
    amplifier: 0
    duration: 100

recipes:
  crafting:
    type: SHAPED
    shape:
      - " G "
      - "GFG"
      - " G "
    ingredients:
      G: GOLD_INGOT
      F: FEATHER

mob-drops:
  skeleton_drop:
    entity-type: SKELETON
    chance: 0.02
    min-amount: 1
    max-amount: 1
```

### Java API Equivalent

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.data.*;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class SpeedRingPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

        // Skip if already exists
        if (api.getItemData("speed_ring") != null) return;

        // Create the item
        ItemData ring = api.createItem(this, "speed_ring");
        ring.setDisplayName("&6&lRing of Swiftness");
        ring.setMaterial("GOLD_NUGGET");
        ring.setSlotType("ring");
        ring.setCustomModelData(20001);
        ring.setItemModel("curiospaper:speed_ring");
        ring.setLore(Arrays.asList(
            "&7An ancient ring imbued with wind magic.",
            "&7Grants swiftness to its wearer.",
            "",
            "&e&oBound to the Ring Slot"
        ));

        // Add speed ability
        AbilityData speed = new AbilityData();
        speed.setTrigger(AbilityData.Trigger.WHILE_EQUIPPED);
        speed.setEffectType(AbilityData.EffectType.POTION_EFFECT);
        speed.setEffectName("SPEED");
        speed.setAmplifier(0);
        speed.setDuration(100);
        ring.getAbilities().put("speed_boost", speed);

        // Add mob drop from skeletons
        MobDropData skeletonDrop = new MobDropData();
        skeletonDrop.setEntityType(EntityType.SKELETON.name());
        skeletonDrop.setChance(0.02);
        skeletonDrop.setMinAmount(1);
        skeletonDrop.setMaxAmount(1);
        api.registerItemMobDrop("speed_ring", skeletonDrop);

        // Save
        api.saveItemData("speed_ring");
    }
}
```

### In-Game Commands

```
/curios create speed_ring
/curios give speed_ring
/curios give speed_ring Steve 2
```

---

## Guardian's Amulet

A necklace that provides damage resistance and extra health.

### YAML Config

```yaml
item-id: guardian_amulet
display-name: "&b&lGuardian's Amulet"
material: NAUTILUS_SHELL
slot-type: necklace
custom-model-data: 20002
lore:
  - "&7A protective amulet from the deep."
  - "&7Shields its wearer from harm."

abilities:
  damage_resist:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: DAMAGE_RESISTANCE
    amplifier: 0
    duration: 200
  health_boost:
    trigger: WHILE_EQUIPPED
    effect-type: PLAYER_MODIFIER
    effect-name: GENERIC_MAX_HEALTH
    amplifier: 4
    duration: 0

recipes:
  crafting:
    type: SHAPED
    shape:
      - "PSP"
      - "SHS"
      - "PSP"
    ingredients:
      P: PRISMARINE_SHARD
      S: SCUTE
      H: HEART_OF_THE_SEA
```

### Java API Equivalent

```java
public void createGuardianAmulet(CuriosPaperAPI api) {
    if (api.getItemData("guardian_amulet") != null) return;

    ItemData amulet = api.createItem(this, "guardian_amulet");
    amulet.setDisplayName("&b&lGuardian's Amulet");
    amulet.setMaterial("NAUTILUS_SHELL");
    amulet.setSlotType("necklace");
    amulet.setCustomModelData(20002);
    amulet.setLore(Arrays.asList(
        "&7A protective amulet from the deep.",
        "&7Shields its wearer from harm."
    ));

    // Damage Resistance
    AbilityData resist = new AbilityData();
    resist.setTrigger(AbilityData.Trigger.WHILE_EQUIPPED);
    resist.setEffectType(AbilityData.EffectType.POTION_EFFECT);
    resist.setEffectName("DAMAGE_RESISTANCE");
    resist.setAmplifier(0);
    resist.setDuration(200);
    amulet.getAbilities().put("damage_resist", resist);

    // +4 Max Health (2 extra hearts)
    AbilityData health = new AbilityData();
    health.setTrigger(AbilityData.Trigger.WHILE_EQUIPPED);
    health.setEffectType(AbilityData.EffectType.PLAYER_MODIFIER);
    health.setEffectName("GENERIC_MAX_HEALTH");
    health.setAmplifier(4);
    health.setDuration(0);
    amulet.getAbilities().put("health_boost", health);

    api.saveItemData("guardian_amulet");
}
```

---

## Night Vision Goggles

A head accessory that grants permanent night vision.

### YAML Config

```yaml
item-id: night_goggles
display-name: "&a&lNight Vision Goggles"
material: GOLDEN_HELMET
slot-type: head
custom-model-data: 20003
lore:
  - "&7Advanced goggles that enhance vision."
  - "&7See clearly in the darkest caves."

abilities:
  night_sight:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: NIGHT_VISION
    amplifier: 0
    duration: 400

recipes:
  crafting:
    type: SHAPED
    shape:
      - "GIG"
      - "GRG"
      - "   "
    ingredients:
      G: GOLD_INGOT
      I: ENDER_EYE
      R: REDSTONE
```

### Java API Equivalent

```java
public void createNightGoggles(CuriosPaperAPI api) {
    if (api.getItemData("night_goggles") != null) return;

    ItemData goggles = api.createItem(this, "night_goggles");
    goggles.setDisplayName("&a&lNight Vision Goggles");
    goggles.setMaterial("GOLDEN_HELMET");
    goggles.setSlotType("head");
    goggles.setCustomModelData(20003);
    goggles.setLore(Arrays.asList(
        "&7Advanced goggles that enhance vision.",
        "&7See clearly in the darkest caves."
    ));

    AbilityData nightVision = new AbilityData();
    nightVision.setTrigger(AbilityData.Trigger.WHILE_EQUIPPED);
    nightVision.setEffectType(AbilityData.EffectType.POTION_EFFECT);
    nightVision.setEffectName("NIGHT_VISION");
    nightVision.setAmplifier(0);
    nightVision.setDuration(400);
    goggles.getAbilities().put("night_sight", nightVision);

    api.saveItemData("night_goggles");
}
```
