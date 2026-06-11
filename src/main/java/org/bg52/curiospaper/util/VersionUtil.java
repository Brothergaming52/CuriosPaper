package org.bg52.curiospaper.util;

import org.bukkit.Bukkit;

/**
 * Utility class for detecting Minecraft server version and feature
 * availability.
 */
public class VersionUtil {

  private static int majorVersion = -1;
  private static int minorVersion = -1;
  private static int patchVersion = -1;
  private static Boolean supportsItemModel = null;
  private static Boolean supportsDataComponents = null;
  private static Boolean supportsSmithingTemplate = null;
  private static Boolean supportsEntityPoseChangeEvent = null;
  private static Boolean supportsLootGenerateEvent = null;

  static {
    parseVersion();
  }

  /**
   * Parse the server version from Bukkit.getVersion()
   * Examples: "1.14.4", "1.21.3", "1.20.1"
   */
  private static void parseVersion() {
    try {
      String versionString = Bukkit.getBukkitVersion();
      // Format: "1.21.3-R0.1-SNAPSHOT" or "1.14.4-R0.1-SNAPSHOT"
      String[] parts = versionString.split("-")[0].split("\\.");

      if (parts.length >= 2) {
        majorVersion = Integer.parseInt(parts[0]);
        minorVersion = Integer.parseInt(parts[1]);
      }
      if (parts.length >= 3) {
        patchVersion = Integer.parseInt(parts[2]);
      } else {
        patchVersion = 0;
      }
    } catch (Exception e) {
      // Fallback to safe defaults if parsing fails
      majorVersion = 1;
      minorVersion = 14;
      patchVersion = 0;
    }
  }

  /**
   * Get the major version number (e.g., 1 from "1.21.3")
   */
  public static int getMajorVersion() {
    return majorVersion;
  }

  /**
   * Get the minor version number (e.g., 21 from "1.21.3")
   */
  public static int getMinorVersion() {
    return minorVersion;
  }

  /**
   * Get the patch version number (e.g., 3 from "1.21.3")
   */
  public static int getPatchVersion() {
    return patchVersion;
  }

  /**
   * Check if the server is running at least the specified version.
   * 
   * @param major Major version (usually 1)
   * @param minor Minor version (e.g., 21 for 1.21)
   * @param patch Patch version (e.g., 3 for 1.21.3)
   * @return true if server version >= specified version
   */
  public static boolean isAtLeast(int major, int minor, int patch) {
    if (majorVersion > major)
      return true;
    if (majorVersion < major)
      return false;
    if (minorVersion > minor)
      return true;
    if (minorVersion < minor)
      return false;
    return patchVersion >= patch;
  }

  /**
   * Check if the server is running at least the specified version.
   * 
   * @param major Major version (usually 1)
   * @param minor Minor version (e.g., 21 for 1.21)
   * @return true if server version >= specified version
   */
  public static boolean isAtLeast(int major, int minor) {
    return isAtLeast(major, minor, 0);
  }

  /**
   * Check if the server supports EntityPoseChangeEvent (1.17+)
   */
  public static boolean supportsEntityPoseChangeEvent() {
    if (supportsEntityPoseChangeEvent == null) {
      if (!isAtLeast(1, 17)) {
        supportsEntityPoseChangeEvent = false;
      } else {
        try {
          Class.forName("org.bukkit.event.entity.EntityPoseChangeEvent");
          supportsEntityPoseChangeEvent = true;
        } catch (Exception e) {
          supportsEntityPoseChangeEvent = false;
        }
      }
    }
    return supportsEntityPoseChangeEvent;
  }

  /**
   * Check if the server supports LootGenerateEvent (1.15+)
   */
  public static boolean supportsLootGenerateEvent() {
    if (supportsLootGenerateEvent == null) {
      if (!isAtLeast(1, 15)) {
        supportsLootGenerateEvent = false;
      } else {
        try {
          Class.forName("org.bukkit.event.world.LootGenerateEvent");
          supportsLootGenerateEvent = true;
        } catch (Exception e) {
          supportsLootGenerateEvent = false;
        }
      }
    }
    return supportsLootGenerateEvent;
  }

  /**
   * Check if the server supports Attribute.GENERIC_SCALE (1.20.5+)
   */
  public static boolean supportsScaleAttribute() {
    return isAtLeast(1, 20, 5);
  }

  /**
   * Check if the server supports ItemMeta.setItemModel(NamespacedKey)
   * This was added in MC 1.21.3 (Paper)
   */
  public static boolean supportsItemModel() {
    if (supportsItemModel == null) {
      if (!isAtLeast(1, 21, 3)) {
        supportsItemModel = false;
      } else {
        // Additional runtime check via reflection
        try {
          Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
          itemMetaClass.getMethod("setItemModel", org.bukkit.NamespacedKey.class);
          supportsItemModel = true;
        } catch (Exception e) {
          supportsItemModel = false;
        }
      }
    }
    return supportsItemModel;
  }

  /**
   * Check if the server supports DataComponentTypes (Paper 1.21+)
   * This is required for GLIDER and EQUIPPABLE components
   */
  public static boolean supportsDataComponents() {
    if (supportsDataComponents == null) {
      if (!isAtLeast(1, 21, 0)) {
        supportsDataComponents = false;
      } else {
        // Additional runtime check via reflection
        try {
          Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
          supportsDataComponents = true;
        } catch (Exception e) {
          supportsDataComponents = false;
        }
      }
    }
    return supportsDataComponents;
  }

  /**
   * Check if the server supports SmithingTransformRecipe (1.20+)
   * with template slot. Older versions use SmithingRecipe with only
   * base + addition.
   */
  public static boolean supportsSmithingTemplate() {
    if (supportsSmithingTemplate == null) {
      if (!isAtLeast(1, 20)) {
        supportsSmithingTemplate = false;
      } else {
        try {
          Class.forName("org.bukkit.inventory.SmithingTransformRecipe");
          supportsSmithingTemplate = true;
        } catch (Exception e) {
          supportsSmithingTemplate = false;
        }
      }
    }
    return supportsSmithingTemplate;
  }

  /**
   * Get a formatted version string for logging
   */
  public static String getVersionString() {
    return majorVersion + "." + minorVersion + "." + patchVersion;
  }

  /**
   * Parse a namespaced key string into a NamespacedKey object.
   * Compatible with Spigot 1.14+ which doesn't have NamespacedKey.fromString()
   * 
   * @param key The key string in format "namespace:key" or just "key" (defaults
   *      to minecraft namespace)
   * @return The NamespacedKey, or null if parsing fails
   */
  @SuppressWarnings("deprecation")
  public static org.bukkit.NamespacedKey parseNamespacedKey(String key) {
    if (key == null || key.trim().isEmpty()) {
      return null;
    }

    try {
      String[] parts = key.split(":", 2);
      if (parts.length == 2) {
        return new org.bukkit.NamespacedKey(parts[0], parts[1]);
      } else {
        // No namespace, default to minecraft
        return org.bukkit.NamespacedKey.minecraft(key);
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Set item model on ItemMeta in a version-safe way.
   * Uses setItemModel(NamespacedKey) on 1.21.3+, falls back to setCustomModelData
   * on older versions.
   * 
   * @param meta      The ItemMeta to modify
   * @param itemModel    The item model string (namespace:key format)
   * @param customModelData The CustomModelData value for older versions (can be
   *            null)
   */
  public static void setItemModelSafe(org.bukkit.inventory.meta.ItemMeta meta, String itemModel,
      Integer customModelData) {
    if (meta == null)
      return;

    if (supportsItemModel() && itemModel != null && !itemModel.trim().isEmpty()) {
      try {
        org.bukkit.NamespacedKey key = parseNamespacedKey(itemModel);
        if (key != null) {
          // Use the interface class for reflection to ensure compatibility
          Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
          java.lang.reflect.Method method = itemMetaClass.getMethod("setItemModel",
              org.bukkit.NamespacedKey.class);
          method.invoke(meta, key);
          return;
        }
      } catch (Exception e) {
        // Log and fall through to CustomModelData
        org.bukkit.Bukkit.getLogger()
            .warning("[CuriosPaper] Failed to set item model via reflection: " + e.getMessage());
      }
    }

    // Fallback: Use CustomModelData
    if (customModelData != null) {
      meta.setCustomModelData(customModelData);
    } else if (itemModel != null) {
      // Try to parse itemModel string as integer for CustomModelData
      try {
        int cmd = Integer.parseInt(itemModel);
        meta.setCustomModelData(cmd);
      } catch (NumberFormatException e) {
        // Cannot convert to CustomModelData, skip
      }
    }
  }

  /**
   * Set item model on ItemMeta using a NamespacedKey directly (for slot
   * configurations).
   * 
   * @param meta      The ItemMeta to modify
   * @param itemModel    The NamespacedKey item model
   * @param customModelData The CustomModelData value for older versions (can be
   *            null)
   */
  // Reflection Cache
  private static Class<?> dataComponentTypesClass;
  private static Object typeGlider; // DataComponentType.NonValued
  private static Object typeEquippable; // DataComponentType.Valued<Equippable>
  private static java.lang.reflect.Method getDataMethod; // getData(DataComponentType.Valued)
  private static java.lang.reflect.Method setDataValuedMethod; // setData(DataComponentType.Valued, Object)
  private static java.lang.reflect.Method setDataNonValuedMethod; // setData(DataComponentType.NonValued)
  private static java.lang.reflect.Method unsetDataMethod; // unsetData(DataComponentType)
  private static java.lang.reflect.Method hasDataMethod; // hasData(DataComponentType)
  private static java.lang.reflect.Method resetDataMethod; // resetData(DataComponentType)
  private static java.lang.reflect.Method keyMethod;
  private static boolean dataComponentsInitAttempted = false;

  /**
   * Tries to initialize reflection for Data Components (1.21+ functionality).
   * GLIDER is a NonValued type (single-arg setData), EQUIPPABLE is Valued
   * (two-arg setData).
   */
  private static void initDataComponents() {
    if (dataComponentsInitAttempted)
      return;
    dataComponentsInitAttempted = true;
    if (!supportsDataComponents())
      return;
    try {
      dataComponentTypesClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
      typeGlider = dataComponentTypesClass.getField("GLIDER").get(null);
      typeEquippable = dataComponentTypesClass.getField("EQUIPPABLE").get(null);

      Class<?> itemStackClass = org.bukkit.inventory.ItemStack.class;

      // Base type for hasData / unsetData / resetData
      Class<?> dataComponentTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType");
      hasDataMethod = itemStackClass.getMethod("hasData", dataComponentTypeClass);
      unsetDataMethod = itemStackClass.getMethod("unsetData", dataComponentTypeClass);
      resetDataMethod = itemStackClass.getMethod("resetData", dataComponentTypeClass);

      // Valued subtype for getData / setData with value
      Class<?> valuedClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
      getDataMethod = itemStackClass.getMethod("getData", valuedClass);
      setDataValuedMethod = itemStackClass.getMethod("setData", valuedClass, Object.class);

      // NonValued subtype for setData without value (unit types like GLIDER)
      Class<?> nonValuedClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$NonValued");
      setDataNonValuedMethod = itemStackClass.getMethod("setData", nonValuedClass);

      // Key factory
      Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");
      keyMethod = keyClass.getMethod("key", String.class, String.class);

    } catch (Exception e) {
      org.bukkit.Bukkit.getLogger()
          .warning("[CuriosPaper] Failed to init data components via reflection: " + e.getMessage());
      supportsDataComponents = false;
    }
  }

  public static boolean hasGlider(org.bukkit.inventory.ItemStack item) {
    if (!supportsDataComponents() || item == null)
      return false;
    try {
      initDataComponents();
      if (hasDataMethod == null)
        return false;
      return (boolean) hasDataMethod.invoke(item, typeGlider);
    } catch (Exception e) {
      return false;
    }
  }

  public static void setGlider(org.bukkit.inventory.ItemStack item, boolean enabled) {
    if (!supportsDataComponents() || item == null)
      return;
    try {
      initDataComponents();
      if (enabled) {
        // GLIDER is NonValued — single-arg setData
        if (setDataNonValuedMethod == null)
          return;
        setDataNonValuedMethod.invoke(item, typeGlider);
      } else {
        if (unsetDataMethod == null)
          return;
        unsetDataMethod.invoke(item, typeGlider);
      }
    } catch (Exception e) {
      org.bukkit.Bukkit.getLogger()
          .warning("[CuriosPaper] Failed to set/unset glider: " + e.getMessage());
    }
  }

  /**
   * Applies the Glider component and sets the Equippable asset to make it look
   * like elytra.
   */
  public static void applyElytraFlight(org.bukkit.inventory.ItemStack chestplate, String assetNamespace,
      String assetKey) {
    if (!supportsDataComponents() || chestplate == null)
      return;
    try {
      initDataComponents();

      // 1. Set Glider — GLIDER is NonValued, just mark it present
      if (setDataNonValuedMethod == null) {
        org.bukkit.Bukkit.getLogger()
            .warning("[CuriosPaper] setDataNonValuedMethod is null, cannot apply glider");
        return;
      }
      setDataNonValuedMethod.invoke(chestplate, typeGlider);

      // 2. Set Equippable asset ID to show wings model
      // Equippable equippable = chestplate.getData(DataComponentTypes.EQUIPPABLE);
      // Equippable built = equippable.toBuilder().assetId(Key.key(ns, key)).build();
      // chestplate.setData(DataComponentTypes.EQUIPPABLE, built);
      if (getDataMethod == null || setDataValuedMethod == null)
        return;

      Object currentEquippable = getDataMethod.invoke(chestplate, typeEquippable);
      if (currentEquippable != null) {
        // Use the public interface classes, not the package-private impl classes
        Class<?> equippableClass = Class.forName("io.papermc.paper.datacomponent.item.Equippable");
        Class<?> builderClass = Class.forName("io.papermc.paper.datacomponent.item.Equippable$Builder");

        java.lang.reflect.Method toBuilder = equippableClass.getMethod("toBuilder");
        Object builder = toBuilder.invoke(currentEquippable);

        Object key = keyMethod.invoke(null, assetNamespace, assetKey);
        java.lang.reflect.Method assetIdMethod = builderClass.getMethod("assetId",
            Class.forName("net.kyori.adventure.key.Key"));
        assetIdMethod.invoke(builder, key);

        java.lang.reflect.Method buildMethod = builderClass.getMethod("build");
        Object newEquippable = buildMethod.invoke(builder);

        setDataValuedMethod.invoke(chestplate, typeEquippable, newEquippable);
      }

    } catch (Exception e) {
      org.bukkit.Bukkit.getLogger()
          .warning("[CuriosPaper] Failed to apply elytra flight: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void removeGlider(org.bukkit.inventory.ItemStack chestplate) {
    if (!supportsDataComponents() || chestplate == null)
      return;
    try {
      initDataComponents();

      // Unset the GLIDER component
      if (unsetDataMethod == null) {
        org.bukkit.Bukkit.getLogger()
            .warning("[CuriosPaper] unsetDataMethod is null, cannot remove glider");
        return;
      }
      unsetDataMethod.invoke(chestplate, typeGlider);

      // Reset Equippable to the item type's prototype default
      // This restores the original asset ID (e.g. diamond_chestplate instead of
      // elytra wings)
      if (resetDataMethod != null) {
        resetDataMethod.invoke(chestplate, typeEquippable);
      }

    } catch (Exception e) {
      org.bukkit.Bukkit.getLogger()
          .warning("[CuriosPaper] Failed to remove glider: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void setItemModelSafe(org.bukkit.inventory.meta.ItemMeta meta, org.bukkit.NamespacedKey itemModel,
      Integer customModelData) {
    if (meta == null)
      return;

    if (supportsItemModel() && itemModel != null) {
      try {
        // Use the interface class for reflection to ensure compatibility
        Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
        java.lang.reflect.Method method = itemMetaClass.getMethod("setItemModel",
            org.bukkit.NamespacedKey.class);
        method.invoke(meta, itemModel);
        return;
      } catch (Exception e) {
        // Log and fall through to CustomModelData
        org.bukkit.Bukkit.getLogger().warning(
            "[CuriosPaper] Failed to set item model (NamespacedKey) via reflection: " + e.getMessage());
      }
    }

    // Fallback to CustomModelData
    if (customModelData != null) {
      meta.setCustomModelData(customModelData);
    }
  }

  /**
   * Set a base64 skin texture on a SkullMeta.
   * Compiles on 1.14.4 and works via reflection on all versions.
   */
  public static java.net.URL getUrlFromBase64(String base64) {
    if (base64 == null || base64.trim().isEmpty()) {
      return null;
    }
    base64 = base64.trim();
    // Try base64 decode first
    try {
      byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64);
      String decoded = new String(decodedBytes);
      int urlIndex = decoded.indexOf("http://textures.minecraft.net/texture/");
      if (urlIndex == -1) {
        urlIndex = decoded.indexOf("https://textures.minecraft.net/texture/");
      }
      if (urlIndex != -1) {
        int end = urlIndex;
        while (end < decoded.length()) {
          char c = decoded.charAt(end);
          if (c == '"' || c == '\\' || c == ' ' || c == '}' || c == '\'') {
            break;
          }
          end++;
        }
        String urlStr = decoded.substring(urlIndex, end);
        if (urlStr.startsWith("http://")) {
          urlStr = "https://" + urlStr.substring(7);
        }
        return new java.net.URL(urlStr);
      }
    } catch (Throwable t) {
      // Not base64
    }

    // Try directly as URL
    try {
      if (base64.startsWith("http://") || base64.startsWith("https://")) {
        String urlStr = base64;
        if (urlStr.startsWith("http://")) {
          urlStr = "https://" + urlStr.substring(7);
        }
        return new java.net.URL(urlStr);
      }
      if (base64.length() > 20 && base64.matches("[a-fA-F0-9]+")) {
        return new java.net.URL("https://textures.minecraft.net/texture/" + base64);
      }
    } catch (Throwable t) {
      // Ignore
    }
    return null;
  }

  public static void setSkullBase64(org.bukkit.inventory.meta.SkullMeta meta, String base64) {
    if (meta == null || base64 == null || base64.trim().isEmpty()) {
      return;
    }

    Throwable path1aErr = null;
    Throwable path1bErr = null;
    Throwable path2Err = null;
    Throwable path3Err = null;

    // Path 1a: Bukkit PlayerProfile + PlayerTextures (Official, stable, avoids property errors on 1.18+)
    try {
      java.lang.reflect.Method createPlayerProfileMethod = org.bukkit.Bukkit.class.getMethod("createPlayerProfile", java.util.UUID.class, String.class);
      createPlayerProfileMethod.setAccessible(true);
      Object playerProfile = createPlayerProfileMethod.invoke(null, java.util.UUID.randomUUID(), "");

      Class<?> playerProfileClass = Class.forName("org.bukkit.profile.PlayerProfile");
      Class<?> playerTexturesClass = Class.forName("org.bukkit.profile.PlayerTextures");

      java.lang.reflect.Method getTexturesMethod = playerProfileClass.getMethod("getTextures");
      getTexturesMethod.setAccessible(true);
      Object playerTextures = getTexturesMethod.invoke(playerProfile);

      java.net.URL url = getUrlFromBase64(base64);
      if (url != null) {
        java.lang.reflect.Method setSkinMethod = playerTexturesClass.getMethod("setSkin", java.net.URL.class);
        setSkinMethod.setAccessible(true);
        setSkinMethod.invoke(playerTextures, url);

        java.lang.reflect.Method setTexturesMethod = playerProfileClass.getMethod("setTextures", playerTexturesClass);
        setTexturesMethod.setAccessible(true);
        setTexturesMethod.invoke(playerProfile, playerTextures);

        java.lang.reflect.Method setOwnerProfileMethod = org.bukkit.inventory.meta.SkullMeta.class.getMethod("setOwnerProfile", playerProfileClass);
        setOwnerProfileMethod.setAccessible(true);
        setOwnerProfileMethod.invoke(meta, playerProfile);
        return; // Success!
      } else {
        path1aErr = new IllegalArgumentException("Parsed skin URL is null from base64 string");
      }
    } catch (Throwable t) {
      path1aErr = t;
    }

    // Path 1b: Bukkit PlayerProfile + ProfileProperty (Fallback 1.18+)
    try {
      java.lang.reflect.Method createPlayerProfileMethod = org.bukkit.Bukkit.class.getMethod("createPlayerProfile", java.util.UUID.class, String.class);
      createPlayerProfileMethod.setAccessible(true);
      Object playerProfile = createPlayerProfileMethod.invoke(null, java.util.UUID.randomUUID(), "");

      Class<?> playerProfileClass = Class.forName("org.bukkit.profile.PlayerProfile");
      java.lang.reflect.Method getPropertiesMethod = playerProfileClass.getMethod("getProperties");
      getPropertiesMethod.setAccessible(true);
      java.util.Collection<Object> properties = (java.util.Collection<Object>) getPropertiesMethod.invoke(playerProfile);

      Class<?> profilePropertyClass = Class.forName("org.bukkit.profile.ProfileProperty");
      java.lang.reflect.Constructor<?> propertyConstructor = profilePropertyClass.getConstructor(String.class, String.class);
      propertyConstructor.setAccessible(true);
      Object property = propertyConstructor.newInstance("textures", base64);

      properties.add(property);

      java.lang.reflect.Method setOwnerProfileMethod = org.bukkit.inventory.meta.SkullMeta.class.getMethod("setOwnerProfile", playerProfileClass);
      setOwnerProfileMethod.setAccessible(true);
      setOwnerProfileMethod.invoke(meta, playerProfile);
      return; // Success!
    } catch (Throwable t) {
      path1bErr = t;
    }

    // Path 2: Paper PlayerProfile API (1.12 - 1.17)
    try {
      java.lang.reflect.Method createProfileMethod = org.bukkit.Bukkit.class.getMethod("createProfile", java.util.UUID.class);
      createProfileMethod.setAccessible(true);
      Object profile = createProfileMethod.invoke(null, java.util.UUID.randomUUID());

      Class<?> paperProfileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
      java.lang.reflect.Method getPropertiesMethod = paperProfileClass.getMethod("getProperties");
      getPropertiesMethod.setAccessible(true);
      java.util.Collection<Object> properties = (java.util.Collection<Object>) getPropertiesMethod.invoke(profile);

      Class<?> profilePropertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
      java.lang.reflect.Constructor<?> propertyConstructor = profilePropertyClass.getConstructor(String.class, String.class);
      propertyConstructor.setAccessible(true);
      Object property = propertyConstructor.newInstance("textures", base64);

      properties.add(property);

      java.lang.reflect.Method setPlayerProfileMethod = org.bukkit.inventory.meta.SkullMeta.class.getMethod("setPlayerProfile", paperProfileClass);
      setPlayerProfileMethod.setAccessible(true);
      setPlayerProfileMethod.invoke(meta, profile);
      return;
    } catch (Throwable t) {
      path2Err = t;
    }

    // Path 3: Legacy GameProfile reflection (1.8 - 1.17)
    try {
      Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
      java.util.UUID uuid = java.util.UUID.randomUUID();
      java.lang.reflect.Constructor<?> profileConstructor = gameProfileClass.getConstructor(java.util.UUID.class, String.class);
      profileConstructor.setAccessible(true);
      Object profile = profileConstructor.newInstance(uuid, "");

      Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
      java.lang.reflect.Constructor<?> propertyConstructor = propertyClass.getConstructor(String.class, String.class);
      propertyConstructor.setAccessible(true);
      Object property = propertyConstructor.newInstance("textures", base64);

      java.lang.reflect.Method getPropertiesMethod = gameProfileClass.getMethod("getProperties");
      getPropertiesMethod.setAccessible(true);
      Object propertiesMap = getPropertiesMethod.invoke(profile);

      java.lang.reflect.Method putMethod = propertiesMap.getClass().getMethod("put", Object.class, Object.class);
      putMethod.setAccessible(true);
      putMethod.invoke(propertiesMap, "textures", property);

      java.lang.reflect.Field profileField = null;
      Class<?> currentClass = meta.getClass();
      while (currentClass != null && currentClass != Object.class) {
        try {
          profileField = currentClass.getDeclaredField("profile");
          break;
        } catch (NoSuchFieldException e) {
          currentClass = currentClass.getSuperclass();
        }
      }
      if (profileField != null) {
        profileField.setAccessible(true);
        profileField.set(meta, profile);
        return;
      } else {
        path3Err = new NoSuchFieldException("Could not find field 'profile' in " + meta.getClass().getName() + " or any of its superclasses");
      }
    } catch (Throwable t) {
      path3Err = t;
    }

    // Log all errors to help with troubleshooting
    org.bukkit.Bukkit.getLogger().warning("[CuriosPaper] All attempts to set skull base64 texture failed!");
    if (path1aErr != null) {
      org.bukkit.Bukkit.getLogger().warning("[CuriosPaper] Path 1a (Bukkit Profile + Textures API) failed: " + path1aErr.toString());
      if (path1aErr instanceof java.lang.reflect.InvocationTargetException) {
        org.bukkit.Bukkit.getLogger().warning("[CuriosPaper] Path 1a target error: " + ((java.lang.reflect.InvocationTargetException) path1aErr).getTargetException().toString());
      }
    }
    if (path1bErr != null) org.bukkit.Bukkit.getLogger().warning("[CuriosPaper] Path 1b (Bukkit Profile + Property API) failed: " + path1bErr.toString());
    if (path2Err != null) org.bukkit.Bukkit.getLogger().warning("[CuriosPaper] Path 2 (Paper Profile API) failed: " + path2Err.toString());
    if (path3Err != null) org.bukkit.Bukkit.getLogger().warning("[CuriosPaper] Path 3 (Mojang GameProfile Reflection) failed: " + path3Err.toString());
  }

  public static String getSkullBase64(org.bukkit.inventory.meta.SkullMeta meta) {
    if (meta == null) {
      return null;
    }

    // Path 1: Bukkit PlayerProfile API (1.18+)
    try {
      java.lang.reflect.Method getOwnerProfileMethod = org.bukkit.inventory.meta.SkullMeta.class.getMethod("getOwnerProfile");
      getOwnerProfileMethod.setAccessible(true);
      Object playerProfile = getOwnerProfileMethod.invoke(meta);
      if (playerProfile != null) {
        // Try getting via properties first
        try {
          Class<?> playerProfileClass = Class.forName("org.bukkit.profile.PlayerProfile");
          java.lang.reflect.Method getPropertiesMethod = playerProfileClass.getMethod("getProperties");
          getPropertiesMethod.setAccessible(true);
          java.util.Collection<?> properties = (java.util.Collection<?>) getPropertiesMethod.invoke(playerProfile);
          for (Object prop : properties) {
            java.lang.reflect.Method getNameMethod = prop.getClass().getMethod("getName");
            getNameMethod.setAccessible(true);
            String name = (String) getNameMethod.invoke(prop);
            if ("textures".equals(name)) {
              java.lang.reflect.Method getValueMethod = prop.getClass().getMethod("getValue");
              getValueMethod.setAccessible(true);
              return (String) getValueMethod.invoke(prop);
            }
          }
        } catch (Throwable e) {
          // Fallback
        }

        // Try getting via PlayerTextures
        try {
          Class<?> playerProfileClass = Class.forName("org.bukkit.profile.PlayerProfile");
          Class<?> playerTexturesClass = Class.forName("org.bukkit.profile.PlayerTextures");

          java.lang.reflect.Method getTexturesMethod = playerProfileClass.getMethod("getTextures");
          getTexturesMethod.setAccessible(true);
          Object playerTextures = getTexturesMethod.invoke(playerProfile);

          java.lang.reflect.Method getSkinMethod = playerTexturesClass.getMethod("getSkin");
          getSkinMethod.setAccessible(true);
          java.net.URL skinUrl = (java.net.URL) getSkinMethod.invoke(playerTextures);
          if (skinUrl != null) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + skinUrl.toString() + "\"}}}";
            return java.util.Base64.getEncoder().encodeToString(json.getBytes());
          }
        } catch (Throwable e) {
          // Fallback
        }
      }
    } catch (Throwable t) {
      // Fallback
    }

    // Path 2: Paper PlayerProfile API (1.12 - 1.17)
    try {
      java.lang.reflect.Method getPlayerProfileMethod = org.bukkit.inventory.meta.SkullMeta.class.getMethod("getPlayerProfile");
      getPlayerProfileMethod.setAccessible(true);
      Object profile = getPlayerProfileMethod.invoke(meta);
      if (profile != null) {
        Class<?> paperProfileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
        java.lang.reflect.Method getPropertiesMethod = paperProfileClass.getMethod("getProperties");
        getPropertiesMethod.setAccessible(true);
        java.util.Collection<?> properties = (java.util.Collection<?>) getPropertiesMethod.invoke(profile);
        for (Object prop : properties) {
          java.lang.reflect.Method getNameMethod = prop.getClass().getMethod("getName");
          getNameMethod.setAccessible(true);
          String name = (String) getNameMethod.invoke(prop);
          if ("textures".equals(name)) {
            java.lang.reflect.Method getValueMethod = prop.getClass().getMethod("getValue");
            getValueMethod.setAccessible(true);
            return (String) getValueMethod.invoke(prop);
          }
        }
      }
    } catch (Throwable t) {
      // Fallback
    }

    // Path 3: Legacy GameProfile reflection (1.8 - 1.20)
    try {
      java.lang.reflect.Field profileField = null;
      Class<?> currentClass = meta.getClass();
      while (currentClass != null && currentClass != Object.class) {
        try {
          profileField = currentClass.getDeclaredField("profile");
          break;
        } catch (NoSuchFieldException e) {
          currentClass = currentClass.getSuperclass();
        }
      }
      if (profileField != null) {
        profileField.setAccessible(true);
        Object profile = profileField.get(meta);
        if (profile != null) {
          java.lang.reflect.Method getPropertiesMethod = profile.getClass().getMethod("getProperties");
          getPropertiesMethod.setAccessible(true);
          Object propertiesMap = getPropertiesMethod.invoke(profile);
          java.lang.reflect.Method getMethod = propertiesMap.getClass().getMethod("get", Object.class);
          getMethod.setAccessible(true);
          java.util.Collection<?> textures = (java.util.Collection<?>) getMethod.invoke(propertiesMap, "textures");
          for (Object prop : textures) {
            java.lang.reflect.Method getValueMethod = prop.getClass().getMethod("getValue");
            getValueMethod.setAccessible(true);
            return (String) getValueMethod.invoke(prop);
          }
        }
      }
    } catch (Throwable t) {
      // Fallback
    }

    return null;
  }

  public static java.util.Map<String, String> getPdcMap(org.bukkit.inventory.meta.ItemMeta meta) {
    java.util.Map<String, String> map = new java.util.HashMap<>();
    if (meta == null) return map;
    try {
      org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
      java.util.Set<org.bukkit.NamespacedKey> keys = null;
      try {
        java.lang.reflect.Method getKeysMethod = container.getClass().getMethod("getKeys");
        keys = (java.util.Set<org.bukkit.NamespacedKey>) getKeysMethod.invoke(container);
      } catch (Exception e) {
        try {
          java.lang.reflect.Field field = container.getClass().getDeclaredField("customDataContainer");
          field.setAccessible(true);
          java.util.Map<?, ?> internalMap = (java.util.Map<?, ?>) field.get(container);
          keys = new java.util.HashSet<>();
          for (Object k : internalMap.keySet()) {
            if (k instanceof org.bukkit.NamespacedKey) {
              keys.add((org.bukkit.NamespacedKey) k);
            }
          }
        } catch (Exception ex) {
          // Ignore
        }
      }

      if (keys != null) {
        for (org.bukkit.NamespacedKey key : keys) {
          String keyStr = key.toString();
          // Skip curiospaper own metadata
          if (keyStr.equals("curiospaper:item_id") || keyStr.endsWith("curios_custom_id") || keyStr.equals("curiospaper:slot_type")) {
            continue;
          }
          if (container.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            map.put(keyStr, "string:" + container.get(key, org.bukkit.persistence.PersistentDataType.STRING));
          } else if (container.has(key, org.bukkit.persistence.PersistentDataType.INTEGER)) {
            map.put(keyStr, "int:" + container.get(key, org.bukkit.persistence.PersistentDataType.INTEGER));
          } else if (container.has(key, org.bukkit.persistence.PersistentDataType.DOUBLE)) {
            map.put(keyStr, "double:" + container.get(key, org.bukkit.persistence.PersistentDataType.DOUBLE));
          } else if (container.has(key, org.bukkit.persistence.PersistentDataType.FLOAT)) {
            map.put(keyStr, "float:" + container.get(key, org.bukkit.persistence.PersistentDataType.FLOAT));
          } else if (container.has(key, org.bukkit.persistence.PersistentDataType.BYTE)) {
            map.put(keyStr, "byte:" + container.get(key, org.bukkit.persistence.PersistentDataType.BYTE));
          } else if (container.has(key, org.bukkit.persistence.PersistentDataType.SHORT)) {
            map.put(keyStr, "short:" + container.get(key, org.bukkit.persistence.PersistentDataType.SHORT));
          } else if (container.has(key, org.bukkit.persistence.PersistentDataType.LONG)) {
            map.put(keyStr, "long:" + container.get(key, org.bukkit.persistence.PersistentDataType.LONG));
          }
        }
      }
    } catch (Throwable t) {
      // Ignore if server version doesn't support PDC (pre-1.14), though our targeted versions all do
    }
    return map;
  }

  public static void applyPdcMap(org.bukkit.inventory.meta.ItemMeta meta, java.util.Map<String, String> map) {
    if (meta == null || map == null) return;
    try {
      org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
      for (java.util.Map.Entry<String, String> entry : map.entrySet()) {
        org.bukkit.NamespacedKey key = parseNamespacedKey(entry.getKey());
        if (key == null) continue;
        String valStr = entry.getValue();
        if (valStr == null || !valStr.contains(":")) continue;
        String[] parts = valStr.split(":", 2);
        String type = parts[0].toLowerCase();
        String value = parts[1];
        try {
          switch (type) {
            case "string":
              container.set(key, org.bukkit.persistence.PersistentDataType.STRING, value);
              break;
            case "int":
            case "integer":
              container.set(key, org.bukkit.persistence.PersistentDataType.INTEGER, Integer.parseInt(value));
              break;
            case "double":
              container.set(key, org.bukkit.persistence.PersistentDataType.DOUBLE, Double.parseDouble(value));
              break;
            case "float":
              container.set(key, org.bukkit.persistence.PersistentDataType.FLOAT, Float.parseFloat(value));
              break;
            case "byte":
              container.set(key, org.bukkit.persistence.PersistentDataType.BYTE, Byte.parseByte(value));
              break;
            case "short":
              container.set(key, org.bukkit.persistence.PersistentDataType.SHORT, Short.parseShort(value));
              break;
            case "long":
              container.set(key, org.bukkit.persistence.PersistentDataType.LONG, Long.parseLong(value));
              break;
          }
        } catch (Exception e) {
          // Ignore parse exceptions
        }
      }
    } catch (Throwable t) {
      // Ignore if server version doesn't support PDC
    }
  }
}
