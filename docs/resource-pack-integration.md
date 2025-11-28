---
layout: default
title: Resource Pack Integration
parent: Developer API # **Links it to the Developer Guide parent**
nav_order: 6
---
# Developer API – Resource Pack Integration

CuriosPaper ships with its **own auto-generated resource pack**, and it lets **other plugins** inject their own assets into that same pack.

This page explains how **your plugin** can:

- Ship textures/models/icons inside its JAR.
- Have CuriosPaper extract and merge them into the server resource pack.
- Reference your models from `config.yml` via `item-model`.
- Keep everything automatic for server owners.

If you’re looking for admin-facing config, see:  
**Configuration → Resource Pack**.  
This page is strictly for **developers**.

---

## 1. How CuriosPaper’s Pack Works (Dev Perspective)

CuriosPaper builds a pack into:

```text
plugins/CuriosPaper/resource-pack-build/
plugins/CuriosPaper/resource-pack.zip
````

It:

* Extracts its own assets (`assets/curiospaper/...`).
* Merges resources from *other plugins* that register assets.
* Hosts `resource-pack.zip` via an embedded HTTP server.
* Advertises that pack to clients (if enabled in config).

Your job:
Provide assets in the **right folder structure** and call the **API hook**.

---

## 2. Folder Layout Inside Your Plugin JAR

Inside *your* plugin project, place Curios-related assets in:

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
         └─ ... (any other asset folders)
```

Key points:

* The **root** must be `resources/` (CuriosPaper extracts from that).
* Then standard Minecraft layout: `assets/<namespace>/...`.
* `<your_namespace>` can be your plugin name or any valid namespace, e.g.:

  * `myaddon`
  * `myserver`
  * `coolcurios`

You **do not** need your own `pack.mcmeta` here – CuriosPaper handles that.

---

## 3. Registering Assets from Your JAR

In your plugin `onEnable`, after CuriosPaper is available, call:

```java
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

        // Register your resource pack assets from this plugin's JAR
        File root = curiosApi.registerResourcePackAssetsFromJar(this);
        getLogger().info("Registered Curios assets from: " + root.getAbsolutePath());
    }
}
```

What this does:

* Looks inside your plugin JAR for `/resources/...`.
* Extracts all files under `resources/` into CuriosPaper’s build folder.
* Merges them with CuriosPaper’s own `assets/` tree.
* Marks the pack for rebuild.

From the server owner’s perspective, it’s **zero extra setup**.

---

## 4. Referencing Your Models in `config.yml`

Once your assets are registered, you can reference your models via `item-model` in CuriosPaper’s `config.yml`.

Example: your plugin provides:

```text
assets/myaddon/models/item/backpack_icon.json
assets/myaddon/textures/item/backpack_icon.png
```

Then in `config.yml`:

```yaml
slots:
  back:
    name: "&5☾ Back Slot ☾"
    icon: "LEATHER_CHESTPLATE"
    item-model: "myaddon:backpack_icon"
    amount: 1
    lore:
      - "&7Carries your relic backpack."
```

Rules:

* `item-model` uses `<namespace>:<path>` (no `.json`).
* Must match `assets/<namespace>/models/item/<path>.json`.
* Texture paths are defined inside the JSON, as usual.

---

## 5. Example Model JSON

Basic `backpack_icon.json`:

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "myaddon:item/backpack_icon"
  }
}
```

Texture file path in your JAR:

```text
resources/assets/myaddon/textures/item/backpack_icon.png
```

CuriosPaper will merge this into its pack and clients will receive it automatically (assuming `resource-pack.enabled = true` and `host-ip`/`port` are set correctly).

---

## 6. Runtime Flow for Your Addon

1. **Server starts.**
2. CuriosPaper loads and builds/updates its resource pack.
3. Your plugin loads, gets `CuriosPaperAPI`.
4. Your plugin calls `registerResourcePackAssetsFromJar(this)`.
5. CuriosPaper:

   * Extracts `resources/assets/myaddon/...` from your JAR.
   * Merges with its `resource-pack-build`.
   * Rebuilds `resource-pack.zip`.
6. Players join:

   * CuriosPaper serves the merged resource pack.
   * Curios GUIs can display icons using your models.

---

## 7. Using Different Namespaces or Multiple Addons

You can use **any namespace** you want. Good patterns:

* One namespace per plugin:

  * `assets/myaddon1/...`
  * `assets/myaddon2/...`
* Or shared namespace for a suite:

  * `assets/myserver/...`

If multiple plugins use the **same namespace and paths**, standard resource-pack override rules apply:

* Last one merged “wins” for conflicting files.
* Design for that if you intend override behavior.

---

## 8. Debugging Your Asset Integration

If icons/models don’t show:

1. **Check folder structure inside JAR**
   Open your plugin JAR and verify:

   ```text
   /resources/assets/<namespace>/models/item/...
   /resources/assets/<namespace>/textures/item/...
   ```

2. **Check server console logs**

   * Does CuriosPaper log any error while extracting/merging?
   * Does your plugin confirm registration?

3. **Check `resource-pack-build`**

   * After startup, verify files are present in:

     ```text
     plugins/CuriosPaper/resource-pack-build/assets/<namespace>/...
     ```

4. **Check `item-model` strings**

   * Make sure `item-model: "myaddon:backpack_icon"` matches your JSON path.

5. **Test pack URL**

   * Visit `http://<host-ip>:<port>/resource-pack.zip` in a browser.
   * If you can’t download, your hosting config is wrong.

---

## 9. Advanced: Registering from External Folder (If Available)

If you also have a **development resource pack folder** outside your JAR, CuriosPaper may expose a method like:

```java
File externalPackFolder = new File(getDataFolder(), "my-addon-resources");
File root = curiosApi.registerResourcePackAssetsFromFolder(this, externalPackFolder);
```

(Depends on the exact API version; if present, it works similar to the JAR variant but reads from the filesystem.)

Use this pattern when:

* You want server owners to drop/override files.
* You’re iterating on assets without rebuilding the plugin JAR.

---

## 10. Summary

* Put your assets in `src/main/resources/resources/assets/<namespace>/...`.
* Call `curiosApi.registerResourcePackAssetsFromJar(this);` on startup.
* Reference models in CuriosPaper’s `config.yml` via `item-model: "<namespace>:<path>"`.
* CuriosPaper handles extraction, merging, zipping, and hosting.
