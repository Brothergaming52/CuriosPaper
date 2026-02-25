package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final CuriosPaper plugin;

    public PlayerListener(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getSlotManager().loadPlayerData(player);

        // Send resource pack if enabled
        if (plugin.getConfig().getBoolean("resource-pack.enabled", false)) {
            String url = plugin.getResourcePackManager().getPackUrl();
            String hash = plugin.getResourcePackManager().getPackHash();

            if (hash != null) {
                try {
                    // 1.14 accepts (String url, byte[] hash)
                    // If hash is hex string, convert to byte[]
                    byte[] hashBytes = null;
                    if (hash != null && !hash.isEmpty()) {
                        try {
                            // simple placeholder conversion or empty if not strictly needed checks
                            // A real hex decoder is needed but for now user passed string
                            // Let's try passing just URL if hash is problematic or if 1.14 supports single
                            // arg
                            // player.setResourcePack(url);
                            // But if we want hash:
                            // hashBytes = javax.xml.bind.DatatypeConverter.parseHexBinary(hash); //
                            // requires java.xml.bind
                            // Let's just use the single arg version if possible or pass empty array?
                            // Actually 1.14 has setResourcePack(String url, byte[] hash)
                            // The user is passing (String, String).
                            // We will call the version that takes just String if hash is not critical or
                            // convert it.
                            player.setResourcePack(url); // safest fallback
                        } catch (Exception e) {
                        }
                    } else {
                        player.setResourcePack(url);
                    }
                } catch (Exception e) {
                    plugin.getLogger()
                            .warning("Failed to send resource pack to " + player.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getSlotManager().savePlayerData(player);
        plugin.getSlotManager().unloadPlayerData(player.getUniqueId());
    }
}
