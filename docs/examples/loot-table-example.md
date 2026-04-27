# Loot Table Example

This example shows how to create a plugin that adds a custom accessory to dungeon chests as loot.

## Complete Plugin Example

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.data.AbilityData;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.LootTableData;
import org.bg52.curiospaper.data.MobDropData;
import org.bg52.curiospaper.event.CuriosLootGenerateEvent;
import org.bg52.curiospaper.event.CuriosMobDropEvent;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class DungeonLootPlugin extends JavaPlugin implements Listener {

    private CuriosPaperAPI api;

    @Override
    public void onEnable() {
        CuriosPaper curiosPaper = CuriosPaper.getInstance();
        if (curiosPaper == null) {
            getLogger().severe("CuriosPaper not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        api = curiosPaper.getCuriosPaperAPI();

        // Create our custom dungeon items
        createDungeonRing();
        createSkeletonCharm();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Dungeon Loot Plugin enabled!");
    }

    /**
     * Creates a ring that appears in dungeon chests with a 20% chance
     */
    private void createDungeonRing() {
        String itemId = "ancient_ring";

        // Don't recreate if it already exists
        if (api.getItemData(itemId) != null) return;

        ItemData item = api.createItem(this, itemId);
        item.setDisplayName("§6§lAncient Ring of Power");
        item.setMaterial("GOLD_NUGGET");
        item.setSlotType("ring");
        item.setLore(java.util.Arrays.asList(
            "§7A ring recovered from an ancient dungeon.",
            "§7It hums with residual magic.",
            "",
            "§a+2 Luck while equipped"
        ));

        // Add a luck boost ability
        AbilityData luck = new AbilityData();
        luck.setTrigger(AbilityData.Trigger.WHILE_EQUIPPED);
        luck.setEffectType(AbilityData.EffectType.PLAYER_MODIFIER);
        luck.setEffectName("GENERIC_LUCK");
        luck.setAmplifier(2);
        luck.setDuration(0);
        item.getAbilities().put("luck_boost", luck);

        // Add to dungeon and mineshaft chests
        item.addLootTable(new LootTableData(
            "minecraft:chests/simple_dungeon", 0.20, 1, 1));
        item.addLootTable(new LootTableData(
            "minecraft:chests/abandoned_mineshaft", 0.10, 1, 1));
        item.addLootTable(new LootTableData(
            "minecraft:chests/stronghold_corridor", 0.15, 1, 1));

        api.saveItemData(itemId);
        getLogger().info("Created " + itemId + " with loot table entries.");
    }

    /**
     * Creates a charm that drops from skeletons with a 3% chance
     */
    private void createSkeletonCharm() {
        String itemId = "bone_charm";

        if (api.getItemData(itemId) != null) return;

        ItemData item = api.createItem(this, itemId);
        item.setDisplayName("§7§lBone Charm");
        item.setMaterial("BONE");
        item.setSlotType("charm");
        item.setLore(java.util.Arrays.asList(
            "§7A charm carved from undead bone.",
            "",
            "§aGrants Night Vision while equipped"
        ));

        // Night vision ability
        AbilityData nightVision = new AbilityData();
        nightVision.setTrigger(AbilityData.Trigger.WHILE_EQUIPPED);
        nightVision.setEffectType(AbilityData.EffectType.POTION_EFFECT);
        nightVision.setEffectName("NIGHT_VISION");
        nightVision.setAmplifier(0);
        nightVision.setDuration(400);
        item.getAbilities().put("night_vision", nightVision);

        // 3% drop from skeletons
        MobDropData drop = new MobDropData();
        drop.setEntityType("SKELETON");
        drop.setChance(0.03);
        drop.setMinAmount(1);
        drop.setMaxAmount(1);
        item.getMobDrops().add(drop);

        api.saveItemData(itemId);
        getLogger().info("Created " + itemId + " with mob drop config.");
    }

    // ========================
    // Event Listeners
    // ========================

    @EventHandler
    public void onLootGenerate(CuriosLootGenerateEvent event) {
        // Log all custom item loot generation
        getLogger().info("[Loot] Generated " + event.getCustomItemId()
            + " in " + event.getLootTableKey());
    }

    @EventHandler
    public void onMobDrop(CuriosMobDropEvent event) {
        // Announce rare drops to the killer
        if (event.getEntity().getKiller() != null) {
            event.getEntity().getKiller().sendMessage(
                ChatColor.GOLD + "★ Rare Drop: "
                + ChatColor.WHITE + event.getItem().getItemMeta().getDisplayName());
        }
    }
}
```

## plugin.yml

```yaml
name: DungeonLootPlugin
version: 1.0.0
main: com.example.DungeonLootPlugin
depend: [CuriosPaper]
description: Adds custom accessories to dungeon loot tables and mob drops
```

## What This Example Covers

- Creating custom items via the API with plugin ownership
- Adding loot table entries for multiple chest types
- Configuring mob drops with probability
- Adding abilities (attribute modifiers and potion effects)
- Listening for `CuriosLootGenerateEvent` and `CuriosMobDropEvent`
- Preventing duplicate creation on reload
