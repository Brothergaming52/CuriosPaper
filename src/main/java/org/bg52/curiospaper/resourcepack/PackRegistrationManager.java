package org.bg52.curiospaper.resourcepack;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.resourcepack.host.PackHostManager;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Manages resource pack asset extraction, geyser support, and automated registration
 * with host engines (Synthetix first priority, then Nexo, ItemsAdder, Oraxen, CraftEngine, RSPM).
 */
public class PackRegistrationManager {

    private final CuriosPaper plugin;
    private final File resourcesDir;
    private final PackHostManager hostManager;

    public PackRegistrationManager(CuriosPaper plugin) {
        this.plugin = plugin;
        this.resourcesDir = new File(plugin.getDataFolder(), "resources");
        this.hostManager = new PackHostManager(plugin);
    }

    public void initialize() {
        // 1. Extract embedded resource assets (textures, models, equipment, pack.png)
        extractResources();

        // 2. Extract Geyser Bedrock mappings
        extractGeyserFiles();

        // 3. Initialize host manager and delegate to host engines
        hostManager.initialize();

        // 4. Inject resources into the active host engine
        if (hostManager.isActive()) {
            hostManager.injectFinalPack(null, resourcesDir);
            plugin.getLogger().info("[PackManager] CuriosPaper resources registered with "
                    + hostManager.getActiveHost().getDisplayName() + "!");
        } else {
            plugin.getLogger().info("[PackManager] No external pack host detected. Resources saved to: "
                    + resourcesDir.getPath());
        }
    }

    public void shutdown() {
        hostManager.shutdown();
    }

    public File getResourcesDir() {
        return resourcesDir;
    }

    public PackHostManager getHostManager() {
        return hostManager;
    }

    private void extractResources() {
        try {
            if (!resourcesDir.exists()) {
                resourcesDir.mkdirs();
            }
            extractEmbeddedResourcesFolder(plugin, "resources/", resourcesDir);
        } catch (Throwable t) {
            plugin.getLogger().warning("[PackManager] Failed to extract embedded resources: " + t.getMessage());
        }
    }

    private void extractGeyserFiles() {
        try {
            File geyserDir = new File(plugin.getDataFolder(), "geyser");
            if (!geyserDir.exists()) {
                geyserDir.mkdirs();
            }

            File mappingsFile = new File(geyserDir, "CuriosPaper_mappings.json");
            try (InputStream in = plugin.getResource("CuriosPaper_mappings.json")) {
                if (in != null) {
                    Files.copy(in, mappingsFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }

            File zipFile = new File(geyserDir, "CuriosPaper_Geyser.zip");
            try (InputStream in = plugin.getResource("CuriosPaper_Geyser.zip")) {
                if (in != null) {
                    Files.copy(in, zipFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }

            deployToGeyserDirectory(mappingsFile, zipFile);
        } catch (Throwable t) {
            plugin.getLogger().warning("[PackManager] Could not extract Geyser files: " + t.getMessage());
        }
    }

    private void deployToGeyserDirectory(File mappingsFile, File zipFile) {
        try {
            File pluginsFolder = plugin.getDataFolder().getParentFile();
            if (pluginsFolder == null) return;

            File geyserFolder = new File(pluginsFolder, "Geyser-Spigot");
            if (!geyserFolder.exists()) {
                geyserFolder = new File(pluginsFolder, "Geyser");
            }

            if (geyserFolder.exists() && geyserFolder.isDirectory()) {
                File customMappingsDir = new File(geyserFolder, "custom_mappings");
                if (!customMappingsDir.exists()) customMappingsDir.mkdirs();
                if (mappingsFile != null && mappingsFile.exists()) {
                    File destMappings = new File(customMappingsDir, mappingsFile.getName());
                    Files.copy(mappingsFile.toPath(), destMappings.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("[PackManager] Deployed " + mappingsFile.getName() + " to " + geyserFolder.getName() + "/custom_mappings");
                }

                File packsDir = new File(geyserFolder, "packs");
                if (!packsDir.exists()) packsDir.mkdirs();
                if (zipFile != null && zipFile.exists()) {
                    File destPack = new File(packsDir, zipFile.getName());
                    Files.copy(zipFile.toPath(), destPack.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("[PackManager] Deployed " + zipFile.getName() + " to " + geyserFolder.getName() + "/packs");
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[PackManager] Failed to deploy Geyser assets: " + t.getMessage());
        }
    }

    private void extractEmbeddedResourcesFolder(Plugin sourcePlugin, String jarPrefix, File targetRoot) throws Exception {
        if (!jarPrefix.endsWith("/")) {
            jarPrefix = jarPrefix + "/";
        }

        URL jarUrl = sourcePlugin.getClass().getProtectionDomain().getCodeSource().getLocation();
        if (jarUrl == null) {
            return;
        }

        File jarFile = new File(jarUrl.toURI());
        if (!jarFile.isFile()) {
            return;
        }

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!name.startsWith(jarPrefix)) continue;
                String relativePath = name.substring(jarPrefix.length());
                if (relativePath.isEmpty()) continue;

                File outFile = new File(targetRoot, relativePath);
                if (entry.isDirectory()) {
                    if (!outFile.exists()) outFile.mkdirs();
                    continue;
                }

                if (!outFile.exists()) {
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    try (InputStream in = jar.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(outFile)) {
                        in.transferTo(out);
                    }
                }
            }
        }
    }
}

