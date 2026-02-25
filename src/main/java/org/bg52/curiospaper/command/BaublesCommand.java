package org.bg52.curiospaper.command;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.inventory.AccessoryGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BaublesCommand implements CommandExecutor {
    private final CuriosPaper plugin;
    private final AccessoryGUI gui;

    public BaublesCommand(CuriosPaper plugin, AccessoryGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (!plugin.getSlotManager().hasPlayerData(player.getUniqueId())) {
            plugin.getSlotManager().loadPlayerData(player);
        }

        gui.openMainGUI(player);
        return true;
    }
}
