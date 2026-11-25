package org.bg52.curiospaper.resourcepack.host;

import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Handles injection of CuriosPaper's final resource pack into ItemsAdder.
 * Supports content pack synchronization into plugins/ItemsAdder/contents/curiospaper/resourcepack/
 * and runtime injection during ItemsAdderPackCompressedEvent.
 */
public class ItemsAdderPackInjector implements PackHostInjector, Listener {

  private final CuriosPaper plugin;
  private final File iaContentPackDir;
  private boolean registered = false;

  public ItemsAdderPackInjector(CuriosPaper plugin) {
    this.plugin = plugin;
    File iaDir = new File(plugin.getDataFolder().getParentFile(), "ItemsAdder");
    this.iaContentPackDir = new File(iaDir, "contents/curiospaper/resourcepack");
  }

  @Override
  public PackHostPlugin getHostPlugin() {
    return PackHostPlugin.ITEMSADDER;
  }

  @Override
  public boolean inject(File packZip, File buildDir) {
    boolean success = false;
    try {
      if (buildDir != null && buildDir.exists() && buildDir.isDirectory()) {
        File assetsSource = new File(buildDir, "assets");
        File assetsTarget = new File(iaContentPackDir, "assets");
        if (assetsSource.exists() && assetsSource.isDirectory()) {
          if (!assetsTarget.exists()) {
            assetsTarget.mkdirs();
          }
          PackFileUtils.copyRecursively(assetsSource, assetsTarget);
          success = true;
          plugin.getLogger().info("[PackHost-ItemsAdder] Successfully synced final resource pack assets to " + assetsTarget.getPath());
        }
      }
    } catch (Exception e) {
      plugin.getLogger().severe("[PackHost-ItemsAdder] Failed to inject pack into ItemsAdder contents: " + e.getMessage());
      e.printStackTrace();
    }
    return success;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void registerDynamicListeners() {
    if (registered) {
      return;
    }

    try {
      Class<?> clazz = Class.forName("dev.lone.itemsadder.api.Events.ItemsAdderPackCompressedEvent");
      if (Event.class.isAssignableFrom(clazz)) {
        Class<? extends Event> eventClass = (Class<? extends Event>) clazz;

        EventExecutor executor = (listener, event) -> {
          try {
            Method getEntryMethod = event.getClass().getMethod("getEntry", String.class);
            Method setEntryMethod = event.getClass().getMethod("setEntry", String.class, byte[].class);

            // 1. Check if ItemsAdder already merged/loaded CuriosPaper content from contents folder
            boolean alreadyMerged = false;
            try {
              if (getEntryMethod.invoke(event, "assets/curiospaper/items/necklace_slot.json") != null
                  || getEntryMethod.invoke(event, "assets/curiospaper/models/item/necklace_slot.json") != null) {
                alreadyMerged = true;
              }
            } catch (Throwable ignored) {}

            if (alreadyMerged) {
              plugin.getLogger().info("[PackHost-ItemsAdder] ItemsAdder already loaded CuriosPaper assets from contents folder; skipping redundant event injection.");
              return;
            }

            File resDir = plugin.getPackRegistrationManager().getResourcesDir();
            if (resDir != null && resDir.exists()) {
              Map<String, byte[]> entries = PackFileUtils.readDirectoryEntries(resDir);
              int injectedCount = 0;
              for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                String path = entry.getKey();

                // CRITICAL: NEVER overwrite pack.mcmeta or pack.png!
                if (path.equalsIgnoreCase("pack.mcmeta") || path.equalsIgnoreCase("pack.png") || !path.startsWith("assets/")) {
                  continue;
                }

                // If ItemsAdder already has this entry (e.g. converted by ItemsAdder or present in atlas), do not overwrite it
                Object existing = getEntryMethod.invoke(event, path);
                if (existing != null) {
                  continue;
                }

                setEntryMethod.invoke(event, path, entry.getValue());
                injectedCount++;
              }
              plugin.getLogger().info("[PackHost-ItemsAdder] Injected " + injectedCount + " CuriosPaper entries into ItemsAdderPackCompressedEvent!");
            }
          } catch (Throwable t) {
            plugin.getLogger().warning("[PackHost-ItemsAdder] Error injecting into ItemsAdderPackCompressedEvent: " + t.getMessage());
          }
        };

        plugin.getServer().getPluginManager().registerEvent(
            eventClass,
            this,
            EventPriority.HIGH,
            executor,
            plugin,
            true
        );
        registered = true;
        plugin.getLogger().info("[PackHost-ItemsAdder] Registered dynamic hook for ItemsAdderPackCompressedEvent.");
      }
    } catch (ClassNotFoundException ignored) {
    } catch (Throwable t) {
      plugin.getLogger().warning("[PackHost-ItemsAdder] Could not register ItemsAdder event listener: " + t.getMessage());
    }
  }
}
