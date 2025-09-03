package org.kazamistudio.shopPlugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.kazamistudio.shopPlugin.ShopPlugin;
import org.kazamistudio.shopPlugin.gui.ShopAdminGUI;

public class ShopAdminCommand implements CommandExecutor {
    private final ShopPlugin plugin;

    public ShopAdminCommand(ShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi!");
            return true;
        }
        if (!p.hasPermission("shop.admin")) {
            p.sendMessage("§cBạn không có quyền sử dụng lệnh này.");
            return true;
        }

        new ShopAdminGUI(plugin).openCategoryMenu(p);
        return true;
    }
}