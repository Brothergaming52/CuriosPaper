package org.bg52.curiospaper.resourcepack.host;

import org.bg52.curiospaper.CuriosPaper;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Handles injection of CuriosPaper's final resource pack into ResourcePackManager (RSPM).
 * Registers CuriosPaper via ResourcePackManagerAPI.registerLocalResourcePack
 * and syncs the compiled resource-pack.zip into plugins/ResourcePackManager/mixer/curiospaper.zip.
 */
public class RspmPackInjector implements PackHostInjector {

  private final CuriosPaper plugin;
  private final File rspmMixerDir;

  public RspmPackInjector(CuriosPaper plugin) {
    this.plugin = plugin;
    File rspmDir = new File(plugin.getDataFolder().getParentFile(), "ResourcePackManager");
    this.rspmMixerDir = new File(rspmDir, "mixer");
  }

  @Override
  public PackHostPlugin getHostPlugin() {
    return PackHostPlugin.RSPM;
  }

  @Override
  public boolean inject(File packZip, File buildDir) {
    boolean success = false;
    try {
      // 1. Copy final zip into RSPM mixer folder for fallback/direct merging
      if (packZip != null && packZip.exists()) {
        if (!rspmMixerDir.exists()) {
          rspmMixerDir.mkdirs();
        }
        File targetZip = new File(rspmMixerDir, "curiospaper.zip");
        PackFileUtils.copyRecursively(packZip, targetZip);
        success = true;
        plugin.getLogger().info("[PackHost-RSPM] Successfully synced final resource pack to " + targetZip.getPath());
      }

      // 2. Register with ResourcePackManagerAPI via reflection
      registerWithApi();

      // 3. Trigger RSPM pack remix
      triggerRspmReload();

    } catch (Exception e) {
      plugin.getLogger().severe("[PackHost-RSPM] Failed to inject pack into ResourcePackManager: " + e.getMessage());
      e.printStackTrace();
    }
    return success;
  }

  @Override
  public void registerDynamicListeners() {
    // RSPM uses API registration rather than Bukkit events
    registerWithApi();
  }

  private void registerWithApi() {
    try {
      Class<?> apiClass = Class.forName("com.magmaguy.resourcepackmanager.api.ResourcePackManagerAPI");
      Method registerMethod = apiClass.getMethod(
          "registerLocalResourcePack",
          String.class,
          String.class,
          boolean.class,
          boolean.class,
          boolean.class,
          String.class
      );

      // Relative path from the server's plugins directory
      String localPath = "CuriosPaper" + File.separator + "resource-pack.zip";
      registerMethod.invoke(null, "CuriosPaper", localPath, false, true, true, "curios reload");
      plugin.getLogger().info("[PackHost-RSPM] Successfully registered CuriosPaper pack with ResourcePackManagerAPI!");
    } catch (ClassNotFoundException ignored) {
    } catch (Throwable t) {
      plugin.getLogger().warning("[PackHost-RSPM] Could not register with ResourcePackManagerAPI: " + t.getMessage());
    }
  }

  private void triggerRspmReload() {
    try {
      Class<?> apiClass = Class.forName("com.magmaguy.resourcepackmanager.api.ResourcePackManagerAPI");
      Method reloadMethod = apiClass.getMethod("reloadResourcePack");
      reloadMethod.invoke(null);
      plugin.getLogger().info("[PackHost-RSPM] Triggered ResourcePackManager pack remix.");
    } catch (Throwable ignored) {
    }
  }
}
