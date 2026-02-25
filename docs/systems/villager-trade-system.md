# Villager Trade System

The Villager Trade System allows custom items to appear as trades offered by villagers, with configurable professions, levels, chances, and costs.

## Configuration

Villager trades are defined per-item in the item's YAML file:

```yaml
villager-trades:
  trade_1:
    professions:
      - CLERIC
      - LIBRARIAN
    chance: 0.3
    trade-levels:
      - 3
      - 4
    cost-items:
      cost_1:
        material: EMERALD
        min-amount: 8
        max-amount: 16
      cost_2:
        material: GOLD_INGOT
        min-amount: 4
        max-amount: 4
```

## Properties

### Trade Properties

| Property | Type | Description |
|---|---|---|
| `professions` | String List | Villager professions that offer this trade |
| `chance` | Double | Probability of the trade appearing (0.0–1.0) |
| `trade-levels` | Integer List | Villager levels at which this trade can appear |
| `cost-items` | Map | Items required to complete the trade |

### Cost Item Properties

| Property | Type | Description |
|---|---|---|
| `material` | String | Material name (e.g., `EMERALD`) |
| `min-amount` | Integer | Minimum cost amount |
| `max-amount` | Integer | Maximum cost amount |

## Professions

Supported Bukkit `Villager.Profession` values:

| Profession | Typical Trades |
|---|---|
| `ARMORER` | Armor and weapons |
| `BUTCHER` | Food items |
| `CARTOGRAPHER` | Maps and banners |
| `CLERIC` | Enchanting and brewing |
| `FARMER` | Crops and food |
| `FISHERMAN` | Fish and rods |
| `FLETCHER` | Arrows and bows |
| `LEATHERWORKER` | Leather items |
| `LIBRARIAN` | Books and enchantments |
| `MASON` | Blocks and building |
| `SHEPHERD` | Wool and dyes |
| `TOOLSMITH` | Tools |
| `WEAPONSMITH` | Weapons |

### All Professions

If `professions` is empty or contains `ALL`, the trade is available from all professions:

```yaml
professions:
  - ALL
```

## Trade Levels

Villager trade levels range from 1 (Novice) to 5 (Master):

| Level | Name |
|---|---|
| 1 | Novice |
| 2 | Apprentice |
| 3 | Journeyman |
| 4 | Expert |
| 5 | Master |

If `trade-levels` is empty, the trade can appear at any level.

## How It Works

1. The `VillagerTradeListener` listens for villager profession changes
2. When a villager gains a new level, it checks all custom items for matching trades
3. For each matching trade, it rolls against the `chance` value
4. If successful, it adds the trade to the villager with a random cost within the configured range

## Multiple Cost Items

Minecraft supports up to 2 cost items per trade. CuriosPaper uses the first two entries from the `cost-items` map:

```yaml
cost-items:
  primary:
    material: EMERALD
    min-amount: 5
    max-amount: 10
  secondary:
    material: DIAMOND
    min-amount: 1
    max-amount: 1
```
