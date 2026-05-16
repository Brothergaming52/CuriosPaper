package org.bg52.curiospaper.util;

import com.google.gson.JsonArray;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private static final String MODRINTH_ID = "JLMPHrRw";

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void check() {
        plugin.getLogger().info("Update check started for " + MODRINTH_ID);
        new Thread(() -> {
            try {
                // Give the server a second to finish starting up
                Thread.sleep(2000);

                URL url = new URL("https://api.modrinth.com/v2/project/" + MODRINTH_ID + "/version");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "CuriosPaper Update Checker");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                if (connection.getResponseCode() == 200) {
                    try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                        JsonArray versions = new com.google.gson.JsonParser().parse(reader).getAsJsonArray();
                        if (versions.size() > 0) {
                            String latestVersion = versions.get(0).getAsJsonObject().get("version_number")
                                    .getAsString();
                            String currentVersion = plugin.getDescription().getVersion();

                            boolean outdated = isNewer(latestVersion, currentVersion);

                            // Build messages using MessagesManager if available
                            org.bg52.curiospaper.manager.MessagesManager msgMgr = null;
                            if (plugin instanceof org.bg52.curiospaper.CuriosPaper) {
                                msgMgr = ((org.bg52.curiospaper.CuriosPaper) plugin).getMessagesManager();
                            }

                            String statusMsg;
                            String downloadMsg;
                            if (msgMgr != null) {
                                statusMsg = outdated
                                        ? msgMgr.get("update.new-version", "version", latestVersion)
                                        : msgMgr.get("update.up-to-date", "version", currentVersion);
                                downloadMsg = outdated
                                        ? msgMgr.get("update.download", "id", MODRINTH_ID)
                                        : null;
                            } else {
                                statusMsg = outdated ? "§eA new version is available: §f" + latestVersion
                                        : "§aYou are running the latest version (§f" + currentVersion + "§a).";
                                downloadMsg = outdated
                                        ? "§eDownload it at: §fhttps://modrinth.com/project/" + MODRINTH_ID
                                        : null;
                            }

                            if (outdated) {
                                plugin.getLogger().warning(
                                        "A new version of " + plugin.getName() + " is available: " + latestVersion);
                                plugin.getLogger().warning("Current version: " + currentVersion);
                                plugin.getLogger()
                                        .warning("Download it at: https://modrinth.com/project/" + MODRINTH_ID);
                            } else {
                                plugin.getLogger().info("The latest version is running (" + currentVersion + ").");
                            }

                            // Notify online admins
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                                    if (player.hasPermission("curiospaper.admin")) {
                                        player.sendMessage(statusMsg);
                                        if (downloadMsg != null)
                                            player.sendMessage(downloadMsg);
                                    }
                                }
                            });

                            // Notify admins on join
                            Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                                @org.bukkit.event.EventHandler
                                public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                                    if (event.getPlayer().hasPermission("curiospaper.admin")) {
                                        event.getPlayer().sendMessage(statusMsg);
                                        if (downloadMsg != null)
                                            event.getPlayer().sendMessage(downloadMsg);
                                    }
                                }
                            }, plugin);
                        }
                    }
                } else {
                    plugin.getLogger().warning("Failed to check for updates: HTTP " + connection.getResponseCode());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        }).start();
    }

    private boolean isNewer(String latest, String current) {
        String[] v1 = latest.split("\\.");
        String[] v2 = current.split("\\.");
        int length = Math.max(v1.length, v2.length);
        for (int i = 0; i < length; i++) {
            int part1 = i < v1.length ? Integer.parseInt(v1[i].replaceAll("[^0-9]", "")) : 0;
            int part2 = i < v2.length ? Integer.parseInt(v2[i].replaceAll("[^0-9]", "")) : 0;
            if (part1 > part2)
                return true;
            if (part1 < part2)
                return false;
        }
        return false;
    }
}
