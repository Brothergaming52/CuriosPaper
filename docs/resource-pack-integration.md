---
layout: default
title: Resource Pack Integration
parent: Developer API
nav_order: 6
---
# Developer API – Resource Pack Integration

CuriosPaper ships with a **full automatic resource pack pipeline** and lets **addons inject their own assets** into that same pack.

This page shows how your plugin can:

- Ship models/textures/icons inside its JAR (HeadBound-style).
- Register those assets with CuriosPaper’s pack builder.
- Reference your models via `item-model` in config.
- Respect CuriosPaper’s **namespace and conflict rules**.
- Debug conflicts using the CuriosPaper tooling.

If you’re looking for admin config, read  
**Configuration → Resource Pack**.  
This page is for **developers** only.

---

## 1. How CuriosPaper’s Pack Works (Dev View)

CuriosPaper builds and serves a pack at:

```text
plugins/CuriosPaper/resource-pack-build/
plugins/CuriosPaper/resource-pack.zip
````

On startup / rebuild it:

1. Extracts **its own assets** (`assets/curiospaper/...`).
2. Extracts **assets from registered plugins**.
3. Enforces **namespace rules**:

    * Reserved `curiospaper` namespace.
    * Optional `minecraft` namespace usage.
    * Optional namespace conflict allowance.
4. Applies **curated JSON merging** for a few whitelisted files only.
5. Writes a final `resource-pack.zip`.
6. Hosts it via an embedded HTTP server.

Your job is just:

* Put files in the **right layout**.
* Call the **API** to register them.

---

## 2. Asset Layout Inside Your Plugin JAR

Inside *your* project, put assets here:

```text
src/main/resources/resources/
 └─ assets/
     └─ <your_namespace>/
         ├─ models/
         │   └─ item/
         │       └─ my_custom_icon.json
         ├─ textures/
         │   └─ item/
         │       └─ my_custom_icon.png
         └─ ... (sounds, lang, whatever you need)
```

Important:

* Top-level folder must be **`resources/`**.
  CuriosPaper scans and extracts from there.
* Everything under that follows **normal Minecraft layout**:
  `assets/<namespace>/...`.
* `<your_namespace>` should be unique, e.g.:

    * `headbound`
    * `myaddon`
    * `myserver`

**Do NOT** hijack `curiospaper` or `minecraft` without knowing what you’re doing (see §7).

You **do not** ship `pack.mcmeta` here; CuriosPaper builds that.

---

## 3. Registering Your Assets With the API

In your plugin `onEnable`, after you have the API:

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;

public class MyCuriosAddon extends JavaPlugin {

    private CuriosPaperAPI curiosApi;

    @Override
    public void onEnable() {
        CuriosPaper curiosPaper = CuriosPaper.getInstance();
        this.curiosApi = curiosPaper.getCuriosPaperAPI();

        if (curiosApi == null) {
            getLogger().severe("CuriosPaper API not available! Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register assets from this plugin's JAR
        File root = curiosApi.registerResourcePackSource(this, getFile());
        getLogger().info("Registered CuriosPaper resource assets from: " + root.getAbsolutePath());

        // now register listeners, commands, etc.
    }
}
```

What this does:

* Scans your plugin JAR for `/resources/...`.
* Copies everything under `resources/` into CuriosPaper’s build directory.
* Marks the resource pack as “dirty” so it’s rebuilt once.
* Your assets become part of the **unified server pack**.

Server owner doesn’t touch anything. Exactly how HeadBound integrates.

---

## 4. Referencing Your Models via `item-model`

Once your assets are registered, you can reference them in CuriosPaper’s `config.yml` (slot icons, etc.).

Say your plugin provides:

```text
resources/assets/headbound/models/item/scouts_lens_icon.json
resources/assets/headbound/textures/item/scouts_lens_icon.png
```

Then in CuriosPaper’s `config.yml`:

```yaml
slots:
  head:
    name: "&e⚜ Head Slot ⚜"
    icon: "GLASS"
    item-model: "headbound:scouts_lens_icon"
    amount: 1
    lore:
      - "&7Equip magical lenses and circlets."
```

Rules:

* `item-model: "<namespace>:<path>"` (no `.json` extension).
* Path must resolve to:
  `assets/<namespace>/models/item/<path>.json`.
* Texture paths are defined inside your JSON as usual.

This is the pattern your HeadBound-like addons should follow.

---

## 5. Example Item Model JSON

Simple icon model:

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "headbound:item/scouts_lens_icon"
  }
}
```

Texture goes in:

```text
resources/assets/headbound/textures/item/scouts_lens_icon.png
```

CuriosPaper merges this into its pack and the client sees it when:

* `resource-pack.enabled: true`
* `host-ip` / `port` are valid and reachable

---

## 6. Runtime Flow for Your Addon

What actually happens:

1. **CuriosPaper starts.**

    * Reads its config.
    * Prepares the build directory.
2. **CuriosPaper runs its pack builder** (dirty-flag controlled).
3. **Your addon enables.**

    * Grabs `CuriosPaperAPI`.
    * Calls `registerResourcePackSource(this, getFile())`.
4. CuriosPaper:

    * Extracts `/resources/...` from your plugin JAR.
    * Writes them into `resource-pack-build/assets/<namespace>/...`.
    * Schedules (or executes) a single rebuild.
5. Resource pack is zipped as `resource-pack.zip`.
6. Embedded HTTP server serves it.
7. Client joins, downloads the unified pack, sees:

    * CuriosPaper’s own slot icons, trims, elytra models.
    * Your addon’s custom icons/models.

---

## 7. Namespaces, Safety Flags & Conflicts

CuriosPaper enforces **strict namespace rules** so one trash addon doesn’t wreck everyone.

Relevant config:

```yaml
resource-pack:
  allow-minecraft-namespace: false
  allow-namespace-conflicts: false
```

### 7.1 Reserved `curiospaper` namespace

* The `curiospaper:` namespace is **owned by CuriosPaper itself**.
* Do NOT stick your random models in there unless you intend to override CuriosPaper assets and accept the risk.
* For normal addons, **use your own namespace** (e.g. `headbound`).

If CuriosPaper detects another plugin trying to “own” the `curiospaper` namespace as its primary source, it will:

* Log a **reserved namespace violation**.
* Ignore or block that plugin’s conflicting assets (depending on version/rules).

### 7.2 `minecraft:` namespace

Controlled by:

```yaml
allow-minecraft-namespace: false
```

* When `false` (default):
  Plugins are **not allowed** to ship assets in `assets/minecraft/...` through CuriosPaper.
  This prevents accidental overrides of vanilla textures/models.
* When `true`:
  Plugins can override vanilla assets **on purpose**. Dangerous but sometimes useful.

If a plugin uses `minecraft:` while it’s disabled, CuriosPaper logs the violation and skips those assets.

### 7.3 Namespace conflicts

Controlled by:

```yaml
allow-namespace-conflicts: false
```

* When `false`:

    * If two plugins both register the same namespace, CuriosPaper treats it as a conflict.
    * One of them gets blocked and you get a loud error telling you to change your namespace.
* When `true`:

    * Multiple plugins can dump assets into the same namespace.
    * Standard pack override rules apply (last in wins).
    * This is only sane when you intentionally design for overrides.

### 7.4 Conflict inspection

Admins (and you, while testing) can run:

```text
/curios rp conflicts
```

It shows:

* Reserved namespace violations
* `minecraft:` namespace violations
* Duplicate namespace owners
* Allowed conflicts (if config permits)

If your addon is misbehaving with assets, this is your first stop.

---

## 8. Curated JSON Merging vs Hard Conflicts

CuriosPaper does **NOT** try to “merge everything”.
That always ends in broken packs.

Only specific, curated files are merge-allowed (example names):

* `curios_item_base.json`
* `curios_combined_override.json`

For those:

* Multiple plugins can contribute entries.
* CuriosPaper merges their contents.

For **all other files**:

* **Strict copy-or-skip**:

    * If a file with the same path already exists, the later one is treated as a conflict.
    * CuriosPaper logs the conflict clearly.
    * It either skips the later file or obeys the configured conflict rules.

As an addon dev, don’t assume blind merging.
Design your assets so collisions either:

* Never happen (unique namespace + paths), or
* Are intentional overrides.

---

## 9. Using a Custom Pack Folder (Optional)

If CuriosPaper exposes a folder-based variant (depends on version), you may see something like:

```java
File externalPackFolder = new File(getDataFolder(), "pack");
File root = curiosApi.registerResourcePackSource(this, externalPackFolder);
```

Pattern:

* You ship a default set in your JAR (under `/resources/`).
* You let server owners drop overrides into `plugins/MyAddon/pack/`.
* You call `registerResourcePackSource` on both (JAR + folder).

Same rules:

* Must have `assets/<namespace>/...`.
* Same namespace safety + conflict rules apply.

Use this when you explicitly want server owners to override your textures/models without repacking the JAR.

---

## 10. Debugging When Things Don’t Show Up

If models/icons are broken, here’s the checklist:

1. **Check your JAR structure**

   Open your plugin JAR and verify:

   ```text
   /resources/assets/<namespace>/models/item/...
   /resources/assets/<namespace>/textures/item/...
   ```

   If `resources/` is missing or you put assets under just `/assets`, CuriosPaper won’t see them.

2. **Check CuriosPaper logs on startup**

   Look for:

    * Resource pack build status
    * Namespace conflict errors
    * Minecraft-namespace violations
    * Reserved namespace usage

3. **Check build directory**

   After server start:

   ```text
   plugins/CuriosPaper/resource-pack-build/assets/<namespace>/...
   ```

   If your files aren’t there, your registration is wrong.

4. **Verify `item-model` IDs**

    * `item-model: "headbound:scouts_lens_icon"`
    * Must match JSON path:
      `assets/headbound/models/item/scouts_lens_icon.json`.

5. **Test the pack URL directly**

   In a browser:

   ```text
   http://<host-ip>:<port>/resource-pack.zip
   ```

    * If it doesn’t download → host/port/firewall issue.
    * If it downloads but models still broken → your paths / JSON are wrong.

6. **Use `/curios rp conflicts`**

   If there’s a namespace or file conflict, CuriosPaper will tell you.

---

## 11. Good vs Bad Integration

### ✅ Do this

* Use a **unique namespace** (`headbound`, `myaddon`, etc.).
* Put assets under `src/main/resources/resources/assets/<namespace>/...`.
* Call `registerResourcePackSource(this, getFile())` in `onEnable`.
* Reference models by `item-model` IDs in CuriosPaper config.
* Use `/curios rp conflicts` when debugging.

### ❌ Don’t do this

* Dump assets into `assets/curiospaper` unless you *intentionally* override CuriosPaper’s models.
* Use `minecraft:` namespace while `allow-minecraft-namespace: false`.
* Assume CuriosPaper will “smart-merge” all your JSON.
* Ship packs separately and then complain that players have to accept 2–3 packs.

---

## 12. Summary

* CuriosPaper owns the **resource pack pipeline**: build, merge, host.
* Your addon:

    * Provides assets under `resources/assets/<namespace>/...`.
    * Registers them via `curiosApi.registerResourcePackSource(...)`.
    * References models with `item-model` in CuriosPaper config.
* Respect **namespace rules** and use the **conflict tooling** instead of blindly overriding everything.

If you follow this pattern (like a properly built HeadBound-style addon), server owners get **one clean pack**, and your accessories look exactly how you intended.
