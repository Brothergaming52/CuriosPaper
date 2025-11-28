---
layout: default
title: Performance & Caching
parent: Configuration # **CRITICAL: Links it to the parent page**
nav_order: 5 # Position within the Configuration drop-down
---
# Performance & Caching

CuriosPaper is designed to run efficiently on servers of all sizes — from small SMPs to large modded RPG networks.  
This section explains how the plugin handles **memory usage**, **player data caching**, and **safety limits**, and how each setting affects performance.

---

## ⚙ Performance Config Section

Found under:

```yaml
performance:
  cache-player-data: true
  unload-on-quit: true
  max-items-per-slot: 54
````

These options help balance memory usage and responsiveness while keeping your server safe from item duplication or overflow issues.

---

## 🧠 `cache-player-data`

```yaml
cache-player-data: true
```

When enabled:

* Player accessory data is **kept in memory** while they are online.
* Read/write operations are **instant**, reducing disk I/O.
* Slots open faster and API calls are more responsive.

### Recommended:

✔ Leave this **enabled** on almost all servers.

### Disable only if:

* You are running extreme memory-constrained environments.
* External tools (database sync, versioning) require file-level consistency at all times.

Disabling this will force CuriosPaper to read/write data from disk more often — much slower.

---

## 🚪 `unload-on-quit`

```yaml
unload-on-quit: true
```

When enabled:

* Player data is removed from cache when they disconnect.
* Reduces memory usage, especially useful for servers with **large player bases**.

### Recommended:

* ✔ Large servers
* ✔ Medium servers
* ✔ Any server with hundreds of unique players

### Disable only if:

* You want faster re-log times for players constantly connecting/disconnecting every few seconds.
* Your server restarts extremely rarely, and RAM usage is not a concern.

---

## 📦 `max-items-per-slot`

```yaml
max-items-per-slot: 54
```

This is a **hard safety cap** that prevents accidental or malicious situations where:

* A slot type contains hundreds or thousands of items
* Player files grow uncontrollably
* Plugins misuse the API and overflow slots
* Corrupted data loops cause infinite additions

This is a fail-safe, not a gameplay limit.

### Why 54?

* 54 is the size of a double chest.
* This mirrors typical GUI capacity limits.

### Recommended values:

* **54** → safest, default choice
* **27** → tighter safety, lower memory usage per player
* **100+** → only if required by custom systems (rare)

If exceeded, CuriosPaper will **block further items** and print warnings to console.

---

# 🔍 How Performance Logic Works

CuriosPaper has a lightweight performance model:

---

## 1. **Cached Accessory Data (when enabled)**

A per-player cache stores:

* Equipped items
* Slot structure
* Internal metadata

This makes operations like:

* Opening the GUI
* Equipping/unequipping accessories
* Running API checks

near-instantaneous.

---

## 2. **Safe On-Demand Loading**

If `cache-player-data` is off or the player re-logs:

* Data is loaded **on first access**
* Validated for integrity
* Cached again (if enabled)

---

## 3. **Unload Mechanism (when enabled)**

When a player leaves:

* Their cache entry is cleared
* Memory is freed
* Data is safely written to disk

This prevents memory creep on large servers.

---

## 4. **Safety Limits & Validation**

Every interaction goes through:

* Slot validation
* Item stack sanitation
* Max-item-per-slot checks

This protects your server from:

* Corrupt NBT
* Plugin conflicts
* Duplication exploits
* Overflow errors

---

# ⚡ Recommended Performance Configurations

### ✔ Small SMP (1–20 players)

```yaml
cache-player-data: true
unload-on-quit: false
max-items-per-slot: 54
```

Reason: RAM is cheap, faster user experience is better.

---

### ✔ Medium Server (20–80 players)

```yaml
cache-player-data: true
unload-on-quit: true
max-items-per-slot: 54
```

---

### ✔ Large Network / Economy / RPG Server (80–1000 players)

```yaml
cache-player-data: true
unload-on-quit: true
max-items-per-slot: 27
```

Reason: Maximize RAM efficiency and enforce stricter safety.

---

### ✔ Development / Testing

```yaml
cache-player-data: true
unload-on-quit: false
max-items-per-slot: 54
```

---

# 🧪 Debugging Performance Issues

If you ever notice:

* Slow GUI opening
* Delayed API responses
* Slot data not updating
* Memory leaks or I/O spikes

Check:

1. `cache-player-data` is **true**
2. `unload-on-quit` matches server scale
3. Items per slot are below `max-items-per-slot`
4. No external plugin is modifying CuriosPaper data files

Most performance problems come from:

* Custom plugins injecting broken items
* Servers disabling caching without understanding the consequences
* Massive player turnover combined with slow disks

---

# 📌 Summary

| Setting              | What it Affects                                 |
| -------------------- | ----------------------------------------------- |
| `cache-player-data`  | Speed of slot operations, disk load, RAM usage  |
| `unload-on-quit`     | Memory cleanup for disconnected players         |
| `max-items-per-slot` | Safety limit preventing overflow or duplication |

CuriosPaper’s performance system is designed to be **fast, safe, and scalable**, requiring minimal tuning for most servers.

---
