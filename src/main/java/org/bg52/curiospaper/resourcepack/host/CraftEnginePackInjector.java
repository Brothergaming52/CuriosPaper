package org.bg52.curiospaper.resourcepack.host;

import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * Handles injection of CuriosPaper's final resource pack into CraftEngine.
 * Synchronizes assets into plugins/CraftEngine/resources/curiospaper/resourcepack/ with pack.yml,
 * and dynamically injects into AsyncResourcePackGenerateEvent.
 */
public class CraftEnginePackInjector implements PackHostInjector, Listener {

  private final CuriosPaper plugin;
  private final File craftEnginePackDir;
  private final File craftEnginePackMeta;
  private boolean registered = false;

  public CraftEnginePackInjector(CuriosPaper plugin) {
    this.plugin = plugin;
    File ceDir = new File(plugin.getDataFolder().getParentFile(), "CraftEngine");
    File curiosCeDir = new File(ceDir, "resources/curiospaper");
    this.craftEnginePackDir = new File(curiosCeDir, "resourcepack");
    this.craftEnginePackMeta = new File(curiosCeDir, "pack.yml");
  }

  @Override
  public PackHostPlugin getHostPlugin() {
    return PackHostPlugin.CRAFTENGINE;
  }

  @Override
  public boolean inject(File packZip, File buildDir) {
    boolean success = false;
    try {
      if (buildDir != null && buildDir.exists() && buildDir.isDirectory()) {
        File assetsSource = new File(buildDir, "assets");
        File assetsTarget = new File(craftEnginePackDir, "assets");
        if (assetsSource.exists() && assetsSource.isDirectory()) {
          if (!assetsTarget.exists()) {
            assetsTarget.mkdirs();
          }
          PackFileUtils.copyRecursively(assetsSource, assetsTarget);
        }

        // Ensure pack.yml exists in the pack root
        if (!craftEnginePackMeta.exists()) {
          try (FileWriter writer = new FileWriter(craftEnginePackMeta)) {
            writer.write("enable: true\n");
            writer.write("namespace: \"curiospaper\"\n");
            writer.write("description: \"CuriosPaper Resource Pack\"\n");
          }
        }
        success = true;
        plugin.getLogger().info("[PackHost-CraftEngine] Successfully synced final resource pack assets to " + craftEnginePackDir.getPath());
      }
    } catch (Exception e) {
      plugin.getLogger().severe("[PackHost-CraftEngine] Failed to inject pack into CraftEngine resources: " + e.getMessage());
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
      Class<?> clazz = Class.forName("net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackGenerateEvent");
      if (Event.class.isAssignableFrom(clazz)) {
        Class<? extends Event> eventClass = (Class<? extends Event>) clazz;

        EventExecutor executor = (listener, event) -> {
          try {
            Method getFolderMethod = event.getClass().getMethod("resourcePackFolder");
            Object pathObj = getFolderMethod.invoke(event);
            if (pathObj instanceof Path) {
              File targetFolder = ((Path) pathObj).toFile();
              File buildDir = plugin.getPackRegistrationManager().getResourcesDir();
              if (buildDir.exists() && buildDir.isDirectory()) {
                File assetsSource = new File(buildDir, "assets");
                File assetsTarget = new File(targetFolder, "assets");
                if (assetsSource.exists() && assetsSource.isDirectory()) {
                  PackFileUtils.copyRecursively(assetsSource, assetsTarget);
                  plugin.getLogger().info("[PackHost-CraftEngine] Dynamically injected CuriosPaper assets into CraftEngine AsyncResourcePackGenerateEvent!");
                }
              }
            }
          } catch (Throwable t) {
            plugin.getLogger().warning("[PackHost-CraftEngine] Error injecting into AsyncResourcePackGenerateEvent: " + t.getMessage());
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
        plugin.getLogger().info("[PackHost-CraftEngine] Registered dynamic hook for CraftEngine AsyncResourcePackGenerateEvent.");
      }
    } catch (ClassNotFoundException ignored) {
    } catch (Throwable t) {
      plugin.getLogger().warning("[PackHost-CraftEngine] Could not register CraftEngine event listener: " + t.getMessage());
    }
  }
}
