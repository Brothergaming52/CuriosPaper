package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.event.AccessoryEquipEvent;
import org.bg52.curiospaper.api.event.PlayerDeathCurioDropEvent;
import org.bg52.curiospaper.inventory.PlayerCuriosInventory;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bg52.curiospaper.scheduler.PlatformScheduler;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles player session lifecycle events: async data loading on join, persistence on quit,
 * and death behavior with support for third-party grave/death chest plugins via PlayerDeathCurioDropEvent.
 */
public class PlayerJoinQuitListener implements Listener {

    private final CuriosPaper plugin;

    public PlayerJoinQuitListener(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getStorageManager().loadPlayer(uuid).thenAccept(inv -> {
            PlatformScheduler.get().runEntity(player, () -> {
                if (player.isOnline()) {
                    plugin.getPacketModelManager().updatePlayerAccessories(player);

                    // Re-apply back slot elytra flight if equipped across any unlocked back slot
                    org.bg52.curiospaper.handler.ElytraBackSlotHandler.BackElytraSlot backSlot = plugin.getElytraBackSlotHandler().findBackSlotElytra(player);
                    if (backSlot != null) {
                        plugin.getElytraBackSlotHandler().handleElytraEquipped(player, backSlot.item());
                    }
                }
            });
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getElytraBackSlotHandler().handleElytraUnequipped(player);
        plugin.getPacketModelManager().despawnAllForPlayer(uuid);
        plugin.getStorageManager().unloadPlayer(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String keepType = plugin.getConfig().getString("features.keep-curio-inventory.type", "Auto");

        boolean keepCurios;
        if ("Always".equalsIgnoreCase(keepType)) {
            keepCurios = true;
        } else if ("Never".equalsIgnoreCase(keepType)) {
            keepCurios = false;
        } else {
            // Auto - Follow vanilla keepInventory gamerule or death event flag
            Boolean keepRule = player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY);
            keepCurios = event.getKeepInventory() || Boolean.TRUE.equals(keepRule);
        }

        if (keepCurios) {
            return;
        }

        PlayerCuriosInventory inv = plugin.getStorageManager().getInventory(player.getUniqueId());
        Map<String, List<ItemStack>> all = inv.getAllSlots();
        boolean changed = false;

        for (Map.Entry<String, List<ItemStack>> entry : all.entrySet()) {
            String slotType = entry.getKey();
            List<ItemStack> list = entry.getValue();
            if (list == null) continue;

            for (int i = 0; i < list.size(); i++) {
                ItemStack item = list.get(i);
                if (item != null && item.getType() != Material.AIR) {
                    // Fire PlayerDeathCurioDropEvent for grave plugins / soulbound items
                    PlayerDeathCurioDropEvent dropEvent = new PlayerDeathCurioDropEvent(player, slotType, i, item);
                    Bukkit.getPluginManager().callEvent(dropEvent);

                    if (!dropEvent.isCancelled()) {
                        // Drop in world
                        event.getDrops().add(item.clone());

                        // Fire AccessoryEquipEvent with UNEQUIP
                        AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
                                player, slotType, i, item, null, AccessoryEquipEvent.Action.UNEQUIP
                        );
                        Bukkit.getPluginManager().callEvent(equipEvent);

                        inv.setItem(slotType, i, null);
                        changed = true;
                    } else {
                        // Dropping was cancelled by a grave or death chest plugin!
                        if (!dropEvent.isKeepInCuriosSlot()) {
                            // Grave plugin captured the item into the grave
                            AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
                                    player, slotType, i, item, null, AccessoryEquipEvent.Action.UNEQUIP
                            );
                            Bukkit.getPluginManager().callEvent(equipEvent);

                            inv.setItem(slotType, i, null);
                            changed = true;
                        }
                    }
                }
            }
        }

        if (changed) {
            plugin.getStorageManager().savePlayer(player.getUniqueId());
        }
    }
}
