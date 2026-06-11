package org.bg52.curiospaper.command;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.manager.MessagesManager;
import org.bg52.curiospaper.resourcepack.ResourcePackManager;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.inventory.EditGUI;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class CuriosCommand implements CommandExecutor, TabCompleter {

  private final CuriosPaper plugin;
  private final CuriosPaperAPI api;
  private final ResourcePackManager rpManager;
  private final NamespacedKey slotTypeKey;
  private final ItemDataManager itemDataManager;
  private final EditGUI editGUI;

  private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#.##");

  public CuriosCommand(CuriosPaper plugin, CuriosPaperAPI api) {
    this.plugin = plugin;
    this.api = api;
    this.rpManager = plugin.getResourcePackManager();
    this.slotTypeKey = api.getSlotTypeKey();
    this.itemDataManager = plugin.getItemDataManager();
    this.editGUI = plugin.getEditGUI();
  }

  private MessagesManager msg() {
    return plugin.getMessagesManager();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

    if (args.length == 0) {
      sendUsage(sender, label);
      return true;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);

    switch (sub) {
      case "rp":
        handleRp(sender, label, Arrays.copyOfRange(args, 1, args.length));
        return true;

      case "debug":
        handleDebug(sender, label, Arrays.copyOfRange(args, 1, args.length));
        return true;

      case "editmenu":
        handleEditMenu(sender);
        return true;

      case "inspect":
        return handleInspect(sender, args);
      case "create":
        return handleCreate(sender, args);
      case "edit":
        return handleEdit(sender, args);
      case "reload":
        return handleReload(sender, label, args);
      case "delete":
      case "remove":
        return handleDelete(sender, args);
      case "list":
        return handleList(sender);
      case "give":
        return handleGive(sender, args);
      case "recordrtp":
        return handleRecordRtp(sender);

      default:
        sendUsage(sender, label);
        return true;
    }
  }

  // ---------------- RP SUBCOMMANDS ----------------

  private void handleRp(CommandSender sender, String label, String[] args) {
    if (!sender.hasPermission("curiospaper.admin")) {
      sender.sendMessage(msg().get("common.no-permission"));
      return;
    }

    if (args.length == 0) {
      sender.sendMessage(msg().get("commands.rp.usage", "label", label));
      return;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);

    switch (sub) {
      case "info":
        cmdRpInfo(sender);
        break;

      case "rebuild":
        cmdRpRebuild(sender);
        break;

      case "conflicts":
        cmdRpConflicts(sender);
        break;

      default:
        sender.sendMessage(msg().get("commands.rp.usage", "label", label));
    }
  }

  private void cmdRpInfo(CommandSender sender) {
    ResourcePackManager.HostingMode mode = rpManager.getHostingMode();
    String host = plugin.getConfig().getString("resource-pack.host-ip", "localhost");
    int port = plugin.getConfig().getInt("resource-pack.port", 8080);
    String url = rpManager.getPackUrl();

    File pack = rpManager.getPackFile();
    long sizeBytes = pack.exists() ? pack.length() : 0L;
    String humanSize = humanReadableSize(sizeBytes);

    String hash = rpManager.getPackHash();
    int sourceCount = rpManager.getRegisteredSources().size();
    Set<String> namespaces = rpManager.getRegisteredNamespaces();
    int conflictCount = rpManager.getConflictLog().size();

    sender.sendMessage(msg().get("commands.rp.info-header"));
    sender.sendMessage("\u00a7bHosting Mode: \u00a7f" + mode.name());
    if (mode == ResourcePackManager.HostingMode.SELF) {
      sender.sendMessage(msg().get("commands.rp.info-host", "host", host, "port", String.valueOf(port)));
    } else if (mode == ResourcePackManager.HostingMode.LINK) {
      sender.sendMessage("\u00a7bDownload Link: \u00a7f" + url);
    }

    sender.sendMessage(msg().get("commands.rp.info-pack-file") +
        (pack.exists() ? msg().get("commands.rp.info-pack-exists", "name", pack.getName(), "size", humanSize)
            : msg().get("commands.rp.info-pack-missing")));

    sender.sendMessage(msg().get("commands.rp.info-hash", "hash", (hash != null ? hash : "none")));
    sender.sendMessage(msg().get("commands.rp.info-sources", "count", String.valueOf(sourceCount)));

    sender.sendMessage(msg().get("commands.rp.info-namespaces",
        "namespaces", (namespaces.isEmpty() ? "none" : String.join(", ", namespaces))));

    sender.sendMessage(msg().get("commands.rp.info-conflicts") +
        (conflictCount > 0 ? msg().get("commands.rp.info-conflicts-count", "count", String.valueOf(conflictCount))
            : msg().get("commands.rp.info-conflicts-zero")));
  }

  private void cmdRpRebuild(CommandSender sender) {
    sender.sendMessage(msg().get("commands.rp.rebuild-start"));
    rpManager.generatePack();
    sender.sendMessage(msg().get("commands.rp.rebuild-complete"));

    ResourcePackManager.HostingMode mode = rpManager.getHostingMode();
    if (mode == ResourcePackManager.HostingMode.NONE) {
      sender.sendMessage(msg().get("commands.rp.rebuild-disabled"));
      return;
    }

    String url = rpManager.getPackUrl();
    if (url == null || url.isEmpty()) {
      sender.sendMessage("\u00a7c[CuriosPaper] Rebuilt pack, but mode is set to LINK and url is empty. Cannot send to players.");
      return;
    }
    String hash = rpManager.getPackHash();

    // Append hash as query param to bust client cache on pack rebuild
    if (hash != null && !hash.isEmpty()) {
      if (url.contains("?")) {
        url = url + "&v=" + hash;
      } else {
        url = url + "?v=" + hash;
      }
    }

    int count = 0;
    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
      try {
        // Use single-arg setResourcePack(url) for maximum version compatibility (1.14+)
        p.setResourcePack(url);
        count++;
      } catch (Exception e) {
        plugin.getLogger().warning("[CuriosPaper] Failed to send resource pack to " + p.getName()
            + ": " + e.getMessage());
      }
    }

    sender.sendMessage(msg().get("commands.rp.rebuild-sent", "count", String.valueOf(count)));
  }

  private void cmdRpConflicts(CommandSender sender) {
    List<String> fileConflicts = rpManager.getConflictLog();
    List<String> nsConflicts = rpManager.getNamespaceConflictLog();

    sender.sendMessage(msg().get("commands.rp.conflicts-header"));

    if (fileConflicts.isEmpty() && nsConflicts.isEmpty()) {
      sender.sendMessage(msg().get("commands.rp.conflicts-none"));
      return;
    }

    if (!nsConflicts.isEmpty()) {
      sender.sendMessage(msg().get("commands.rp.conflicts-namespace-header"));
      for (String line : nsConflicts) {
        sender.sendMessage(msg().get("commands.rp.conflicts-entry", "line", line));
      }
    }

    if (!fileConflicts.isEmpty()) {
      sender.sendMessage(msg().get("commands.rp.conflicts-file-header"));
      for (String line : fileConflicts) {
        sender.sendMessage(msg().get("commands.rp.conflicts-entry", "line", line));
      }
    }
  }

  private void handleEditMenu(CommandSender sender) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(msg().get("common.only-players"));
      return;
    }

    if (!sender.hasPermission("curiospaper.admin")) {
      sender.sendMessage(msg().get("common.no-permission"));
      return;
    }

    plugin.getEditMenuGUI().open((Player) sender);
  }

  private boolean handleRecordRtp(CommandSender sender) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(msg().get("common.only-players"));
      return true;
    }
    Player player = (Player) sender;
    if (!player.hasPermission("curiospaper.admin")) {
      sender.sendMessage(msg().get("common.no-permission-exclaim"));
      return true;
    }

    if (!plugin.getModelStandManager().isRtpEnabled()) {
      player.sendMessage("§c[CuriosPaper] RTP checks are currently disabled in config.yml (features.rtp.enabled is false).");
      return true;
    }

    plugin.getModelStandManager().toggleRecording(player);
    return true;
  }

  // ---------------- INSPECT SUBCOMMAND ----------------

  /**
   * Tracks active inspect sessions: admin UUID -> target UUID
   */
  private static final Map<UUID, UUID> activeInspectSessions = new HashMap<>();
  /**
   * Tracks the slot type of active inspect slot GUIs: admin UUID -> slot type
   */
  private static final Map<UUID, String> activeInspectSlotTypes = new HashMap<>();
  /**
   * Tracks the previous items in the inspect slot GUI for change detection: admin UUID -> items snapshot
   */
  private static final Map<UUID, List<org.bukkit.inventory.ItemStack>> activeInspectPreviousItems = new HashMap<>();
  /**
   * Tracks the accessory slot positions in the inspect slot GUI: admin UUID -> int[]
   */
  private static final Map<UUID, int[]> activeInspectSlotPositions = new HashMap<>();

  public static Map<UUID, UUID> getActiveInspectSessions() { return activeInspectSessions; }
  public static Map<UUID, String> getActiveInspectSlotTypes() { return activeInspectSlotTypes; }
  public static Map<UUID, List<org.bukkit.inventory.ItemStack>> getActiveInspectPreviousItems() { return activeInspectPreviousItems; }
  public static Map<UUID, int[]> getActiveInspectSlotPositions() { return activeInspectSlotPositions; }

  private boolean handleInspect(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(msg().get("common.only-players"));
      return true;
    }
    Player admin = (Player) sender;
    if (!admin.hasPermission("curiospaper.admin")) {
      admin.sendMessage(msg().get("common.no-permission-exclaim"));
      return true;
    }

    if (args.length < 2) {
      admin.sendMessage(msg().get("commands.inspect.usage"));
      return true;
    }

    // Support both online and offline players
    String targetName = args[1];
    UUID targetUUID = null;
    String resolvedName = targetName;

    // Try online player first
    Player onlineTarget = Bukkit.getPlayer(targetName);
    if (onlineTarget != null) {
      targetUUID = onlineTarget.getUniqueId();
      resolvedName = onlineTarget.getName();
    } else {
      // Try offline player
      @SuppressWarnings("deprecation")
      org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
      if (offlineTarget.hasPlayedBefore() || offlineTarget.isOnline()) {
        targetUUID = offlineTarget.getUniqueId();
        resolvedName = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;
      }
    }

    if (targetUUID == null) {
      admin.sendMessage(msg().get("common.player-not-found", "player", targetName));
      return true;
    }

    // Ensure player data is loaded (works for offline players too)
    if (!plugin.getSlotManager().hasPlayerData(targetUUID)) {
      plugin.getSlotManager().loadPlayerData(targetUUID);
    }

    // If a specific slot type is provided, show that slot directly
    if (args.length >= 3) {
      String slotType = args[2].toLowerCase();
      if (!api.isValidSlotType(slotType)) {
        admin.sendMessage(msg().get("commands.inspect.invalid-slot", "slot", slotType));
        return true;
      }
      openInspectSlotGUI(admin, targetUUID, resolvedName, slotType);
    } else {
      openInspectOverviewGUI(admin, targetUUID, resolvedName);
    }

    return true;
  }

  /**
   * Opens an overview of all curios slot types for the target player.
   */
  private void openInspectOverviewGUI(Player admin, UUID targetUUID, String targetName) {
    List<String> slotTypes = api.getAllSlotTypes();
    int size = 27;
    if (slotTypes.size() > 7) size = 45;
    if (slotTypes.size() > 14) size = 54;

    org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, size,
        msg().get("commands.inspect.gui-title", "player", targetName));

    // Fill with glass panes
    org.bukkit.inventory.ItemStack filler = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
    org.bukkit.inventory.meta.ItemMeta fillerMeta = filler.getItemMeta();
    if (fillerMeta != null) {
      fillerMeta.setDisplayName("\u00a7r");
      filler.setItemMeta(fillerMeta);
    }
    for (int i = 0; i < size; i++) gui.setItem(i, filler);

    // Place slot type buttons
    int[] positions = calculatePositions(slotTypes.size(), size);

    for (int i = 0; i < slotTypes.size() && i < positions.length; i++) {
      String slotType = slotTypes.get(i);
      org.bg52.curiospaper.config.SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(slotType);
      if (config == null) continue;

      int equipped = api.countEquippedItems(targetUUID, slotType);
      int total = config.getAmount();

      org.bukkit.inventory.ItemStack button = new org.bukkit.inventory.ItemStack(config.getIcon());
      org.bukkit.inventory.meta.ItemMeta meta = button.getItemMeta();
      if (meta != null) {
        meta.setDisplayName("\u00a76" + config.getName());
        List<String> lore = new java.util.ArrayList<>();
        lore.add("\u00a77Equipped: \u00a7f" + equipped + "/" + total);
        lore.add("");
        lore.add("\u00a78\u25b6 Click to manage items");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "inspect_slot_type"),
            org.bukkit.persistence.PersistentDataType.STRING, slotType);
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "inspect_target_uuid"),
            org.bukkit.persistence.PersistentDataType.STRING, targetUUID.toString());
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "inspect_target_name"),
            org.bukkit.persistence.PersistentDataType.STRING, targetName);
        button.setItemMeta(meta);
      }
      gui.setItem(positions[i], button);
    }

    // Track session
    activeInspectSessions.put(admin.getUniqueId(), targetUUID);

    admin.openInventory(gui);
  }

  /**
   * Opens an editable view of a specific slot type's items for the target player.
   * Admins can take items out. Changes are saved on close with proper unequip events.
   */
  private void openInspectSlotGUI(Player admin, UUID targetUUID, String targetName, String slotType) {
    org.bg52.curiospaper.config.SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(slotType);
    if (config == null) return;

    List<org.bukkit.inventory.ItemStack> items = api.getEquippedItems(targetUUID, slotType);
    int slotAmount = config.getAmount();
    int size = 27;
    if (slotAmount > 5) size = 45;
    if (slotAmount > 16) size = 54;

    org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, size,
        msg().get("commands.inspect.slot-gui-title",
            "player", targetName, "slot", config.getName()));

    // Border
    org.bukkit.inventory.ItemStack border = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLACK_STAINED_GLASS_PANE);
    org.bukkit.inventory.meta.ItemMeta borderMeta = border.getItemMeta();
    if (borderMeta != null) {
      borderMeta.setDisplayName("\u00a7r");
      border.setItemMeta(borderMeta);
    }
    for (int i = 0; i < 9; i++) gui.setItem(i, border);
    for (int i = size - 9; i < size; i++) gui.setItem(i, border);
    for (int row = 1; row < (size / 9) - 1; row++) {
      gui.setItem(row * 9, border);
      gui.setItem(row * 9 + 8, border);
    }

    // Fill inner with filler
    org.bukkit.inventory.ItemStack filler = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
    org.bukkit.inventory.meta.ItemMeta fillerMeta = filler.getItemMeta();
    if (fillerMeta != null) {
      fillerMeta.setDisplayName("\u00a7r");
      filler.setItemMeta(fillerMeta);
    }
    for (int i = 0; i < size; i++) {
      if (gui.getItem(i) == null) gui.setItem(i, filler);
    }

    // Place items in center — these slots are editable
    int[] itemPositions = calculatePositions(slotAmount, size);

    // Snapshot the current items BEFORE placing them so we can detect changes on close
    List<org.bukkit.inventory.ItemStack> previousItems = new java.util.ArrayList<>();
    for (int i = 0; i < slotAmount; i++) {
      org.bukkit.inventory.ItemStack item = (i < items.size()) ? items.get(i) : null;
      previousItems.add(item != null ? item.clone() : null);
    }

    for (int i = 0; i < slotAmount && i < itemPositions.length; i++) {
      if (i < items.size() && items.get(i) != null
          && items.get(i).getType() != org.bukkit.Material.AIR) {
        gui.setItem(itemPositions[i], items.get(i));
      } else {
        // Empty slot placeholder
        org.bukkit.inventory.ItemStack empty = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta emptyMeta = empty.getItemMeta();
        if (emptyMeta != null) {
          emptyMeta.setDisplayName("\u00a77Empty Slot");
          emptyMeta.getPersistentDataContainer().set(
              new NamespacedKey(plugin, "inspect_placeholder"),
              org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
          empty.setItemMeta(emptyMeta);
        }
        gui.setItem(itemPositions[i], empty);
      }
    }

    // Back button
    org.bukkit.inventory.ItemStack back = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW);
    org.bukkit.inventory.meta.ItemMeta backMeta = back.getItemMeta();
    if (backMeta != null) {
      backMeta.setDisplayName("\u00a7c\u2190 Back");
      backMeta.setLore(java.util.Arrays.asList("\u00a77Return to slot overview"));
      backMeta.getPersistentDataContainer().set(
          new NamespacedKey(plugin, "inspect_back"),
          org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
      backMeta.getPersistentDataContainer().set(
          new NamespacedKey(plugin, "inspect_target_uuid"),
          org.bukkit.persistence.PersistentDataType.STRING, targetUUID.toString());
      backMeta.getPersistentDataContainer().set(
          new NamespacedKey(plugin, "inspect_target_name"),
          org.bukkit.persistence.PersistentDataType.STRING, targetName);
      back.setItemMeta(backMeta);
    }
    gui.setItem(size - 9, back);

    // Track session data for close handler
    activeInspectSessions.put(admin.getUniqueId(), targetUUID);
    activeInspectSlotTypes.put(admin.getUniqueId(), slotType);
    activeInspectPreviousItems.put(admin.getUniqueId(), previousItems);
    activeInspectSlotPositions.put(admin.getUniqueId(), itemPositions);

    admin.openInventory(gui);
  }

  /**
   * Helper to calculate centered inventory positions for a given count of items.
   */
  private int[] calculatePositions(int count, int invSize) {
    if (count <= 3) return new int[]{11, 13, 15};
    if (count <= 5) return new int[]{10, 12, 13, 14, 16};
    if (count <= 7) return new int[]{10, 11, 12, 13, 14, 15, 16};

    java.util.List<Integer> posList = new java.util.ArrayList<>();
    int rows = invSize / 9;
    int itemsPerRow = Math.min(7, count);
    int neededRows = (count + itemsPerRow - 1) / itemsPerRow;
    int startRow = Math.max(1, (rows - neededRows) / 2);
    for (int row = 0; row < neededRows && posList.size() < count; row++) {
      int itemsInRow = Math.min(itemsPerRow, count - posList.size());
      int startCol = (9 - itemsInRow) / 2;
      for (int col = 0; col < itemsInRow; col++) {
        posList.add((startRow + row) * 9 + startCol + col);
      }
    }
    return posList.stream().mapToInt(Integer::intValue).toArray();
  }

  /**
   * Checks if a title belongs to an inspect overview GUI.
   */
  public static boolean isInspectOverviewGUI(String title) {
    String stripped = org.bukkit.ChatColor.stripColor(title);
    return stripped != null && stripped.startsWith("Inspect: ");
  }

  /**
   * Checks if a title belongs to an inspect slot GUI.
   * Slot GUI title starts with section-8 and is NOT the overview.
   */
  public static boolean isInspectSlotGUI(String title) {
    return title != null && title.startsWith("\u00a78") && !isInspectOverviewGUI(title);
  }

  /**
   * Checks if a title belongs to any inspect GUI (overview or slot).
   */
  public static boolean isInspectGUI(String title) {
    return isInspectOverviewGUI(title) || isInspectSlotGUI(title);
  }

  // ---------------- ITEM EDIT SUBCOMMANDS ----------------

  private boolean handleCreate(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(msg().get("common.only-players"));
      return true;
    }
    Player player = (Player) sender;
    if (!player.hasPermission("curiospaper.admin")) {
      sender.sendMessage(msg().get("common.no-permission-exclaim"));
      return true;
    }

    if (args.length < 2) {
      player.sendMessage("§cUsage: /curios create item <item_id> [source_slot] OR /curios create slot <slot_name>");
      return true;
    }

    String type = args[1].toLowerCase();
    if (type.equals("slot")) {
      if (args.length < 3) {
        player.sendMessage("§cUsage: /curios create slot <slot_name>");
        return true;
      }
      String slotName = args[2].toLowerCase();
      if (plugin.getConfigManager().hasSlotType(slotName)) {
        player.sendMessage("§cSlot type '" + slotName + "' already exists!");
        return true;
      }

      org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
      String path = "slots." + slotName;
      config.set(path + ".name", slotName.substring(0, 1).toUpperCase() + slotName.substring(1));
      config.set(path + ".icon", "BARRIER");
      config.set(path + ".amount", 1);
      config.set(path + ".lore", Arrays.asList("&7A custom slot for " + slotName + " accessories."));

      plugin.saveConfig();
      plugin.getConfigManager().reload();

      player.sendMessage("§a✓ Custom slot '" + slotName + "' created and saved in config.yml!");
      player.sendMessage("§ePlease edit the config.yml to configure its icon, amount, lore, or custom-model-data.");
      return true;
    }

    // Otherwise, item creation (handles "item" prefix or old style "/curios create <itemId>")
    String itemId;
    String sourceSlot = null;
    if (type.equals("item")) {
      if (args.length < 3) {
        player.sendMessage("§cUsage: /curios create item <item_id> [source_slot]");
        return true;
      }
      itemId = args[2].toLowerCase();
      if (args.length >= 4) {
        sourceSlot = args[3].toLowerCase();
      }
    } else {
      // Old syntax
      itemId = type;
      if (args.length >= 3) {
        sourceSlot = args[2].toLowerCase();
      }
    }

    if (itemDataManager == null) {
      player.sendMessage(msg().get("commands.create.editor-disabled"));
      return true;
    }

    if (itemDataManager.hasItem(itemId)) {
      player.sendMessage(msg().get("commands.create.already-exists"));
      return true;
    }

    ItemStack sourceItem = null;
    if (sourceSlot != null) {
      if (sourceSlot.equals("mainhand") || sourceSlot.equals("hand")) {
        sourceItem = player.getInventory().getItemInMainHand();
      } else if (sourceSlot.equals("offhand")) {
        sourceItem = player.getInventory().getItemInOffHand();
      } else if (sourceSlot.equals("armor.head") || sourceSlot.equals("head")) {
        sourceItem = player.getInventory().getHelmet();
      } else if (sourceSlot.equals("armor.chest") || sourceSlot.equals("chest")) {
        sourceItem = player.getInventory().getChestplate();
      } else if (sourceSlot.equals("armor.legs") || sourceSlot.equals("legs")) {
        sourceItem = player.getInventory().getLeggings();
      } else if (sourceSlot.equals("armor.feet") || sourceSlot.equals("feet")) {
        sourceItem = player.getInventory().getBoots();
      } else if (sourceSlot.startsWith("slot_")) {
        try {
          int slotNum = Integer.parseInt(sourceSlot.substring(5));
          if (slotNum >= 0 && slotNum < player.getInventory().getSize()) {
            sourceItem = player.getInventory().getItem(slotNum);
          } else {
            player.sendMessage("§cInvalid slot number: " + slotNum);
            return true;
          }
        } catch (NumberFormatException e) {
          player.sendMessage("§cInvalid slot format. Use slot_0, slot_1, etc.");
          return true;
        }
      } else {
        // Try parsing direct integer
        try {
          int slotNum = Integer.parseInt(sourceSlot);
          if (slotNum >= 0 && slotNum < player.getInventory().getSize()) {
            sourceItem = player.getInventory().getItem(slotNum);
          } else {
            player.sendMessage("§cInvalid slot number: " + slotNum);
            return true;
          }
        } catch (NumberFormatException e) {
          player.sendMessage("§cUnknown slot type: " + sourceSlot + ". Valid slots: mainhand, offhand, head, chest, legs, feet, slot_N");
          return true;
        }
      }

      if (sourceItem == null || sourceItem.getType() == org.bukkit.Material.AIR) {
        player.sendMessage("§cThe selected source slot is empty!");
        return true;
      }
    }

    ItemData item = itemDataManager.createItem(itemId);
    if (item == null) {
      player.sendMessage(msg().get("commands.create.failed"));
      return true;
    }

    if (sourceItem != null) {
      item.setMaterial(sourceItem.getType().name());
      if (sourceItem.hasItemMeta()) {
        ItemMeta meta = sourceItem.getItemMeta();
        if (meta.hasDisplayName()) {
          item.setDisplayName(meta.getDisplayName().replace('§', '&'));
        } else {
          item.setDisplayName("&f" + itemId);
        }
        if (meta.hasLore() && meta.getLore() != null) {
          List<String> lore = new ArrayList<>();
          for (String line : meta.getLore()) {
            lore.add(line.replace('§', '&'));
          }
          item.setLore(lore);
        }
        if (meta.hasCustomModelData()) {
          item.setCustomModelData(meta.getCustomModelData());
        }

        // If player head, copy its base64 texture to itemModel
        if (sourceItem.getType() == org.bukkit.Material.PLAYER_HEAD && meta instanceof org.bukkit.inventory.meta.SkullMeta) {
          String base64 = org.bg52.curiospaper.util.VersionUtil.getSkullBase64((org.bukkit.inventory.meta.SkullMeta) meta);
          if (base64 != null) {
            item.setItemModel(base64);
          }
        } else if (org.bg52.curiospaper.util.VersionUtil.supportsItemModel()) {
          // Copy itemModel if supported
          try {
            Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
            java.lang.reflect.Method getModelMethod = itemMetaClass.getMethod("getItemModel");
            org.bukkit.NamespacedKey modelKey = (org.bukkit.NamespacedKey) getModelMethod.invoke(meta);
            if (modelKey != null) {
              item.setItemModel(modelKey.toString());
            }
          } catch (Exception e) {
            // Ignore
          }
        }

        // Copy enchants
        if (meta.hasEnchants()) {
          Map<String, Integer> enchants = new HashMap<>();
          for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            String enchantName = entry.getKey().getKey().getKey();
            enchants.put(enchantName, entry.getValue());
          }
          item.setEnchants(enchants);
        }

        if (meta.isUnbreakable()) {
          item.setUnbreakable(true);
        }

        // Copy NBT (PDC)
        Map<String, String> nbtMap = org.bg52.curiospaper.util.VersionUtil.getPdcMap(meta);
        item.setNbt(nbtMap);
      }
      player.sendMessage("§a✓ Copied item properties from " + sourceSlot + " to " + itemId + "!");
    } else {
      // Set default values
      item.setDisplayName("&f" + itemId);
      item.setMaterial("PAPER");
    }

    if (itemDataManager.saveItemData(itemId)) {
      player.sendMessage(msg().get("commands.create.success", "item", itemId));
      player.sendMessage(msg().get("commands.create.success-hint", "item", itemId));
    } else {
      player.sendMessage(msg().get("commands.create.save-failed"));
    }

    return true;
  }

  private boolean handleEdit(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(msg().get("common.only-players"));
      return true;
    }
    Player player = (Player) sender;
    if (!player.hasPermission("curiospaper.admin")) {
      sender.sendMessage(msg().get("common.no-permission-exclaim"));
      return true;
    }

    if (args.length < 2) {
      player.sendMessage(msg().get("commands.edit.usage"));
      return true;
    }

    String itemId = args[1].toLowerCase();

    if (itemDataManager == null || editGUI == null) {
      player.sendMessage(msg().get("commands.edit.editor-disabled"));
      return true;
    }

    if (!itemDataManager.hasItem(itemId)) {
      player.sendMessage(msg().get("commands.edit.not-found", "item", itemId));
      return true;
    }

    editGUI.open(player, itemId);
    return true;
  }

  private boolean handleReload(CommandSender sender, String label, String[] args) {
    if (!sender.hasPermission("curiospaper.admin")) {
      sender.sendMessage(msg().get("common.no-permission"));
      return true;
    }

    if (args.length < 2) {
      sender.sendMessage(msg().get("commands.reload-usage", "label", label));
      return true;
    }

    String sub = args[1].toLowerCase(Locale.ROOT);

    switch (sub) {
      case "config":
        plugin.getConfigManager().reload();
        // Also reload messages.yml
        plugin.getMessagesManager().reload();
        // Also reload custom GUI layout from config
        if (plugin.getGUI() != null) {
          plugin.getGUI().loadCustomLayout();
        }
        // Re-init slot activity since slots might have changed
        plugin.getConfigManager().initSlotActivityFromItems();
        // Reload resource pack manager configuration and web server
        plugin.getResourcePackManager().reload();
        sender.sendMessage(msg().get("commands.reload.config-success"));
        sender.sendMessage(msg().get("commands.reload.config-warning"));
        break;

      case "items":
        if (itemDataManager != null) {
          // Unregister recipes first
          if (plugin.getRecipeListener() != null) {
            plugin.getRecipeListener().unregisterAllRecipes();
          }

          itemDataManager.reload();

          // Recalculate which slots are active based on new item data
          plugin.getConfigManager().recalculateSlotActivityFromItems();

          // Re-register recipes
          if (plugin.getRecipeListener() != null) {
            plugin.getRecipeListener().registerAllRecipes();
          }
          sender.sendMessage(msg().get("commands.reload.items-success"));
        } else {
          sender.sendMessage(msg().get("commands.reload.items-disabled"));
        }
        break;

      case "messages":
        plugin.getMessagesManager().reload();
        sender.sendMessage(msg().get("commands.reload.messages-success"));
        break;

      default:
        sender.sendMessage(msg().get("commands.reload-usage", "label", label));
        break;
    }

    return true;
  }

  private boolean handleDelete(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(msg().get("common.only-players"));
      return true;
    }
    Player player = (Player) sender;
    if (!player.hasPermission("curiospaper.admin")) {
      sender.sendMessage(msg().get("common.no-permission-exclaim"));
      return true;
    }

    if (args.length < 2) {
      player.sendMessage(msg().get("commands.delete.usage"));
      return true;
    }

    String itemId = args[1].toLowerCase();

    if (itemDataManager == null) {
      player.sendMessage(msg().get("commands.delete.editor-disabled"));
      return true;
    }

    if (!itemDataManager.hasItem(itemId)) {
      player.sendMessage(msg().get("commands.delete.not-found"));
      return true;
    }

    if (itemDataManager.deleteItem(itemId)) {
      player.sendMessage(msg().get("commands.delete.success", "item", itemId));
    } else {
      player.sendMessage(msg().get("commands.delete.failed"));
    }

    return true;
  }

  private boolean handleList(CommandSender sender) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(msg().get("common.only-players"));
      return true;
    }
    Player player = (Player) sender;

    if (itemDataManager.getAllItemIds().isEmpty()) {
      player.sendMessage(msg().get("commands.list.empty"));
      return true;
    }

    plugin.getItemListGUI().open(player);
    return true;
  }

  private boolean handleGive(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(msg().get("common.only-players"));
      return true;
    }
    Player senderPlayer = (Player) sender;
    if (!senderPlayer.hasPermission("curiospaper.admin")) {
      senderPlayer.sendMessage(msg().get("common.no-permission-exclaim"));
      return true;
    }

    if (args.length < 2) {
      senderPlayer.sendMessage(msg().get("commands.give.usage"));
      return true;
    }

    String itemId = args[1].toLowerCase();

    if (itemDataManager == null) {
      senderPlayer.sendMessage(msg().get("commands.give.editor-disabled"));
      return true;
    }

    if (!itemDataManager.hasItem(itemId)) {
      senderPlayer.sendMessage(msg().get("commands.give.not-found"));
      return true;
    }

    Player target = senderPlayer;
    int amount = 1;

    if (args.length >= 3) {
      Player p = Bukkit.getPlayer(args[2]);
      if (p == null) {
        try {
          int parsed = Integer.parseInt(args[2]);
          amount = clampAmount(parsed);
        } catch (NumberFormatException ignored) {
          senderPlayer.sendMessage(msg().get("common.player-not-found", "player", args[2]));
          return true;
        }
      } else {
        target = p;
      }
    }

    if (args.length >= 4) {
      try {
        amount = clampAmount(Integer.parseInt(args[3]));
      } catch (NumberFormatException e) {
        senderPlayer.sendMessage(msg().get("common.invalid-amount", "amount", args[3]));
        return true;
      }
    }

    ItemData data = itemDataManager.getItemData(itemId);
    if (data == null) {
      senderPlayer.sendMessage(msg().get("commands.give.load-failed", "item", itemId));
      return true;
    }

    ItemStack stack = buildItemStack(data, amount);

    if (target.getInventory().addItem(stack).isEmpty()) {
      target.sendMessage(msg().get("commands.give.received", "amount", String.valueOf(amount), "item", itemId));
      if (!target.equals(senderPlayer)) {
        senderPlayer.sendMessage(msg().get("commands.give.sent",
            "amount", String.valueOf(amount), "item", itemId, "player", target.getName()));
      }
    } else {
      target.getWorld().dropItemNaturally(target.getLocation(), stack);
      target.sendMessage(msg().get("commands.give.dropped-self"));
      if (!target.equals(senderPlayer)) {
        senderPlayer.sendMessage(msg().get("commands.give.dropped-other", "player", target.getName()));
      }
    }

    return true;
  }

  private int clampAmount(int v) {
    if (v < 1)
      return 1;
    if (v > 64)
      return 64;
    return v;
  }

  private ItemStack buildItemStack(ItemData data, int amount) {
    ItemStack item = plugin.getCuriosPaperAPI().createItemStack(data.getItemId());
    if (item == null) {
      return new ItemStack(Material.PAPER, amount);
    }
    item.setAmount(Math.max(1, Math.min(64, amount)));
    return item;
  }

  // ---------------- DEBUG SUBCOMMANDS ----------------

  private void handleDebug(CommandSender sender, String label, String[] args) {
    if (!sender.hasPermission("curiospaper.admin")) {
      sender.sendMessage(msg().get("commands.debug.no-permission"));
      return;
    }

    if (args.length == 0) {
      sender.sendMessage(msg().get("commands.debug.usage", "label", label));
      return;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);

    switch (sub) {
      case "player":
        if (args.length < 2) {
          sender.sendMessage(msg().get("commands.debug.player.usage", "label", label));
          return;
        }
        cmdDebugPlayer(sender, args[1]);
        break;

      case "item":
        cmdDebugItem(sender);
        break;

      default:
        sender.sendMessage(msg().get("commands.debug.usage", "label", label));
    }
  }

  private void cmdDebugPlayer(CommandSender sender, String name) {
    OfflinePlayer target = Bukkit.getOfflinePlayer(name);
    UUID uuid = target.getUniqueId();

    if (!target.hasPlayedBefore() && !target.isOnline()) {
      sender.sendMessage(msg().get("commands.debug.player.never-joined", "player", name));
      return;
    }

    sender.sendMessage(msg().get("commands.debug.player.header", "player", target.getName()));

    List<String> slotTypes = api.getAllSlotTypes();
    if (slotTypes.isEmpty()) {
      sender.sendMessage(msg().get("commands.debug.player.no-slots"));
      return;
    }

    for (String slotType : slotTypes) {
      int amount = api.getSlotAmount(slotType);
      List<ItemStack> items = api.getEquippedItems(uuid, slotType);

      sender.sendMessage(msg().get("commands.debug.player.slot-info",
          "slot", slotType,
          "count", String.valueOf(amount),
          "equipped", String.valueOf(api.countEquippedItems(uuid, slotType))));

      for (int i = 0; i < items.size(); i++) {
        ItemStack stack = items.get(i);
        if (stack == null || stack.getType() == Material.AIR) {
          continue;
        }

        ItemMeta meta = stack.getItemMeta();
        String displayName = (meta != null && meta.hasDisplayName())
            ? meta.getDisplayName()
            : stack.getType().name();

        String requiredSlot = api.getAccessorySlotType(stack);
        boolean slotValid = requiredSlot != null && api.isValidSlotType(requiredSlot);

        sender.sendMessage(msg().get("commands.debug.player.item-entry",
            "index", String.valueOf(i),
            "name", displayName,
            "material", stack.getType().name()));

        if (requiredSlot != null) {
          if (slotValid) {
            sender.sendMessage(msg().get("commands.debug.player.required-slot-valid", "slot", requiredSlot));
          } else {
            sender.sendMessage(msg().get("commands.debug.player.required-slot-invalid", "slot", requiredSlot));
          }
        } else {
          sender.sendMessage(msg().get("commands.debug.player.required-slot-none"));
        }

        // PDC debug
        if (meta != null) {
          if (meta != null) {
            sender.sendMessage(msg().get("commands.debug.player.pdc-keys"));
          }
        }
      }
    }
  }

  private void cmdDebugItem(CommandSender sender) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(msg().get("commands.debug.item.only-players"));
      return;
    }

    Player player = (Player) sender;
    ItemStack stack = player.getInventory().getItemInMainHand();

    if (stack == null || stack.getType() == Material.AIR) {
      sender.sendMessage(msg().get("commands.debug.item.no-item"));
      return;
    }

    ItemMeta meta = stack.getItemMeta();
    String displayName = (meta != null && meta.hasDisplayName())
        ? meta.getDisplayName()
        : stack.getType().name();

    sender.sendMessage(msg().get("commands.debug.item.header"));
    sender.sendMessage(msg().get("commands.debug.item.type", "type", stack.getType().name()));
    sender.sendMessage(msg().get("commands.debug.item.name", "name", displayName));

    String requiredSlot = null;
    boolean isAccessory = false;
    boolean validSlot = false;

    if (meta != null) {
      PersistentDataContainer pdc = meta.getPersistentDataContainer();
      requiredSlot = pdc.get(slotTypeKey, PersistentDataType.STRING);
      if (requiredSlot != null) {
        isAccessory = true;
        validSlot = api.isValidSlotType(requiredSlot);
      }

      sender.sendMessage(isAccessory
          ? msg().get("commands.debug.item.is-accessory-true")
          : msg().get("commands.debug.item.is-accessory-false"));

      if (requiredSlot != null) {
        if (validSlot) {
          sender.sendMessage(msg().get("commands.debug.item.required-slot-valid", "slot", requiredSlot));
        } else {
          sender.sendMessage(msg().get("commands.debug.item.required-slot-invalid", "slot", requiredSlot));
        }
      } else {
        sender.sendMessage(msg().get("commands.debug.item.required-slot-none"));
      }

      // Dump all PDC keys
      sender.sendMessage(msg().get("commands.debug.item.pdc-keys"));
    } else {
      sender.sendMessage(msg().get("commands.debug.item.no-meta"));
    }
  }

  // ---------------- TAB COMPLETION ----------------

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

    if (args.length == 1) {
      return partial(args[0],
          Arrays.asList("rp", "debug", "editmenu", "inspect", "create", "edit", "delete", "remove", "list", "give", "reload", "recordrtp"));
    }

    if (args.length == 2) {
      String sub = args[0].toLowerCase(Locale.ROOT);
      switch (sub) {
        case "rp":
          return partial(args[1], Arrays.asList("info", "rebuild", "conflicts"));
        case "debug":
          return partial(args[1], Arrays.asList("player", "item"));
        case "reload":
          return partial(args[1], Arrays.asList("config", "items", "messages"));
        case "inspect": {
          String prefix = args[1].toLowerCase(Locale.ROOT);
          return Bukkit.getOnlinePlayers().stream()
              .map(Player::getName)
              .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
              .collect(Collectors.toList());
        }
        case "create":
          return partial(args[1], Arrays.asList("item", "slot"));
        case "edit":
        case "delete":
        case "remove":
        case "give":
          if (itemDataManager != null) {
            return partial(args[1], new ArrayList<>(itemDataManager.getAllItemIds()));
          }
          break;
      }
    }

    if (args.length == 3) {
      if (args[0].equalsIgnoreCase("inspect")) {
        // Tab complete slot types
        return partial(args[2], new ArrayList<>(api.getAllSlotTypes()));
      }
      if (args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("player")) {
        String prefix = args[2].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
            .collect(Collectors.toList());
      }
      if (args[0].equalsIgnoreCase("give")) {
        List<String> options = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        options.addAll(Arrays.asList("1", "16", "32", "64"));
        return partial(args[2], options);
      }
    }

    if (args.length == 4) {
      if (args[0].equalsIgnoreCase("give")) {
        return partial(args[3], Arrays.asList("1", "16", "32", "64"));
      }
      if (args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("item")) {
        return partial(args[3], Arrays.asList("mainhand", "offhand", "head", "chest", "legs", "feet"));
      }
    }

    return Collections.emptyList();
  }

  private List<String> partial(String token, List<String> options) {
    String lower = token.toLowerCase(Locale.ROOT);
    return options.stream()
        .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower))
        .collect(Collectors.toList());
  }

  // ---------------- UTILS ----------------

  private void sendUsage(CommandSender sender, String label) {
    sender.sendMessage(msg().get("commands.usage-header"));
    sender.sendMessage(msg().get("commands.usage-rp", "label", label));
    sender.sendMessage(msg().get("commands.usage-editmenu", "label", label));
    sender.sendMessage(msg().get("commands.usage-create", "label", label));
    sender.sendMessage(msg().get("commands.usage-edit", "label", label));
    sender.sendMessage(msg().get("commands.usage-delete", "label", label));
    sender.sendMessage(msg().get("commands.usage-reload", "label", label));
    sender.sendMessage(msg().get("commands.usage-list", "label", label));
    sender.sendMessage(msg().get("commands.usage-give", "label", label));
    sender.sendMessage(msg().get("commands.usage-inspect", "label", label));
    sender.sendMessage(msg().get("commands.usage-debug-player", "label", label));
    sender.sendMessage(msg().get("commands.usage-debug-item", "label", label));
    sender.sendMessage("§6/" + label + " recordrtp §7- Start/stop recording of RTP trigger sequences");
  }

  private String humanReadableSize(long bytes) {
    if (bytes <= 0)
      return "0 B";
    String[] units = { "B", "KB", "MB", "GB" };
    int unitIndex = (int) (Math.log10(bytes) / Math.log10(1024));
    double value = bytes / Math.pow(1024, unitIndex);
    return SIZE_FORMAT.format(value) + " " + units[unitIndex];
  }
}
