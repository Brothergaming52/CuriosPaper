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
    boolean enabled = plugin.getConfig().getBoolean("resource-pack.enabled", false);
    String host = plugin.getConfig().getString("resource-pack.host-ip", "localhost");
    int port = plugin.getConfig().getInt("resource-pack.port", 8080);

    File pack = rpManager.getPackFile();
    long sizeBytes = pack.exists() ? pack.length() : 0L;
    String humanSize = humanReadableSize(sizeBytes);

    String hash = rpManager.getPackHash();
    int sourceCount = rpManager.getRegisteredSources().size();
    Set<String> namespaces = rpManager.getRegisteredNamespaces();
    int conflictCount = rpManager.getConflictLog().size();

    sender.sendMessage(msg().get("commands.rp.info-header"));
    sender.sendMessage(msg().get("commands.rp.info-enabled")
        + (enabled ? msg().get("commands.rp.info-enabled-true") : msg().get("commands.rp.info-enabled-false")));
    sender.sendMessage(msg().get("commands.rp.info-host", "host", host, "port", String.valueOf(port)));

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

    if (!plugin.getConfig().getBoolean("resource-pack.enabled", false)) {
      sender.sendMessage(msg().get("commands.rp.rebuild-disabled"));
      return;
    }

    String url = rpManager.getPackUrl();
    String hash = rpManager.getPackHash();

    // Append hash as query param to bust client cache on pack rebuild
    if (hash != null && !hash.isEmpty()) {
      url = url + "?v=" + hash;
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
      player.sendMessage(msg().get("commands.create.usage"));
      return true;
    }

    String itemId = args[1].toLowerCase();

    if (itemDataManager == null) {
      player.sendMessage(msg().get("commands.create.editor-disabled"));
      return true;
    }

    if (itemDataManager.hasItem(itemId)) {
      player.sendMessage(msg().get("commands.create.already-exists"));
      return true;
    }

    ItemData item = itemDataManager.createItem(itemId);
    if (item == null) {
      player.sendMessage(msg().get("commands.create.failed"));
      return true;
    }

    // Set default values
    item.setDisplayName("&f" + itemId);
    item.setMaterial("PAPER");

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
          Arrays.asList("rp", "debug", "editmenu", "create", "edit", "delete", "remove", "list", "give", "reload"));
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
    sender.sendMessage(msg().get("commands.usage-debug-player", "label", label));
    sender.sendMessage(msg().get("commands.usage-debug-item", "label", label));
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
