package org.bg52.curiospaper.resourcepack.host;

import java.io.File;

/**
 * Strategy interface for injecting CuriosPaper's final resource pack into a host plugin.
 */
public interface PackHostInjector {

  /**
   * The host plugin associated with this injector.
   */
  PackHostPlugin getHostPlugin();

  /**
   * Injects CuriosPaper's final resource pack (both the zip and the unzipped build directory)
   * into the host plugin's combination pipeline.
   *
   * @param packZip The final compiled resource-pack.zip.
   * @param buildDir The unzipped final resource pack directory.
   * @return true if injection was successful.
   */
  boolean inject(File packZip, File buildDir);

  /**
   * Registers dynamic event listeners for the host plugin's pack generation cycle.
   */
  void registerDynamicListeners();
}
