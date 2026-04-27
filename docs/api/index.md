# Developer API

CuriosPaper provides a comprehensive API for other plugins to create accessories, manage slots, configure 3D models, register loot tables, and hook into the accessory system. This section shows you how to build plugins that integrate with CuriosPaper.

## Sections

| Page | Description |
|---|---|
| [Getting the API](getting-api.md) | How to access the CuriosPaperAPI instance |
| [Creating Accessories](creating-accessories.md) | Programmatically create and manage custom items |
| [Loot Tables & Mob Drops](loot-tables-mob-drops.md) | Register loot table entries, mob drops, and villager trades |
| [3D Models](3d-models.md) | Configure 3D model attachments for items and mob drops |
| [Resource Pack Assets](resource-pack-assets.md) | Contribute textures and models to the server resource pack |
| [Abilities](abilities.md) | Add abilities to items via the API |
| [Events](events.md) | Custom events for equip/unequip, loot generation, and mob drops |

## What Can You Build?

Here are some ideas for plugins that use the CuriosPaper API:

- **RPG Class System** — Grant class-specific accessories with stat bonuses
- **Dungeon Rewards** — Create unique accessory drops from custom bosses
- **Quest Rewards** — Give players accessories for completing quests
- **Economy Integration** — Sell accessories through a shop plugin
- **Achievement Badges** — Award collectible charms for milestones
- **Seasonal Events** — Create limited-time holiday accessories
- **Custom Loot Tables** — Add custom items to dungeon chests and mob drops
- **3D Cosmetics** — Attach visible 3D models to equipped accessories

## Quick Start Example

Here's a minimal example of a plugin that creates a ring accessory and gives it to a player:

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class MyAccessoryPlugin extends JavaPlugin {

    private CuriosPaperAPI curiosAPI;

    @Override
    public void onEnable() {
        // Get the CuriosPaper API
        CuriosPaper curiosPaper = CuriosPaper.getInstance();
        if (curiosPaper == null) {
            getLogger().severe("CuriosPaper not found! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        curiosAPI = curiosPaper.getCuriosPaperAPI();
        getLogger().info("Connected to CuriosPaper API!");
    }

    // Call this from a command or event
    public void giveRingToPlayer(Player player) {
        // Create a basic ring item
        ItemStack ring = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = ring.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Heroic Ring");
        ring.setItemMeta(meta);

        // Tag it as a ring accessory
        ItemStack taggedRing = curiosAPI.tagAccessoryItem(ring, "ring");

        // Give it to the player
        player.getInventory().addItem(taggedRing);
        player.sendMessage(ChatColor.GREEN + "You received a Heroic Ring!");
    }
}
```

## Maven Dependency

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.Brothergaming52</groupId>
    <artifactId>CuriosPaper</artifactId>
    <version>1.3.0</version>
    <scope>provided</scope>
</dependency>
```

## plugin.yml

Add CuriosPaper as a dependency in your plugin:

```yaml
name: MyAccessoryPlugin
version: 1.0.0
main: com.example.MyAccessoryPlugin
depend: [CuriosPaper]
# or use softdepend if CuriosPaper is optional:
# softdepend: [CuriosPaper]
```

!!! tip "Hard vs Soft Depend"
    Use `depend` if your plugin **requires** CuriosPaper to function. Use `softdepend` if CuriosPaper integration is optional and your plugin can work without it.
