# Resource Pack Assets API

CuriosPaper allows other plugins to contribute resource pack assets that are automatically merged into the generated server resource pack.

## Overview

When CuriosPaper builds its resource pack, it can include assets from external plugins. This is useful when your plugin provides custom textures, models, or sounds that need to be part of the server's resource pack.

## Methods

### registerResourcePackAssets

Registers a folder containing resource pack assets to be included in the next pack build.

```java
void registerResourcePackAssets(Plugin plugin, File folder)
```

**Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `plugin` | `Plugin` | The plugin registering the assets |
| `folder` | `File` | Folder containing the `assets/` directory structure |

**Folder Structure:**

```
your-folder/
└── assets/
    └── minecraft/
        ├── textures/
        │   └── item/
        │       └── my_custom_item.png
        └── models/
            └── item/
                └── my_custom_item.json
```

### Example

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

        // Register a folder from your plugin's data directory
        File assetsFolder = new File(getDataFolder(), "resources");
        api.registerResourcePackAssets(this, assetsFolder);
    }
}
```

---

### registerResourcePackAssetsFromJar

Extracts and registers resource pack assets embedded inside your plugin's JAR file. This is the recommended approach — it automatically extracts files from your JAR's `resources/` directory to disk and registers them.

```java
File registerResourcePackAssetsFromJar(Plugin plugin)
```

**Returns:** The `File` pointing to the extracted folder.

**JAR Structure:**

```
your-plugin.jar
└── resources/
    └── assets/
        └── minecraft/
            ├── textures/
            │   └── item/
            │       └── my_ring.png
            └── models/
                └── item/
                    └── my_ring.json
```

### Example: Embedding Assets in JAR

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

        // Extract resources/ from your JAR and register them
        // Files are extracted to plugins/MyPlugin/resources/
        // Existing files on disk are NOT overwritten (server owners can edit them)
        api.registerResourcePackAssetsFromJar(this);
    }
}
```

### File Extraction Behavior

- Files are extracted from the JAR's `resources/` directory to `plugins/<YourPlugin>/resources/`
- **Existing files are NOT overwritten** — this allows server owners to customize textures
- Directories are created automatically if they don't exist
- After extraction, the folder is registered with the resource pack builder

---

## External Resource Pack Combining

CuriosPaper also supports combining external `.zip` resource packs located in a configured folder. Enable this in `config.yml`:

```yaml
resource-pack:
  combine-external-rp: true
```

When enabled, CuriosPaper will look for ZIP files in the `external-resource-packs` folder within the plugin directory and merge them into the final generated pack.

---

## Rebuild After Registration

After registering assets, you may want to trigger a pack rebuild:

```java
// Rebuild via command (admin)
// /curios rp rebuild

// Or trigger programmatically by calling the rebuild
// The pack is also rebuilt on server startup
```

!!! tip "Registration Timing"
    Register your assets in `onEnable()` before CuriosPaper builds the pack. CuriosPaper collects all registered sources before generating the final resource pack.
