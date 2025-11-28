---
layout: default
title: Storage & Backups
parent: Configuration # **CRITICAL: Links it to the parent page**
nav_order: 4 # Position within the Configuration drop-down
---
# Storage & Backups

CuriosPaper stores all player accessory data in a persistent data file inside the plugin’s folder.  
This page explains how storage works internally, how often data is saved, how backups are generated, and what each setting in the `storage:` configuration block does.

---

## 📁 Where Player Data Is Stored

By default, CuriosPaper stores player data in:

```

plugins/CuriosPaper/data/

```

Inside this folder, each player has an individual YAML file:

```

<uuid>.yml

````

This file contains:

- Equipped items for each slot type  
- Internal metadata  
- Accessory state  

This system is optimized for **speed**, **readability**, and **safe incremental saving**.

---

## ⚙ Storage Config Section

You will find the entire storage configuration here:

```yaml
storage:
  type: "yaml"
  save-interval: 300
  save-on-close: true
  create-backups: false
  backup-interval: 3600
  max-backups: 5
````

Below is the full explanation for each property.

---

## 🧩 `type`

```yaml
type: "yaml"
```

Currently, only `"yaml"` is supported.
This is stable, human-readable, and safe for most servers.

Future implementations may add:

* SQL
* Mongo
* JSON
* Redis-backed caching

But for now: **YAML is the only option**.

---

## ⏲ `save-interval`

```yaml
save-interval: 300
```

How often (in **seconds**) CuriosPaper automatically saves modified player data.

* `300` = save every 5 minutes
* `0` = disable timed saves

### Recommended values:

| Server Size         | Recommended                     |
| ------------------- | ------------------------------- |
| Small SMP           | `300`                           |
| Medium server       | `180`                           |
| Large network       | `60`–`120`                      |
| Min/max performance | `0` (if you only save manually) |

Timed saves prevent data loss during crashes or power failures.

---

## 📥 `save-on-close`

```yaml
save-on-close: true
```

If enabled:

* Whenever a player **closes the accessory GUI**, their data is saved immediately.

Recommended to keep this **on** unless you’re doing custom data handling externally.

---

## 💾 `create-backups`

```yaml
create-backups: false
```

If enabled:

* CuriosPaper will generate **backup copies** of each player’s data file before overwriting.

Recommended to enable on:

* RPG servers
* Servers with valuable items stored in accessory slots
* Beta servers or unstable builds

Disabled by default for performance reasons.

---

## 🔁 `backup-interval`

```yaml
backup-interval: 3600  # seconds (1 hour)
```

How often backup files are created *when backups are enabled*.

Typical values:

| Usage            | Time      | Value           |
| ---------------- | --------- | --------------- |
| Frequent backups | 15 min    | `900`           |
| Normal           | 1 hour    | `3600`          |
| Low I/O servers  | 4–6 hours | `14400`–`21600` |

Backups are stored in:

```
plugins/CuriosPaper/backups/
```

---

## 📦 `max-backups`

```yaml
max-backups: 5
```

The maximum number of backup copies stored **per player**.

When the limit is exceeded, the oldest backup is deleted automatically.

### Examples:

* `max-backups: 5` → keeps last 5 backups
* `max-backups: 20` → keeps a long-term history
* `max-backups: 1` → only one snapshot exists at any time

---

## 🧠 How the Save System Works Internally

CuriosPaper uses a **hybrid save system** consisting of:

### 1. Cached Player Data

Data is held in memory while the player is online.

### 2. Burst Save Operations

Saves happen:

* On timer (based on `save-interval`)
* On inventory close (`save-on-close`)
* On player quit
* On server shutdown
* When forced by API usage

### 3. Safe Write Method

The plugin writes changes to a **temporary file**, then swaps it in atomically:

1. Write → `12345.tmp`
2. Replace → `12345.yml`
3. (Optional) Save backup

This ensures:

* No corruption
* No half-written files
* Safety during unexpected crashes

---

## 🛟 Best Practice Recommendations

### ✔ For SMP / normal servers

```
save-interval: 300
save-on-close: true
create-backups: true
backup-interval: 3600
max-backups: 5
```

### ✔ For large servers or high player counts

```
save-interval: 120
save-on-close: false
create-backups: false
```

(Centralized backups should be handled externally.)

### ✔ For testing / development

```
save-interval: 0
save-on-close: true
create-backups: false
```

Useful when reloading constantly and rapidly modifying config.

---

## 🧪 Verifying Saved Data

You can confirm the system is working:

### 1. Open `/plugins/CuriosPaper/data/`

* Check that a file `<uuid>.yml` exists.
* Open it and see updates as players modify slots.

### 2. If backups enabled:

Open `/plugins/CuriosPaper/backups/`
And verify multiple timestamped files appear.

### 3. Server console:

Look for logs during shut down or auto-save cycles.

---

## 📌 Summary

| Setting           | What it Controls                      |
| ----------------- | ------------------------------------- |
| `save-interval`   | How often data is saved automatically |
| `save-on-close`   | Saves immediately on GUI close        |
| `create-backups`  | Whether backup files are made         |
| `backup-interval` | How often backups are created         |
| `max-backups`     | How many backups per player to keep   |

The storage system is designed to be **safe, simple, and configurable**, scaling from tiny SMPs to large professional servers.

---
