# Death Behavior

CuriosPaper provides configurable behavior for what happens to a player's equipped accessories when they die.

## Configuration

```yaml
features:
  keep-curio-inventory:
    type: "Auto"
```

## Modes

| Mode | Behavior |
|---|---|
| `Always` | Players **always keep** their curio inventory on death, regardless of the `keepInventory` gamerule |
| `Auto` | Follows the vanilla `keepInventory` gamerule — keep items if `true`, drop if `false` (default) |
| `Never` | Players **always drop** their curio inventory on death, regardless of the `keepInventory` gamerule |

## How It Works

When a player dies, the `PlayerDeathListener` checks the configured mode:

1. **Always:** Accessories remain in their curio slots. Nothing is added to the death drops.
2. **Auto:** Checks `event.getKeepInventory()` (which reflects the `keepInventory` gamerule and any plugins that modify it). If `false`, all accessories are added to the death drops list and cleared from the curio slots.
3. **Never:** All accessories are forcibly added to the death drops list and cleared from curio slots, even if `keepInventory` is `true`.

## Dropped Items

When accessories are dropped on death:

- Each non-empty accessory item from all slot types is added to `event.getDrops()`
- Items appear as ground drops at the player's death location
- All curio slots are cleared after dropping

## Use Cases

| Scenario | Recommended Mode |
|---|---|
| Casual / Creative server | `Always` |
| Vanilla survival | `Auto` |
| Hardcore / PvP server | `Never` |
| Different behavior per world | Use a plugin to modify `keepInventory` per-world, then use `Auto` |

## Example: PvP Arena Integration

If you use a PvP arena plugin that sets `keepInventory` per-region, use `Auto` mode. CuriosPaper will automatically follow whatever the arena plugin decides:

```yaml
features:
  keep-curio-inventory:
    type: "Auto"
```

!!! warning "Never Mode"
    When using `Never` mode, players will **always** lose their accessories on death, even in creative mode or when `keepInventory` is enabled. Use with caution.
