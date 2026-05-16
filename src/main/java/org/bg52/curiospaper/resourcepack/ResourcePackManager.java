package org.bg52.curiospaper.resourcepack;

import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class ResourcePackManager {
  private final CuriosPaper plugin;
  private final File resourcePackDir;
  private final File packFile;
  private final File externalPacksDir;
  private final File tempDir;

  public static class SourceEntry {
    public final Plugin plugin;
    public final File folder;

    public SourceEntry(Plugin plugin, File folder) {
      this.plugin = plugin;
      this.folder = folder;
    }
  }

  private final List<SourceEntry> registeredSources;
  private ResourcePackHost server;
  private String packHash;

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  // Dirty build flag
  private boolean dirty = false;

  // Namespace rules
  private final Set<String> reservedNamespaces = new HashSet<>(Arrays.asList("curiospaper"));
  private final Map<String, Plugin> namespaceOwners = new HashMap<>();

  // Conflict tracking
  private final List<String> conflictLog = new ArrayList<>();
  private final List<String> namespaceConflictLog = new ArrayList<>();

  // Config options
  private boolean allowMinecraftNamespace;
  private boolean allowNamespaceConflicts;

  public ResourcePackManager(CuriosPaper plugin) {
    this.plugin = plugin;
    this.resourcePackDir = new File(plugin.getDataFolder(), "resource-pack-build");
    this.packFile = new File(plugin.getDataFolder(), "resource-pack.zip");
    this.externalPacksDir = new File(plugin.getDataFolder(), "external-resource-packs");
    this.tempDir = new File(this.externalPacksDir, "temp");
    this.registeredSources = new ArrayList<>();
  }

  // --- Exposed for commands / debugging ---

  public List<String> getConflictLog() {
    return Collections.unmodifiableList(conflictLog);
  }

  public List<String> getNamespaceConflictLog() {
    return Collections.unmodifiableList(namespaceConflictLog);
  }

  public List<SourceEntry> getRegisteredSources() {
    return Collections.unmodifiableList(registeredSources);
  }

  public Set<String> getRegisteredNamespaces() {
    return Collections.unmodifiableSet(namespaceOwners.keySet());
  }

  public File getPackFile() {
    return packFile;
  }

  public boolean isDirty() {
    return dirty;
  }

  public void registerResource(Plugin plugin, File sourceFolder) {
    if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
      plugin.getLogger().warning("Resource folder missing for plugin: " + plugin.getName());
      return;
    }

    // 1) Detect namespace from folder
    // Required folder structure: assets/<namespace>/
    File assets = new File(sourceFolder, "assets");
    if (!assets.exists()) {
      plugin.getLogger().warning("Plugin " + plugin.getName() +
          " tried to register a resource folder WITHOUT /assets/. Ignored.");
      return;
    }

    File[] namespaces = assets.listFiles(File::isDirectory);
    if (namespaces == null || namespaces.length == 0) {
      plugin.getLogger().warning("Plugin " + plugin.getName() +
          " has no namespaces inside /assets/. Ignored.");
      return;
    }

    for (File nsFolder : namespaces) {
      String namespace = nsFolder.getName().toLowerCase(Locale.ROOT);

      // Reserved namespace check
      if (reservedNamespaces.contains(namespace)) {
        if (plugin == this.plugin) {
          // CuriosPaper itself – allowed
          namespaceOwners.put(namespace, plugin);
          continue;
        }

        String msg = "[CuriosPaper-RP] Plugin " + plugin.getName()
            + " attempted to use reserved namespace '" + namespace + "'. "
            + "Its resources will NOT be registered. Use your own namespace.";

        this.plugin.getLogger().severe(msg);
        namespaceConflictLog.add("RESERVED NAMESPACE: plugin=" + plugin.getName()
            + " namespace=" + namespace);

        return; // abort registration for this plugin
      }

      // Minecraft namespace check
      if (namespace.equals("minecraft")) {
        if (plugin == this.plugin) {
          // Core plugin is always allowed to touch minecraft namespace
          namespaceOwners.put(namespace, plugin);
          continue;
        }

        if (!allowMinecraftNamespace) {
          String msg = "[CuriosPaper-RP] Plugin " + plugin.getName()
              + " attempted to override minecraft namespace but config disallows it. "
              + "Skipping its 'minecraft' assets.";

          this.plugin.getLogger().warning(msg);
          namespaceConflictLog.add("MINECRAFT NAMESPACE BLOCKED: plugin=" + plugin.getName());
          continue;
        }
        // else fall through to ownership checks
      }

      // Namespace ownership enforcement
      if (namespaceOwners.containsKey(namespace)) {
        Plugin owner = namespaceOwners.get(namespace);

        // minecraft namespace is always shared/mergeable — file-level
        // conflicts are resolved later in copyFolderStrict/handleConflict
        if (namespace.equals("minecraft")) {
          this.plugin.getLogger().info("[CuriosPaper-RP] Merging 'minecraft' namespace resources from "
              + plugin.getName() + " (owned by " + owner.getName() + ").");
          continue;
        }

        if (owner != plugin && !allowNamespaceConflicts) {
          String msg = "[CuriosPaper-RP] Namespace '" + namespace
              + "' is already owned by plugin " + owner.getName()
              + ". Resources from " + plugin.getName() + " will NOT be registered.";

          this.plugin.getLogger().severe(msg);
          namespaceConflictLog.add("NAMESPACE OWNER CONFLICT: namespace=" + namespace
              + " owner=" + owner.getName()
              + " conflicting=" + plugin.getName());

          return;
        }

        if (owner != plugin && allowNamespaceConflicts) {
          String msg = "[CuriosPaper-RP] WARNING: Namespace conflict allowed for '"
              + namespace + "' between " + owner.getName() + " and " + plugin.getName();
          this.plugin.getLogger().warning(msg);
          // Optional: also log as a soft conflict:
          namespaceConflictLog.add("NAMESPACE CONFLICT ALLOWED: namespace=" + namespace
              + " owner=" + owner.getName()
              + " conflicting=" + plugin.getName());
        }
      } else {
        namespaceOwners.put(namespace, plugin);
      }
    }

    registeredSources.add(new SourceEntry(plugin, sourceFolder));
    dirty = true;
  }

  public void initialize() {

    this.allowMinecraftNamespace = plugin.getConfig().getBoolean("resource-pack.allow-minecraft-namespace", false);

    this.allowNamespaceConflicts = plugin.getConfig().getBoolean("resource-pack.allow-namespace-conflicts", false);

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

    // register own resources — NO auto-build
    registerResource(plugin, ownResources);

    // set dirty, but don’t build yet
    dirty = true;

    // Process external zip packs before scheduling the build
    processExternalPacks();

    // delayed build — allow addons time to register
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      if (dirty)
        generatePack();
    }, 200L); // 10 seconds

    // Start server if enabled
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
        if (!name.startsWith(jarPrefix))
          continue;

        String relativePath = name.substring(jarPrefix.length());
        if (relativePath.isEmpty())
          continue; // it's just the folder itself

        File outFile = new File(targetRoot, relativePath);

        if (entry.isDirectory()) {
          // Ensure directory exists
          if (!outFile.exists() && !outFile.mkdirs()) {
            plugin.getLogger()
                .warning("Failed to create directory for resource: " + outFile.getAbsolutePath());
          }
          continue;
        }

        // Don’t overwrite user-edited files – only create if missing
        if (outFile.exists())
          continue;

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

  private void processExternalPacks() {
    if (!plugin.getConfig().getBoolean("resource-pack.combine-external-rp", false)) {
      return;
    }

    if (!externalPacksDir.exists()) {
      externalPacksDir.mkdirs();
      plugin.getLogger().info("Created " + externalPacksDir.getName() + " directory for external resource packs.");
      return; // Nothing to process yet
    }

    File[] zipFiles = externalPacksDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".zip"));

    if (zipFiles == null || zipFiles.length == 0) {
      return;
    }

    plugin.getLogger().info("Found " + zipFiles.length + " external resource pack(s). Processing...");

    // Clean up temp dir if it exists from a previous bad run
    if (tempDir.exists()) {
      deleteDirectory(tempDir);
    }
    tempDir.mkdirs();

    for (File zipFile : zipFiles) {
      try {
        String folderName = zipFile.getName().substring(0, zipFile.getName().length() - 4);
        File extractDir = new File(tempDir, folderName);

        plugin.getLogger().info("Extracting external pack: " + zipFile.getName());
        unzip(zipFile, extractDir);

        File validRoot = findAssetsFolder(extractDir);
        if (validRoot != null) {
          // Register the extracted resource pack using CuriosPaper as the 'owner' plugin
          // wrapper
          registerResource(plugin, validRoot);
          plugin.getLogger().info("Successfully registered external pack: " + zipFile.getName());
        } else {
          plugin.getLogger().warning("Failed to find 'assets' folder in external pack: " + zipFile.getName());
        }
      } catch (Exception e) {
        plugin.getLogger().severe("Error processing external pack " + zipFile.getName() + ": " + e.getMessage());
        e.printStackTrace();
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
    dirty = false;
    conflictLog.clear();

    plugin.getLogger().info("Building CuriosPaper resource pack...");

    if (resourcePackDir.exists())
      deleteDirectory(resourcePackDir);
    resourcePackDir.mkdirs();

    for (SourceEntry entry : registeredSources) {
      Plugin pl = entry.plugin;
      File src = entry.folder;

      try {
        copyFolderStrict(pl, src, resourcePackDir);
      } catch (Exception e) {
        plugin.getLogger().severe("Failed while copying " + pl.getName() + ": " + e.getMessage());
      }
    }

    // Ensure mcmeta exists
    if (!new File(resourcePackDir, "pack.mcmeta").exists()) {
      createDefaultMcmeta(resourcePackDir);
    }

    try {
      zipDirectory(resourcePackDir, packFile);
      this.packHash = calculateSha1(packFile);
      plugin.getLogger().info("Pack built. Hash: " + this.packHash);

      // Clean up extracted temp resource packs to save space
      if (tempDir.exists()) {
        deleteDirectory(tempDir);
        plugin.getLogger().info("Cleaned up temporary extracted resource pack files.");
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to zip pack: " + e.getMessage());
    }

    if (!conflictLog.isEmpty()) {
      plugin.getLogger().warning("=== CuriosPaper Resource Pack Conflicts ===");
      conflictLog.forEach(plugin.getLogger()::warning);
    }
  }

  private void createDefaultMcmeta(File dir) {
    File mcmeta = new File(dir, "pack.mcmeta");
    try (FileWriter writer = new FileWriter(mcmeta)) {
      writer.write("{\n" +
          " \"pack\": {\n" +
          "  \"pack_format\": 15,\n" +
          "  \"description\": \"CuriosPaper Generated Pack\"\n" +
          " }\n" +
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

  private void copyFolderStrict(Plugin plugin, File source, File target) throws IOException {
    if (source.isDirectory()) {
      if (!target.exists())
        target.mkdirs();

      for (File f : Objects.requireNonNull(source.listFiles())) {
        copyFolderStrict(plugin, f, new File(target, f.getName()));
      }

      return;
    }

    // File
    if (target.exists()) {
      handleConflict(plugin, source, target);
      return;
    }

    Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
  }

  private void handleConflict(Plugin conflictingPlugin, File src, File dest) throws IOException {
    String path = dest.getPath().replace("\\", "/");

    // Reject everything EXCEPT curated mergeable files
    if (isMergeableJson(path)) {
      mergeJsonFiles(src, dest);
      return;
    }

    conflictLog.add("CONFLICT: " + path +
        " | Existing owner: " + findOwnerByPath(path) +
        " | Conflicting plugin: " + conflictingPlugin.getName());

    // keep first file, skip conflicting one
  }

  private boolean isMergeableJson(String path) {
    return path.endsWith("curios_combined_override.json")
        || path.endsWith("curios_item_base.json")
        || (path.contains("/assets/minecraft/") && path.endsWith(".json"));
  }

  private void mergeJsonFiles(File src, File dest) throws IOException {
    JsonElement destJson;
    JsonElement srcJson;

    try (Reader destReader = new FileReader(dest);
        Reader srcReader = new FileReader(src)) {

      destJson = new com.google.gson.JsonParser().parse(destReader);
      srcJson = new com.google.gson.JsonParser().parse(srcReader);
    }

    if (!destJson.isJsonObject() || !srcJson.isJsonObject()) {
      plugin.getLogger().warning("Cannot merge JSON (non-object): " + dest.getPath());
      return;
    }

    JsonObject destObj = destJson.getAsJsonObject();
    JsonObject srcObj = srcJson.getAsJsonObject();

    deepMerge(destObj, srcObj);

    try (Writer writer = new FileWriter(dest)) {
      gson.toJson(destObj, writer);
    }

    plugin.getLogger().info("Merged JSON file: " + dest.getPath());
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
            destArray.add(el);
          }
        } else {
          // If primitives or mismatched types, we could overwrite or keep dest.
          // Keeping destination is usually safer to maintain the base file's core
          // properties (like parent model).
        }
      }
    }
  }

  /**
   * Try to find which plugin owns the existing file at 'path' by matching
   * relative paths against each registered source.
   */
  private String findOwnerByPath(String path) {
    try {
      Path destPath = Paths.get(path);
      Path root = resourcePackDir.toPath();

      Path rel;
      try {
        rel = root.relativize(destPath);
      } catch (IllegalArgumentException e) {
        // Not under our root (shouldn't happen)
        return "Unknown";
      }

      for (SourceEntry entry : registeredSources) {
        Plugin pl = entry.plugin;
        File srcRoot = entry.folder;

        File candidate = new File(srcRoot, rel.toString());
        if (candidate.exists()) {
          return pl.getName();
        }
      }
    } catch (Exception e) {
      // ignore, fall through
    }
    return "Unknown";
  }

  private void zipDirectory(File sourceDir, File zipFile) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {

      Path sourcePath = sourceDir.toPath();
      Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          zos.putNextEntry(new ZipEntry(sourcePath
              .relativize(file)
              .toString()
              .replace("\\", "/")));
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
      int n;
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

  private void unzip(File zipFile, File destDir) throws IOException {
    if (!destDir.exists()) {
      destDir.mkdirs();
    }
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        File newFile = newFile(destDir, zipEntry);
        if (zipEntry.isDirectory()) {
          if (!newFile.isDirectory() && !newFile.mkdirs()) {
            throw new IOException("Failed to create directory " + newFile);
          }
        } else {
          File parent = newFile.getParentFile();
          if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory " + parent);
          }
          try (FileOutputStream fos = new FileOutputStream(newFile)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          }
        }
        zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
    }
  }

  private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
    File destFile = new File(destinationDir, zipEntry.getName());

    String destDirPath = destinationDir.getCanonicalPath();
    String destFilePath = destFile.getCanonicalPath();

    if (!destFilePath.startsWith(destDirPath + File.separator)) {
      throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
    }

    return destFile;
  }

  private File findAssetsFolder(File root) {
    if (root == null || !root.exists() || !root.isDirectory())
      return null;

    File assetsFolder = new File(root, "assets");
    if (assetsFolder.exists() && assetsFolder.isDirectory()) {
      return root; // Return the folder that CONTAINS assets, as that's what registerResource
                   // expects
    }

    File[] files = root.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          File found = findAssetsFolder(file);
          if (found != null) {
            return found;
          }
        }
      }
    }
    return null;
  }
}
