# Mob Drop System

The Mob Drop System allows custom items to drop from mobs when they are killed, with configurable entity types, drop chances, and quantities.

## Configuration

Mob drops are defined per-item in the item's YAML file:

```yaml
mob-drops:
  zombie_drop:
    entity-type: ZOMBIE
    chance: 0.05
    min-amount: 1
    max-amount: 1
  skeleton_drop:
    entity-type: SKELETON
    chance: 0.1
    min-amount: 1
    max-amount: 3
```

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `entity-type` | String | — | Bukkit `EntityType` name (e.g., `ZOMBIE`, `SKELETON`) |
| `chance` | Double | `0.05` | Drop probability from `0.0` (0%) to `1.0` (100%) |
| `min-amount` | Integer | `1` | Minimum number of items to drop |
| `max-amount` | Integer | `1` | Maximum number of items to drop |

## Supported Entity Types

Any living entity type is supported. Common examples:

| Entity | Key |
|---|---|
| Zombie | `ZOMBIE` |
| Skeleton | `SKELETON` |
| Creeper | `CREEPER` |
| Spider | `SPIDER` |
| Enderman | `ENDERMAN` |
| Wither Skeleton | `WITHER_SKELETON` |
| Blaze | `BLAZE` |
| Pillager | `PILLAGER` |
| Warden | `WARDEN` |

!!! info "Living Entities Only"
    Only entities that extend `LivingEntity` (i.e., `EntityType.isAlive()` returns `true`) are valid. Non-living entities like arrows or boats will fail validation.

## How It Works

1. The `MobDropListener` listens for `EntityDeathEvent`
2. When a mob dies, it checks all custom items for matching mob drops
3. For each matching drop, it rolls against the `chance` value
4. If successful, it generates a random amount between `min-amount` and `max-amount`
5. The custom item is dropped at the mob's death location

## Multiple Drops

An item can have drops from multiple entity types:

```yaml
mob-drops:
  common_drop:
    entity-type: ZOMBIE
    chance: 0.05
    min-amount: 1
    max-amount: 1
  rare_drop:
    entity-type: ENDER_DRAGON
    chance: 1.0
    min-amount: 1
    max-amount: 3
```

## Validation

A mob drop configuration is valid when:

- `entity-type` is a valid `EntityType` name
- The entity type represents a living entity
- `chance` is between `0.0` and `1.0`
- `min-amount` is at least `1`
- `max-amount` is greater than or equal to `min-amount`
