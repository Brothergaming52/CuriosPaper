package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerListener implements Listener {
  private final CuriosPaper plugin;

  public PlayerListener(CuriosPaper plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    plugin.getSlotManager().loadPlayerData(player);

    // Send resource pack if mode is not NONE
    org.bg52.curiospaper.resourcepack.ResourcePackManager.HostingMode mode = plugin.getResourcePackManager().getHostingMode();
    if (mode != org.bg52.curiospaper.resourcepack.ResourcePackManager.HostingMode.NONE) {
      String url = plugin.getResourcePackManager().getPackUrl();
      if (url == null || url.isEmpty()) {
        plugin.getLogger().warning("Resource pack mode is set to LINK, but resource-pack.url is empty!");
        return;
      }
      String hash = plugin.getResourcePackManager().getPackHash();

      try {
        // Append hash as query param to bust client cache on pack rebuild.
        // Uses single-arg setResourcePack(String) which works on all versions (1.14+).
        if (hash != null && !hash.isEmpty()) {
          if (url.contains("?")) {
            url = url + "&v=" + hash;
          } else {
            url = url + "?v=" + hash;
          }
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

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    ItemStack item = event.getItemInHand();
    if (item == null || !item.hasItemMeta()) {
      return;
    }
    NamespacedKey itemIdKey = plugin.getCuriosPaperAPI().getItemIdKey();
    PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
    String curiosId = pdc.get(itemIdKey, PersistentDataType.STRING);
    if (curiosId != null && !curiosId.isEmpty()) {
      org.bg52.curiospaper.data.ItemData itemData = plugin.getCuriosPaperAPI().getItemData(curiosId);
      if (itemData != null && !itemData.isPlaceable()) {
        event.setCancelled(true);
      }
    }
  }
}
