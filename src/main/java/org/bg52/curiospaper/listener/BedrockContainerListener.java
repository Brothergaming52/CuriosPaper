package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.inventory.BedrockAnvilGUI;
import org.bg52.curiospaper.inventory.BedrockSmithingGUI;
import org.bg52.curiospaper.util.BedrockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener that intercepts Anvil and Smithing Table interactions for Bedrock Edition players
 * and opens custom 3-row Chest GUIs instead of vanilla container windows.
 */
public class BedrockContainerListener implements Listener {

    private final CuriosPaper plugin;
    private final BedrockAnvilGUI anvilGUI;
    private final BedrockSmithingGUI smithingGUI;

    public BedrockContainerListener(CuriosPaper plugin, BedrockAnvilGUI anvilGUI, BedrockSmithingGUI smithingGUI) {
        this.plugin = plugin;
        this.anvilGUI = anvilGUI;
        this.smithingGUI = smithingGUI;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();

        if (!BedrockUtil.isBedrockPlayer(player)) {
            return;
        }

        Material type = block.getType();
        if (isAnvilBlock(type)) {
            if (plugin.getConfig().getBoolean("features.use-custom-anvil-gui", true)) {
                event.setCancelled(true);
                anvilGUI.open(player);
            }
        } else if (type == Material.SMITHING_TABLE) {
            if (plugin.getConfig().getBoolean("features.use-custom-smithing-gui", true)) {
                event.setCancelled(true);
                smithingGUI.open(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (!BedrockUtil.isBedrockPlayer(player)) {
            return;
        }

        InventoryType type = event.getInventory().getType();
        if (type == InventoryType.ANVIL) {
            if (!plugin.getConfig().getBoolean("features.use-custom-anvil-gui", true)) {
                return;
            }
            if (player.hasMetadata("curiospaper_bypass_anvil")) {
                player.removeMetadata("curiospaper_bypass_anvil", plugin);
                return;
            }
            // Only cancel if it's not our own custom Bedrock anvil GUI
            if (event.getInventory().getHolder() != anvilGUI) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> anvilGUI.open(player));
            }
        } else if ("SMITHING".equals(type.name())) {
            if (!plugin.getConfig().getBoolean("features.use-custom-smithing-gui", true)) {
                return;
            }
            if (player.hasMetadata("curiospaper_bypass_smithing")) {
                player.removeMetadata("curiospaper_bypass_smithing", plugin);
                return;
            }
            if (event.getInventory().getHolder() != smithingGUI) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> smithingGUI.open(player));
            }
        }
    }

    private boolean isAnvilBlock(Material material) {
        if (material == Material.ANVIL) return true;
        try {
            if (material == Material.valueOf("CHIPPED_ANVIL") || material == Material.valueOf("DAMAGED_ANVIL")) {
                return true;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return false;
    }
}
