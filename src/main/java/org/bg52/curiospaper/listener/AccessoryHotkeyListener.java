package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.config.ConfigManager;
import org.bg52.curiospaper.inventory.AccessoryGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AccessoryHotkeyListener implements Listener {

    private final CuriosPaper plugin;
    private final AccessoryGUI gui;

    // Track last sneak time for double sneak detection
    private final Map<UUID, Long> lastSneakTime = new HashMap<>();

    // Track sneak hold tasks
    private final Map<UUID, BukkitTask> sneakHoldTasks = new HashMap<>();

    private static final long DOUBLE_SNEAK_WINDOW_MS = 500; // 500ms window for double sneak

    public AccessoryHotkeyListener(CuriosPaper plugin, AccessoryGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        // Check if hotkey system is enabled
        if (!plugin.getConfigManager().isHotkeyEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if player has the correct hotbar slot selected
        // Bukkit uses 0-8 for hotbar, config uses 1-9
        int selectedSlot = player.getInventory().getHeldItemSlot(); // 0-8
        int configuredSlot = plugin.getConfigManager().getHotkeySlot() - 1; // Convert to 0-8

        if (selectedSlot != configuredSlot) {
            return;
        }

        ConfigManager.SneakType sneakType = plugin.getConfigManager().getHotkeySneakType();

        if (event.isSneaking()) {
            // Player started sneaking
            handleSneakStart(player, playerId, sneakType);
        } else {
            // Player stopped sneaking
            handleSneakStop(playerId);
        }
    }

    private void handleSneakStart(Player player, UUID playerId, ConfigManager.SneakType sneakType) {
        long currentTime = System.currentTimeMillis();

        switch (sneakType) {
            case SINGLE:
                // Open accessory GUI on single sneak
                openAccessoryGUI(player);
                break;

            case DOUBLE:
                // Check if this is a double sneak
                Long lastTime = lastSneakTime.get(playerId);
                if (lastTime != null && (currentTime - lastTime) <= DOUBLE_SNEAK_WINDOW_MS) {
                    // Double sneak detected!
                    openAccessoryGUI(player);
                    lastSneakTime.remove(playerId); // Reset to prevent triple sneak
                } else {
                    // First sneak, record time
                    lastSneakTime.put(playerId, currentTime);
                }
                break;

            case HOLD:
                // Start a task to check if player holds sneak long enough
                int holdDuration = plugin.getConfigManager().getSneakHoldDuration();
                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Check if player is still sneaking after duration
                    if (player.isSneaking()) {
                        openAccessoryGUI(player);
                    }
                    sneakHoldTasks.remove(playerId);
                }, holdDuration * 20L); // Convert seconds to ticks

                sneakHoldTasks.put(playerId, task);
                break;
        }
    }

    private void handleSneakStop(UUID playerId) {
        // Cancel any pending hold tasks
        BukkitTask task = sneakHoldTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private void openAccessoryGUI(Player player) {
        // Ensure player data is loaded
        if (!plugin.getSlotManager().hasPlayerData(player.getUniqueId())) {
            plugin.getSlotManager().loadPlayerData(player);
        }
        gui.openMainGUI(player);
    }

    // Cleanup method for when plugin disables
    public void cleanup() {
        // Cancel all pending tasks
        for (BukkitTask task : sneakHoldTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        sneakHoldTasks.clear();
        lastSneakTime.clear();
    }
}
