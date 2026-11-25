package org.bg52.curiospaper.listener;

import com.google.common.collect.Multimap;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.event.AccessoryEquipEvent;
import org.bg52.curiospaper.slot.SlotDefinition;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bg52.curiospaper.scheduler.PlatformScheduler;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise listener that applies and removes attribute modifiers and abilities
 * when accessories are equipped or unequipped in Curios slots.
 */
public class AbilityListener implements Listener {

    private final CuriosPaper plugin;
    private final Map<UUID, Set<String>> activeModifiers = new ConcurrentHashMap<>();
    private PlatformScheduler.TaskHandle reconciliationTask;

    private static final String MODIFIER_PREFIX = "curios_meta_";
    private static Constructor<AttributeModifier> modernConstructor = null;
    private static Constructor<AttributeModifier> legacyConstructor = null;

    static {
        try {
            modernConstructor = AttributeModifier.class.getConstructor(
                    NamespacedKey.class, double.class, AttributeModifier.Operation.class, EquipmentSlotGroup.class
            );
        } catch (Throwable t) {
            try {
                legacyConstructor = AttributeModifier.class.getConstructor(
                        UUID.class, String.class, double.class, AttributeModifier.Operation.class
                );
            } catch (Throwable ignored) {}
        }
    }

    public AbilityListener(CuriosPaper plugin) {
        this.plugin = plugin;
        startReconciliationTask();
    }

    public void startReconciliationTask() {
        // Runs every 5 seconds (100 ticks) to clean up orphaned modifiers
        this.reconciliationTask = PlatformScheduler.get().runGlobalTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlatformScheduler.get().runEntity(player, () -> {
                    if (player.isOnline()) {
                        reconcileModifiers(player);
                    }
                });
            }
        }, 100L, 100L);
    }

    public void shutdown() {
        if (reconciliationTask != null) {
            reconciliationTask.cancel();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeAllCuriosModifiers(player);
        }
        activeModifiers.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        if (!plugin.getConfig().getBoolean("item-engines.apply-item-meta-attributes", true)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack prev = event.getPreviousItem();
        ItemStack cur = event.getNewItem();
        String slotType = event.getSlotType();
        int subIndex = event.getSubIndex();

        // 1. Remove modifiers from previous item
        if (prev != null && !prev.getType().isAir()) {
            removeItemMetaAttributes(player, slotType, subIndex, prev);
        }

        // 2. Apply modifiers from new item
        if (cur != null && !cur.getType().isAir()) {
            applyItemMetaAttributes(player, slotType, subIndex, cur);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Load attributes after inventory loads
        PlatformScheduler.get().runEntityLater(player, () -> {
            if (player.isOnline()) {
                reapplyAllCuriosAttributes(player);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeAllCuriosModifiers(player);
        activeModifiers.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlatformScheduler.get().runEntityLater(player, () -> {
            if (player.isOnline()) {
                reapplyAllCuriosAttributes(player);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        PlatformScheduler.get().runEntityLater(player, () -> {
            if (player.isOnline()) {
                reapplyAllCuriosAttributes(player);
            }
        }, 2L);
    }

    public void reapplyAllCuriosAttributes(Player player) {
        removeAllCuriosModifiers(player);
        if (!plugin.getConfig().getBoolean("item-engines.apply-item-meta-attributes", true)) {
            return;
        }

        for (SlotDefinition def : plugin.getSlotRegistry().getAllSlots()) {
            List<ItemStack> items = plugin.getCuriosPaperAPI().getEquippedItems(player, def.getId());
            for (int sub = 0; sub < items.size(); sub++) {
                ItemStack item = items.get(sub);
                if (item != null && !item.getType().isAir()) {
                    applyItemMetaAttributes(player, def.getId(), sub, item);
                }
            }
        }
    }

    private void applyItemMetaAttributes(Player player, String slotType, int subIndex, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasAttributeModifiers()) return;

        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers == null || modifiers.isEmpty()) return;

        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            Attribute attr = entry.getKey();
            AttributeModifier mod = entry.getValue();
            AttributeInstance instance = player.getAttribute(attr);
            if (instance == null || mod == null) continue;

            String modifierKey = MODIFIER_PREFIX + slotType + "_" + subIndex + "_" + attr.name().toLowerCase(Locale.ROOT);
            AttributeModifier customMod = buildModifier(modifierKey, mod.getAmount(), mod.getOperation());
            if (customMod == null) continue;

            // Remove existing if present
            removeMatchingModifier(instance, modifierKey);

            instance.addModifier(customMod);
            activeModifiers.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(modifierKey);
        }
    }

    private void removeItemMetaAttributes(Player player, String slotType, int subIndex, ItemStack item) {
        String prefix = MODIFIER_PREFIX + slotType + "_" + subIndex + "_";
        for (Attribute attr : Attribute.values()) {
            try {
                AttributeInstance instance = player.getAttribute(attr);
                if (instance != null) {
                    removePrefixModifier(instance, prefix);
                }
            } catch (Throwable ignored) {}
        }
        Set<String> mods = activeModifiers.get(player.getUniqueId());
        if (mods != null) {
            mods.removeIf(k -> k.startsWith(prefix));
        }
    }

    private void removeMatchingModifier(AttributeInstance instance, String keyName) {
        List<AttributeModifier> toRemove = new ArrayList<>();
        for (AttributeModifier mod : instance.getModifiers()) {
            if (matchesKey(mod, keyName)) {
                toRemove.add(mod);
            }
        }
        for (AttributeModifier mod : toRemove) {
            instance.removeModifier(mod);
        }
    }

    private void removePrefixModifier(AttributeInstance instance, String prefix) {
        List<AttributeModifier> toRemove = new ArrayList<>();
        for (AttributeModifier mod : instance.getModifiers()) {
            if (matchesPrefix(mod, prefix)) {
                toRemove.add(mod);
            }
        }
        for (AttributeModifier mod : toRemove) {
            instance.removeModifier(mod);
        }
    }

    private boolean matchesKey(AttributeModifier mod, String keyName) {
        try {
            java.lang.reflect.Method m = mod.getClass().getMethod("getKey");
            Object keyObj = m.invoke(mod);
            if (keyObj instanceof NamespacedKey) {
                NamespacedKey nk = (NamespacedKey) keyObj;
                if (nk.getKey().equalsIgnoreCase(keyName)) return true;
            }
        } catch (Throwable ignored) {}

        return keyName.equalsIgnoreCase(mod.getName());
    }

    private boolean matchesPrefix(AttributeModifier mod, String prefix) {
        try {
            java.lang.reflect.Method m = mod.getClass().getMethod("getKey");
            Object keyObj = m.invoke(mod);
            if (keyObj instanceof NamespacedKey) {
                NamespacedKey nk = (NamespacedKey) keyObj;
                if (nk.getKey().startsWith(prefix)) return true;
            }
        } catch (Throwable ignored) {}

        return mod.getName() != null && mod.getName().startsWith(prefix);
    }

    private AttributeModifier buildModifier(String keyName, double amount, AttributeModifier.Operation operation) {
        try {
            if (modernConstructor != null) {
                NamespacedKey key = new NamespacedKey(plugin, keyName);
                return modernConstructor.newInstance(key, amount, operation, EquipmentSlotGroup.ANY);
            }
        } catch (Throwable ignored) {}

        try {
            if (legacyConstructor != null) {
                UUID uuid = UUID.nameUUIDFromBytes(keyName.getBytes());
                return legacyConstructor.newInstance(uuid, keyName, amount, operation);
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private void reconcileModifiers(Player player) {
        Set<String> tracked = activeModifiers.get(player.getUniqueId());
        if (tracked == null || tracked.isEmpty()) return;

        // Build valid prefixes currently worn
        Set<String> validPrefixes = new HashSet<>();
        for (SlotDefinition def : plugin.getSlotRegistry().getAllSlots()) {
            List<ItemStack> items = plugin.getCuriosPaperAPI().getEquippedItems(player, def.getId());
            for (int sub = 0; sub < items.size(); sub++) {
                ItemStack item = items.get(sub);
                if (item != null && !item.getType().isAir() && item.hasItemMeta() && item.getItemMeta().hasAttributeModifiers()) {
                    validPrefixes.add(MODIFIER_PREFIX + def.getId() + "_" + sub + "_");
                }
            }
        }

        // Remove any modifier that doesn't match an actively worn item
        for (Attribute attr : Attribute.values()) {
            try {
                AttributeInstance instance = player.getAttribute(attr);
                if (instance == null) continue;

                List<AttributeModifier> toRemove = new ArrayList<>();
                for (AttributeModifier mod : instance.getModifiers()) {
                    String name = mod.getName();
                    if (name != null && name.startsWith(MODIFIER_PREFIX)) {
                        boolean valid = false;
                        for (String validPfx : validPrefixes) {
                            if (name.startsWith(validPfx)) {
                                valid = true;
                                break;
                            }
                        }
                        if (!valid) {
                            toRemove.add(mod);
                        }
                    }
                }
                for (AttributeModifier mod : toRemove) {
                    instance.removeModifier(mod);
                }
            } catch (Throwable ignored) {}
        }
    }

    private void removeAllCuriosModifiers(Player player) {
        for (Attribute attr : Attribute.values()) {
            try {
                AttributeInstance instance = player.getAttribute(attr);
                if (instance == null) continue;

                List<AttributeModifier> toRemove = new ArrayList<>();
                for (AttributeModifier mod : instance.getModifiers()) {
                    String name = mod.getName();
                    if (name != null && name.startsWith(MODIFIER_PREFIX)) {
                        toRemove.add(mod);
                    }
                }
                for (AttributeModifier mod : toRemove) {
                    instance.removeModifier(mod);
                }
            } catch (Throwable ignored) {}
        }
    }
}
