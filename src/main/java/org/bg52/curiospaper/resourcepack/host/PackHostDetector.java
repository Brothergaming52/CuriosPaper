package org.bg52.curiospaper.resourcepack.host;

import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Detects whether external plugins are installed, whether they are hosting a resource pack,
 * and whether they allow custom resource pack generation and combination.
 */
public class PackHostDetector {

  private final CuriosPaper plugin;
  private final File pluginsDir;

  public PackHostDetector(CuriosPaper plugin) {
    this.plugin = plugin;
    this.pluginsDir = plugin.getDataFolder().getParentFile();
  }

  /**
   * Checks whether the target plugin is installed and enabled on the server.
   */
  public boolean isPluginInstalled(PackHostPlugin host) {
    if (host == null || host == PackHostPlugin.NONE || host == PackHostPlugin.AUTO) {
      return false;
    }
    return Bukkit.getPluginManager().isPluginEnabled(host.getPluginName());
  }

  /**
   * Checks if the given plugin is currently configured and actively hosting / generating a resource pack.
   */
  public boolean isHostingPack(PackHostPlugin host) {
    if (!isPluginInstalled(host)) {
      return false;
    }

    try {
      switch (host) {
        case SYNTHETIX:
          return checkSynthetixHosting();
        case RSPM:
          return checkRspmHosting();
        case NEXO:
          return checkNexoHosting();
        case ITEMSADDER:
          return checkItemsAdderHosting();
        case ORAXEN:
          return checkOraxenHosting();
        case CRAFTENGINE:
          return checkCraftEngineHosting();
        default:
          return false;
      }
    } catch (Throwable t) {
      if (plugin.getConfig().getBoolean("debug.enabled", false)) {
        plugin.getLogger().warning("[PackHostDetector] Error checking hosting status for " + host.getDisplayName() + ": " + t.getMessage());
      }
      return true;
    }
  }

  /**
   * Checks if the given plugin allows custom resource pack generation and combination.
   */
  public boolean allowsCustomRpCombination(PackHostPlugin host) {
    if (!isPluginInstalled(host)) {
      return false;
    }

    switch (host) {
      case SYNTHETIX:
        return true;

      case RSPM:
        try {
          return hasClass("com.magmaguy.resourcepackmanager.api.ResourcePackManagerAPI")
              || new File(pluginsDir, "ResourcePackManager").isDirectory();
        } catch (Throwable ignored) {
          return true;
        }

      case NEXO:
        try {
          File nexoConfig = new File(pluginsDir, "Nexo/config.yml");
          if (nexoConfig.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(nexoConfig);
            if (!yaml.getBoolean("Pack.generation.generate_pack", true)) {
              return false;
            }
          }
          return hasClass("com.nexomc.nexo.api.NexoPack")
              || hasClass("com.nexomc.nexo.api.events.resourcepack.NexoPrePackGenerateEvent")
              || new File(pluginsDir, "Nexo").isDirectory();
        } catch (Throwable ignored) {
          return true;
        }

      case ITEMSADDER:
        try {
          return hasClass("dev.lone.itemsadder.api.Events.ItemsAdderPackCompressedEvent")
              || new File(pluginsDir, "ItemsAdder").isDirectory();
        } catch (Throwable ignored) {
          return true;
        }

      case ORAXEN:
        try {
          return hasClass("io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent")
              || new File(pluginsDir, "Oraxen").isDirectory();
        } catch (Throwable ignored) {
          return true;
        }

      case CRAFTENGINE:
        try {
          return hasClass("net.momirealms.craftengine.bukkit.api.event.ResourcePackGenerateEvent")
              || new File(pluginsDir, "CraftEngine").isDirectory();
        } catch (Throwable ignored) {
          return true;
        }

      default:
        return false;
    }
  }

  /**
   * Detects the best active host plugin according to desired preference and installed state.
   */
  public PackHostPlugin detectActiveHost(PackHostPlugin desired) {
    if (desired == PackHostPlugin.NONE) {
      return null;
    }

    if (desired != null && desired != PackHostPlugin.AUTO) {
      if (!isPluginInstalled(desired)) {
        plugin.getLogger().warning("[CuriosPaper-RP] Desired pack host plugin '" + desired.getDisplayName()
            + "' is specified in config, but the plugin is not installed or enabled!");
        return null;
      }

      if (!isHostingPack(desired)) {
        plugin.getLogger().warning("[CuriosPaper-RP] Desired pack host plugin '" + desired.getDisplayName()
            + "' is enabled, but does not appear to be hosting a resource pack in its config!");
        return null;
      }

      if (!allowsCustomRpCombination(desired)) {
        plugin.getLogger().warning("[CuriosPaper-RP] Desired pack host plugin '" + desired.getDisplayName()
            + "' is active, but does not allow custom resource pack generation and combination!");
        return null;
      }

      return desired;
    }

    // AUTO mode: check candidate plugins in priority order (Synthetix is #1)
    PackHostPlugin[] candidates = {
        PackHostPlugin.SYNTHETIX,
        PackHostPlugin.RSPM,
        PackHostPlugin.NEXO,
        PackHostPlugin.ITEMSADDER,
        PackHostPlugin.ORAXEN,
        PackHostPlugin.CRAFTENGINE
    };

    for (PackHostPlugin candidate : candidates) {
      if (isPluginInstalled(candidate) && isHostingPack(candidate) && allowsCustomRpCombination(candidate)) {
        plugin.getLogger().info("[CuriosPaper-RP] Auto-detected resource pack host plugin: " + candidate.getDisplayName());
        return candidate;
      }
    }

    return null;
  }

  private boolean checkSynthetixHosting() {
    return Bukkit.getPluginManager().isPluginEnabled("Synthetix");
  }

  private boolean checkNexoHosting() {
    File file = new File(pluginsDir, "Nexo/config.yml");
    if (file.exists()) {
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
      boolean genPack = yaml.getBoolean("Pack.generation.generate_pack", true);
      boolean selfHost = yaml.getBoolean("Pack.hosting.self_host.enabled", false);
      boolean externalHost = yaml.getBoolean("Pack.hosting.external_host.enabled", false);
      return genPack && (selfHost || externalHost);
    }
    return true;
  }

  private boolean checkItemsAdderHosting() {
    File file = new File(pluginsDir, "ItemsAdder/config.yml");
    if (file.exists()) {
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
      boolean noHost = yaml.getBoolean("resource-pack.hosting.no-host.enabled", false);
      boolean simpleSelf = yaml.getBoolean("resource-pack.hosting.simple_self_host.enabled", true);
      boolean selfHost = yaml.getBoolean("resource-pack.hosting.self-host.enabled", false);
      boolean externalHost = yaml.getBoolean("resource-pack.hosting.external-host.enabled", false);
      boolean autoApply = yaml.getBoolean("resource-pack.auto_apply.enabled", true);

      if (noHost && !autoApply) {
        return false;
      }
      return simpleSelf || selfHost || externalHost || autoApply;
    }
    return true;
  }

  private boolean checkOraxenHosting() {
    File file = new File(pluginsDir, "Oraxen/settings.yml");
    if (file.exists()) {
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
      boolean generate = yaml.getBoolean("Pack.generate", true);
      boolean upload = yaml.getBoolean("Pack.upload.enabled", true);
      boolean sendPack = yaml.getBoolean("Pack.send_pack", true);
      return generate && (upload || sendPack);
    }
    return true;
  }

  private boolean checkCraftEngineHosting() {
    File file = new File(pluginsDir, "CraftEngine/config.yml");
    if (file.exists()) {
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
      return yaml.getBoolean("pack.enabled", true);
    }
    return true;
  }

  private boolean checkRspmHosting() {
    File file = new File(pluginsDir, "ResourcePackManager/config.yml");
    if (file.exists()) {
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
      boolean selfHost = yaml.getBoolean("selfHostEnabled", true);
      boolean autoHost = yaml.getBoolean("autoHost", true);
      return selfHost || autoHost;
    }
    return true;
  }

  private boolean hasClass(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
