package org.bg52.curiospaper.resourcepack.host;

import org.bg52.curiospaper.CuriosPaper;

import java.io.File;
import java.util.*;

/**
 * Coordinates external resource pack host detection, capability verification,
 * dynamic event hooks, and pack injection.
 */
public class PackHostManager {

  private final CuriosPaper plugin;
  private final PackHostDetector detector;
  private final Map<PackHostPlugin, PackHostInjector> injectors;

  private PackHostPlugin desiredHost = PackHostPlugin.AUTO;
  private PackHostPlugin activeHost = null;

  private String lastInjectionStatus = null;
  private long lastInjectionTime = 0L;

  public PackHostManager(CuriosPaper plugin) {
    this.plugin = plugin;
    this.detector = new PackHostDetector(plugin);
    this.injectors = new EnumMap<>(PackHostPlugin.class);

    // Register built-in injectors (Synthetix is first priority)
    this.injectors.put(PackHostPlugin.SYNTHETIX, new SynthetixPackInjector(plugin));
    this.injectors.put(PackHostPlugin.RSPM, new RspmPackInjector(plugin));
    this.injectors.put(PackHostPlugin.NEXO, new NexoPackInjector(plugin));
    this.injectors.put(PackHostPlugin.ITEMSADDER, new ItemsAdderPackInjector(plugin));
    this.injectors.put(PackHostPlugin.ORAXEN, new OraxenPackInjector(plugin));
    this.injectors.put(PackHostPlugin.CRAFTENGINE, new CraftEnginePackInjector(plugin));
  }

  /**
   * Initializes host detection and registers dynamic event hooks.
   */
  public void initialize() {
    loadConfiguration();

    // Check if host plugin injection should be attempted
    boolean shouldAttempt = shouldAttemptInjection();

    if (shouldAttempt) {
      this.activeHost = detector.detectActiveHost(desiredHost);

      if (this.activeHost != null && this.activeHost != PackHostPlugin.NONE) {
        PackHostInjector injector = injectors.get(this.activeHost);
        if (injector != null) {
          injector.registerDynamicListeners();
          plugin.getLogger().info("[CuriosPaper-RP] Resource pack hosting delegated to "
              + activeHost.getDisplayName() + ". Embedded HTTP server will remain disabled.");
        }
      } else {
        if (desiredHost != PackHostPlugin.AUTO && desiredHost != PackHostPlugin.NONE) {
          plugin.getLogger().warning("[CuriosPaper-RP] Could not use desired pack host plugin '"
              + desiredHost.getDisplayName() + "'. Operating in fallback mode.");
        }
      }
    } else {
      this.activeHost = null;
    }
  }

  /**
   * Shuts down or clears injection states.
   */
  public void shutdown() {
    this.activeHost = null;
    this.lastInjectionStatus = null;
  }

  /**
   * Reloads configuration and refreshes host detection.
   */
  public void reload() {
    shutdown();
    initialize();
  }

  /**
   * Injects the newly generated final resource pack into the active host plugin.
   *
   * @param packZip The final resource-pack.zip.
   * @param buildDir The unzipped final resource pack directory.
   * @return true if successfully injected.
   */
  public boolean injectFinalPack(File packZip, File buildDir) {
    if (!isActive()) {
      return false;
    }

    PackHostInjector injector = injectors.get(activeHost);
    if (injector == null) {
      return false;
    }

    plugin.getLogger().info("[CuriosPaper-RP] Injecting final resource pack into " + activeHost.getDisplayName() + "...");
    boolean ok = injector.inject(packZip, buildDir);
    this.lastInjectionTime = System.currentTimeMillis();
    if (ok) {
      this.lastInjectionStatus = "Successfully injected into " + activeHost.getDisplayName();
      plugin.getLogger().info("[CuriosPaper-RP] Pack injection into " + activeHost.getDisplayName() + " completed successfully.");
    } else {
      this.lastInjectionStatus = "Failed to inject into " + activeHost.getDisplayName();
      plugin.getLogger().severe("[CuriosPaper-RP] Pack injection into " + activeHost.getDisplayName() + " encountered errors.");
    }
    return ok;
  }

  /**
   * Manually sets and forces injection into a specific host plugin.
   */
  public boolean forceInject(PackHostPlugin target, File packZip, File buildDir) {
    if (target == null || target == PackHostPlugin.NONE || target == PackHostPlugin.AUTO) {
      return false;
    }

    if (!detector.isPluginInstalled(target)) {
      plugin.getLogger().warning("[CuriosPaper-RP] Target plugin " + target.getDisplayName() + " is not installed.");
      return false;
    }

    PackHostInjector injector = injectors.get(target);
    if (injector != null) {
      injector.registerDynamicListeners();
      boolean ok = injector.inject(packZip, buildDir);
      if (ok) {
        this.activeHost = target;
        this.lastInjectionTime = System.currentTimeMillis();
        this.lastInjectionStatus = "Manually injected into " + target.getDisplayName();
      }
      return ok;
    }
    return false;
  }

  /**
   * Reads host configuration from config.yml.
   */
  private void loadConfiguration() {
    String hostStr = plugin.getConfig().getString("resource-pack.host-plugin", "AUTO");
    this.desiredHost = PackHostPlugin.fromString(hostStr);
  }

  /**
   * Determines whether host plugin detection and injection should be performed.
   */
  private boolean shouldAttemptInjection() {
    if (desiredHost == PackHostPlugin.NONE) {
      return false;
    }

    String mode = plugin.getConfig().getString("resource-pack.mode", "AUTO");
    if ("NONE".equalsIgnoreCase(mode) || "LINK".equalsIgnoreCase(mode)) {
      return false;
    }

    return true;
  }

  public boolean isActive() {
    return activeHost != null && activeHost != PackHostPlugin.NONE;
  }

  public PackHostPlugin getActiveHost() {
    return activeHost;
  }

  public PackHostPlugin getDesiredHost() {
    return desiredHost;
  }

  public PackHostDetector getDetector() {
    return detector;
  }

  public String getLastInjectionStatus() {
    return lastInjectionStatus;
  }

  public long getLastInjectionTime() {
    return lastInjectionTime;
  }

  public Map<PackHostPlugin, PackHostInjector> getInjectors() {
    return Collections.unmodifiableMap(injectors);
  }
}
