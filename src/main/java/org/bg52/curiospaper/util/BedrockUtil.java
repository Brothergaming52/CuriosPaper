package org.bg52.curiospaper.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Utility class to check if a player is connected from Bedrock Edition
 * via Floodgate or Geyser API. Uses reflection to avoid compile-time dependencies.
 */
public class BedrockUtil {

    private static Boolean floodgateAvailable = null;
    private static Boolean geyserAvailable = null;

    /**
     * Checks whether the given player is playing from Bedrock Edition.
     */
    public static boolean isBedrockPlayer(Player player) {
        if (player == null) return false;

        UUID uuid = player.getUniqueId();

        // 1. Check Floodgate API
        if (isFloodgateAvailable()) {
            try {
                Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Object apiInstance = apiClass.getMethod("getInstance").invoke(null);
                Object isFloodgate = apiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(apiInstance, uuid);
                if (Boolean.TRUE.equals(isFloodgate)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
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
            } catch (Throwable ignored) {
            }
        }

        // 3. Fallback UUID check for Floodgate offline mode prefix (00000000-0000-0000-....)
        // Floodgate UUIDs start with 00000000-0000-0000 in default Floodgate setup
        if (uuid.getMostSignificantBits() == 0L) {
            return true;
        }

        return false;
    }

    private static boolean isFloodgateAvailable() {
        if (floodgateAvailable == null) {
            try {
                Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                floodgateAvailable = Bukkit.getPluginManager().isPluginEnabled("floodgate");
            } catch (ClassNotFoundException e) {
                floodgateAvailable = false;
            }
        }
        return floodgateAvailable;
    }

    private static boolean isGeyserAvailable() {
        if (geyserAvailable == null) {
            try {
                Class.forName("org.geysermc.geyser.api.GeyserApi");
                geyserAvailable = Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot") 
                        || Bukkit.getPluginManager().isPluginEnabled("Geyser");
            } catch (ClassNotFoundException e) {
                geyserAvailable = false;
            }
        }
        return geyserAvailable;
    }
}
