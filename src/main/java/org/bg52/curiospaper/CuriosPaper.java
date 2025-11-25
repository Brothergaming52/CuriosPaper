package org.bg52.curiospaper;

import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.api.CuriosPaperAPIImpl;
import org.bg52.curiospaper.command.BaublesCommand;
import org.bg52.curiospaper.config.ConfigManager;
import org.bg52.curiospaper.handler.ElytraBackSlotHandler;
import org.bg52.curiospaper.inventory.AccessoryGUI;
import org.bg52.curiospaper.listener.InventoryListener;
import org.bg52.curiospaper.listener.PlayerListener;
import org.bg52.curiospaper.manager.SlotManager;
import org.bg52.curiospaper.resourcepack.ResourcePackManager;
import org.bg52.curiospaper.util.AutoSaveTask;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class CuriosPaper extends JavaPlugin {
    private static CuriosPaper instance;
    private ConfigManager configManager;
    private SlotManager slotManager;
    private CuriosPaperAPI api;
    private AccessoryGUI gui;
    private AutoSaveTask autoSaveTask;
    private ResourcePackManager resourcePackManager;
    private ElytraBackSlotHandler elytraHandler;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);

        slotManager = new SlotManager(this);

        // Initialize Resource Pack Manager
        resourcePackManager = new ResourcePackManager(this);
        resourcePackManager.initialize();

        api = new CuriosPaperAPIImpl(this);

        gui = new AccessoryGUI(this);

        BaublesCommand baublesCommand = new BaublesCommand(this, gui);
        getCommand("baubles").setExecutor(baublesCommand);

        getServer().getPluginManager().registerEvents(new InventoryListener(this, gui), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Register Elytra Back Slot Handler if enabled
        if (getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
            elytraHandler = new ElytraBackSlotHandler(this);
            getServer().getPluginManager().registerEvents(elytraHandler, this);
            getLogger().info("Elytra back slot feature enabled!");
        }

        int saveInterval = getConfig().getInt("storage.save-interval", 300) * 20;
        autoSaveTask = new AutoSaveTask(this);
        autoSaveTask.runTaskTimer(this, saveInterval, saveInterval);

        getLogger().info("CuriosPaper has been enabled!");
        getLogger().info("Loaded " + configManager.getSlotConfigurations().size() + " slot types.");
    }

    @Override
    public void onDisable() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        if (resourcePackManager != null) {
            resourcePackManager.shutdown();
        }

        slotManager.saveAllPlayerData();

        getLogger().info("CuriosPaper has been disabled!");
    }

    public static CuriosPaper getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SlotManager getSlotManager() {
        return slotManager;
    }

    public CuriosPaperAPI getCuriosPaperAPI() {
        return api;
    }

    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    public NamespacedKey getSlotTypeKey() {
        return api.getSlotTypeKey();
    }

    public AccessoryGUI getGUI() {
        return gui;
    }
}
