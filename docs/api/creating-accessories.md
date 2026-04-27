# Creating Accessories via API

This page shows practical examples of how to create, tag, manage, and query accessories from your own plugin.

## Example 1: Tagging Existing Items

The simplest approach — take any `ItemStack` and tag it as an accessory:

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class AccessoryUtil {

    private final CuriosPaperAPI api;

    public AccessoryUtil() {
        this.api = CuriosPaper.getInstance().getCuriosPaperAPI();
    }

    /**
     * Creates a tagged ring accessory from scratch.
     */
    public ItemStack createRing(String name, Material material) {
        ItemStack ring = new ItemStack(material);
        ItemMeta meta = ring.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "A custom ring accessory.",
            ChatColor.GRAY + "Place in the ring slot."
        ));
        ring.setItemMeta(meta);

        // Tag it as a ring accessory — this adds the PDC tag
        return api.tagAccessoryItem(ring, "ring");
    }

    /**
     * Gives a player a tagged accessory and notifies them.
     */
    public void giveAccessory(Player player, ItemStack accessory) {
        player.getInventory().addItem(accessory);
        String slotType = api.getAccessorySlotType(accessory);
        player.sendMessage(ChatColor.GREEN + "Received a " + slotType + " accessory!");
    }
}
```

## Example 2: Full Custom Item with ItemData

For items that need recipes, abilities, mob drops, and full CuriosPaper integration:

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.AbilityData;
import org.bg52.curiospaper.data.RecipeData;
import org.bg52.curiospaper.data.MobDropData;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class FireAmuletPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

        // Create the item (using 'this' tracks ownership to your plugin)
        ItemData fireAmulet = api.createItem(this, "fire_amulet");

        if (fireAmulet == null) {
            getLogger().info("fire_amulet already exists, skipping creation.");
            return;
        }

        // Set properties
        fireAmulet.setDisplayName("&c&lAmulet of Fire");
        fireAmulet.setMaterial("BLAZE_POWDER");
        fireAmulet.setSlotType("necklace");
        fireAmulet.setCustomModelData(30001);
        fireAmulet.setItemModel("myplugin:fire_amulet");
        fireAmulet.setLore(Arrays.asList(
            "&7Forged in the heart of a volcano.",
            "&7Protects the wearer from fire.",
            "",
            "&eBound to the Necklace slot"
        ));

        // Configure 3D Model Attachment
        // itemId, enabled, modelItem, customModelData, itemModel, pitchUpLimit, pitchDownLimit
        api.setItemModelConfig("fire_amulet", true, "GOLD_BLOCK", null, "myplugin:amulet_3d", 45.0f, 60.0f);

        // Add a WHILE_EQUIPPED fire resistance ability
        AbilityData fireResist = new AbilityData();
        fireResist.setTrigger(AbilityData.Trigger.WHILE_EQUIPPED);
        fireResist.setEffectType(AbilityData.EffectType.POTION_EFFECT);
        fireResist.setEffectName("FIRE_RESISTANCE");
        fireResist.setAmplifier(0);
        fireResist.setDuration(200);
        fireAmulet.getAbilities().put("fire_resist", fireResist);

        // Add a mob drop from Blazes
        MobDropData blazeDrop = new MobDropData();
        blazeDrop.setEntityType(EntityType.BLAZE.name());
        blazeDrop.setChance(0.05);
        blazeDrop.setMinAmount(1);
        blazeDrop.setMaxAmount(1);
        api.registerItemMobDrop("fire_amulet", blazeDrop);

        // Save to disk
        api.saveItemData("fire_amulet");

        getLogger().info("Fire Amulet registered!");
    }
}
```

## Example 3: Registering Custom Slots at Runtime

Create entirely new accessory slot types from your plugin:

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class EarringPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

        // Register a new "earring" slot type
        boolean success = api.registerSlot(
            "earring",                              // slot key
            "&a✦ Earring Slots ✦",                 // display name
            Material.DIAMOND,                       // icon material
            "myplugin:earring_slot",               // item model (1.21.3+)
            10020,                                  // custom model data (1.14-1.21.2)
            2,                                      // 2 earring slots per player
            Arrays.asList("&7Sparkling earring slots.")  // lore
        );

        if (success) {
            getLogger().info("Earring slot type registered!");
        } else {
            getLogger().warning("Failed to register earring slot type.");
        }
    }

    @Override
    public void onDisable() {
        // Optionally unregister when your plugin is disabled
        CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();
        api.unregisterSlot("earring");
    }
}
```

## Example 4: Querying Player Accessories

Check what a player has equipped — useful for RPG plugins, stat systems, etc:

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AccessoryInspector {

    private final CuriosPaperAPI api;

    public AccessoryInspector() {
        this.api = CuriosPaper.getInstance().getCuriosPaperAPI();
    }

    /**
     * Displays all equipped accessories to the player.
     */
    public void showEquipment(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Your Accessories ===");

        List<String> slotTypes = api.getAllSlotTypes();

        for (String slotType : slotTypes) {
            if (!api.hasEquippedItems(player, slotType)) continue;

            List<ItemStack> items = api.getEquippedItems(player, slotType);
            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                if (item != null && item.hasItemMeta()) {
                    String name = item.getItemMeta().getDisplayName();
                    player.sendMessage(ChatColor.YELLOW + slotType + "[" + i + "]: "
                        + ChatColor.WHITE + name);
                }
            }
        }
    }

    /**
     * Checks if a player has a specific custom item equipped anywhere.
     */
    public boolean hasItemEquipped(Player player, String targetSlotType) {
        return api.hasEquippedItems(player, targetSlotType)
            && api.countEquippedItems(player, targetSlotType) > 0;
    }

    /**
     * Gets the total number of accessories a player has equipped.
     */
    public int getTotalEquippedCount(Player player) {
        int total = 0;
        for (String slotType : api.getAllSlotTypes()) {
            total += api.countEquippedItems(player, slotType);
        }
        return total;
    }

    /**
     * Checks what type of accessory an item in the player's hand is.
     */
    public void inspectHeldItem(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();

        if (api.isValidAccessory(held)) {
            String slot = api.getAccessorySlotType(held);
            player.sendMessage(ChatColor.GREEN + "This is a " + slot + " accessory!");

            // Check if it fits a specific slot
            if (api.isValidAccessory(held, "ring")) {
                player.sendMessage(ChatColor.AQUA + "It can be equipped in the ring slot.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "This is not an accessory.");
        }
    }
}
```

## Example 5: Programmatic Equip/Unequip

Set or clear accessories for a player directly:

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AccessoryManager {

    private final CuriosPaperAPI api;

    public AccessoryManager() {
        this.api = CuriosPaper.getInstance().getCuriosPaperAPI();
    }

    /**
     * Force-equips an item in a player's first empty ring slot.
     */
    public boolean equipInFirstEmptySlot(Player player, ItemStack accessory, String slotType) {
        List<ItemStack> currentItems = api.getEquippedItems(player, slotType);
        int slotCount = api.getSlotAmount(slotType);

        for (int i = 0; i < slotCount; i++) {
            if (i >= currentItems.size() || currentItems.get(i) == null) {
                // Found an empty slot — set the item
                List<ItemStack> updated = new ArrayList<>(currentItems);
                // Ensure the list is big enough
                while (updated.size() <= i) updated.add(null);
                updated.set(i, accessory);
                api.setEquippedItems(player, slotType, updated);
                return true;
            }
        }
        return false; // No empty slots
    }

    /**
     * Clears all accessories from a player for a specific slot type.
     */
    public void clearSlot(Player player, String slotType) {
        api.clearEquippedItems(player.getUniqueId(), slotType);
    }

    /**
     * Strips ALL accessories from a player (e.g., on death or jail).
     */
    public List<ItemStack> stripAllAccessories(Player player) {
        List<ItemStack> stripped = new ArrayList<>();

        for (String slotType : api.getAllSlotTypes()) {
            List<ItemStack> items = api.getEquippedItems(player, slotType);
            for (ItemStack item : items) {
                if (item != null) stripped.add(item);
            }
            api.clearEquippedItems(player.getUniqueId(), slotType);
        }

        return stripped; // Return removed items so caller can store/drop them
    }
}
```

## Example 6: Resource Pack Integration

Register your plugin's custom textures to be included in CuriosPaper's auto-generated resource pack:

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TexturedAccessoryPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

        // Register your resource pack assets
        // Your folder should contain: assets/myplugin/models/item/...
        File rpFolder = new File(getDataFolder(), "resourcepack");
        if (rpFolder.exists()) {
            api.registerResourcePackAssets(this, rpFolder);
            getLogger().info("Custom textures registered with CuriosPaper!");
        }
    }
}
```

Your resource pack folder structure:

```
plugins/MyPlugin/resourcepack/
└── assets/
    └── myplugin/
        ├── models/
        │   └── item/
        │       └── fire_amulet.json
        └── textures/
            └── item/
                └── fire_amulet.png
```
