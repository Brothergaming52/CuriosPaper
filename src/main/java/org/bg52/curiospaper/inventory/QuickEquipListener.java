package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.event.AccessoryEquipEvent;
import org.bg52.curiospaper.slot.SlotDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Listener that allows players to shift + right-click with an accessory in hand to instantly equip it.
 */
public class QuickEquipListener implements Listener {

    private final CuriosPaper plugin;

    public QuickEquipListener(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        boolean quickEquipEnabled = plugin.getConfig().getBoolean("features.quick-equip-shift-right-click",
                plugin.getConfig().getBoolean("features.quick-equip-right-click", true));
        if (!quickEquipEnabled) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Shift + Right Click is strictly required
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        // Only handle main hand to prevent double triggers
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack held = event.getItem();
        if (held == null || held.getType() == Material.AIR) {
            return;
        }

        // If shift right-clicking on a block with a placeable block item, prioritize block placement!
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && isPlaceableBlock(held)) {
            return;
        }

        String targetSlot = plugin.getItemEngineManager().resolveCuriosSlot(held);
        if (targetSlot == null) {
            return;
        }

        PlayerCuriosInventory inv = plugin.getStorageManager().getInventory(player.getUniqueId());
        if (inv == null) {
            return;
        }

        // Support comma-separated multi-slot tags (e.g. "ring, charm")
        String[] candidateSlots = targetSlot.split(",\\s*");
        String chosenSlot = null;
        SlotDefinition chosenSlotDef = null;
        int emptySubIndex = -1;

        for (String candidate : candidateSlots) {
            SlotDefinition def = plugin.getSlotRegistry().getSlot(candidate.trim());
            if (def != null) {
                int idx = inv.getFirstEmptySlot(def, player);
                if (idx >= 0) {
                    chosenSlot = def.getId();
                    chosenSlotDef = def;
                    emptySubIndex = idx;
                    break;
                } else if (chosenSlotDef == null) {
                    chosenSlotDef = def;
                }
            }
        }

        if (emptySubIndex < 0 || chosenSlot == null || chosenSlotDef == null) {
            plugin.getLanguageManager().sendMessage(player, "quick-equip.no-slots");
            event.setCancelled(true);
            return; // No open slot available
        }

        // Valid quick equip!
        event.setCancelled(true);

        ItemStack toEquip = held.clone();
        toEquip.setAmount(1);

        inv.setItem(chosenSlot, emptySubIndex, toEquip);

        AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
                player, chosenSlot, emptySubIndex, null, toEquip, AccessoryEquipEvent.Action.EQUIP
        );
        Bukkit.getPluginManager().callEvent(equipEvent);
        if (equipEvent.isCancelled()) {
            inv.setItem(chosenSlot, emptySubIndex, null);
            return;
        }

        // Deduct 1 from player's hand
        if (held.getAmount() > 1) {
            held.setAmount(held.getAmount() - 1);
            player.getInventory().setItemInMainHand(held);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        plugin.getCuriosGUI().playEquipSound(player);
        plugin.getItemEngineManager().notifyEquip(player, toEquip);
        plugin.getPacketModelManager().updatePlayerAccessories(player);

        String itemName = toEquip.hasItemMeta() && toEquip.getItemMeta().hasDisplayName()
                ? toEquip.getItemMeta().getDisplayName() : toEquip.getType().name();
        plugin.getLanguageManager().sendMessage(player, "quick-equip.equipped", "item", itemName, "slot", chosenSlotDef.getName());

        // Async save
        plugin.getStorageManager().savePlayer(player.getUniqueId());
    }

    private boolean isPlaceableBlock(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getType().isBlock()) return true;

        // Check Synthetix Custom Block
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Synthetix")) {
                String blockId = org.bg52.synthetix.block.CustomBlockManager.getBlockIdFromItem(item);
                if (blockId != null) {
                    var synth = org.bg52.synthetix.api.SynthetixAPI.getOptional();
                    if (synth.isPresent() && synth.get().getCustomBlockRegistry() != null) {
                        if (synth.get().getCustomBlockRegistry().has(blockId)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Check PDC keys directly as fallback
        if (item.hasItemMeta()) {
            org.bukkit.persistence.PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(new org.bukkit.NamespacedKey("synthetix", "block_id"), org.bukkit.persistence.PersistentDataType.STRING)) {
                return true;
            }
            if (pdc.has(new org.bukkit.NamespacedKey("probablybackpacks", "backpack_uuid"), org.bukkit.persistence.PersistentDataType.STRING)) {
                return true;
            }
            if (pdc.has(new org.bukkit.NamespacedKey("probablybackpacks", "backpack_id"), org.bukkit.persistence.PersistentDataType.STRING)) {
                return true;
            }
        }
        return false;
    }
}

