package org.bg52.curiospaper.resourcepack.host;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.scheduler.PlatformScheduler;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Handles injection of CuriosPaper's final resource pack into Nexo.
 * Supports pre-generation synchronization into plugins/Nexo/pack/external_packs/,
 * dynamic injection during NexoPrePackGenerateEvent, runtime merging via
 * NexoPack.mergePackFromZip, and automatic '/nexo reload pack' invocation
 * when packs are rebuilt at runtime.
 */
public class NexoPackInjector implements PackHostInjector, Listener {

  private final CuriosPaper plugin;
  private final File nexoExternalPacksDir;
  private boolean registered = false;
  private volatile boolean inPrePackGenerate = false;
  private volatile boolean serverLoaded = false;

  public NexoPackInjector(CuriosPaper plugin) {
    this.plugin = plugin;
    File nexoDir = new File(plugin.getDataFolder().getParentFile(), "Nexo");
    this.nexoExternalPacksDir = new File(nexoDir, "pack/external_packs");

    // Track when server tick loop has started
    PlatformScheduler.get().runGlobal(() -> {
      this.serverLoaded = true;
    });
  }

  @Override
  public PackHostPlugin getHostPlugin() {
    return PackHostPlugin.NEXO;
  }

  @Override
  public boolean inject(File packZip, File buildDir) {
    boolean success = false;
    try {
      if ((packZip != null && packZip.exists()) || (buildDir != null && buildDir.exists())) {
        if (!nexoExternalPacksDir.exists()) {
          nexoExternalPacksDir.mkdirs();
        }
        File targetZip = new File(nexoExternalPacksDir, "curiospaper.zip");
        PackFileUtils.writeHostCleanZip(packZip, buildDir, targetZip);
        success = true;
        plugin.getLogger().info("[PackHost-Nexo] Successfully synced final resource pack to " + targetZip.getPath());
      }

      // Try runtime dynamic merge via NexoPack.mergePackFromZip(File)
      if (packZip != null && packZip.exists()) {
        try {
          Class<?> nexoPackClass = Class.forName("com.nexomc.nexo.api.NexoPack");
          Method mergeMethod = nexoPackClass.getMethod("mergePackFromZip", File.class);
          mergeMethod.invoke(null, packZip);
          plugin.getLogger().info("[PackHost-Nexo] Successfully merged CuriosPaper pack into Nexo runtime pack!");
        } catch (Throwable t) {
          if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info("[PackHost-Nexo] Note: Runtime mergePackFromZip skipped (" + t.getMessage() + "), external_packs sync is active.");
          }
        }
      }

      // If injection occurred outside of NexoPrePackGenerateEvent and the server is running,
      // trigger Nexo to rebuild its pack so external_packs/curiospaper.zip is packaged and served
      if (!inPrePackGenerate && serverLoaded) {
        requestNexoPackReload();
      }
    } catch (Exception e) {
      plugin.getLogger().severe("[PackHost-Nexo] Failed to inject pack into Nexo: " + e.getMessage());
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
      Class<?> clazz = Class.forName("com.nexomc.nexo.api.events.resourcepack.NexoPrePackGenerateEvent");
      if (Event.class.isAssignableFrom(clazz)) {
        Class<? extends Event> eventClass = (Class<? extends Event>) clazz;

        EventExecutor executor = (listener, event) -> {
          inPrePackGenerate = true;
          try {
            File resDir = plugin.getPackRegistrationManager().getResourcesDir();
            if (resDir != null && resDir.exists()) {
              if (!nexoExternalPacksDir.exists()) {
                nexoExternalPacksDir.mkdirs();
              }
              File targetZip = new File(nexoExternalPacksDir, "curiospaper.zip");
              PackFileUtils.writeHostCleanZip(null, resDir, targetZip);
              plugin.getLogger().info("[PackHost-Nexo] Synced CuriosPaper pack to " + targetZip.getPath() + " for Nexo generation.");

              // Invoke addResourcePack(File) for Nexo 1.25+ compatibility
              try {
                Method addResourcePack = event.getClass().getMethod("addResourcePack", File.class);
                addResourcePack.invoke(event, targetZip);
                plugin.getLogger().info("[PackHost-Nexo] Successfully injected CuriosPaper pack into NexoPrePackGenerateEvent!");
              } catch (NoSuchMethodException ignored) {
              }
            }
          } catch (Throwable t) {
            plugin.getLogger().warning("[PackHost-Nexo] Error preparing pack for NexoPrePackGenerateEvent: " + t.getMessage());
          } finally {
            inPrePackGenerate = false;
          }
        };

        // Register at LOWEST priority to execute first before Nexo begins gathering pack files
        plugin.getServer().getPluginManager().registerEvent(
            eventClass,
            this,
            EventPriority.LOWEST,
            executor,
            plugin,
            true
        );
        registered = true;
        plugin.getLogger().info("[PackHost-Nexo] Registered dynamic hook for NexoPrePackGenerateEvent (Priority: LOWEST).");
      }
    } catch (ClassNotFoundException ignored) {
    } catch (Throwable t) {
      plugin.getLogger().warning("[PackHost-Nexo] Could not register Nexo event listener: " + t.getMessage());
    }
  }

  private void requestNexoPackReload() {
    PlatformScheduler.get().runGlobalLater(() -> {
      try {
        if (plugin.getServer().getPluginManager().isPluginEnabled("Nexo")) {
          plugin.getLogger().info("[PackHost-Nexo] Requesting Nexo pack reload to apply updated external pack...");
          plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "nexo reload pack");
        }
      } catch (Throwable t) {
        plugin.getLogger().warning("[PackHost-Nexo] Could not execute '/nexo reload pack': " + t.getMessage());
      }
    }, 10L);
  }
}
