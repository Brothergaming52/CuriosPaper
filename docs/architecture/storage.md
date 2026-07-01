# Storage Architecture

CuriosPaper features a pluggable, asynchronous storage architecture. It supports flat-file YAML (default/legacy), SQLite, MySQL, and MongoDB backends for persisting player accessory slot data.

## Storage API & Engines

The storage layer is controlled by `CuriosStorageAPI`, which acts as the unified entry point. Depending on the configured `storage.type` in `config.yml`, it instantiates the corresponding `StorageProvider` implementation:

```
                  ┌──────────────────────┐
                  │   CuriosStorageAPI   │
                  └──────────┬───────────┘
                             │
            ┌────────────────┼────────────────┐
            ▼                ▼                ▼
     ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
     │SQLiteProvider│ │ MySQLProvider│ │MongoDBProvider│
     └──────────────┘ └──────────────┘ └──────────────┘
```

- **YAML Engine (Legacy / Fallback):** Reads and writes flat `.yml` files in the `playerdata/` directory.
- **SQLite Engine:** Stores player inventories in a local, single-file SQLite database located at `storage/curiospaper.db`.
- **MySQL Engine:** Uses a remote MySQL/MariaDB database with HikariCP connection pooling for fast, concurrent queries. Ideal for proxy networks (BungeeCord/Velocity) to synchronize inventories across servers.
- **MongoDB Engine:** Uses a MongoDB document collection for server networks seeking a NoSQL database structure.

---

## Player Data Storage

### Database Schema / Structure

For SQL backends (SQLite, MySQL), CuriosPaper initializes a table `curiospaper_playerdata` with the following structure:

| Column | Type | Description |
|---|---|---|
| `uuid` | `VARCHAR(36)` | The player's Unique Identifier (Primary Key) |
| `slot_type` | `VARCHAR(32)` | The curios slot type (e.g. `ring`, `back`) |
| `slot_index` | `INT` | The index of the slot (0-indexed) |
| `item_data` | `TEXT` / `LONGTEXT` | The Base64 encoded serialized item stack |
| `last_updated` | `TIMESTAMP` | Time of the last inventory save |

For MongoDB, documents are stored in a collection named `playerdata` with a structure representing the player's unique UUID and an array of accessories serialized into Base64 item format.

---

## Data Serialization

When storing item stacks in database engines:
- Items are serialized to a custom Base64 string format using CuriosPaper's internal `ItemStackSerializer` (based on Bukkit NBT/Data Component serialization).
- Serialization preserves all custom display names, lore, attribute modifiers, enchantments, and Persistent Data Container (PDC) metadata components.
- Empty slots are stored as null/empty database entries.

---

## Auto-Migration on First Boot

If you transition from flat-file YAML to any database backend (SQLite, MySQL, MongoDB), CuriosPaper provides an automatic data migration routine:

1. **Detection:** On startup, the plugin reads `storage.type`. If it is a database backend and `storage.auto-migrate` is set to `true`, the migration system initializes.
2. **Parsing:** The plugin scans the `plugins/CuriosPaper/playerdata/` folder for any legacy `.yml` files matching player UUIDs.
3. **Execution:** For each file found, CuriosPaper reads the accessory slots, serializes the items, and inserts them into the selected database provider.
4. **Renaming:** After a successful database insertion, the YAML file is renamed to `<UUID>.yml.migrated` to prevent duplicate migration runs.
5. **Logging:** Detailed progress logs are outputted to the server console.

---

## Data Lifecycle

```
[Player Join] ──► [SlotManager] ──► [CuriosStorageAPI] ──► Asynchronous DB Fetch ──► Cache in Memory
                                                                                          │
[Player Quit] ◄── [SlotManager] ◄── [CuriosStorageAPI] ◄── Asynchronous DB Save  ◄────────┘
```

### 1. On Player Join
- The server fires `PlayerJoinEvent`.
- `SlotManager` calls `CuriosStorageAPI#loadPlayerAccessories(UUID)`.
- If database mode is active, the database provider queries the database asynchronously, returns a `CompletableFuture<Map<String, List<ItemStack>>>`, and caches the inventory list in memory.
- If YAML mode is active, the `.yml` file is loaded from disk.

### 2. On Player Quit
- The server fires `PlayerQuitEvent`.
- `SlotManager` triggers `CuriosStorageAPI#savePlayerAccessories(UUID, Map)`.
- The database provider encodes the accessories and issues an asynchronous database query.
- If `performance.unload-on-quit` is enabled, player data is removed from the memory cache.

### 3. Periodic Auto-Save
- A repeating task runs every `save-interval` seconds (default: 300).
- CuriosPaper iterates over all active player caches in memory and writes their state to the database asynchronously.

### 4. Server Shutdown
- During `onDisable()`, the auto-save scheduler is halted.
- The plugin saves all cached player data.
- The storage system waits (`joins` all outstanding futures) to ensure no player data is lost.
- Connection pools (HikariCP) and Mongo clients are safely shut down.
