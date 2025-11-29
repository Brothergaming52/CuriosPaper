---
layout: default
title: Resource Pack Config
parent: Configuration # **CRITICAL: Links it to the parent page**
nav_order: 3 # Position within the Configuration drop-down
---

# **Resource Pack Configuration**

CuriosPaper includes a **full automatic resource pack pipeline** that:

* Builds a pack from CuriosPaper’s internal assets
* Registers & merges assets from *other plugins*
* Enforces namespace rules (conflict prevention)
* Applies merging rules for specific JSON files
* Hosts the final ZIP through an embedded HTTP server

This system is **not optional** if you want custom slot icons, elytra-overrides, or addon plugin assets to work correctly.

---

# **What the Resource Pack Actually Does**

CuriosPaper generates and serves a pack that contains:

* ✔ Slot icons (`item-model`)
* ✔ Slot GUI items
* ✔ All built-in elytra/chestplate models (trim-aware)
* ✔ Built-in accessory models
* ✔ Assets added by other plugins (like HeadBound)
* ✔ Overrides for atlas + trim JSON files
* ✔ JSON merges for Curios-specific curated files

The build output is:

```
plugins/CuriosPaper/resource-pack-build/
```

The hosted pack:

```
plugins/CuriosPaper/resource-pack.zip
```

CuriosPaper rebuilds the pack only **once per startup** using a **dirty-flag build system** so startup doesn’t get spammed by repeated builds.

---

# **Resource-Pack Section in config.yml**

```yaml
resource-pack:
  enabled: true
  port: 8080
  host-ip: "localhost"
  base-material: "PAPER"

  allow-minecraft-namespace: false
  allow-namespace-conflicts: false
```

Below is the **full, accurate explanation** of every setting.

---

# **`enabled` – Toggle Pack System**

```yaml
enabled: true
```

If disabled:

* No pack will be created
* No pack will be hosted
* Slot icons will **NOT** use custom models
* Addon plugins’ models will be ignored
* Any `item-model:` config values are useless

**Do NOT disable this unless you are providing your own custom, full replacement resource pack.**

---

# **`port` – Embedded HTTP Server Port**

```yaml
port: 8080
```

CuriosPaper hosts the pack via a lightweight built-in HTTP server.

Rules you MUST follow:

* It **cannot** be your server’s main port
* It MUST be open in the firewall
* It MUST NOT be used by another program
* If the port is blocked → players cannot download the pack

Safe examples: `8080`, `8081`, `9000`, `1337`.

---

# **`host-ip` – Public IP or Domain**

```yaml
host-ip: "localhost"
```

This is the address sent to clients as:

```
http://<host-ip>:<port>/resource-pack.zip
```

Examples:

```yaml
host-ip: "123.45.67.89"
host-ip: "play.example.com"
host-ip: "mc.yourserver.net"
```

If wrong → players see **“Failed to download resource pack”**.

ALWAYS use a domain if you have one.

---

# **`base-material` – Base Item for Slot Icons**

```yaml
base-material: "PAPER"
```

This is the underlying vanilla item used for slot icons.

Popular options:

* `PAPER`
* `LEATHER_HORSE_ARMOR`
* `TOTEM_OF_UNDYING`

This affects ONLY the underlying item model parent — all icon rendering still uses your custom `item-model` JSON.

---

# **`allow-minecraft-namespace` – Control Use of `minecraft:` Namespace**

```yaml
allow-minecraft-namespace: false
```

The `minecraft:` namespace is **dangerous** because addon plugins can accidentally override **vanilla game assets**.

If:

* `false` → Addons **cannot** use `minecraft:` namespace
* `true` → Addons may override vanilla files (use cautiously)

If a plugin attempts to use `minecraft:` while this option is `false`, CuriosPaper:

* Logs a namespace violation
* Ignores the addon’s conflicting assets
* Starts normally (no crashes)

---

# **`allow-namespace-conflicts` – Prevent Plugin Conflicts**

```yaml
allow-namespace-conflicts: false
```

When two plugins use the same namespace, it creates guaranteed file collisions and model conflicts.

If:

* `false` → CuriosPaper **blocks the plugin from loading**, logs a clear error, and tells the server owner to change the namespace
* `true` → Both plugins load, but their assets may conflict (not recommended)

This protects server owners from “silent texture overrides” that are a nightmare to debug.

---

# **How the Resource Pack Build Pipeline Works**

### ✔ Extract CuriosPaper internal assets

Includes:

* Slot icons
* Back-slot elytra models
* Material + trim variants
* Required atlas overrides

### ✔ Extract assets from other plugins

Only when added via:

```java
api.registerResourcePackAssetsFromJar(plugin);
```

Plugins must include:

```
resources/assets/<namespace>/...
```

### ✔ Enforce namespace rules

Violations are logged through:

```
/curios rp conflicts
```

### ✔ Curated JSON Merges

Only specific CuriosPaper JSON files allow merges
All other files:

* Strict copy-or-skip
* No auto-merging
* Conflicts reported in the conflict log

### ✔ Final ZIP creation

Built into:

```
resource-pack.zip
```

Served as:

```
http://<host-ip>:<port>/resource-pack.zip
```

---

# **Plugin Integration (Example: HeadBound)**

HeadBound adds dozens of custom models:

```
resources/assets/curiospaper/models/item/...
```

CuriosPaper:

* Detects the assets
* Extracts them
* Merges them
* Assigns the correct namespace
* Injects them into the final server pack

This is EXACTLY how every addon should integrate with your system.

---

# **Testing Your Pack**

### Test hosting:

Open in a browser:

```
http://<host-ip>:<port>/resource-pack.zip
```

### Test client:

* Missing textures → wrong `item-model`
* Pack download failure → host-ip or port wrong
* Vanilla icons show → pack disabled or load failure
* Wrong custom models → namespace conflict

---

# **Common Issues & Fixes**

### ❌ “Pack failed to download”

* Invalid `host-ip`
* Firewall blocking port
* Using server’s main port
* Reverse proxy not forwarding correctly

### ❌ “My plugin’s models don’t show”

* Wrong folder structure
* Wrong namespace
* Not registered via API
* `allow-minecraft-namespace: false` blocking them

### ❌ “Two plugins are overwriting each other”

* Both using same namespace
* `allow-namespace-conflicts: true`

---

# **Final Summary**

| Setting                     | Purpose                                            |
| --------------------------- | -------------------------------------------------- |
| `enabled`                   | Enables the entire pack system                     |
| `port`                      | Embedded HTTP server port                          |
| `host-ip`                   | Public host for the ZIP                            |
| `base-material`             | Base item material for slot icons                  |
| `allow-minecraft-namespace` | Allow/disallow vanilla namespace overrides         |
| `allow-namespace-conflicts` | Prevent or allow multi-plugin namespace collisions |

CuriosPaper’s resource pack system is **strict**, **safe**, and **addon-ready**.
Use it to build a unified, conflict-free visual experience for all accessories on your server.

---
