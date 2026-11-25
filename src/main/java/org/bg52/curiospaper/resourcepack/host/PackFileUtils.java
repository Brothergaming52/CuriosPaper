package org.bg52.curiospaper.resourcepack.host;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility methods for copying and manipulating resource pack files and directories.
 */
public final class PackFileUtils {

  private PackFileUtils() {}

  /**
   * Copies a file or directory recursively to the target destination.
   */
  public static void copyRecursively(File source, File target) throws IOException {
    if (!source.exists()) {
      return;
    }

    if (source.isDirectory()) {
      if (!target.exists()) {
        target.mkdirs();
      }
      File[] children = source.listFiles();
      if (children != null) {
        for (File child : children) {
          copyRecursively(child, new File(target, child.getName()));
        }
      }
    } else {
      File parent = target.getParentFile();
      if (parent != null && !parent.exists()) {
        parent.mkdirs();
      }
      Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Writes a clean host zip containing strictly the assets/ hierarchy,
   * explicitly omitting pack.png and pack.mcmeta.
   * This prevents host plugins (such as Oraxen) from throwing duplicate file errors
   * ("Duplicate file detected: pack.png - Attempting to migrate it") and corrupting
   * their ZipOutputStream ("Failed to write zip entry pack.png: no current ZIP entry").
   *
   * @param packZip   Compiled resource-pack.zip fallback
   * @param buildDir  Compiled resource-pack-build folder
   * @param targetZip Target destination zip (e.g. plugins/Oraxen/pack/uploads/curiospaper.zip)
   */
  public static void writeHostCleanZip(File packZip, File buildDir, File targetZip) throws IOException {
    File parent = targetZip.getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }
    File tempZip = new File(parent, targetZip.getName() + ".tmp");
    if (tempZip.exists()) {
      tempZip.delete();
    }

    byte[] buffer = new byte[65536];

    try (FileOutputStream fos = new FileOutputStream(tempZip);
         BufferedOutputStream bos = new BufferedOutputStream(fos, 65536);
         ZipOutputStream zos = new ZipOutputStream(bos)) {
      zos.setLevel(java.util.zip.Deflater.BEST_SPEED);

      if (buildDir != null && buildDir.exists() && buildDir.isDirectory()) {
        Path sourcePath = buildDir.toPath();
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String entryPath = sourcePath.relativize(file).toString().replace('\\', '/');
            // Strictly exclude pack.png, pack.mcmeta, and only include assets/
            if (entryPath.equalsIgnoreCase("pack.png") || entryPath.equalsIgnoreCase("pack.mcmeta")
                || !entryPath.startsWith("assets/")) {
              return FileVisitResult.CONTINUE;
            }

            ZipEntry entry = new ZipEntry(entryPath);
            zos.putNextEntry(entry);
            try (InputStream in = new BufferedInputStream(Files.newInputStream(file), 65536)) {
              int len;
              while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
              }
            }
            zos.closeEntry();
            return FileVisitResult.CONTINUE;
          }
        });
      } else if (packZip != null && packZip.exists() && packZip.isFile()) {
        try (ZipFile zf = new ZipFile(packZip)) {
          Enumeration<? extends ZipEntry> entries = zf.entries();
          while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
              continue;
            }
            String entryPath = entry.getName().replace('\\', '/');
            if (entryPath.equalsIgnoreCase("pack.png") || entryPath.equalsIgnoreCase("pack.mcmeta")
                || !entryPath.startsWith("assets/")) {
              continue;
            }

            ZipEntry newEntry = new ZipEntry(entryPath);
            zos.putNextEntry(newEntry);
            try (InputStream in = new BufferedInputStream(zf.getInputStream(entry), 65536)) {
              int len;
              while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
              }
            }
            zos.closeEntry();
          }
        }
      }
    }

    if (targetZip.exists()) {
      targetZip.delete();
    }
    if (!tempZip.renameTo(targetZip)) {
      Files.move(tempZip.toPath(), targetZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Reads all files from a directory recursively into a map of relative path -> byte[].
   */
  public static Map<String, byte[]> readDirectoryEntries(File dir) throws IOException {
    Map<String, byte[]> entries = new HashMap<>();
    if (dir == null || !dir.exists() || !dir.isDirectory()) {
      return entries;
    }
    Path rootPath = dir.toPath();
    Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String relative = rootPath.relativize(file).toString().replace('\\', '/');
        entries.put(relative, Files.readAllBytes(file));
        return FileVisitResult.CONTINUE;
      }
    });
    return entries;
  }

  /**
   * Reads all entries from a zip file into a map of path -> byte[].
   */
  public static Map<String, byte[]> readZipEntries(File zipFile) throws IOException {
    Map<String, byte[]> entries = new HashMap<>();
    if (zipFile == null || !zipFile.exists() || !zipFile.isFile()) {
      return entries;
    }

    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry entry;
      byte[] buffer = new byte[8192];
      while ((entry = zis.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          int len;
          while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
          }
          entries.put(entry.getName().replace('\\', '/'), baos.toByteArray());
        }
        zis.closeEntry();
      }
    }
    return entries;
  }
}

