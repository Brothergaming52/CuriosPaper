package org.bg52.curiospaper.handler;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import net.kyori.adventure.key.Key;

import java.util.*;

/**
 * Handles elytra equipping in back slots with automatic gliding attribute management.
 * When an elytra is equipped in a back slot:
 * - If player has a chestplate: adds glider component to chestplate
 * - If player has no chestplate: secretly equips elytra with invisible item model
 */
public class ElytraBackSlotHandler implements Listener {
    private final CuriosPaper plugin;
    private final NamespacedKey secretElytraKey;
    private final Set<UUID> playersWithSecretElytra;

    public ElytraBackSlotHandler(CuriosPaper plugin) {
        this.plugin = plugin;
        this.secretElytraKey = new NamespacedKey(plugin, "secret_elytra");
        this.playersWithSecretElytra = new HashSet<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        // Only process back slot events
        if (!"back".equalsIgnoreCase(event.getSlotType())) {
            return;
        }

        // Check if feature is enabled
        if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack newItem = event.getNewItem();
        ItemStack previousItem = event.getPreviousItem();

        // Handle elytra being equipped
        if (newItem != null && newItem.getType() == Material.ELYTRA) {
            handleElytraEquipped(player);
        }
        // Handle elytra being unequipped
        else if (previousItem != null && previousItem.getType() == Material.ELYTRA) {
            handleElytraUnequipped(player);
        }
    }

    @Nullable
    private ItemStack getBackSlotElytra(Player player) {
        ItemStack backItem = plugin.getCuriosPaperAPI().getEquippedItem(player, "back", 0);
        if (backItem != null && backItem.getType() == Material.ELYTRA) {
            return backItem;
        }
        return null;
    }

    private void setBackSlotElytra(Player player, @Nullable ItemStack item) {
        // Adjust this to your real API
        plugin.getCuriosPaperAPI().setEquippedItem(player, "back", 0, item);
    }

    /**
     * Called when an elytra is equipped in the back slot
     */
    private void handleElytraEquipped(Player player) {
        // Check the durability of the back-slot elytra: if it's at 1, treat as broken
        ItemStack back = getBackSlotElytra(player);
        if (back != null) {
            ItemMeta meta = back.getItemMeta();
            if (meta instanceof Damageable dmgMeta) {
                int max = back.getType().getMaxDurability();
                int maxUsableDamage = max - 1;
                if (dmgMeta.getDamage() >= maxUsableDamage) {
                    // Elytra is effectively broken; don't enable any gliding
                    //plugin.getLogger().info("Back-slot elytra for " + player.getName()
                    //        + " is already at 1 durability; not enabling glider/secret elytra.");
                    return;
                }
            }
        }

        ItemStack chestplate = player.getInventory().getChestplate();

        if (chestplate != null && !chestplate.getType().isAir() && isChestplate(chestplate.getType())) {
            // Player has a chestplate - add glider component to it
            addGliderToChestplate(player, chestplate);
        } else {
            // No chestplate - equip secret elytra with invisible item model
            equipSecretElytra(player);
        }
    }

    /**
     * Called when an elytra is unequipped from the back slot
     */
    private void handleElytraUnequipped(Player player) {
        // Remove glider from chestplate if present
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && !chestplate.getType().isAir() && isChestplate(chestplate.getType())) {
            removeGliderFromChestplate(player, chestplate);
        }

        // Remove secret elytra if equipped
        removeSecretElytra(player);
    }

    private boolean isSecretElytra(ItemStack stack) {
        if (stack == null || stack.getType() != Material.ELYTRA) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(secretElytraKey, PersistentDataType.BYTE);
    }

    private void wipeSecretElytra(Player player) {
        // Chest slot
        ItemStack chest = player.getInventory().getChestplate();
        if (isSecretElytra(chest)) {
            player.getInventory().setChestplate(null);
        }

        // Main + armor + offhand inventory contents
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            if (isSecretElytra(contents[i])) {
                contents[i] = null;
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setContents(contents);
        }

        // Cursor item
        ItemStack cursor = player.getItemOnCursor();
        if (isSecretElytra(cursor)) {
            player.setItemOnCursor(null);
        }
    }

    /**
     * Prevent picking up the secret elytra with empty hands.
     * Still allow swapping it with a chestplate item.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorSlotProtectSecretElytra(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
            return;
        }

        if (event.getClickedInventory() != player.getInventory()) {
            return;
        }

        // Chest armor slot
        if (event.getSlot() != 38) {
            return;
        }

        ItemStack chest = player.getInventory().getChestplate();
        if (!isSecretElytra(chest)) {
            return;
        }

        ItemStack cursor = event.getCursor();
        boolean cursorIsAir = (cursor == null || cursor.getType().isAir());

        // If cursor is empty, player is trying to pick up the secret elytra -> block it
        if (cursorIsAir) {
            event.setCancelled(true);
            return;
        }

        // If cursor has a chestplate, we want to allow the swap so your existing MONITOR
        // handler can run and convert it to GLIDER. So do not cancel here.
        // Everything else (random item) you can choose to block or allow.
        // If you want to forbid swapping with non-chestplate items, uncomment this:

        /*
        if (!isChestplate(cursor.getType())) {
            event.setCancelled(true);
        }
        */
    }


    /**
     * Adds the glider component to a chestplate
     */
    private void addGliderToChestplate(Player player, ItemStack chestplate) {
        try {
            if (chestplate == null || chestplate.getType() == Material.AIR) {
                plugin.getLogger().warning("Tried to add glider to null/air chestplate for " + player.getName());
                return;
            }

            // 1) Enable gliding
            chestplate.setData(DataComponentTypes.GLIDER);

            // 2) Swap equipment model to one that includes wings
            Equippable equippable = chestplate.getData(DataComponentTypes.EQUIPPABLE);
            if (equippable != null) {
                Equippable.Builder builder = equippable.toBuilder();

                Key assetId = resolveChestplateWingsAsset(chestplate.getType());
                if (assetId != null) {
                    builder.assetId(assetId);
                    chestplate.setData(DataComponentTypes.EQUIPPABLE, builder.build());
                } else {
                    plugin.getLogger().warning("No wings assetId mapping for chestplate material "
                            + chestplate.getType() + " for " + player.getName());
                }
            }

            player.getInventory().setChestplate(chestplate);
            //plugin.getLogger().info("Added glider + wings asset (by material) to " + player.getName() + "'s chestplate");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to add glider to chestplate for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            equipSecretElytra(player);
        }
    }

    private @Nullable Key resolveChestplateWingsAsset(Material type) {
        if (!type.name().endsWith("_CHESTPLATE")) {
            return null;
        }

        // Turn "NETHERITE_CHESTPLATE" -> "netherite"
        String base = type.name()
                .toLowerCase(Locale.ROOT)
                .replace("_chestplate", "");

        // Final key: curiospaper:elytra_<material>_chestplate
        return Key.key("curiospaper", "elytra_" + base + "_chestplate");
    }

    /**
     * Redirect durability damage from chestplate/secret-elytra to the back-slot elytra.
     * Caps at 1 durability (vanilla Elytra behavior).
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
            return;
        }

        ItemStack damagedItem = event.getItem();
        if (damagedItem == null) {
            return;
        }

        // We only want to redirect if:
        //  - This item is our GLIDER chestplate, OR
        //  - This item is our secret elytra
        boolean isGliderChestplate =
                isChestplate(damagedItem.getType()) &&
                        damagedItem.hasData(DataComponentTypes.GLIDER);
        boolean isOurSecretElytra = isSecretElytra(damagedItem);

        if (!isGliderChestplate && !isOurSecretElytra) {
            return;
        }

        // And only if we actually have a back-slot elytra
        ItemStack backElytra = getBackSlotElytra(player);
        if (backElytra == null) {
            return;
        }

        int damage = event.getDamage();
        event.setCancelled(true); // don't damage the chest/secret item

        ItemMeta meta = backElytra.getItemMeta();
        if (!(meta instanceof Damageable dmgMeta)) {
            return; // shouldn't happen for Elytra
        }

        int currentDamage = dmgMeta.getDamage();
        int max = backElytra.getType().getMaxDurability();

        // Max usable damage is maxDurability - 1 (item disabled but not broken)
        int maxUsableDamage = max - 1;

        int newDamage = currentDamage + damage;
        if (newDamage > maxUsableDamage) {
            newDamage = maxUsableDamage;
        }

        // Apply capped damage
        dmgMeta.setDamage(newDamage);
        backElytra.setItemMeta(meta);
        setBackSlotElytra(player, backElytra);

        // If we've hit "broken but not destroyed" state -> disable gliding
        if (newDamage >= maxUsableDamage) {
            // Disable GLIDER chestplate OR secret-elytra chest behavior
            if (isGliderChestplate) {
                ItemStack chestplateNow = player.getInventory().getChestplate();
                if (chestplateNow != null && isChestplate(chestplateNow.getType())) {
                    removeGliderFromChestplate(player, chestplateNow);
                }
            } else if (isOurSecretElytra) {
                removeSecretElytra(player);
            }

            plugin.getLogger().info("Back-slot elytra for " + player.getName()
                    + " reached 1 durability; gliding disabled but item kept.");
        }
    }

    /**
     * Removes the glider component from a chestplate
     */
    private void removeGliderFromChestplate(Player player, ItemStack chestplate) {
        try {
            clearGliderComponents(chestplate);
            player.getInventory().setChestplate(chestplate);
            //plugin.getLogger().info("Removed glider + reset asset for " + player.getName() + "'s chestplate");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove glider from chestplate for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearGliderComponents(ItemStack chestplate) {
        try {
            if (chestplate.hasData(DataComponentTypes.GLIDER)) {
                chestplate.unsetData(DataComponentTypes.GLIDER);
            }

            Equippable defaultEq = chestplate.getType().getDefaultData(DataComponentTypes.EQUIPPABLE);
            if (defaultEq != null) {
                chestplate.setData(DataComponentTypes.EQUIPPABLE, defaultEq);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to clear glider components on dropped chestplate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Stop tracking this player
        playersWithSecretElytra.remove(player.getUniqueId());

        // Clean up drops:
        //  - Remove secret (invisible) elytra entirely
        //  - Convert any GLIDER chestplates back to normal items
        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (drop == null || drop.getType().isAir()) continue;

            if (isSecretElytra(drop)) {
                // Don't drop our internal "fake" elytra
                it.remove();
                continue;
            }

            if (isChestplate(drop.getType()) && drop.hasData(DataComponentTypes.GLIDER)) {
                clearGliderComponents(drop);
            }
        }
    }

    /**
     * Equips a secret elytra with invisible item model (inventory) but normal entity model (wings in F5)
     */
    private void equipSecretElytra(Player player) {
        wipeSecretElytra(player);

        ItemStack secretElytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = secretElytra.getItemMeta();

        if (meta != null) {
            // Mark this as a secret elytra
            meta.getPersistentDataContainer().set(secretElytraKey, PersistentDataType.BYTE, (byte) 1);

            // Set invisible item model for inventory display
            // This makes the elytra invisible when seen in inventory
            // But the entity model (wings in F5) stays normal
            meta.setItemModel(NamespacedKey.fromString("curiospaper:invisible"));

            secretElytra.setItemMeta(meta);
        }

        player.getInventory().setChestplate(secretElytra);
        playersWithSecretElytra.add(player.getUniqueId());
        //plugin.getLogger().info("Equipped secret elytra (invisible in inventory) for " + player.getName());
    }

    /**
     * Removes the secret elytra from the chest slot
     */
    private void removeSecretElytra(Player player) {
        if (!playersWithSecretElytra.contains(player.getUniqueId())) {
            return;
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
            ItemMeta meta = chestplate.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(secretElytraKey, PersistentDataType.BYTE)) {
                // This is our secret elytra - remove it
                player.getInventory().setChestplate(null);
                playersWithSecretElytra.remove(player.getUniqueId());
                wipeSecretElytra(player);
                //plugin.getLogger().info("Removed secret elytra from " + player.getName());
            }
        }
    }

    /**
     * Checks if player has an elytra equipped in back slot
     */
    private boolean hasElytraInBackSlot(Player player) {
        ItemStack backItem = plugin.getCuriosPaperAPI().getEquippedItem(player, "back", 0);
        return backItem != null && backItem.getType() == Material.ELYTRA;
    }

    /**
     * Handles when a player changes their chestplate while having elytra in back slot
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
            return;
        }

        // Check if they're interacting with their chest armor slot
        if (event.getSlot() == 38 && event.getClickedInventory() == player.getInventory()) {
            // Delay the check slightly to allow the item to be placed first
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (hasElytraInBackSlot(player)) {
                    handleChestplateChange(player);
                }
            }, 1L);
        }
    }

    /**
     * Handles chestplate changes when elytra is in back slot
     */
    private void handleChestplateChange(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();

        // Check if secret elytra was replaced with a chestplate
        if (playersWithSecretElytra.contains(player.getUniqueId())) {
            if (chestplate != null && isChestplate(chestplate.getType())) {
                // Remove ALL secret elytra traces (including cursor) first
                playersWithSecretElytra.remove(player.getUniqueId());
                wipeSecretElytra(player);

                // Re-read chestplate (in case chest slot was cleared during wipe)
                chestplate = player.getInventory().getChestplate();
                if (chestplate != null && isChestplate(chestplate.getType())) {
                    addGliderToChestplate(player, chestplate);
                }
            }
        } else {
            // Player has chestplate with glider, check if it was removed or swapped
            if (chestplate == null || chestplate.getType().isAir()) {
                // Chestplate removed - equip secret elytra
                equipSecretElytra(player);
            } else if (isChestplate(chestplate.getType())) {
                // Chestplate swapped - ensure new one has glider
                // First remove glider from old one if it's still in inventory
                // Then add to new one
                addGliderToChestplate(player, chestplate);
            }
        }
    }

    /**
     * Checks if a material is a chestplate
     */
    private boolean isChestplate(Material material) {
        return material == Material.LEATHER_CHESTPLATE ||
               material == Material.CHAINMAIL_CHESTPLATE ||
               material == Material.IRON_CHESTPLATE ||
               material == Material.GOLDEN_CHESTPLATE ||
               material == Material.DIAMOND_CHESTPLATE ||
               material == Material.NETHERITE_CHESTPLATE;
    }

    /**
     * Clean up secret elytra tracking when player quits
     */
    public void onPlayerQuit(UUID playerId) {
        playersWithSecretElytra.remove(playerId);
    }
}
