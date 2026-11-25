package org.bg52.curiospaper;

import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.api.CuriosPaperAPIImpl;
import org.bg52.curiospaper.command.CuriosCommand;
import org.bg52.curiospaper.engine.ItemEngineManager;
import org.bg52.curiospaper.handler.ElytraBackSlotHandler;
import org.bg52.curiospaper.inventory.CuriosGUI;
import org.bg52.curiospaper.inventory.InspectGUI;
import org.bg52.curiospaper.inventory.QuickEquipListener;
import org.bg52.curiospaper.keybind.CuriosKeybindHook;
import org.bg52.curiospaper.lang.LanguageManager;
import org.bg52.curiospaper.listener.AbilityListener;
import org.bg52.curiospaper.listener.PlayerJoinQuitListener;
import org.bg52.curiospaper.model.PacketModelManager;
import org.bg52.curiospaper.resourcepack.PackRegistrationManager;
import org.bg52.curiospaper.slot.SlotRegistry;
import org.bg52.curiospaper.storage.CuriosStorageManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * CuriosPaper 3.0 - High-Performance Modular Accessory System for Paper 1.20.6 to 1.21+.
 */
public final class CuriosPaper extends JavaPlugin {

    private static CuriosPaper instance;

    private LanguageManager languageManager;
    private SlotRegistry slotRegistry;
    private ItemEngineManager itemEngineManager;
    private CuriosStorageManager storageManager;
    private CuriosGUI curiosGUI;
    private InspectGUI inspectGUI;
    private ElytraBackSlotHandler elytraBackSlotHandler;
    private PacketModelManager packetModelManager;
    private CuriosKeybindHook keybindHook;
    private PackRegistrationManager packRegistrationManager;
    private AbilityListener abilityListener;
    private org.bg52.curiospaper.citizens.CitizensManager citizensManager;
    private CuriosPaperAPI api;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize unified Folia/Paper cross-platform scheduler
        new org.bg52.curiospaper.scheduler.PlatformScheduler(this);

        // 1. Save default config and resources
        saveDefaultConfig();

        // 2. Initialize Language Manager
        this.languageManager = new LanguageManager(this);

        getLogger().info("==================================================================");
        getLogger().info("  CuriosPaper 3.0 (Paper 1.20.6 - 1.21.x+)");
        getLogger().info("  Clean Architecture | Synthetix Engine Soft-Dependency");
        getLogger().info("==================================================================");

        // 3. Initialize Slot Registry (slots.yml)
        this.slotRegistry = new SlotRegistry(this);

        // 4. Initialize Item Engine Manager (Synthetix, Nexo, ItemsAdder, Oraxen, CraftEngine)
        this.itemEngineManager = new ItemEngineManager(this);
        this.itemEngineManager.initialize();

        // 5. Initialize Storage Manager (Unified Synthetix AddonDataStore or fallback YAML)
        this.storageManager = new CuriosStorageManager(this);
        this.storageManager.initialize();

        // 6. Initialize Packet-Level 3D Model Manager
        this.packetModelManager = new PacketModelManager(this);
        this.packetModelManager.initialize();
        getServer().getPluginManager().registerEvents(this.packetModelManager, this);

        // 7. Initialize GUI Controllers
        this.curiosGUI = new CuriosGUI(this);
        getServer().getPluginManager().registerEvents(this.curiosGUI, this);

        this.inspectGUI = new InspectGUI(this);
        getServer().getPluginManager().registerEvents(this.inspectGUI, this);

        // 8. Initialize Gameplay Handlers
        this.elytraBackSlotHandler = new ElytraBackSlotHandler(this);
        getServer().getPluginManager().registerEvents(this.elytraBackSlotHandler, this);

        QuickEquipListener quickEquipListener = new QuickEquipListener(this);
        getServer().getPluginManager().registerEvents(quickEquipListener, this);

        PlayerJoinQuitListener sessionListener = new PlayerJoinQuitListener(this);
        getServer().getPluginManager().registerEvents(sessionListener, this);

        // Attribute modifier and ability applicator
        this.abilityListener = new AbilityListener(this);
        getServer().getPluginManager().registerEvents(this.abilityListener, this);

        // Optional Citizens NPC Integration
        this.citizensManager = new org.bg52.curiospaper.citizens.CitizensManager(this);
        this.citizensManager.initialize();

        // 9. Initialize Keybind Integration (Synthetix Keybind Engine or Standalone)
        this.keybindHook = new CuriosKeybindHook(this);
        this.keybindHook.initialize();

        // 10. Initialize Resource Pack Extraction & Registration (Synthetix Priority 1)
        this.packRegistrationManager = new PackRegistrationManager(this);
        this.packRegistrationManager.initialize();

        // 11. Hook into Synthetix Item Editor Extension SPI
        try {
            if (getServer().getPluginManager().isPluginEnabled("Synthetix") && org.bg52.synthetix.api.SynthetixAPI.getOptional().isPresent()) {
                org.bg52.synthetix.api.SynthetixAPI.get().getEditorRegistry().registerCategory(
                        new org.bg52.curiospaper.engine.synthetix.CuriosItemEditorCategory(this)
                );
                getLogger().info("[Synthetix] Hooked Curios accessory settings into Synthetix Item Editor!");
            }
        } catch (Throwable ignored) {}

        // 12. Register Public API Service
        this.api = new CuriosPaperAPIImpl(this);
        getServer().getServicesManager().register(CuriosPaperAPI.class, this.api, this, ServicePriority.Normal);

        // 13. Register PlaceholderAPI Expansion
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new org.bg52.curiospaper.placeholder.CuriosExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI!");
        }

        // 14. Register Commands
        PluginCommand curiosCmd = getCommand("curios");
        if (curiosCmd != null) {
            CuriosCommand executor = new CuriosCommand(this);
            curiosCmd.setExecutor(executor);
            curiosCmd.setTabCompleter(executor);

            PluginCommand baublesCmd = getCommand("baubles");
            if (baublesCmd != null) {
                baublesCmd.setExecutor(executor);
                baublesCmd.setTabCompleter(executor);
            }
        }

        // 15. Schedule a 1-tick delayed post-boot scan so all external plugins (PaperArtifacts, Nexo, Oraxen, etc.)
        // have fully registered their custom items before we finalize active GUI slots.
        org.bg52.curiospaper.scheduler.PlatformScheduler.get().runGlobalLater(() -> {
            if (this.slotRegistry != null) {
                this.slotRegistry.refreshActiveSlots();
                getLogger().info("[SlotRegistry] Post-boot scan completed. Active accessory slots with registered items: " + this.slotRegistry.getActiveSlotIds());
            }
        }, 1L);

        int pluginId = 29508;
        new Metrics(this, pluginId);

        getLogger().info("CuriosPaper 3.0 successfully enabled!");
    }

    @Override
    public void onDisable() {
        // Unregister service
        getServer().getServicesManager().unregisterAll(this);
        org.bg52.curiospaper.scheduler.PlatformScheduler.get().cancelTasks();

        // Shutdown components in reverse order
        try {
            if (getServer().getPluginManager().isPluginEnabled("Synthetix") && org.bg52.synthetix.api.SynthetixAPI.getOptional().isPresent()) {
                org.bg52.synthetix.api.SynthetixAPI.get().getEditorRegistry().unregisterCategory("curios_accessory");
            }
        } catch (Throwable ignored) {}

        if (citizensManager != null) {
            citizensManager.shutdown();
        }

        if (abilityListener != null) {
            abilityListener.shutdown();
        }

        if (packRegistrationManager != null) {
            packRegistrationManager.shutdown();
        }

        if (keybindHook != null) {
            keybindHook.shutdown();
        }

        if (packetModelManager != null) {
            packetModelManager.shutdown();
        }

        if (itemEngineManager != null) {
            itemEngineManager.shutdown();
        }

        if (storageManager != null) {
            storageManager.shutdown();
        }

        getLogger().info("CuriosPaper 3.0 disabled cleanly.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (languageManager != null) {
            languageManager.load();
        }
        if (slotRegistry != null) {
            slotRegistry.load();
        }
        if (keybindHook != null) {
            keybindHook.shutdown();
            keybindHook.initialize();
        }
        if (packetModelManager != null) {
            packetModelManager.shutdown();
            packetModelManager.initialize();
        }
    }

    public static CuriosPaper getInstance() {
        return instance;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public SlotRegistry getSlotRegistry() {
        return slotRegistry;
    }

    public ItemEngineManager getItemEngineManager() {
        return itemEngineManager;
    }

    public CuriosStorageManager getStorageManager() {
        return storageManager;
    }

    public CuriosGUI getCuriosGUI() {
        return curiosGUI;
    }

    public InspectGUI getInspectGUI() {
        return inspectGUI;
    }

    public ElytraBackSlotHandler getElytraBackSlotHandler() {
        return elytraBackSlotHandler;
    }

    public PacketModelManager getPacketModelManager() {
        return packetModelManager;
    }

    public PackRegistrationManager getPackRegistrationManager() {
        return packRegistrationManager;
    }

    public CuriosPaperAPI getCuriosPaperAPI() {
        return api;
    }

    public AbilityListener getAbilityListener() {
        return abilityListener;
    }

    public org.bg52.curiospaper.citizens.CitizensManager getCitizensManager() {
        return citizensManager;
    }
}
