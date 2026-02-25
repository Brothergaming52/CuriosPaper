# Ability System

The Ability System allows custom accessories to grant potion effects and attribute modifiers when equipped.

## Overview

Abilities are defined per-item and consist of:

- **Trigger** — When the ability activates
- **Effect Type** — What kind of effect to apply
- **Effect Name** — The specific effect or attribute
- **Amplifier** — Effect strength (0–9)
- **Duration** — How long the effect lasts (in ticks)

## Triggers

| Trigger | Behavior |
|---|---|
| `EQUIP` | Fires **once** when the item is placed into a slot |
| `DE_EQUIP` | Fires **once** when the item is removed from a slot |
| `WHILE_EQUIPPED` | Fires **repeatedly** while the item remains in a slot |

## Effect Types

### Potion Effects (`POTION_EFFECT`)

Applies a Bukkit `PotionEffect` to the player. The effect is applied with:

- The specified amplifier (0 = Level I)
- The specified duration in ticks
- `ambient: true` — Ambient particles
- `particles: false` — Hides particles (less visual noise)

For `WHILE_EQUIPPED`, the effect is re-applied periodically to maintain the buff.

### Player Modifiers (`PLAYER_MODIFIER`)

Modifies a player's attribute (e.g., max health, attack damage) using Bukkit's `AttributeModifier` system.

- Applied on equip, removed on de-equip
- The amplifier value is used as the modifier amount
- Uses `AttributeModifier.Operation.ADD_NUMBER` by default

## Ability Listener

The `AbilityListener` handles ability application:

1. Listens for `AccessoryEquipEvent` events
2. On `EQUIP`: Applies `EQUIP` and starts `WHILE_EQUIPPED` abilities
3. On `UNEQUIP`: Applies `DE_EQUIP` and stops `WHILE_EQUIPPED` abilities
4. On `SWAP`: Removes old abilities, applies new ones

## Data Format

Abilities are stored in item YAML files:

```yaml
abilities:
  speed_boost:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: SPEED
    amplifier: 1
    duration: 200
  strength_on_equip:
    trigger: EQUIP
    effect-type: POTION_EFFECT
    effect-name: INCREASE_DAMAGE
    amplifier: 0
    duration: 600
```

## Validation

An ability is considered valid when:

- `trigger` is not null
- `effectType` is not null
- `effectName` is not null and not empty
- `amplifier` is between 0 and 9

Invalid abilities are silently skipped during loading.

## Multiple Abilities

Items can have **multiple abilities** with different triggers. For example, a ring could:

- Grant Speed I while equipped
- Apply Regeneration II for 10 seconds on equip
- Remove a weakness debuff on de-equip
