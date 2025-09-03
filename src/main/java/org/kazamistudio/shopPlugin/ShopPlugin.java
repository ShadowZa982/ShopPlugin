package org.kazamistudio.shopPlugin;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.kazamistudio.shopPlugin.commands.ShopAdminCommand;
import org.kazamistudio.shopPlugin.commands.ShopCommand;
import org.kazamistudio.shopPlugin.delivery.DeliveryManager;
import org.kazamistudio.shopPlugin.gui.ShopItemEditorGUI;
import org.kazamistudio.shopPlugin.listener.ShopEditListener;
import org.kazamistudio.shopPlugin.listener.ShopListener;
import org.kazamistudio.shopPlugin.manager.ShopManager;
import org.kazamistudio.shopPlugin.utils.ColorUtil;
import org.kazamistudio.shopPlugin.utils.EconomyUtil;
import org.kazamistudio.shopPlugin.utils.UpdateChecker;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ShopPlugin extends JavaPlugin {

    private final Set<UUID> pendingDeliveries = new HashSet<>();

    private static ShopPlugin instance;
    private ShopManager shopManager;
    private DeliveryManager deliveryManager;

    private final ShopItemEditorGUI editorGUI = new ShopItemEditorGUI();

    private UpdateChecker checker;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!EconomyUtil.setup()) {
            getLogger().severe("Không tìm thấy hệ thống tiền tệ nào khả dụng! Plugin sẽ disable.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        shopManager = new ShopManager(this);
        shopManager.loadAllCategories();
        deliveryManager = new DeliveryManager(this);

        checker = new UpdateChecker(this);
        getServer().getPluginManager().registerEvents(checker, this);

        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("shopreload").setExecutor((sender, cmd, label, args) -> {
            reloadConfig();
            EconomyUtil.reload();
            shopManager.loadAllCategories();

            String msg = getConfig().getString("messages.shop-reloaded", "&aShop reloaded.");
            sender.sendMessage(ColorUtil.translate(msg));
            return true;
        });
        getCommand("shopadmin").setExecutor(new ShopAdminCommand(this));

        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopEditListener(this), this);

        logStartupMessage(true);
    }

    @Override
    public void onDisable() {
        logStartupMessage(false);
    }

    public static ShopPlugin getInstance() { return instance; }
    public ShopManager getShopManager() { return shopManager; }
    public DeliveryManager getDeliveryManager() { return deliveryManager; }

    private void logStartupMessage(boolean enable) {
        if (enable) {
            logWithColor("&b========== &fKazami Studio &b==========");
            logWithColor("&7[&a✔&7] &fPlugin: &b" + getDescription().getName());
            logWithColor("&7[&a✔&7] &fVersion: &a" + getDescription().getVersion());
            logWithColor("&7[&a✔&7] &fAuthor: &eFox Studio");
            logWithColor("&7[&a✔&7] &fDiscord: &9https://discord.gg/kQsg6JyT");
            logWithColor("&7[&a✔&7] &6" + getDescription().getName() + " &ađã được bật!");
            logWithColor("&b=====================================");
        } else {
            logWithColor("&c[&6" + getDescription().getName() + "&c] Plugin đã tắt.");
        }
    }

    public ShopItemEditorGUI getEditorGUI() {
        return editorGUI;
    }

    private void logWithColor(String msg) {
        getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    public Set<UUID> getPendingDeliveries() {
        return pendingDeliveries;
    }
}
