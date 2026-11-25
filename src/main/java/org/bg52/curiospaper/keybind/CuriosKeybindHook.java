package org.bg52.curiospaper.keybind;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.scheduler.PlatformScheduler;
import org.bg52.synthetix.api.SynthetixAPI;
import org.bg52.synthetix.keybind.KeybindAction;
import org.bg52.synthetix.keybind.KeybindType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges CuriosPaper with Synthetix's universal keybind engine,
 * with fallback standalone listener support. Fully supports old 'features.hotkey' config keys.
 */
public class CuriosKeybindHook {

    private final CuriosPaper plugin;
    private StandaloneKeybindListener fallbackListener;

    public CuriosKeybindHook(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        boolean enabled = plugin.getConfig().getBoolean("features.hotkey.enabled",
                plugin.getConfig().getBoolean("keybind.enabled", true));
        if (!enabled) {
            return;
        }

        // Resolve hotkey settings (checking features.hotkey first, then keybind)
        String typeStr = "DOUBLE_SNEAK";
        if (plugin.getConfig().contains("features.hotkey.sneak-type")) {
            String sneakType = plugin.getConfig().getString("features.hotkey.sneak-type", "double").toLowerCase(Locale.ROOT);
            typeStr = switch (sneakType) {
                case "hold" -> "SNEAK_HOLD";
                case "single" -> "SNEAK_TAP";
                default -> "DOUBLE_SNEAK";
            };
        } else if (plugin.getConfig().contains("keybind.type")) {
            typeStr = plugin.getConfig().getString("keybind.type", "DOUBLE_SNEAK");
        }

        int hotbarSlot = plugin.getConfig().getInt("features.hotkey.slot",
                plugin.getConfig().getInt("keybind.hotbar-slot", 8));
        long cooldownMs = plugin.getConfig().getLong("keybind.cooldown-ms", 500);
        int holdDurationSeconds = plugin.getConfig().getInt("features.hotkey.sneak-hold-duration", 3);
        long holdDurationMs = holdDurationSeconds * 1000L;

        KeybindType keybindType;
        try {
            keybindType = KeybindType.valueOf(typeStr.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            keybindType = KeybindType.DOUBLE_SNEAK;
        }

        // Convert hotbar slot: 1-9 to 0-8 for Bukkit inventory slots (8 = 8th hotbar slot = index 7)
        int targetSlotIndex = (hotbarSlot >= 1 && hotbarSlot <= 9) ? hotbarSlot - 1 : (hotbarSlot >= 0 ? hotbarSlot : -1);

        // 1. Try to register with Synthetix Keybind Manager
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Synthetix") && SynthetixAPI.getOptional().isPresent()) {
                // Unregister default or conflicting curios_open actions in Synthetix so CuriosPaper's configured slot is strictly respected
                SynthetixAPI.get().getKeybindManager().unregisterKeybind("custom:curios_open");
                SynthetixAPI.get().getKeybindManager().unregisterKeybind("curios_open");
                try {
                    SynthetixAPI.get().getKeybindManager().deleteCustomKeybind("curios_open");
                } catch (Throwable ignored) {}

                KeybindAction action = KeybindAction.builder("curiospaper:open_menu", keybindType)
                        .hotbarSlot(targetSlotIndex)
                        .holdDurationMs(holdDurationMs)
                        .cooldownMs(cooldownMs)
                        .handler(ctx -> {
                            Player player = ctx.getPlayer();
                            if (player.hasPermission("curiospaper.use")) {
                                plugin.getCuriosGUI().open(player);
                            }
                        })
                        .build();

                SynthetixAPI.get().getKeybindManager().registerKeybind(action);
                plugin.getLogger().info("[Keybind] Registered Curios inventory keybind (" + keybindType + ", slot " + hotbarSlot + " [index " + targetSlotIndex + "]) with Synthetix Core Engine!");
                return;
            }
        } catch (Throwable ignored) {}

        // 2. Standalone Fallback Listener
        plugin.getLogger().info("[Keybind] Registering standalone keybind listener for Curios inventory (slot " + hotbarSlot + " [index " + targetSlotIndex + "]).");
        this.fallbackListener = new StandaloneKeybindListener(typeStr, targetSlotIndex, cooldownMs, holdDurationSeconds);
        Bukkit.getPluginManager().registerEvents(this.fallbackListener, plugin);
    }

    public void shutdown() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Synthetix") && SynthetixAPI.getOptional().isPresent()) {
                SynthetixAPI.get().getKeybindManager().unregisterKeybind("curiospaper:open_menu");
            }
        } catch (Throwable ignored) {}

        if (fallbackListener != null) {
            fallbackListener.clear();
        }
    }

    private class StandaloneKeybindListener implements Listener {
        private final String sneakMode;
        private final int targetSlotIndex;
        private final long cooldown;
        private final int holdDurationSeconds;
        private final Map<UUID, Long> lastSneakStart = new ConcurrentHashMap<>();
        private final Map<UUID, Integer> lastSneakSlot = new ConcurrentHashMap<>();
        private final Map<UUID, Long> lastOpenTimestamp = new ConcurrentHashMap<>();
        private final Map<UUID, PlatformScheduler.TaskHandle> sneakHoldTasks = new ConcurrentHashMap<>();

        public StandaloneKeybindListener(String sneakMode, int targetSlotIndex, long cooldown, int holdDurationSeconds) {
            this.sneakMode = sneakMode;
            this.targetSlotIndex = targetSlotIndex;
            this.cooldown = cooldown;
            this.holdDurationSeconds = holdDurationSeconds;
        }

        public void clear() {
            for (PlatformScheduler.TaskHandle task : sneakHoldTasks.values()) {
                if (task != null) task.cancel();
            }
            sneakHoldTasks.clear();
            lastSneakStart.clear();
            lastSneakSlot.clear();
            lastOpenTimestamp.clear();
        }

        @EventHandler(priority = EventPriority.NORMAL)
        public void onSneakToggle(PlayerToggleSneakEvent event) {
            Player player = event.getPlayer();
            if (!player.hasPermission("curiospaper.use")) return;

            int heldSlot = player.getInventory().getHeldItemSlot();
            UUID uuid = player.getUniqueId();

            // Strict Hotbar Slot Check: If configured slot is specified (0-8), held slot must match
            if (targetSlotIndex >= 0 && heldSlot != targetSlotIndex) {
                PlatformScheduler.TaskHandle task = sneakHoldTasks.remove(uuid);
                if (task != null) task.cancel();
                lastSneakStart.remove(uuid);
                lastSneakSlot.remove(uuid);
                return;
            }

            long now = System.currentTimeMillis();
            Long lastOpen = lastOpenTimestamp.get(uuid);
            if (lastOpen != null && (now - lastOpen) < cooldown) {
                return;
            }

            if (event.isSneaking()) {
                if ("single".equalsIgnoreCase(sneakMode) || "SNEAK_TAP".equalsIgnoreCase(sneakMode)) {
                    lastOpenTimestamp.put(uuid, now);
                    plugin.getCuriosGUI().open(player);
                } else if ("double".equalsIgnoreCase(sneakMode) || "DOUBLE_SNEAK".equalsIgnoreCase(sneakMode)) {
                    Long lastStart = lastSneakStart.get(uuid);
                    Integer prevSlot = lastSneakSlot.get(uuid);
                    long window = plugin.getConfig().getLong("keybind.timeout-ms", 500);

                    if (lastStart != null && (now - lastStart) <= window && (targetSlotIndex < 0 || (prevSlot != null && prevSlot == targetSlotIndex))) {
                        lastSneakStart.remove(uuid);
                        lastSneakSlot.remove(uuid);
                        lastOpenTimestamp.put(uuid, now);
                        plugin.getCuriosGUI().open(player);
                    } else {
                        lastSneakStart.put(uuid, now);
                        lastSneakSlot.put(uuid, heldSlot);
                    }
                } else if ("hold".equalsIgnoreCase(sneakMode) || "SNEAK_HOLD".equalsIgnoreCase(sneakMode)) {
                    PlatformScheduler.TaskHandle existing = sneakHoldTasks.remove(uuid);
                    if (existing != null) existing.cancel();

                    PlatformScheduler.TaskHandle task = PlatformScheduler.get().runEntityLater(player, () -> {
                        if (player.isOnline() && player.isSneaking() && (targetSlotIndex < 0 || player.getInventory().getHeldItemSlot() == targetSlotIndex)) {
                            lastOpenTimestamp.put(uuid, System.currentTimeMillis());
                            plugin.getCuriosGUI().open(player);
                        }
                        sneakHoldTasks.remove(uuid);
                    }, holdDurationSeconds * 20L);
                    sneakHoldTasks.put(uuid, task);
                }
            } else {
                // Player stopped sneaking
                PlatformScheduler.TaskHandle task = sneakHoldTasks.remove(uuid);
                if (task != null) task.cancel();
            }
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onSwapHand(PlayerSwapHandItemsEvent event) {
            if (!"SNEAK_SWAP_HAND".equalsIgnoreCase(sneakMode) && !"SWAP_HAND".equalsIgnoreCase(sneakMode)) {
                return;
            }

            Player player = event.getPlayer();
            if (!player.hasPermission("curiospaper.use")) return;

            boolean requireSneak = "SNEAK_SWAP_HAND".equalsIgnoreCase(sneakMode);
            if (requireSneak && !player.isSneaking()) return;

            if (targetSlotIndex >= 0 && player.getInventory().getHeldItemSlot() != targetSlotIndex) {
                return;
            }

            long now = System.currentTimeMillis();
            Long lastOpen = lastOpenTimestamp.get(player.getUniqueId());
            if (lastOpen != null && (now - lastOpen) < cooldown) {
                return;
            }

            event.setCancelled(true);
            lastOpenTimestamp.put(player.getUniqueId(), now);
            plugin.getCuriosGUI().open(player);
        }
    }
}
