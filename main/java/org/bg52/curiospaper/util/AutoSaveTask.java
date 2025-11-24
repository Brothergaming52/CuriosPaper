package org.bg52.curiospaper.util;

import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoSaveTask extends BukkitRunnable {
    private final CuriosPaper plugin;

    public AutoSaveTask(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getSlotManager().saveAllPlayerData();
        plugin.getLogger().info("Auto-saved all player accessory data.");
    }
}
