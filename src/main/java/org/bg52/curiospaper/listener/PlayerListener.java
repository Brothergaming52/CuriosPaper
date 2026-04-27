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

      try {
        // Append hash as query param to bust client cache on pack rebuild.
        // Uses single-arg setResourcePack(String) which works on all versions (1.14+).
        if (hash != null && !hash.isEmpty()) {
          url = url + "?v=" + hash;
        }
        player.setResourcePack(url);
      } catch (Exception e) {
        plugin.getLogger()
            .warning("Failed to send resource pack to " + player.getName() + ": " + e.getMessage());
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
