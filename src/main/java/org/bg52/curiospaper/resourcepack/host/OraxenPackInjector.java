package org.bg52.curiospaper.resourcepack.host;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.scheduler.PlatformScheduler;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Handles injection of CuriosPaper's final resource pack into Oraxen.
 * 
 * Supports:
 * 1. Synchronizing compiled pack to plugins/Oraxen/pack/uploads/curiospaper.zip so Oraxen's
 *    PackMerger detects it on boot and full pack reloads.
 * 2. Dynamic injection and deep JSON model merging directly into OraxenPackGeneratedEvent's
 *    virtual file list (event.getOutput()), ensuring assets are immediately included in
 *    Oraxen's uploaded pack without requiring a server reboot.
 * 3. Dynamic runtime reload trigger via OraxenPack.reloadPack() or '/oraxen reload pack'
 *    when CuriosPaper's pack is modified during runtime.
 */
public class OraxenPackInjector implements PackHostInjector, Listener {

  private final CuriosPaper plugin;
  private final File oraxenUploadsDir;
  private boolean registered = false;
  private volatile boolean inPackGenerate = false;
  private volatile boolean serverLoaded = false;

  public OraxenPackInjector(CuriosPaper plugin) {
    this.plugin = plugin;
    File oraxenDir = new File(plugin.getDataFolder().getParentFile(), "Oraxen");
    this.oraxenUploadsDir = new File(oraxenDir, "pack/uploads");

    // Track when server tick loop has started
    PlatformScheduler.get().runGlobal(() -> {
      this.serverLoaded = true;
    });
  }

  @Override
  public PackHostPlugin getHostPlugin() {
    return PackHostPlugin.ORAXEN;
  }

  @Override
  public boolean inject(File packZip, File buildDir) {
    boolean success = false;
    try {
      if ((packZip != null && packZip.exists()) || (buildDir != null && buildDir.exists())) {
        if (!oraxenUploadsDir.exists()) {
          oraxenUploadsDir.mkdirs();
        }
        File targetZip = new File(oraxenUploadsDir, "curiospaper.zip");
        PackFileUtils.writeHostCleanZip(packZip, buildDir, targetZip);
        success = true;
        plugin.getLogger().info("[PackHost-Oraxen] Successfully synced final resource pack to " + targetZip.getPath());
      }

      // If injection occurred outside of OraxenPackGeneratedEvent and the server is running,
      // trigger Oraxen to reload so uploads/curiospaper.zip is packaged and uploaded
      if (!inPackGenerate && serverLoaded) {
        requestOraxenPackReload();
      }
    } catch (Exception e) {
      plugin.getLogger().severe("[PackHost-Oraxen] Failed to inject pack into Oraxen uploads: " + e.getMessage());
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
      Class<?> clazz = Class.forName("io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent");
      if (Event.class.isAssignableFrom(clazz)) {
        Class<? extends Event> eventClass = (Class<? extends Event>) clazz;

        EventExecutor executor = (listener, event) -> {
          inPackGenerate = true;
          try {
            // If Oraxen already merged CuriosPaper assets from uploads/curiospaper.zip, skip redundant injection!
            if (isCuriosAlreadyMerged(event)) {
              plugin.getLogger().info("[PackHost-Oraxen] Oraxen already merged curiospaper.zip from uploads; "
                  + "skipping redundant virtual injection.");
              return;
            }

            File buildDir = plugin.getPackRegistrationManager().getResourcesDir();
            if (buildDir == null || !buildDir.exists()) {
              return;
            }

            // Keep plugins/Oraxen/pack/uploads/curiospaper.zip clean and in sync
            if (!oraxenUploadsDir.exists()) {
              oraxenUploadsDir.mkdirs();
            }
            File targetZip = new File(oraxenUploadsDir, "curiospaper.zip");
            if (!targetZip.exists() || targetZip.length() == 0) {
              PackFileUtils.writeHostCleanZip(null, buildDir, targetZip);
            }

            // 5. Inject virtual files directly into Oraxen's current pack generation output
            injectIntoOraxenEvent(event, targetZip);
          } catch (Throwable t) {
            plugin.getLogger().warning("[PackHost-Oraxen] Error during OraxenPackGeneratedEvent injection: " + t.getMessage());
          } finally {
            inPackGenerate = false;
          }
        };

        plugin.getServer().getPluginManager().registerEvent(
            eventClass,
            this,
            EventPriority.NORMAL,
            executor,
            plugin,
            true
        );
        registered = true;
        plugin.getLogger().info("[PackHost-Oraxen] Registered dynamic hook for OraxenPackGeneratedEvent.");
      }
    } catch (ClassNotFoundException ignored) {
    } catch (Throwable t) {
      plugin.getLogger().warning("[PackHost-Oraxen] Could not register Oraxen event listener: " + t.getMessage());
    }
  }

  /**
   * Checks whether Oraxen already extracted and merged CuriosPaper pack files
   * from plugins/Oraxen/pack/uploads/curiospaper.zip.
   */
  private boolean isCuriosAlreadyMerged(Event event) {
    try {
      Method getOutputMethod = event.getClass().getMethod("getOutput");
      Object outputObj = getOutputMethod.invoke(event);
      if (!(outputObj instanceof List)) {
        return false;
      }
      List<?> output = (List<?>) outputObj;
      if (output.isEmpty()) {
        return false;
      }

      Class<?> vfClass = Class.forName("io.th0rgal.oraxen.utils.VirtualFile");
      Method getPathMethod = vfClass.getMethod("getPath");

      for (Object vf : output) {
        String path = (String) getPathMethod.invoke(vf);
        if (path != null && (path.startsWith("assets/curiospaper/") || path.startsWith("assets/probablybackpacks/"))) {
          return true;
        }
      }
    } catch (Throwable ignored) {
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private void injectIntoOraxenEvent(Event event, File packZip) {
    if (packZip == null || !packZip.exists()) {
      return;
    }

    try {
      Method getOutputMethod = event.getClass().getMethod("getOutput");
      Object outputObj = getOutputMethod.invoke(event);
      if (!(outputObj instanceof List)) {
        return;
      }
      List<Object> output = (List<Object>) outputObj;

      Class<?> vfClass = Class.forName("io.th0rgal.oraxen.utils.VirtualFile");
      Method getPathMethod = vfClass.getMethod("getPath");
      Method getInputStreamMethod = vfClass.getMethod("getInputStream");
      Method setInputStreamMethod = vfClass.getMethod("setInputStream", InputStream.class);
      Constructor<?> vfConstructor = vfClass.getConstructor(String.class, String.class, InputStream.class);

      // Index existing files in Oraxen's output
      Map<String, Object> existingFiles = new HashMap<>();
      for (Object vf : output) {
        String path = (String) getPathMethod.invoke(vf);
        if (path != null) {
          existingFiles.put(path, vf);
        }
      }

      int injectedCount = 0;
      int mergedCount = 0;

      try (ZipFile zf = new ZipFile(packZip)) {
        Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          if (entry.isDirectory()) {
            continue;
          }

          String entryPath = entry.getName().replace('\\', '/');

          // Preserve Oraxen's own pack.mcmeta and pack.png; strictly require assets/
          if ("pack.mcmeta".equalsIgnoreCase(entryPath) || "pack.png".equalsIgnoreCase(entryPath)
              || !entryPath.startsWith("assets/")) {
            continue;
          }

          byte[] entryBytes;
          try (InputStream in = zf.getInputStream(entry)) {
            entryBytes = in.readAllBytes();
          }

          if (existingFiles.containsKey(entryPath)) {
            Object existingVf = existingFiles.get(entryPath);
            InputStream existingIn = (InputStream) getInputStreamMethod.invoke(existingVf);
            byte[] existingBytes = existingIn != null ? existingIn.readAllBytes() : null;

            // If existing file is already identical, do not re-merge or duplicate!
            if (existingBytes != null && Arrays.equals(existingBytes, entryBytes)) {
              setInputStreamMethod.invoke(existingVf, new ByteArrayInputStream(existingBytes));
              continue;
            }

            // File conflict: merge JSON model / asset files if applicable
            if (entryPath.endsWith(".json")) {
              try {
                if (existingBytes != null) {
                  String existingStr = new String(existingBytes, StandardCharsets.UTF_8);
                  String newStr = new String(entryBytes, StandardCharsets.UTF_8);

                  JsonElement destElement = JsonParser.parseString(existingStr);
                  JsonElement srcElement = JsonParser.parseString(newStr);

                  if (destElement.isJsonObject() && srcElement.isJsonObject()) {
                    JsonObject destObj = destElement.getAsJsonObject();
                    JsonObject srcObj = srcElement.getAsJsonObject();
                    deepMerge(destObj, srcObj);

                    byte[] mergedBytes = destObj.toString().getBytes(StandardCharsets.UTF_8);
                    setInputStreamMethod.invoke(existingVf, new ByteArrayInputStream(mergedBytes));
                    mergedCount++;
                  } else {
                    setInputStreamMethod.invoke(existingVf, new ByteArrayInputStream(existingBytes));
                  }
                }
              } catch (Throwable mergeErr) {
                if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                  plugin.getLogger().warning("[PackHost-Oraxen] Could not merge JSON for " + entryPath + ": " + mergeErr.getMessage());
                }
              }
            } else if (existingBytes != null) {
              setInputStreamMethod.invoke(existingVf, new ByteArrayInputStream(existingBytes));
            }
          } else {
            // New file: inject into Oraxen virtual output list
            int lastSlash = entryPath.lastIndexOf('/');
            String parentFolder = lastSlash >= 0 ? entryPath.substring(0, lastSlash) : "";
            String name = lastSlash >= 0 ? entryPath.substring(lastSlash + 1) : entryPath;

            Object newVf = vfConstructor.newInstance(parentFolder, name, new ByteArrayInputStream(entryBytes));
            output.add(newVf);
            existingFiles.put(entryPath, newVf);
            injectedCount++;
          }
        }
      }

      plugin.getLogger().info("[PackHost-Oraxen] Successfully injected " + injectedCount
          + " resources and merged " + mergedCount + " JSON model(s) into Oraxen pack.");

    } catch (Throwable t) {
      plugin.getLogger().warning("[PackHost-Oraxen] Failed to inject resources into Oraxen: " + t.getMessage());
      if (plugin.getConfig().getBoolean("debug.enabled", false)) {
        t.printStackTrace();
      }
    }
  }

  private void deepMerge(JsonObject dest, JsonObject src) {
    for (Map.Entry<String, JsonElement> entry : src.entrySet()) {
      String key = entry.getKey();
      JsonElement value = entry.getValue();

      if (!dest.has(key)) {
        dest.add(key, value);
      } else {
        JsonElement destValue = dest.get(key);
        if (destValue.isJsonObject() && value.isJsonObject()) {
          deepMerge(destValue.getAsJsonObject(), value.getAsJsonObject());
        } else if (destValue.isJsonArray() && value.isJsonArray()) {
          JsonArray destArray = destValue.getAsJsonArray();
          for (JsonElement el : value.getAsJsonArray()) {
            if (!destArray.contains(el)) {
              destArray.add(el);
            }
          }
        }
      }
    }
  }

  private void requestOraxenPackReload() {
    PlatformScheduler.get().runGlobalLater(() -> {
      try {
        if (plugin.getServer().getPluginManager().isPluginEnabled("Oraxen")) {
          plugin.getLogger().info("[PackHost-Oraxen] Requesting Oraxen pack reload to apply updated Curios pack...");
          try {
            Class<?> oraxenPackClass = Class.forName("io.th0rgal.oraxen.api.OraxenPack");
            Method reloadMethod = oraxenPackClass.getMethod("reloadPack");
            reloadMethod.invoke(null);
          } catch (Throwable t) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "oraxen reload pack");
          }
        }
      } catch (Throwable t) {
        plugin.getLogger().warning("[PackHost-Oraxen] Could not trigger Oraxen pack reload: " + t.getMessage());
      }
    }, 20L);
  }
}
