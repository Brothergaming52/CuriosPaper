# Performance & Storage

CuriosPaper provides several options to tune performance and configure data storage.

## Storage Settings

```yaml
storage:
  type: "yaml"
  save-interval: 300
  save-on-close: true
  create-backups: false
  backup-interval: 3600
  max-backups: 5
```

| Setting | Default | Description |
|---|---|---|
| `type` | `yaml` | Storage backend (currently only `yaml` is supported) |
| `save-interval` | `300` | Auto-save interval in seconds (0 to disable) |
| `save-on-close` | `true` | Save player data when the accessory GUI is closed |
| `create-backups` | `false` | Create periodic backups of player data |
| `backup-interval` | `3600` | Backup interval in seconds (1 hour) |
| `max-backups` | `5` | Maximum number of backup files to keep |

!!! tip "Recommended Production Settings"
    For production servers, consider enabling backups:
    ```yaml
    storage:
      save-interval: 300
      save-on-close: true
      create-backups: true
      backup-interval: 3600
      max-backups: 5
    ```

## Performance Settings

```yaml
performance:
  cache-player-data: true
  unload-on-quit: true
  max-items-per-slot: 54
```

| Setting | Default | Description |
|---|---|---|
| `cache-player-data` | `true` | Cache player accessory data in memory for faster access |
| `unload-on-quit` | `true` | Unload player data from memory after disconnect |
| `max-items-per-slot` | `54` | Maximum items per slot type (safety limit) |

### Memory Management

- **`cache-player-data: true`** (recommended) — Keeps player data in a `HashMap` for O(1) access. Data is loaded on join and unloaded on quit (if `unload-on-quit` is true).
- **`unload-on-quit: true`** (recommended) — Frees memory when players disconnect. Set to `false` only if you need to access offline player data frequently.

## Debug Settings

```yaml
debug:
  enabled: false
  log-api-calls: false
  log-inventory-events: false
  log-slot-positions: false
```

| Setting | Default | Description |
|---|---|---|
| `enabled` | `false` | Enable debug logging |
| `log-api-calls` | `false` | Log all API method calls |
| `log-inventory-events` | `false` | Log inventory click and interaction events |
| `log-slot-positions` | `false` | Log GUI slot position calculations |

!!! warning "Debug in Production"
    Keep all debug options **disabled** on production servers. Debug logging can significantly impact performance and flood the console.

## Feature Toggles

```yaml
features:
  add-slot-lore-to-items: true
  item-editor:
    enabled: true
  allow-elytra-on-back-slot: true
  show-empty-slots: true
  play-gui-sound: true
  play-equip-sound: true
  play-unequip-sound: true
```

| Feature | Default | Description |
|---|---|---|
| `add-slot-lore-to-items` | `true` | Adds "Required Slot: ..." lore to tagged items |
| `item-editor.enabled` | `true` | Enables `/edit` command and custom item system |
| `allow-elytra-on-back-slot` | `true` | Allows elytra in back slots (requires 1.21.3+) |
| `show-empty-slots` | `true` | Show placeholder icons for empty slots in GUI |
| `play-gui-sound` | `true` | Play sound when opening the accessory GUI |
| `play-equip-sound` | `true` | Play sound when equipping an accessory |
| `play-unequip-sound` | `true` | Play sound when unequipping an accessory |
