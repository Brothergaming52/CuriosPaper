package org.bg52.curiospaper.command;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.inventory.EditGUI;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command handler for /edit command
 * Allows players to create and edit custom items in-game
 */
public class EditCommand implements CommandExecutor, TabCompleter {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final EditGUI editGUI;

    public EditCommand(CuriosPaper plugin) {
        this.plugin = plugin;
        this.itemDataManager = plugin.getItemDataManager();
        this.editGUI = plugin.getEditGUI();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("curiospaper.edit")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "create":
                return handleCreate(player, args);
            case "gui":
            case "edit":
                return handleEdit(player, args);
            case "delete":
            case "remove":
                return handleDelete(player, args);
            case "list":
                return handleList(player);
            case "give":
                return handleGive(player, args);
            default:
                sendUsage(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /edit create <itemname>");
            return true;
        }

        String itemId = args[1].toLowerCase();

        if (itemDataManager.hasItem(itemId)) {
            player.sendMessage("§cAn item with that name already exists!");
            return true;
        }

        ItemData item = itemDataManager.createItem(itemId);
        if (item == null) {
            player.sendMessage("§cFailed to create item!");
            return true;
        }

        // Set default values
        item.setDisplayName("§f" + itemId);
        item.setMaterial("PAPER");

        if (itemDataManager.saveItemData(itemId)) {
            player.sendMessage("§a✔ Created item: §e" + itemId);
            player.sendMessage("§7Use §e/edit gui " + itemId + " §7to configure it!");
        } else {
            player.sendMessage("§cFailed to save item data!");
        }

        return true;
    }

    private boolean handleEdit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /edit gui <itemname>");
            return true;
        }

        String itemId = args[1].toLowerCase();

        if (!itemDataManager.hasItem(itemId)) {
            player.sendMessage("§cItem not found! Use §e/edit create " + itemId + " §cto create it.");
            return true;
        }

        editGUI.open(player, itemId);
        return true;
    }

    private boolean handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /edit delete <itemname>");
            return true;
        }

        String itemId = args[1].toLowerCase();

        if (!itemDataManager.hasItem(itemId)) {
            player.sendMessage("§cItem not found!");
            return true;
        }

        if (itemDataManager.deleteItem(itemId)) {
            player.sendMessage("§a✔ Deleted item: §e" + itemId);
        } else {
            player.sendMessage("§cFailed to delete item!");
        }

        return true;
    }

    private boolean handleList(Player player) {
        if (itemDataManager.getAllItemIds().isEmpty()) {
            player.sendMessage("§7No custom items have been created yet.");
            return true;
        }

        player.sendMessage("§e▬▬▬ Custom Items ▬▬▬");
        for (String itemId : itemDataManager.getAllItemIds()) {
            ItemData data = itemDataManager.getItemData(itemId);
            player.sendMessage("§6• §e" + itemId + " §7- " + (data != null ? data.getDisplayName() : "Unknown"));
        }
        player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        return true;
    }

    /**
     * /edit give <itemId> [player] [amount]
     */
    private boolean handleGive(Player senderPlayer, String[] args) {
        if (args.length < 2) {
            senderPlayer.sendMessage("§cUsage: /edit give <itemId> [player] [amount]");
            return true;
        }

        String itemId = args[1].toLowerCase();

        if (!itemDataManager.hasItem(itemId)) {
            senderPlayer.sendMessage("§cItem not found! Use §e/edit list §cto view items.");
            return true;
        }

        Player target = senderPlayer;
        int amount = 1;

        if (args.length >= 3) {
            Player p = Bukkit.getPlayer(args[2]);
            if (p == null) {
                // If third arg is not a player, maybe it's an amount. Try parse as amount.
                try {
                    int parsed = Integer.parseInt(args[2]);
                    amount = clampAmount(parsed);
                } catch (NumberFormatException ignored) {
                    senderPlayer.sendMessage("§cPlayer not found: " + args[2]);
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
                senderPlayer.sendMessage("§cInvalid amount: " + args[3]);
                return true;
            }
        }

        ItemData data = itemDataManager.getItemData(itemId);
        if (data == null) {
            senderPlayer.sendMessage("§cFailed to load item data: " + itemId);
            return true;
        }

        ItemStack stack = buildItemStack(data, amount);

        // Give item to target reliably (try inventory, otherwise drop at feet)
        if (target.getInventory().addItem(stack).isEmpty()) {
            target.sendMessage("§a✔ You received §e" + amount + "x §f" + itemId);
            if (!target.equals(senderPlayer)) {
                senderPlayer.sendMessage("§a✔ Gave §e" + amount + "x §f" + itemId + " §ato §e" + target.getName());
            }
        } else {
            target.getWorld().dropItemNaturally(target.getLocation(), stack);
            target.sendMessage("§a✔ Inventory full — dropped item at your feet.");
            if (!target.equals(senderPlayer)) {
                senderPlayer.sendMessage("§a✔ Inventory full — dropped item at " + target.getName() + "'s feet.");
            }
        }

        return true;
    }

    private int clampAmount(int v) {
        if (v < 1) return 1;
        if (v > 64) return 64;
        return v;
    }

    /**
     * Builds an ItemStack from ItemData.
     * Supports both Integer (CustomModelData) and NamespacedKey (Item Model) formats.
     */
    private ItemStack buildItemStack(ItemData data, int amount) {
        Material mat = Material.PAPER;
        try {
            if (data.getMaterial() != null) {
                mat = Material.valueOf(data.getMaterial().toUpperCase());
            }
        } catch (Exception ignored) {
            // fallback to PAPER
        }

        ItemStack item = new ItemStack(mat, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // FIX: Color codes applied here
            if (data.getDisplayName() != null && !data.getDisplayName().isBlank()) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', data.getDisplayName()));
            }

            if (data.getLore() != null && !data.getLore().isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : data.getLore()) {
                    coloredLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }

            // FIX: Hybrid handling for CustomModelData (int) vs ItemModel (String/Key)
            if (data.getItemModel() != null && !data.getItemModel().isBlank()) {
                String modelStr = data.getItemModel().trim();

                // 1. Try to parse as CustomModelData (Integer)
                try {
                    int customModelData = Integer.parseInt(modelStr);
                    meta.setCustomModelData(customModelData);
                } catch (NumberFormatException e) {
                    // 2. Not an integer, treat as Item Model (NamespacedKey)
                    try {
                        org.bukkit.NamespacedKey key;
                        if (modelStr.contains(":")) {
                            key = org.bukkit.NamespacedKey.fromString(modelStr);
                        } else {
                            key = new org.bukkit.NamespacedKey(plugin, modelStr);
                        }

                        // Check if method exists (Paper 1.21+)
                        // Using suppression in case you compile on older API, but runtime checks are better
                        meta.setItemModel(key);
                    } catch (Exception ex) {
                        // Fallback or log if needed
                    }
                }
            }

            item.setItemMeta(meta);
        }
        item = plugin.getCuriosPaperAPI().tagAccessoryItem(item, data.getSlotType());
        return item;
    }

    private void sendUsage(Player player) {
        player.sendMessage("§e▬▬▬ /edit Command ▬▬▬");
        player.sendMessage("§6/edit create <name> §7- Create a new item");
        player.sendMessage("§6/edit gui <name> §7- Open edit  GUI");
        player.sendMessage("§6/edit delete <name> §7- Delete an item");
        player.sendMessage("§6/edit list §7- List all items");
        player.sendMessage("§6/edit give <item> [player] [amount] §7- Give a saved custom item");
        player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Subcommand completion
            completions.addAll(Arrays.asList("create", "gui", "delete", "list", "give"));
        } else if (args.length == 2) {
            // Item name completion for gui, delete and give
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("gui") || subcommand.equals("edit") || subcommand.equals("delete") || subcommand.equals("give")) {
                completions.addAll(itemDataManager.getAllItemIds());
            }
        } else if (args.length == 3) {
            // If 'give' suggest online players or amounts
            if (args[0].equalsIgnoreCase("give")) {
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                completions.addAll(Arrays.asList("1","16","32","64"));
            }
        }

        // Filter based on what the player has typed
        String partial = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(partial));

        return completions;
    }
}
