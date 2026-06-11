package org.bg52.curiospaper;

import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.api.CuriosPaperAPIImpl;
import org.bg52.curiospaper.command.BaublesCommand;
import org.bg52.curiospaper.config.ConfigManager;
import org.bg52.curiospaper.handler.ElytraBackSlotHandler;
import org.bg52.curiospaper.inventory.*;
import org.bg52.curiospaper.listener.AbilityListener;
import org.bg52.curiospaper.listener.AccessoryHotkeyListener;
import org.bg52.curiospaper.listener.InventoryListener;
import org.bg52.curiospaper.listener.PlayerListener;
import org.bg52.curiospaper.listener.QuickEquipListener;
import org.bg52.curiospaper.listener.RecipeListener;
import org.bg52.curiospaper.manager.ChatInputManager;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bg52.curiospaper.manager.MessagesManager;
import org.bg52.curiospaper.manager.SlotManager;
import org.bg52.curiospaper.model.ModelStandManager;
import org.bg52.curiospaper.resourcepack.ResourcePackManager;
import org.bg52.curiospaper.util.AutoSaveTask;
import org.bstats.bukkit.Metrics;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class CuriosPaper extends JavaPlugin {
  private static CuriosPaper instance;
  private ConfigManager configManager;
  private MessagesManager messagesManager;
  private SlotManager slotManager;
  private ItemDataManager itemDataManager;
  private ChatInputManager chatInputManager;
  private CuriosPaperAPI api;
  private AccessoryGUI gui;
  private EditMenuGUI editMenuGUI;
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
  private org.bg52.curiospaper.inventory.NbtEditorGUI nbtEditor;
  private AbilityListener abilityListener;
  private ModelConfigGUI modelConfigGUI;
  private org.bg52.curiospaper.inventory.MobDropModelConfigGUI mobDropModelConfigGUI;
  private ModelStandManager modelStandManager;
  private AccessoryHotkeyListener accessoryHotkeyListener;
  private ItemListGUI itemListGUI;
  private ItemRecipeListGUI itemRecipeListGUI;
  private RecipeViewGUI recipeViewGUI;

  @Override
  public void onEnable() {
    instance = this;

    saveDefaultConfig();

    // Initialize messages first so all components can use it
    messagesManager = new MessagesManager(this);

    configManager = new ConfigManager(this);

    slotManager = new SlotManager(this);

    // Initialize Item Data Manager
    boolean itemEditorEnabled = getConfig().getBoolean("features.item-editor.enabled", true);
    if (itemEditorEnabled) {
      itemDataManager = new ItemDataManager(this);

      // Populate the slot activity registry from already-loaded item files.
      // External plugins that register items via tagAccessoryItem() will mark their
      // slots active on their own via the API hook.
      configManager.initSlotActivityFromItems();

      // Initialize Chat Input Manager
      chatInputManager = new ChatInputManager(this);
      getServer().getPluginManager().registerEvents(chatInputManager, this);

      editGUI = new EditGUI(this);

      modelConfigGUI = new ModelConfigGUI(this);
      mobDropModelConfigGUI = new org.bg52.curiospaper.inventory.MobDropModelConfigGUI(this);

      itemListGUI = new ItemListGUI(this);
      itemRecipeListGUI = new ItemRecipeListGUI(this);
      recipeViewGUI = new RecipeViewGUI(this);
    }

    // Initialize Resource Pack Manager
    resourcePackManager = new ResourcePackManager(this);
    resourcePackManager.initialize();

    api = new CuriosPaperAPIImpl(this);

    gui = new AccessoryGUI(this);
    gui.loadCustomLayout();
    editMenuGUI = new EditMenuGUI(this);

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
      // Register EditGUI listener
      getServer().getPluginManager().registerEvents(editGUI, this);

      // Register ModelConfigGUI listener
      getServer().getPluginManager().registerEvents(modelConfigGUI, this);

      // Register MobDropModelConfigGUI listener
      getServer().getPluginManager().registerEvents(mobDropModelConfigGUI, this);

      // Register new List and Recipe GUI listeners
      getServer().getPluginManager().registerEvents(itemListGUI, this);
      getServer().getPluginManager().registerEvents(itemRecipeListGUI, this);
      getServer().getPluginManager().registerEvents(recipeViewGUI, this);
    }

    getCommand("curios").setExecutor(
        new org.bg52.curiospaper.command.CuriosCommand(this, this.api()));
    getCommand("curios").setTabCompleter(
        new org.bg52.curiospaper.command.CuriosCommand(this, this.api()));

    getServer().getPluginManager().registerEvents(new InventoryListener(this, gui), this);
    getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    getServer().getPluginManager().registerEvents(new QuickEquipListener(this), this);
    getServer().getPluginManager().registerEvents(new org.bg52.curiospaper.listener.PlayerDeathListener(this), this);

    // Register hotkey listener
    accessoryHotkeyListener = new AccessoryHotkeyListener(this, gui);
    getServer().getPluginManager().registerEvents(accessoryHotkeyListener, this);

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

      this.nbtEditor = new org.bg52.curiospaper.inventory.NbtEditorGUI(this);
      getServer().getPluginManager().registerEvents(this.nbtEditor, this);

      this.abilityListener = new AbilityListener(this);
      getServer().getPluginManager().registerEvents(this.abilityListener, this);
    }

    // Register Elytra Back Slot Handler if enabled AND server supports
    // DataComponents (1.21.3+)
    if (getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
      if (org.bg52.curiospaper.util.VersionUtil.supportsDataComponents()) {
        elytraHandler = new ElytraBackSlotHandler(this);
        getServer().getPluginManager().registerEvents(elytraHandler, this);
        getLogger().info("Elytra back slot feature enabled!");
      } else {
        getLogger().warning("Elytra back slot feature requires Minecraft 1.21.3+ with Paper. " +
            "Your server is running " + org.bg52.curiospaper.util.VersionUtil.getVersionString() +
            ". This feature has been disabled.");
      }
    }

    int saveInterval = getConfig().getInt("storage.save-interval", 300) * 20;
    autoSaveTask = new AutoSaveTask(this);
    autoSaveTask.runTaskTimer(this, saveInterval, saveInterval);

    int pluginId = 29508;
    new Metrics(this, pluginId);

    // Initialize 3D Model Stand Manager
    modelStandManager = new org.bg52.curiospaper.model.ModelStandManager(this);
    modelStandManager.initialize();

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

  public org.bg52.curiospaper.inventory.NbtEditorGUI getNbtEditor() {
    return nbtEditor;
  }

  public ModelConfigGUI getModelConfigGUI() {
    return modelConfigGUI;
  }

  public org.bg52.curiospaper.inventory.MobDropModelConfigGUI getMobDropModelConfigGUI() {
    return mobDropModelConfigGUI;
  }

  public org.bg52.curiospaper.model.ModelStandManager getModelStandManager() {
    return modelStandManager;
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

    // Clean up all 3D model armor stands
    if (modelStandManager != null) {
      modelStandManager.shutdown();
    }

    // Clean up hotkey listener
    if (accessoryHotkeyListener != null) {
      accessoryHotkeyListener.cleanup();
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

  public EditMenuGUI getEditMenuGUI() {
    return editMenuGUI;
  }

  public ItemDataManager getItemDataManager() {
    return itemDataManager;
  }

  public MessagesManager getMessagesManager() {
    return messagesManager;
  }

  public ChatInputManager getChatInputManager() {
    return chatInputManager;
  }

  public ItemListGUI getItemListGUI() {
    return itemListGUI;
  }

  public ItemRecipeListGUI getItemRecipeListGUI() {
    return itemRecipeListGUI;
  }

  public RecipeViewGUI getRecipeViewGUI() {
    return recipeViewGUI;
  }
}
