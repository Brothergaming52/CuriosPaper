package org.bg52.curiospaper.command;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.resourcepack.ResourcePackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#.##");

    public CuriosCommand(CuriosPaper plugin, CuriosPaperAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.rpManager = plugin.getResourcePackManager();
        this.slotTypeKey = api.getSlotTypeKey();
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null) return null;
        hex = hex.trim();
        if (hex.length() % 2 != 0) return null;

        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) return null;
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
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

            default:
                sendUsage(sender, label);
                return true;
        }
    }

    // ---------------- RP SUBCOMMANDS ----------------

    private void handleRp(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("curiospaper.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " rp <info|rebuild|conflicts>");
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
                sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " rp <info|rebuild|conflicts>");
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

        sender.sendMessage(ChatColor.GOLD + "==== CuriosPaper Resource Pack ====");
        sender.sendMessage(ChatColor.YELLOW + "Enabled: " + (enabled ? ChatColor.GREEN + "true" : ChatColor.RED + "false"));
        sender.sendMessage(ChatColor.YELLOW + "Host: " + ChatColor.AQUA + host + ChatColor.GRAY + ":" + ChatColor.AQUA + port);

        sender.sendMessage(ChatColor.YELLOW + "Pack file: " +
                (pack.exists() ? ChatColor.GREEN + pack.getName() + ChatColor.GRAY + " (" + humanSize + ")"
                        : ChatColor.RED + "NOT GENERATED"));

        sender.sendMessage(ChatColor.YELLOW + "Hash: " + ChatColor.AQUA + (hash != null ? hash : "none"));
        sender.sendMessage(ChatColor.YELLOW + "Registered sources: " + ChatColor.AQUA + sourceCount);

        sender.sendMessage(ChatColor.YELLOW + "Namespaces: " +
                ChatColor.AQUA + (namespaces.isEmpty() ? "none" : String.join(", ", namespaces)));

        sender.sendMessage(ChatColor.YELLOW + "Last build conflicts: " +
                (conflictCount > 0 ? ChatColor.RED + String.valueOf(conflictCount) : ChatColor.GREEN + "0"));
    }

    private void cmdRpRebuild(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Rebuilding CuriosPaper resource pack...");
        rpManager.generatePack();
        sender.sendMessage(ChatColor.GREEN + "Resource pack rebuild complete.");

        if (!plugin.getConfig().getBoolean("resource-pack.enabled", false)) {
            sender.sendMessage(ChatColor.GRAY + "Resource pack HTTP server is disabled; not sending pack to players.");
            return;
        }

        String url = rpManager.getPackUrl();
        String hashHex = rpManager.getPackHash();
        byte[] hashBytes = hexToBytes(hashHex);

        int count = 0;
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            try {
                if (hashBytes != null) {
                    // Newer API: you can also use the variant with required + prompt if you want
                    p.setResourcePack(url, hashBytes);
                } else {
                    p.setResourcePack(url);
                }
                count++;
            } catch (Exception e) {
                plugin.getLogger().warning("[CuriosPaper] Failed to send resource pack to " + p.getName()
                        + ": " + e.getMessage());
            }
        }

        sender.sendMessage(ChatColor.YELLOW + "Re-sent resource pack to "
                + ChatColor.AQUA + count + ChatColor.YELLOW + " online players.");
    }

    private void cmdRpConflicts(CommandSender sender) {
        List<String> fileConflicts = rpManager.getConflictLog();
        List<String> nsConflicts = rpManager.getNamespaceConflictLog();

        sender.sendMessage(ChatColor.GOLD + "==== CuriosPaper RP Conflicts ====");

        if (fileConflicts.isEmpty() && nsConflicts.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "No conflicts recorded.");
            return;
        }

        if (!nsConflicts.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Namespace conflicts:");
            for (String line : nsConflicts) {
                sender.sendMessage(ChatColor.RED + "- " + line);
            }
        }

        if (!fileConflicts.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "File conflicts:");
            for (String line : fileConflicts) {
                sender.sendMessage(ChatColor.RED + "- " + line);
            }
        }
    }


    // ---------------- DEBUG SUBCOMMANDS ----------------

    private void handleDebug(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("curiospaper.debug")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use debug commands.");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " debug <player <name> | item>");
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "player":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " debug player <name>");
                    return;
                }
                cmdDebugPlayer(sender, args[1]);
                break;

            case "item":
                cmdDebugItem(sender);
                break;

            default:
                sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " debug <player <name> | item>");
        }
    }

    private void cmdDebugPlayer(CommandSender sender, String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        UUID uuid = target.getUniqueId();

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + name + "' has never joined.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "==== Curios Debug: Player " + target.getName() + " ====");

        List<String> slotTypes = api.getAllSlotTypes();
        if (slotTypes.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No slot types configured.");
            return;
        }

        for (String slotType : slotTypes) {
            int amount = api.getSlotAmount(slotType);
            List<ItemStack> items = api.getEquippedItems(uuid, slotType);

            sender.sendMessage(ChatColor.AQUA + "Slot: " + slotType +
                    ChatColor.GRAY + " (count=" + amount + ", equipped=" + api.countEquippedItems(uuid, slotType) + ")");

            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }

                ItemMeta meta = stack.getItemMeta();
                String displayName = (meta != null && meta.hasDisplayName())
                        ? meta.getDisplayName()
                        : stack.getType().name();

                String requiredSlot = api.getAccessorySlotType(stack);
                boolean slotValid = requiredSlot != null && api.isValidSlotType(requiredSlot);

                sender.sendMessage(ChatColor.GRAY + "  [" + i + "] "
                        + ChatColor.WHITE + displayName
                        + ChatColor.DARK_GRAY + " (" + stack.getType().name() + ")");

                sender.sendMessage(ChatColor.GRAY + "     Required Slot: "
                        + (requiredSlot != null
                        ? (slotValid ? ChatColor.GREEN + requiredSlot : ChatColor.RED + requiredSlot + " (INVALID)")
                        : ChatColor.RED + "none"));

                // PDC debug
                if (meta != null) {
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    Set<NamespacedKey> keys = pdc.getKeys();
                    if (!keys.isEmpty()) {
                        sender.sendMessage(ChatColor.GRAY + "     PDC Keys:");
                        for (NamespacedKey key : keys) {
                            sender.sendMessage(ChatColor.DARK_GRAY + "       - "
                                    + ChatColor.YELLOW + key.getNamespace()
                                    + ChatColor.GRAY + ":" + ChatColor.YELLOW + key.getKey());
                        }
                    }
                }
            }
        }
    }

    private void cmdDebugItem(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /curios debug item.");
            return;
        }

        Player player = (Player) sender;
        ItemStack stack = player.getInventory().getItemInMainHand();

        if (stack == null || stack.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "You must be holding an item in your main hand.");
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        String displayName = (meta != null && meta.hasDisplayName())
                ? meta.getDisplayName()
                : stack.getType().name();

        sender.sendMessage(ChatColor.GOLD + "==== Curios Debug: Item in hand ====");
        sender.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.AQUA + stack.getType().name());
        sender.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.AQUA + displayName);

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

            sender.sendMessage(ChatColor.YELLOW + "Is accessory: " +
                    (isAccessory ? ChatColor.GREEN + "true" : ChatColor.RED + "false"));

            sender.sendMessage(ChatColor.YELLOW + "Required slot: " +
                    (requiredSlot != null
                            ? (validSlot ? ChatColor.GREEN + requiredSlot : ChatColor.RED + requiredSlot + " (INVALID)")
                            : ChatColor.RED + "none"));

            // Dump all PDC keys
            Set<NamespacedKey> keys = pdc.getKeys();
            if (!keys.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "Curios PDC keys:");
                for (NamespacedKey key : keys) {
                    sender.sendMessage(ChatColor.DARK_GRAY + " - "
                            + ChatColor.YELLOW + key.getNamespace()
                            + ChatColor.GRAY + ":" + ChatColor.YELLOW + key.getKey());
                }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Curios PDC keys: " + ChatColor.GRAY + "none");
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Item has no meta.");
        }
    }

    // ---------------- TAB COMPLETION ----------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return partial(args[0], Arrays.asList("rp", "debug"));
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "rp":
                    return partial(args[1], Arrays.asList("info", "rebuild", "conflicts"));
                case "debug":
                    return partial(args[1], Arrays.asList("player", "item"));
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("player")) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toList());
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
        sender.sendMessage(ChatColor.GOLD + "CuriosPaper Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " rp info");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " rp rebuild");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " rp conflicts");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " debug player <name>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " debug item");
    }

    private String humanReadableSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = (int) (Math.log10(bytes) / Math.log10(1024));
        double value = bytes / Math.pow(1024, unitIndex);
        return SIZE_FORMAT.format(value) + " " + units[unitIndex];
    }
}
