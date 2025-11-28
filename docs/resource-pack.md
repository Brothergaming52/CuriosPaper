---
layout: default
title: Resource Pack Config
parent: Configuration # **CRITICAL: Links it to the parent page**
nav_order: 3 # Position within the Configuration drop-down
---
# Resource Pack Configuration

CuriosPaper includes a **fully automatic resource pack system** that generates, merges, and serves a custom pack containing:

- Slot icons (`item-model`)
- Textures and model files shipped by CuriosPaper
- Assets registered by other plugins using the API
- Your own custom additions (if placed in the correct folder)

This page explains how the resource pack system works, how to configure the host IP/port, and how to safely integrate your own models.

---

## 📦 What the Resource Pack Does

CuriosPaper’s resource pack is responsible for all visual elements:

✔ Slot icons  
✔ Per-slot item models  
✔ Plugin-registered asset overrides  
✔ Patterns and GUI textures (if provided)  

Players **must download the pack** for your slot icons and models to appear correctly.

The pack is generated into:

```

plugins/CuriosPaper/resource-pack-build/

```

and served as:

```

plugins/CuriosPaper/resource-pack.zip

````

---

## ⚙ Resource Pack Config Section

You will find this section at the top of `config.yml`:

```yaml
resource-pack:
  enabled: true
  port: 8080
  host-ip: "localhost"
  base-material: "PAPER"
````

Below is a detailed explanation of each option.

---

## 🟢 `enabled`

Controls whether CuriosPaper builds and serves the resource pack.

```yaml
enabled: true
```

If disabled:

* No pack will be generated
* Slot icons will fall back to vanilla items
* Any `item-model:` values will not work

**You should keep this enabled unless you are manually providing your own pack.**

---

## 🔌 `port`

Defines the port used by CuriosPaper’s built-in HTTP server.

```yaml
port: 8080
```

### Important notes:

* **You MUST use a port different from your server’s main port.**
  Example: if your server runs on 25565 → use 8080, 9090, etc.
* If the port is blocked by a firewall, players won’t receive the pack.
* If the port is in use by another process → startup will warn you.

### Example safe ports:

* 8080
* 8081
* 9000
* 1337

---

## 🌐 `host-ip`

This is the IP or hostname players download the pack from.

```yaml
host-ip: "localhost"
```

Change this to match your server’s public address:

### Examples:

```yaml
host-ip: "your.server.ip.here"
host-ip: "play.example.net"
host-ip: "mc.myserver.com"
```

### If set incorrectly:

Players will get a “resource pack failed to download” message.

**Tip:** Use your domain whenever possible.

---

## 🧱 `base-material`

Defines which Minecraft item is used as the base material for slot icons.

```yaml
base-material: "PAPER"
```

This maps to the item used to display custom `item-model` textures.

### Recommended options:

* `PAPER` (simple, clean, safe)
* `LEATHER_HORSE_ARMOR` (good for 3D slot icons)
* `FEATHER` (light decorative base)
* `TOTEM_OF_UNDYING` (stylized icons)

Changing this allows for completely different visual styles.

---

## 🗂 Resource Pack Structure

CuriosPaper builds a complete pack using:

```
resource-pack-build/
 ├─ assets/
 │   ├─ curiospaper/
 │   │   └─ models/item/...
 │   ├─ <other plugins>/
 │   │   └─ models/item/...
 │   └─ minecraft/
 │       └─ textures/...   (if overridden)
 └─ pack.mcmeta
```

CuriosPaper automatically:

* Extracts its internal assets
* Merges any registered plugin assets
* Rebuilds the pack when needed
* Serves the ZIP via embedded HTTP

You never need to manually zip or host anything.

---

## 🔧 How Item Models Work

Each slot type has an `item-model` value:

```yaml
item-model: "curiospaper:back_slot"
```

This corresponds to a JSON model file inside:

```
assets/curiospaper/models/item/back_slot.json
```

CuriosPaper injects these into the pack.

### If you want your **own custom model**:

Place your model file in:

```
src/main/resources/resources/assets/<namespace>/models/item/
```

Then reference it in config:

```yaml
item-model: "myplugin:custom_back_slot"
```

CuriosPaper will detect and merge this automatically.

---

## 🔌 How Other Plugins Register Pack Assets

If another plugin wants to add models, textures, or icons to CuriosPaper’s resource pack, they can use the API:

```java
File root = curiosApi.registerResourcePackAssetsFromJar(this);
```

They just need to include a directory inside their JAR:

```
resources/assets/<namespace>/...
```

CuriosPaper handles:

* Extraction
* Merging
* Cleanup
* Repackaging

---

## 🧪 Testing Your Setup

### Check the pack builds:

Start the server → look for:

```
[CuriosPaper] Resource pack built successfully.
```

### Check serving:

Open this in a browser:

```
http://<host-ip>:<port>/resource-pack.zip
```

If the file downloads → pack hosting works.

### Check client behavior:

Join the server with resource packs enabled:

* If icons appear → everything is correct.
* If icons appear as missing textures → `item-model` paths are wrong.
* If pack fails to download → `host-ip` or `port` is wrong.

---

## ✔ Common Issues & Fixes

### **“Resource pack failed to download”**

* Wrong `host-ip`
* Port blocked by firewall
* Wrong IP behind proxy
* Wrong port

### **“Icons look like vanilla paper instead of custom icons”**

* Pack disabled
* Wrong `item-model` path
* Wrong base-material
* Host IP unreachable

### **“My plugin’s custom models aren’t showing up”**

* Incorrect folder structure
* Wrong namespace
* Forgot to register assets via API

---

## 📌 Summary

| Setting         | Controls                               |
| --------------- | -------------------------------------- |
| `enabled`       | Turns CuriosPaper’s pack system on/off |
| `port`          | Web server port for pack hosting       |
| `host-ip`       | Where clients download the ZIP from    |
| `base-material` | Base item used for custom icons        |

The resource pack system is one of CuriosPaper’s most powerful features — use it to deliver seamless visual integration for your accessory system.

---
