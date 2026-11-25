package org.bg52.curiospaper.resourcepack.host;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.synthetix.api.SynthetixAPI;
import org.bukkit.Bukkit;

import java.io.File;

/**
 * High-priority pack injector delegating resource pack serving, compilation,
 * and hosting to Synthetix.
 */
public class SynthetixPackInjector implements PackHostInjector {

    private final CuriosPaper plugin;

    public SynthetixPackInjector(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public PackHostPlugin getHostPlugin() {
        return PackHostPlugin.SYNTHETIX;
    }

    @Override
    public boolean inject(File packZip, File buildDir) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Synthetix")) {
            return false;
        }
        try {
            if (buildDir != null && buildDir.isDirectory()) {
                SynthetixAPI.get().registerResourcePack(plugin, buildDir);
            } else if (packZip != null && packZip.exists()) {
                SynthetixAPI.get().registerResourcePackZip(plugin, packZip);
            }
            SynthetixAPI.get().rebuildResourcePack();
            plugin.getLogger().info("[PackHost-Synthetix] Successfully registered CuriosPaper pack with Synthetix (Priority 1)!");
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("[PackHost-Synthetix] Failed to register pack with Synthetix: " + t.getMessage());
            return false;
        }
    }

    @Override
    public void registerDynamicListeners() {
        // Synthetix automatically merges registered pack folders
    }
}

