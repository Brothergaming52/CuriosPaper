package org.bg52.curiospaper.command;

import net.citizensnpcs.api.npc.NPC;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.citizens.CitizensManager;
import org.bg52.curiospaper.citizens.CuriosTrait;
import org.bg52.curiospaper.resourcepack.host.PackHostPlugin;
import org.bg52.curiospaper.slot.SlotDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

/**
 * Universal command handler for /curios (with aliases /accessories, /baubles).
 * Completely localized through LanguageManager.
 */
public class CuriosCommand implements CommandExecutor, TabCompleter {

    private final CuriosPaper plugin;

    public CuriosCommand(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                plugin.getLanguageManager().sendMessage(sender, "common.only-players");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("curiospaper.use")) {
                plugin.getLanguageManager().sendMessage(player, "common.no-permission");
                return true;
            }
            plugin.getCuriosGUI().open(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "open": {
                if (args.length == 1) {
                    if (!(sender instanceof Player)) {
                        plugin.getLanguageManager().sendMessage(sender, "common.only-players");
                        return true;
                    }
                    Player player = (Player) sender;
                    if (!player.hasPermission("curiospaper.use")) {
                        plugin.getLanguageManager().sendMessage(player, "common.no-permission");
                        return true;
                    }
                    plugin.getCuriosGUI().open(player);
                    return true;
                }

                if (!sender.hasPermission("curiospaper.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, "common.no-permission");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getLanguageManager().sendMessage(sender, "common.player-not-found", "player", args[1]);
                    return true;
                }

                plugin.getCuriosGUI().open(target);
                return true;
            }

            case "inspect": {
                if (!sender.hasPermission("curiospaper.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, "common.no-permission");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    plugin.getLanguageManager().sendMessage(sender, "common.only-players");
                    return true;
                }
                if (args.length < 2) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.usage-inspect", "label", label);
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getLanguageManager().sendMessage(sender, "common.player-not-found", "player", args[1]);
                    return true;
                }

                plugin.getInspectGUI().open((Player) sender, target);
                return true;
            }

            case "tag": {
                if (!sender.hasPermission("curiospaper.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, "common.no-permission");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    plugin.getLanguageManager().sendMessage(sender, "common.only-players");
                    return true;
                }
                if (args.length < 2) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.usage-tag", "label", label);
                    return true;
                }

                String slot = args[1].toLowerCase(Locale.ROOT);
                SlotDefinition slotDef = plugin.getSlotRegistry().getSlot(slot);
                if (slotDef == null) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.tag.invalid-slot", "slot", slot);
                    return true;
                }

                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType() == Material.AIR) {
                    plugin.getLanguageManager().sendMessage(player, "commands.tag.no-item");
                    return true;
                }

                ItemStack tagged = plugin.getItemEngineManager().tagAccessoryItem(held, slotDef.getId());
                player.getInventory().setItemInMainHand(tagged);
                plugin.getLanguageManager().sendMessage(player, "commands.tag.success", "slot", slotDef.getName());
                return true;
            }

            case "untag": {
                if (!sender.hasPermission("curiospaper.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, "common.no-permission");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    plugin.getLanguageManager().sendMessage(sender, "common.only-players");
                    return true;
                }

                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType() == Material.AIR) {
                    plugin.getLanguageManager().sendMessage(player, "commands.tag.no-item");
                    return true;
                }

                ItemStack untagged = plugin.getItemEngineManager().untagAccessoryItem(held);
                player.getInventory().setItemInMainHand(untagged);
                plugin.getLanguageManager().sendMessage(player, "commands.tag.removed");
                return true;
            }

            case "clear": {
                if (!sender.hasPermission("curiospaper.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, "common.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.usage-clear", "label", label);
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getLanguageManager().sendMessage(sender, "common.player-not-found", "player", args[1]);
                    return true;
                }

                if (args.length >= 3) {
                    String slot = args[2].toLowerCase(Locale.ROOT);
                    plugin.getStorageManager().getInventory(target.getUniqueId()).clear(slot);
                    plugin.getLanguageManager().sendMessage(sender, "commands.clear.slot-success", "slot", slot, "player", target.getName());
                } else {
                    plugin.getStorageManager().getInventory(target.getUniqueId()).clear();
                    plugin.getLanguageManager().sendMessage(sender, "commands.clear.all-success", "player", target.getName());
                }

                plugin.getPacketModelManager().updatePlayerAccessories(target);
                if (plugin.getAbilityListener() != null) {
                    plugin.getAbilityListener().reapplyAllCuriosAttributes(target);
                }
                plugin.getStorageManager().savePlayer(target.getUniqueId());
                return true;
            }

            case "give": {
                if (!sender.hasPermission("curiospaper.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, "common.no-permission");
                    return true;
                }
                if (args.length < 3) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.usage-give", "label", label);
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getLanguageManager().sendMessage(sender, "common.player-not-found", "player", args[1]);
                    return true;
                }

                String itemId = args[2];
                int amount = 1;
                if (args.length >= 4) {
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException ignored) {}
                }

                ItemStack item = plugin.getItemEngineManager().createItemStack(itemId);
                if (item == null) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.give.failed", "item", itemId);
                    return true;
                }

                item.setAmount(amount);
                target.getInventory().addItem(item);
                plugin.getLanguageManager().sendMessage(sender, "commands.give.sent", "amount", String.valueOf(amount), "item", itemId, "player", target.getName());
                plugin.getLanguageManager().sendMessage(target, "commands.give.received", "amount", String.valueOf(amount), "item", itemId);
                return true;
            }

            case "rp", "pack", "resourcepack": {
                if (!sender.hasPermission("curiospaper.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, "common.no-permission");
                    return true;
                }

                if (args.length >= 2 && args[1].equalsIgnoreCase("inject")) {
                    if (args.length >= 3) {
                        PackHostPlugin host = PackHostPlugin.fromString(args[2]);
                        File resDir = plugin.getPackRegistrationManager().getResourcesDir();
                        plugin.getLanguageManager().sendMessage(sender, "commands.rp.inject-start", "host", host.getDisplayName());
                        boolean ok = plugin.getPackRegistrationManager().getHostManager().forceInject(host, null, resDir);
                        if (ok) {
                            plugin.getLanguageManager().sendMessage(sender, "commands.rp.inject-success", "host", host.getDisplayName());
                        } else {
                            plugin.getLanguageManager().sendMessage(sender, "commands.rp.inject-failed", "host", host.getDisplayName());
                        }
                    } else {
                        plugin.getLanguageManager().sendMessage(sender, "commands.rp.usage", "label", label);
                    }
                    return true;
                }

                PackHostPlugin active = plugin.getPackRegistrationManager().getHostManager().getActiveHost();
                sender.sendMessage(plugin.getLanguageManager().getComponent("commands.rp.info-header"));
                sender.sendMessage(plugin.getLanguageManager().getComponent("commands.rp.info-host", "host", active != null ? active.getDisplayName() : "None"));
                sender.sendMessage(plugin.getLanguageManager().getComponent("commands.rp.info-sources", "dir", plugin.getPackRegistrationManager().getResourcesDir().getPath()));
                sender.sendMessage(plugin.getLanguageManager().getComponent("commands.rp.info-namespaces"));
                return true;
            }

            case "npc": {
                if (!sender.hasPermission("curiospaper.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, "common.no-permission");
                    return true;
                }

                CitizensManager citizens = plugin.getCitizensManager();
                if (citizens == null || !citizens.isAvailable()) {
                    plugin.getLanguageManager().sendMessage(sender, "citizens.not-installed");
                    return true;
                }

                if (args.length < 2) {
                    NPC npc = citizens.resolveNPC(sender, null);
                    if (npc == null) {
                        plugin.getLanguageManager().sendMessage(sender, "citizens.no-npc-selected");
                        return true;
                    }
                    if (!(sender instanceof Player)) {
                        plugin.getLanguageManager().sendMessage(sender, "common.only-players");
                        return true;
                    }
                    citizens.openGUI((Player) sender, npc);
                    return true;
                }

                NPC npc = citizens.resolveNPC(sender, args[1]);
                if (npc == null) {
                    plugin.getLanguageManager().sendMessage(sender, "citizens.npc-not-found", "npc", args[1]);
                    return true;
                }

                if (args.length >= 3 && args[2].equalsIgnoreCase("clear")) {
                    CuriosTrait trait = npc.getTraitNullable(CuriosTrait.class);
                    if (trait != null) {
                        trait.clearAllItems();
                    }
                    plugin.getLanguageManager().sendMessage(sender, "citizens.cleared", "npc", npc.getName());
                    return true;
                }

                if (!(sender instanceof Player)) {
                    plugin.getLanguageManager().sendMessage(sender, "common.only-players");
                    return true;
                }
                citizens.openGUI((Player) sender, npc);
                return true;
            }

            case "debug": {
                if (!sender.hasPermission("curiospaper.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.debug.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.debug.usage", "label", label);
                    return true;
                }

                if (args[1].equalsIgnoreCase("item")) {
                    if (!(sender instanceof Player)) {
                        plugin.getLanguageManager().sendMessage(sender, "common.only-players");
                        return true;
                    }
                    Player p = (Player) sender;
                    ItemStack item = p.getInventory().getItemInMainHand();
                    if (item == null || item.getType() == Material.AIR) {
                        plugin.getLanguageManager().sendMessage(p, "commands.debug.no-item");
                        return true;
                    }

                    p.sendMessage(plugin.getLanguageManager().getComponent("commands.debug.header-item"));
                    p.sendMessage(plugin.getLanguageManager().getComponent("commands.debug.type", "type", item.getType().name()));
                    ItemMeta meta = item.getItemMeta();
                    p.sendMessage(plugin.getLanguageManager().getComponent("commands.debug.name", "name", meta != null && meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name()));
                    String resolvedSlot = plugin.getItemEngineManager().resolveCuriosSlot(item);
                    if (resolvedSlot != null) {
                        p.sendMessage(plugin.getLanguageManager().getComponent("commands.debug.is-accessory-true"));
                        p.sendMessage(plugin.getLanguageManager().getComponent("commands.debug.required-slot-valid", "slot", resolvedSlot));
                    } else {
                        p.sendMessage(plugin.getLanguageManager().getComponent("commands.debug.is-accessory-false"));
                        p.sendMessage(plugin.getLanguageManager().getComponent("commands.debug.required-slot-none"));
                    }
                    return true;
                }

                if (args[1].equalsIgnoreCase("player")) {
                    String pName = args.length >= 3 ? args[2] : sender.getName();
                    Player target = Bukkit.getPlayer(pName);
                    if (target == null) {
                        plugin.getLanguageManager().sendMessage(sender, "common.player-not-found", "player", pName);
                        return true;
                    }

                    sender.sendMessage(plugin.getLanguageManager().getComponent("commands.debug.header-player", "player", target.getName()));
                    for (SlotDefinition def : plugin.getSlotRegistry().getAllSlots()) {
                        List<ItemStack> list = plugin.getCuriosPaperAPI().getEquippedItems(target, def.getId());
                        sender.sendMessage(plugin.getLanguageManager().getComponent("commands.debug.slot-info",
                                "slot", def.getName(),
                                "count", String.valueOf(def.getMaxSlots()),
                                "equipped", String.valueOf(list.size())
                        ));
                    }
                    return true;
                }
                return true;
            }

            case "reload": {
                if (!sender.hasPermission("curiospaper.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, "common.no-permission");
                    return true;
                }

                if (args.length >= 2) {
                    String what = args[1].toLowerCase(Locale.ROOT);
                    if (what.equals("config")) {
                        plugin.reloadConfig();
                        plugin.getLanguageManager().sendMessage(sender, "commands.reload.config-success");
                        return true;
                    } else if (what.equals("slots")) {
                        plugin.getSlotRegistry().load();
                        plugin.getLanguageManager().sendMessage(sender, "commands.reload.slots-success");
                        return true;
                    } else if (what.equals("languages") || what.equals("lang") || what.equals("messages")) {
                        plugin.getLanguageManager().load();
                        plugin.getLanguageManager().sendMessage(sender, "commands.reload.languages-success");
                        return true;
                    }
                }

                plugin.reloadPluginConfig();
                plugin.getLanguageManager().sendMessage(sender, "commands.reload.all-success");
                return true;
            }

            default:
                sender.sendMessage(plugin.getLanguageManager().getComponent("commands.usage-header"));
                sender.sendMessage(plugin.getLanguageManager().getComponent("commands.usage-open", "label", label));
                if (sender.hasPermission("curiospaper.admin")) {
                    sender.sendMessage(plugin.getLanguageManager().getComponent("commands.usage-inspect", "label", label));
                    sender.sendMessage(plugin.getLanguageManager().getComponent("commands.usage-tag", "label", label));
                    sender.sendMessage(plugin.getLanguageManager().getComponent("commands.usage-untag", "label", label));
                    sender.sendMessage(plugin.getLanguageManager().getComponent("commands.usage-give", "label", label));
                    sender.sendMessage(plugin.getLanguageManager().getComponent("commands.usage-clear", "label", label));
                    sender.sendMessage(plugin.getLanguageManager().getComponent("commands.usage-npc", "label", label));
                    sender.sendMessage(plugin.getLanguageManager().getComponent("commands.usage-rp", "label", label));
                    sender.sendMessage(plugin.getLanguageManager().getComponent("commands.usage-debug", "label", label));
                    sender.sendMessage(plugin.getLanguageManager().getComponent("commands.usage-reload", "label", label));
                }
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(Collections.singletonList("open"));
            if (sender.hasPermission("curiospaper.admin")) {
                list.addAll(Arrays.asList("inspect", "tag", "untag", "give", "clear", "npc", "rp", "debug", "reload"));
            }
            return filter(list, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("open") || sub.equals("inspect") || sub.equals("clear") || sub.equals("give")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    players.add(p.getName());
                }
                return filter(players, args[1]);
            }
            if (sub.equals("tag")) {
                return filter(new ArrayList<>(plugin.getSlotRegistry().getSlotIds()), args[1]);
            }
            if (sub.equals("rp") || sub.equals("pack")) {
                return filter(Arrays.asList("status", "inject"), args[1]);
            }
            if (sub.equals("debug")) {
                return filter(Arrays.asList("player", "item"), args[1]);
            }
            if (sub.equals("reload")) {
                return filter(Arrays.asList("all", "config", "slots", "languages"), args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("clear")) {
                return filter(new ArrayList<>(plugin.getSlotRegistry().getSlotIds()), args[2]);
            }
            if ((sub.equals("rp") || sub.equals("pack")) && args[1].equalsIgnoreCase("inject")) {
                return filter(Arrays.asList("synthetix", "rspm", "nexo", "itemsadder", "oraxen", "craftengine"), args[2]);
            }
            if (sub.equals("debug") && args[1].equalsIgnoreCase("player")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    players.add(p.getName());
                }
                return filter(players, args[2]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String query) {
        List<String> filtered = new ArrayList<>();
        String lower = query.toLowerCase(Locale.ROOT);
        for (String opt : options) {
            if (opt.toLowerCase(Locale.ROOT).startsWith(lower)) {
                filtered.add(opt);
            }
        }
        return filtered;
    }
}
