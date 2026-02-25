# Abilities API

This page shows how to programmatically create and manage abilities for custom accessories.

## Example 1: Creating a Buff Plugin

A complete plugin that creates a set of buff accessories:

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.data.AbilityData;
import org.bg52.curiospaper.data.ItemData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class BuffAccessoryPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

        // --- Speed Ring ---
        createBuffItem(api, "speed_ring", "&b&lRing of Swiftness",
            "GOLD_NUGGET", "ring",
            AbilityData.EffectType.POTION_EFFECT, "SPEED", 0, 100,
            "&7Grants the wearer incredible speed.");

        // --- Health Amulet (+4 max health = 2 extra hearts) ---
        createBuffItem(api, "health_amulet", "&c&lAmulet of Vitality",
            "NAUTILUS_SHELL", "necklace",
            AbilityData.EffectType.PLAYER_MODIFIER, "GENERIC_MAX_HEALTH", 4, 0,
            "&7Grants 2 extra hearts.");

        // --- Attack Gloves ---
        createBuffItem(api, "attack_gloves", "&4&lGloves of Fury",
            "LEATHER", "hands",
            AbilityData.EffectType.PLAYER_MODIFIER, "GENERIC_ATTACK_DAMAGE", 3, 0,
            "&7+3 attack damage.");

        getLogger().info("Buff accessories registered!");
    }

    private void createBuffItem(CuriosPaperAPI api, String id, String name,
            String material, String slotType,
            AbilityData.EffectType effectType, String effectName,
            int amplifier, int duration, String description) {

        // Don't overwrite if already exists
        if (api.getItemData(id) != null) return;

        ItemData item = api.createItem(this, id);
        item.setDisplayName(name);
        item.setMaterial(material);
        item.setSlotType(slotType);
        item.setLore(Arrays.asList(description));

        // Create the ability
        AbilityData ability = new AbilityData();
        ability.setTrigger(AbilityData.Trigger.WHILE_EQUIPPED);
        ability.setEffectType(effectType);
        ability.setEffectName(effectName);
        ability.setAmplifier(amplifier);
        ability.setDuration(duration);

        item.getAbilities().put("main_buff", ability);
        api.saveItemData(id);
    }
}
```

## Example 2: Multi-Ability Items

Items with multiple abilities triggered at different times:

```java
import org.bg52.curiospaper.data.AbilityData;
import org.bg52.curiospaper.data.ItemData;

public void createWarriorBelt(CuriosPaperAPI api) {
    ItemData belt = api.createItem(this, "warrior_belt");
    belt.setDisplayName("&e&lWarrior's Belt");
    belt.setMaterial("LEATHER");
    belt.setSlotType("belt");
    belt.setLore(Arrays.asList(
        "&7A battle-worn belt of a legendary warrior.",
        "&7Grants strength on equip and speed while worn.",
        "&7Applies weakness when removed."
    ));

    // Ability 1: Strength II for 30 seconds on equip
    AbilityData equipStrength = new AbilityData();
    equipStrength.setTrigger(AbilityData.Trigger.EQUIP);
    equipStrength.setEffectType(AbilityData.EffectType.POTION_EFFECT);
    equipStrength.setEffectName("INCREASE_DAMAGE");
    equipStrength.setAmplifier(1); // Strength II
    equipStrength.setDuration(600); // 30 seconds
    belt.getAbilities().put("equip_strength", equipStrength);

    // Ability 2: Speed I while equipped (continuous)
    AbilityData passiveSpeed = new AbilityData();
    passiveSpeed.setTrigger(AbilityData.Trigger.WHILE_EQUIPPED);
    passiveSpeed.setEffectType(AbilityData.EffectType.POTION_EFFECT);
    passiveSpeed.setEffectName("SPEED");
    passiveSpeed.setAmplifier(0); // Speed I
    passiveSpeed.setDuration(100);
    belt.getAbilities().put("passive_speed", passiveSpeed);

    // Ability 3: Weakness I for 20 seconds on de-equip
    AbilityData deequipWeakness = new AbilityData();
    deequipWeakness.setTrigger(AbilityData.Trigger.DE_EQUIP);
    deequipWeakness.setEffectType(AbilityData.EffectType.POTION_EFFECT);
    deequipWeakness.setEffectName("WEAKNESS");
    deequipWeakness.setAmplifier(0);
    deequipWeakness.setDuration(400); // 20 seconds
    belt.getAbilities().put("deequip_weakness", deequipWeakness);

    api.saveItemData("warrior_belt");
}
```

## Example 3: Stat Modifier Set

Create a full set of accessories that modify player attributes:

```java
public void createStatSet(CuriosPaperAPI api) {
    // Ring: +2 Luck
    createModifier(api, "lucky_ring", "&a&lRing of Fortune",
        "GOLD_NUGGET", "ring", "GENERIC_LUCK", 2);

    // Necklace: +4 Armor
    createModifier(api, "iron_amulet", "&7&lIron Amulet",
        "IRON_NUGGET", "necklace", "GENERIC_ARMOR", 4);

    // Bracelet: +0.5 Knockback Resistance
    createModifier(api, "steady_bracelet", "&9&lSteady Bracelet",
        "CHAIN", "bracelet", "GENERIC_KNOCKBACK_RESISTANCE", 1);

    // Charm: +0.04 Movement Speed (~20% faster)
    createModifier(api, "swift_charm", "&b&lSwift Charm",
        "EMERALD", "charm", "GENERIC_MOVEMENT_SPEED", 1);
}

private void createModifier(CuriosPaperAPI api, String id, String name,
        String material, String slotType, String attribute, int amplifier) {

    if (api.getItemData(id) != null) return;

    ItemData item = api.createItem(this, id);
    item.setDisplayName(name);
    item.setMaterial(material);
    item.setSlotType(slotType);

    AbilityData modifier = new AbilityData();
    modifier.setTrigger(AbilityData.Trigger.WHILE_EQUIPPED);
    modifier.setEffectType(AbilityData.EffectType.PLAYER_MODIFIER);
    modifier.setEffectName(attribute);
    modifier.setAmplifier(amplifier);
    modifier.setDuration(0);

    item.getAbilities().put("modifier", modifier);
    api.saveItemData(id);
}
```

## AbilityData Class Reference

### Trigger Enum

| Value | Description |
|---|---|
| `AbilityData.Trigger.EQUIP` | Fires once on equip |
| `AbilityData.Trigger.DE_EQUIP` | Fires once on de-equip |
| `AbilityData.Trigger.WHILE_EQUIPPED` | Fires continuously |

### EffectType Enum

| Value | Description |
|---|---|
| `AbilityData.EffectType.POTION_EFFECT` | Bukkit PotionEffect |
| `AbilityData.EffectType.PLAYER_MODIFIER` | Bukkit AttributeModifier |

### Methods

| Method | Type | Description |
|---|---|---|
| `getTrigger()` / `setTrigger(Trigger)` | `Trigger` | When the ability activates |
| `getEffectType()` / `setEffectType(EffectType)` | `EffectType` | What kind of effect |
| `getEffectName()` / `setEffectName(String)` | `String` | PotionEffectType or Attribute name |
| `getAmplifier()` / `setAmplifier(int)` | `int` | Effect strength (0-based) |
| `getDuration()` / `setDuration(int)` | `int` | Duration in ticks |
| `isValid()` | `boolean` | Validates the ability data |

### Validation

```java
AbilityData ability = new AbilityData();
ability.setTrigger(AbilityData.Trigger.WHILE_EQUIPPED);
ability.setEffectType(AbilityData.EffectType.POTION_EFFECT);
ability.setEffectName("SPEED");
ability.setAmplifier(0);
ability.setDuration(100);

if (ability.isValid()) {
    // Safe to add to item
    item.getAbilities().put("my_ability", ability);
} else {
    // Missing required fields — check trigger, effectType, effectName
}
```
