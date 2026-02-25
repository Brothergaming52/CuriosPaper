# Abilities Configuration

Abilities are effects attached to custom items that trigger when an accessory is equipped, de-equipped, or while it remains equipped.

## Ability Properties

| Property | Type | Values | Description |
|---|---|---|---|
| `trigger` | Enum | `EQUIP`, `DE_EQUIP`, `WHILE_EQUIPPED` | When the ability activates |
| `effect-type` | Enum | `POTION_EFFECT`, `PLAYER_MODIFIER` | Type of effect to apply |
| `effect-name` | String | See tables below | Name of the effect or attribute |
| `amplifier` | Integer | `0` – `9` | Effect level (0 = Level I, 1 = Level II, etc.) |
| `duration` | Integer | `0+` (ticks) | Duration in ticks (20 ticks = 1 second) |

## Triggers

| Trigger | Description | Use Case |
|---|---|---|
| `EQUIP` | Fires once when the item is placed into a slot | One-time burst effects |
| `DE_EQUIP` | Fires once when the item is removed from a slot | Cleanup or removal effects |
| `WHILE_EQUIPPED` | Fires continuously while the item is in a slot | Persistent buffs |

## Effect Types

### Potion Effects

Use `effect-type: POTION_EFFECT` with `effect-name` set to any valid Bukkit `PotionEffectType`:

| Effect Name | Description |
|---|---|
| `SPEED` | Movement speed boost |
| `SLOW` | Movement speed reduction |
| `FAST_DIGGING` | Haste |
| `SLOW_DIGGING` | Mining fatigue |
| `INCREASE_DAMAGE` | Strength |
| `HEAL` | Instant health |
| `REGENERATION` | Health regeneration |
| `DAMAGE_RESISTANCE` | Damage reduction |
| `FIRE_RESISTANCE` | Fire immunity |
| `WATER_BREATHING` | Underwater breathing |
| `INVISIBILITY` | Invisibility |
| `NIGHT_VISION` | Night vision |
| `JUMP` | Jump boost |
| `ABSORPTION` | Absorption hearts |

### Player Modifiers

Use `effect-type: PLAYER_MODIFIER` with `effect-name` set to a Bukkit `Attribute`:

| Attribute | Description |
|---|---|
| `GENERIC_MAX_HEALTH` | Maximum health |
| `GENERIC_ATTACK_DAMAGE` | Melee damage |
| `GENERIC_ATTACK_SPEED` | Attack speed |
| `GENERIC_ARMOR` | Armor points |
| `GENERIC_ARMOR_TOUGHNESS` | Armor toughness |
| `GENERIC_MOVEMENT_SPEED` | Movement speed |
| `GENERIC_KNOCKBACK_RESISTANCE` | Knockback resistance |
| `GENERIC_LUCK` | Luck |

## Example Configurations

### Speed Ring (WHILE_EQUIPPED)

```yaml
abilities:
  speed_boost:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: SPEED
    amplifier: 0
    duration: 100
```

### Health Amulet (EQUIP + DE_EQUIP)

An amulet that grants extra hearts when worn:

```yaml
abilities:
  health_boost:
    trigger: WHILE_EQUIPPED
    effect-type: PLAYER_MODIFIER
    effect-name: GENERIC_MAX_HEALTH
    amplifier: 4
    duration: 0
```

### Night Vision Goggles

```yaml
abilities:
  night_sight:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: NIGHT_VISION
    amplifier: 0
    duration: 400
```

!!! tip "Duration for WHILE_EQUIPPED"
    For `WHILE_EQUIPPED` triggers, set a duration longer than the refresh interval to avoid flickering. A value of `100` ticks (5 seconds) works well.
