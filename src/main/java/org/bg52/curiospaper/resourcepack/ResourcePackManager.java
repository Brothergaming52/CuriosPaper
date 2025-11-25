package org.bg52.curiospaper.resourcepack;

import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackManager {
    private final CuriosPaper plugin;
    private final File resourcePackDir;
    private final File packFile;
    private final Map<Plugin, File> registeredSources;
    private ResourcePackHost server;
    private String packHash;

    public ResourcePackManager(CuriosPaper plugin) {
        this.plugin = plugin;
        this.resourcePackDir = new File(plugin.getDataFolder(), "resource-pack-build");
        this.packFile = new File(plugin.getDataFolder(), "resource-pack.zip");
        this.registeredSources = new HashMap<>();
    }

    public void registerResource(Plugin plugin, File sourceFolder) {
        if (sourceFolder.exists() && sourceFolder.isDirectory()) {
            registeredSources.put(plugin, sourceFolder);
            this.plugin.getLogger().info("Registered resource pack source for plugin: " + plugin.getName());
            generatePack();
        } else {
            this.plugin.getLogger().warning(
                    "Failed to register resource pack source for " + plugin.getName() + ": Directory not found.");
        }
    }

    public void initialize() {
        // Target: <plugin data folder>/resources
        File ownResources = new File(plugin.getDataFolder(), "resources");
        if (!ownResources.exists()) {
            if (!ownResources.mkdirs()) {
                plugin.getLogger().severe("Failed to create resources directory: " + ownResources.getAbsolutePath());
            }
        }

        // 1) Stream ALL files from /resources in the JAR into the data folder
        try {
            extractEmbeddedResourcesFolder("resources/", ownResources);
            //plugin.getLogger().info("Extracted embedded resources to: " + ownResources.getAbsolutePath());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to extract embedded resources: " + e.getMessage());
            e.printStackTrace();
        }

        // 2) Ensure pack.mcmeta exists (either from JAR or generated)
        File mcmeta = new File(ownResources, "pack.mcmeta");
        if (!mcmeta.exists()) {
            createDefaultMcmeta(ownResources);
            plugin.getLogger().info("Created default pack.mcmeta in resources folder.");
        }

        // 3) Register own resources as a pack source
        registerResource(plugin, ownResources);

        // Generate pack from all registered sources
        //generatePack();

        // 5) Start server if enabled
        if (plugin.getConfig().getBoolean("resource-pack.enabled", false)) {
            int port = plugin.getConfig().getInt("resource-pack.port", 8080);
            server = new ResourcePackServer(plugin, port, packFile);
            server.start();
        }
    }

    private void extractEmbeddedResourcesFolder(String jarPrefix, File targetRoot) throws Exception {
        // jarPrefix should end with "/", e.g. "resources/"
        if (!jarPrefix.endsWith("/")) {
            jarPrefix = jarPrefix + "/";
        }

        // Locate the plugin JAR file
        java.net.URL jarUrl = plugin.getClass()
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();

        if (jarUrl == null) {
            plugin.getLogger().warning("Could not locate plugin JAR; skipping embedded resources extraction.");
            return;
        }

        File jarFile = new File(jarUrl.toURI());
        if (!jarFile.isFile()) {
            plugin.getLogger().warning("Plugin code source is not a file: " + jarFile.getAbsolutePath());
            return;
        }

        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String name = entry.getName();

                // Only care about entries under the given prefix, e.g. "resources/"
                if (!name.startsWith(jarPrefix)) continue;

                String relativePath = name.substring(jarPrefix.length());
                if (relativePath.isEmpty()) continue; // it's just the folder itself

                File outFile = new File(targetRoot, relativePath);

                if (entry.isDirectory()) {
                    // Ensure directory exists
                    if (!outFile.exists() && !outFile.mkdirs()) {
                        plugin.getLogger().warning("Failed to create directory for resource: " + outFile.getAbsolutePath());
                    }
                    continue;
                }

                // Don’t overwrite user-edited files – only create if missing
                if (outFile.exists()) continue;

                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    plugin.getLogger().warning("Failed to create parent directories for: " + outFile.getAbsolutePath());
                    continue;
                }

                try (InputStream in = jar.getInputStream(entry);
                     OutputStream out = new FileOutputStream(outFile)) {

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }
        }
    }


    public void shutdown() {
        if (server != null) {
            server.stop();
        }
    }

    public String getPackHash() {
        return packHash;
    }

    public String getPackUrl() {
        String host = plugin.getConfig().getString("resource-pack.host-ip", "localhost");
        int port = plugin.getConfig().getInt("resource-pack.port", 8080);

        if (port == -1 && server != null) {
            port = server.getPort();
        }

        return "http://" + host + ":" + port + "/pack.zip";
    }

    public void generatePack() {
        plugin.getLogger().info("Generating resource pack...");

        // Clean build directory
        if (resourcePackDir.exists()) {
            deleteDirectory(resourcePackDir);
        }
        resourcePackDir.mkdirs();

        // Copy all registered resources
        for (Map.Entry<Plugin, File> entry : registeredSources.entrySet()) {
            try {
                copyDirectory(entry.getValue(), resourcePackDir);
            } catch (IOException e) {
                plugin.getLogger()
                        .severe("Failed to copy resources for " + entry.getKey().getName() + ": " + e.getMessage());
            }
        }

        // Ensure pack.mcmeta exists
        if (!new File(resourcePackDir, "pack.mcmeta").exists()) {
            createDefaultMcmeta(resourcePackDir);
        }

        // Zip it
        try {
            zipDirectory(resourcePackDir, packFile);
            plugin.getLogger().info("Resource pack generated successfully: " + packFile.getName());

            // Calculate hash
            this.packHash = calculateSha1(packFile);
            plugin.getLogger().info("Pack Hash: " + packHash);

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to zip resource pack: " + e.getMessage());
        }
    }

    private void createDefaultMcmeta(File dir) {
        File mcmeta = new File(dir, "pack.mcmeta");
        try (FileWriter writer = new FileWriter(mcmeta)) {
            writer.write("{\n" +
                    "  \"pack\": {\n" +
                    "    \"pack_format\": 15,\n" +
                    "    \"description\": \"CuriosPaper Generated Pack\"\n" +
                    "  }\n" +
                    "}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (!target.exists()) {
            target.mkdirs();
        }

        for (String file : source.list()) {
            File srcFile = new File(source, file);
            File destFile = new File(target, file);

            if (srcFile.isDirectory()) {
                copyDirectory(srcFile, destFile);
            } else {
                Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Path sourcePath = sourceDir.toPath();
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(sourcePath.relativize(file).toString().replace("\\", "/")));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private String calculateSha1(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int n = 0;
            while ((n = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}