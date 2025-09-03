package org.kazamistudio.shopPlugin.utils;

import me.realized.tokenmanager.api.TokenManager;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.fox.vneconomy.api.EconomyAPI;
import org.kazamistudio.shopPlugin.ShopPlugin;

import java.util.Arrays;
import java.util.List;

public class EconomyUtil {

    private static Economy vaultEcon;
    private static PlayerPointsAPI playerPointsAPI;
    private static TokenManager tokenManager;
    private static String type;

    // ====================== INIT ======================

    public static boolean setup() {
        type = ShopPlugin.getInstance().getConfig().getString("currency.type", "vault").toLowerCase();
        List<String> priority = Arrays.asList("vneconomy", "vault", "playerpoints", "tokenmanager");

        if (!priority.contains(type)) {
            Bukkit.getLogger().warning("❌ Loại tiền không hợp lệ trong config: " + type);
            return tryFallback(priority);
        }

        if (initType(type)) {
            Bukkit.getLogger().info("✅ ShopPlugin kết nối thành công với loại tiền: " + type);
            return true;
        }

        Bukkit.getLogger().warning("❌ Không tìm thấy plugin cho loại tiền: " + type);
        return tryFallback(priority);
    }

    private static boolean tryFallback(List<String> priority) {
        for (String p : priority) {
            if (initType(p)) {
                Bukkit.getLogger().warning("⚠ ShopPlugin tự động chuyển sang loại tiền: " + p);
                type = p;
                ShopPlugin.getInstance().getConfig().set("currency.type", p);
                ShopPlugin.getInstance().saveConfig();
                return true;
            }
        }
        Bukkit.getLogger().severe("❌ ShopPlugin không tìm thấy plugin tiền tệ nào khả dụng!");
        return false;
    }

    private static boolean initType(String t) {
        switch (t) {
            case "vneconomy" -> {
                return Bukkit.getPluginManager().getPlugin("VNEconomy") != null;
            }
            case "vault" -> {
                if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
                RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp == null) return false;
                vaultEcon = rsp.getProvider();
                return vaultEcon != null;
            }
            case "playerpoints" -> {
                if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) return false;
                playerPointsAPI = PlayerPoints.getInstance().getAPI();
                return playerPointsAPI != null;
            }
            case "tokenmanager" -> {
                if (Bukkit.getPluginManager().getPlugin("TokenManager") == null) return false;
                RegisteredServiceProvider<TokenManager> rsp = Bukkit.getServicesManager().getRegistration(TokenManager.class);
                if (rsp == null) return false;
                tokenManager = rsp.getProvider();
                return tokenManager != null;
            }
        }
        return false;
    }

    // ====================== API MẶC ĐỊNH (dựa theo config.type) ======================

    public static double getBalance(OfflinePlayer player) {
        return getBalance(player, type);
    }

    public static boolean has(OfflinePlayer player, double amount) {
        return has(player, amount, type);
    }

    public static boolean withdraw(OfflinePlayer player, double amount) {
        return withdraw(player, amount, type);
    }

    public static void deposit(OfflinePlayer player, double amount) {
        deposit(player, amount, type);
    }

    // ====================== API TÙY CHỌN CURRENCY ======================

    public static double getBalance(OfflinePlayer player, String currency) {
        if (currency == null) currency = type;

        return switch (currency.toLowerCase()) {
            case "vneconomy" -> EconomyAPI.getBalance(player.getUniqueId());
            case "vault" -> vaultEcon != null ? vaultEcon.getBalance(player) : 0;
            case "playerpoints" -> playerPointsAPI != null ? playerPointsAPI.look(player.getUniqueId()) : 0;
            case "tokenmanager" -> {
                if (!player.isOnline() || tokenManager == null) yield 0;
                yield tokenManager.getTokens(player.getPlayer()).orElse(0L);
            }
            default -> 0;
        };
    }

    public static boolean has(OfflinePlayer player, double amount, String currency) {
        return getBalance(player, currency) >= amount;
    }

    public static boolean withdraw(OfflinePlayer player, double amount, String currency) {
        if (amount <= 0) return true;
        if (currency == null) currency = type;

        return switch (currency.toLowerCase()) {
            case "vneconomy" -> EconomyAPI.take(player.getUniqueId(), amount);
            case "vault" -> vaultEcon != null && vaultEcon.withdrawPlayer(player, amount).transactionSuccess();
            case "playerpoints" -> {
                if (playerPointsAPI != null && playerPointsAPI.look(player.getUniqueId()) >= amount) {
                    playerPointsAPI.take(player.getUniqueId(), (int) amount);
                    yield true;
                }
                yield false;
            }
            case "tokenmanager" -> {
                if (!player.isOnline() || tokenManager == null) yield false;
                long balance = tokenManager.getTokens(player.getPlayer()).orElse(0L);
                if (balance >= (long) amount) {
                    tokenManager.removeTokens(player.getPlayer(), (long) amount);
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    public static void deposit(OfflinePlayer player, double amount, String currency) {
        if (amount <= 0) return;
        if (currency == null) currency = type;

        switch (currency.toLowerCase()) {
            case "vneconomy" -> EconomyAPI.give(player.getUniqueId(), amount);
            case "vault" -> {
                if (vaultEcon != null) vaultEcon.depositPlayer(player, amount);
            }
            case "playerpoints" -> {
                if (playerPointsAPI != null) playerPointsAPI.give(player.getUniqueId(), (int) amount);
            }
            case "tokenmanager" -> {
                if (player.isOnline() && tokenManager != null) {
                    tokenManager.addTokens(player.getPlayer(), (long) amount);
                }
            }
        }
    }

    // ====================== MISC ======================

    public static boolean reload() {
        ShopPlugin.getInstance().reloadConfig();
        type = ShopPlugin.getInstance().getConfig().getString("currency.type", "vault").toLowerCase();
        Bukkit.getLogger().info("🔄 ShopPlugin reload loại tiền: " + type);
        return setup();
    }

    public static String getType() {
        return type;
    }
}
