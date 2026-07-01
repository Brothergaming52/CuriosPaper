# Performance & Storage

CuriosPaper provides several options to tune performance and configure data storage.

## Storage Settings

```yaml
storage:
  # Storage type: 'yaml', 'sqlite', 'mysql', 'mongodb'
  type: "sqlite"

  # Auto-save interval in seconds (300 = 5 minutes, 0 to disable)
  save-interval: 300

  # Save player data when the accessory GUI is closed
  save-on-close: true

  # Automatically migrate existing YAML player data to the selected database on startup
  auto-migrate: true

  # SQLite settings (used when type is 'sqlite')
  sqlite:
    file: "storage/curiospaper.db"

  # MySQL settings (used when type is 'mysql')
  mysql:
    host: "localhost"
    port: 3306
    database: "curiospaper"
    username: "root"
    password: ""
    pool-size: 10
    use-ssl: false

  # MongoDB settings (used when type is 'mongodb')
  mongodb:
    connection-string: "mongodb://localhost:27017"
    database: "curiospaper"
```

| Setting | Default | Description |
|---|---|---|
| `type` | `yaml` | Storage backend: `yaml` (flat-files), `sqlite` (local DB file), `mysql` (external MySQL server), or `mongodb` (MongoDB collection) |
| `save-interval` | `300` | Auto-save interval in seconds (0 to disable) |
| `save-on-close` | `true` | Save player data when the accessory GUI is closed |
| `auto-migrate` | `true` | Automatically migrate existing flat-file YAML player data files to the selected database backend on startup |
| `sqlite.file` | `storage/curiospaper.db` | Relative file path for the local SQLite database file |
| `mysql.host` | `localhost` | Host address of the MySQL/MariaDB database server |
| `mysql.port` | `3306` | Port of the MySQL/MariaDB database server |
| `mysql.database` | `curiospaper` | Name of the database to use |
| `mysql.username` | `root` | Database username |
| `mysql.password` | `""` | Database password |
| `mysql.pool-size` | `10` | HikariCP connection pool size |
| `mysql.use-ssl` | `false` | Use SSL for database connections |
| `mongodb.connection-string` | `mongodb://localhost:27017` | MongoDB connection URL |
| `mongodb.database` | `curiospaper` | Name of the MongoDB database |

!!! tip "Recommended Production Settings"
    For production networks, use MySQL or MongoDB to share accessory slot inventories across servers seamlessly:
    ```yaml
    storage:
      type: "mysql"
      save-interval: 300
      save-on-close: true
      auto-migrate: true
      mysql:
        host: "mysql.internal.net"
        port: 3306
        database: "curiospaper"
        username: "curios_user"
        password: "secure_password"
        pool-size: 10
        use-ssl: true
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
