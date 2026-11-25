package org.bg52.curiospaper.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Resilient reflection utility to detect if a player is connected from Bedrock Edition
 * via Floodgate or Geyser API, or via standard Floodgate offline UUID prefix.
 */
public final class BedrockUtil {

    private static Boolean floodgateAvailable = null;
    private static Boolean geyserAvailable = null;

    private BedrockUtil() {}

    /**
     * Checks whether the given player is playing from Bedrock Edition.
     */
    public static boolean isBedrockPlayer(Player player) {
        if (player == null) return false;
        return isBedrockPlayer(player.getUniqueId(), player.getName());
    }

    public static boolean isBedrockPlayer(UUID uuid, String name) {
        if (uuid == null) return false;

        // 1. Check Floodgate API
        if (isFloodgateAvailable()) {
            try {
                Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Object apiInstance = apiClass.getMethod("getInstance").invoke(null);
                Object isFloodgate = apiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(apiInstance, uuid);
                if (Boolean.TRUE.equals(isFloodgate)) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Check Geyser API
        if (isGeyserAvailable()) {
            try {
                Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
                Object apiInstance = apiClass.getMethod("api").invoke(null);
                Object isBedrock = apiClass.getMethod("isBedrockPlayer", UUID.class).invoke(apiInstance, uuid);
                if (Boolean.TRUE.equals(isBedrock)) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // 3. Fallback UUID check for Floodgate offline mode prefix (00000000-0000-0000-....)
        if (uuid.getMostSignificantBits() == 0L) {
            return true;
        }

        // 4. Fallback name prefix check (Floodgate default prefix is '.' or '*')
        if (name != null && (name.startsWith(".") || name.startsWith("*"))) {
            return true;
        }

        return false;
    }

    public static boolean isBedrockPresent() {
        return isFloodgateAvailable() || isGeyserAvailable();
    }

    private static boolean isFloodgateAvailable() {
        if (floodgateAvailable == null || !floodgateAvailable) {
            try {
                Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                floodgateAvailable = Bukkit.getPluginManager().isPluginEnabled("floodgate");
            } catch (ClassNotFoundException e) {
                floodgateAvailable = false;
            }
        }
        return floodgateAvailable != null && floodgateAvailable;
    }

    private static boolean isGeyserAvailable() {
        if (geyserAvailable == null || !geyserAvailable) {
            try {
                Class.forName("org.geysermc.geyser.api.GeyserApi");
                geyserAvailable = Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot")
                        || Bukkit.getPluginManager().isPluginEnabled("Geyser");
            } catch (ClassNotFoundException e) {
                geyserAvailable = false;
            }
        }
        return geyserAvailable != null && geyserAvailable;
    }
}
