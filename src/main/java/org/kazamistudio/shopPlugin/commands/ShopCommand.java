package org.kazamistudio.shopPlugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.kazamistudio.shopPlugin.ShopPlugin;
import org.kazamistudio.shopPlugin.gui.ShopGUI;

public class ShopCommand implements CommandExecutor {

    private final ShopPlugin plugin;
    private final ShopGUI gui;

    public ShopCommand(ShopPlugin plugin) {
        this.plugin = plugin;
        this.gui = new ShopGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ có người chơi mới dùng được lệnh này.");
            return true;
        }

        Player player = (Player) sender;

        String category = args.length > 0 ? args[0] : plugin.getShopManager().getCategoryNames()
                .stream().findFirst().orElse(null);

        if (category == null) {
            player.sendMessage("Không có category nào để mở.");
            return true;
        }

        player.openInventory(gui.create(player, category, 0));
        return true;
    }
}
