package org.bg52.curiospaper;

import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.api.CuriosPaperAPIImpl;
import org.bg52.curiospaper.command.BaublesCommand;
import org.bg52.curiospaper.config.ConfigManager;
import org.bg52.curiospaper.handler.ElytraBackSlotHandler;
import org.bg52.curiospaper.inventory.*;
import org.bg52.curiospaper.listener.AbilityListener;
import org.bg52.curiospaper.listener.InventoryListener;
import org.bg52.curiospaper.listener.PlayerListener;
import org.bg52.curiospaper.listener.RecipeListener;
import org.bg52.curiospaper.manager.ChatInputManager;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bg52.curiospaper.manager.SlotManager;
import org.bg52.curiospaper.resourcepack.ResourcePackManager;
import org.bg52.curiospaper.util.AutoSaveTask;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class CuriosPaper extends JavaPlugin {
    private static CuriosPaper instance;
    private ConfigManager configManager;
    private SlotManager slotManager;
    private ItemDataManager itemDataManager;
    private ChatInputManager chatInputManager;
    private CuriosPaperAPI api;
    private AccessoryGUI gui;
    private AutoSaveTask autoSaveTask;
    private ResourcePackManager resourcePackManager;
    private ElytraBackSlotHandler elytraHandler;
    private RecipeListener recipeListener;
    private EditGUI editGUI;
    private RecipeEditorGUI recipeEditor;
    private LootTableBrowser lootTableBrowser;
    private MobDropEditor mobDropEditor;
    private TradeEditor tradeEditor;
    private AbilityEditorGUI abilityEditor;
    private AbilityListener abilityListener;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);

        slotManager = new SlotManager(this);

        // Initialize Item Data Manager
        boolean itemEditorEnabled = getConfig().getBoolean("features.item-editor.enabled", true);
        if (itemEditorEnabled) {
            itemDataManager = new ItemDataManager(this);

            // Initialize Chat Input Manager
            chatInputManager = new ChatInputManager(this);
            getServer().getPluginManager().registerEvents(chatInputManager, this);

            editGUI = new EditGUI(this);
        }

        // Initialize Resource Pack Manager
        resourcePackManager = new ResourcePackManager(this);
        resourcePackManager.initialize();

        api = new CuriosPaperAPIImpl(this);

        gui = new AccessoryGUI(this);

        if (itemEditorEnabled) {
            // Register RecipeListener and register all recipes
            recipeListener = new RecipeListener(this, itemDataManager);
            getServer().getPluginManager().registerEvents(recipeListener, this);
            recipeListener.registerAllRecipes();

            recipeEditor = new RecipeEditorGUI(this);
            getServer().getPluginManager().registerEvents(recipeEditor, this);

            this.lootTableBrowser = new LootTableBrowser(this);
            getServer().getPluginManager().registerEvents(this.lootTableBrowser, this);

            this.mobDropEditor = new MobDropEditor(this);
            getServer().getPluginManager().registerEvents(mobDropEditor, this);
        }

        BaublesCommand baublesCommand = new BaublesCommand(this, gui);
        getCommand("baubles").setExecutor(baublesCommand);

        if (itemEditorEnabled) {
            // Register Edit Command
            org.bg52.curiospaper.command.EditCommand editCommand = new org.bg52.curiospaper.command.EditCommand(this);
            getCommand("edit").setExecutor(editCommand);
            getCommand("edit").setTabCompleter(editCommand);

            // Register EditGUI listener
            getServer().getPluginManager().registerEvents(editGUI, this);
        }

        getCommand("curios").setExecutor(
                new org.bg52.curiospaper.command.CuriosCommand(this, this.api()));
        getCommand("curios").setTabCompleter(
                new org.bg52.curiospaper.command.CuriosCommand(this, this.api()));

        getServer().getPluginManager().registerEvents(new InventoryListener(this, gui), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        if (itemEditorEnabled) {
            // Register loot table and mob drop listeners
            getServer().getPluginManager()
                    .registerEvents(new org.bg52.curiospaper.listener.LootTableListener(this, itemDataManager), this);
            getServer().getPluginManager()
                    .registerEvents(new org.bg52.curiospaper.listener.MobDropListener(this, itemDataManager), this);

            // Register TradeEditor
            this.tradeEditor = new TradeEditor(this);
            getServer().getPluginManager().registerEvents(tradeEditor, this);

            // Register VillagerTradeListener
            getServer().getPluginManager()
                    .registerEvents(new org.bg52.curiospaper.listener.VillagerTradeListener(this, itemDataManager),
                            this);

            this.abilityEditor = new AbilityEditorGUI(this);
            getServer().getPluginManager().registerEvents(this.abilityEditor, this);

            this.abilityListener = new AbilityListener(this);
            getServer().getPluginManager().registerEvents(this.abilityListener, this);
        }

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

    private CuriosPaperAPI api() {
        return api;
    }

    public EditGUI getEditGUI() {
        return editGUI;
    }

    public RecipeEditorGUI getRecipeEditor() {
        return recipeEditor;
    }

    public RecipeListener getRecipeListener() {
        return recipeListener;
    }

    public LootTableBrowser getLootTableBrowser() {
        return this.lootTableBrowser;
    }

    public MobDropEditor getMobDropEditor() {
        return mobDropEditor;
    }

    public TradeEditor getTradeEditor() {
        return tradeEditor;
    }

    public AbilityEditorGUI getAbilityEditor() {
        return abilityEditor;
    }

    @Override
    public void onDisable() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        // Unregister recipes before cleaning up items
        if (recipeListener != null) {
            recipeListener.unregisterAllRecipes();
        }

        // Clean up external items to prevent stale data on restart
        if (itemDataManager != null) {
            itemDataManager.cleanupExternalItems();
        }

        if (resourcePackManager != null) {
            resourcePackManager.shutdown();
        }

        if (abilityListener != null) {
            abilityListener.shutdown();
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

    public ItemDataManager getItemDataManager() {
        return itemDataManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }
}
