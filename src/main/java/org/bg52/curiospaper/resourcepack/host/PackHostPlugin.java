package org.bg52.curiospaper.resourcepack.host;

import java.util.Locale;

/**
 * Enumeration of external plugins that can host and merge resource packs.
 */
public enum PackHostPlugin {
  SYNTHETIX("Synthetix", "Synthetix"),
  RSPM("ResourcePackManager", "ResourcePackManager (RSPM)"),
  NEXO("Nexo", "Nexo"),
  ITEMSADDER("ItemsAdder", "ItemsAdder"),
  ORAXEN("Oraxen", "Oraxen"),
  CRAFTENGINE("CraftEngine", "CraftEngine"),
  AUTO("AUTO", "Automatic Detection"),
  NONE("NONE", "None");

  private final String pluginName;
  private final String displayName;

  PackHostPlugin(String pluginName, String displayName) {
    this.pluginName = pluginName;
    this.displayName = displayName;
  }

  public String getPluginName() {
    return pluginName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static PackHostPlugin fromString(String str) {
    if (str == null || str.trim().isEmpty()) {
      return AUTO;
    }
    String s = str.trim().toUpperCase(Locale.ROOT);
    if (s.equals("SYNTHETIX") || s.equals("SYNTH")) {
      return SYNTHETIX;
    }
    if (s.equals("RSPM") || s.equals("RESOURCEPACKMANAGER") || s.equals("RESOURCE_PACK_MANAGER")) {
      return RSPM;
    }
    for (PackHostPlugin p : values()) {
      if (p.name().equalsIgnoreCase(s) || p.pluginName.equalsIgnoreCase(s)) {
        return p;
      }
    }
    return AUTO;
  }
}
