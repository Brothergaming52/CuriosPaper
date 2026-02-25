# Ability Example

Examples of different ability configurations for accessories.

## Potion Effect Abilities

### Speed Ring

Speed I while equipped:

```yaml
abilities:
  speed:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: SPEED
    amplifier: 0
    duration: 100
```

### Berserker Gloves

Strength II while equipped:

```yaml
abilities:
  strength:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: INCREASE_DAMAGE
    amplifier: 1
    duration: 200
```

### Healing Touch

Regeneration I while equipped:

```yaml
abilities:
  regen:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: REGENERATION
    amplifier: 0
    duration: 100
```

### Fire Ward Charm

Fire resistance while equipped:

```yaml
abilities:
  fire_resist:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: FIRE_RESISTANCE
    amplifier: 0
    duration: 300
```

## Attribute Modifier Abilities

### Health Amulet

+4 max health (2 hearts) while equipped:

```yaml
abilities:
  extra_health:
    trigger: WHILE_EQUIPPED
    effect-type: PLAYER_MODIFIER
    effect-name: GENERIC_MAX_HEALTH
    amplifier: 4
    duration: 0
```

### Attack Ring

+3 attack damage while equipped:

```yaml
abilities:
  attack_boost:
    trigger: WHILE_EQUIPPED
    effect-type: PLAYER_MODIFIER
    effect-name: GENERIC_ATTACK_DAMAGE
    amplifier: 3
    duration: 0
```

### Lucky Charm

+2 luck while equipped:

```yaml
abilities:
  luck:
    trigger: WHILE_EQUIPPED
    effect-type: PLAYER_MODIFIER
    effect-name: GENERIC_LUCK
    amplifier: 2
    duration: 0
```

## Multi-Trigger Abilities

### Warrior's Belt

Strength on equip, slowness removal on equip, speed while equipped:

```yaml
abilities:
  equip_strength:
    trigger: EQUIP
    effect-type: POTION_EFFECT
    effect-name: INCREASE_DAMAGE
    amplifier: 2
    duration: 600
  passive_speed:
    trigger: WHILE_EQUIPPED
    effect-type: POTION_EFFECT
    effect-name: SPEED
    amplifier: 0
    duration: 100
```

### Draining Bracelet

Grants absorption on equip, applies weakness on de-equip:

```yaml
abilities:
  absorb_on_equip:
    trigger: EQUIP
    effect-type: POTION_EFFECT
    effect-name: ABSORPTION
    amplifier: 1
    duration: 1200
  weakness_on_remove:
    trigger: DE_EQUIP
    effect-type: POTION_EFFECT
    effect-name: WEAKNESS
    amplifier: 0
    duration: 400
```
