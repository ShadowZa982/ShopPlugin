package org.kazamistudio.shopPlugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.JSONObject;
import org.kazamistudio.shopPlugin.ShopPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker implements Listener {

    private final String apiUrl = "https://api.github.com/repos/ShadowZa982/ShopPlugin/releases/latest";
    private final ShopPlugin plugin;

    private String latestVersion = "";
    private String currentVersion = "";
    private boolean isNewVersion = false;

    public UpdateChecker(ShopPlugin plugin) {
        this.plugin = plugin;

        // lấy version hiện tại từ config.yml (fallback sang plugin.yml)
        currentVersion = plugin.getConfig().getString("update-checker.current-version",
                plugin.getDescription().getVersion());

        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) {
            Bukkit.getConsoleSender().sendMessage("[Shopee] §7UpdateChecker disabled in config.yml");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setInstanceFollowRedirects(true);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    Bukkit.getConsoleSender().sendMessage("[Shopee] §cKhông thể kiểm tra phiên bản mới!");
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                latestVersion = json.getString("tag_name").trim();

                // So sánh với config trước
                if (!currentVersion.equals(latestVersion)) {
                    isNewVersion = true;
                    Bukkit.getConsoleSender().sendMessage("[Shopee] §aCó phiên bản mới §6" + latestVersion + "§a (bạn đang dùng: " + currentVersion + ")");
                    Bukkit.getConsoleSender().sendMessage("https://github.com/ShadowZa982/ShopPlugin/releases/latest");
                }

            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("[Shopee] §cLỗi khi kiểm tra phiên bản mới");
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().isOp() && isNewVersion) {
            event.getPlayer().sendMessage("[Shopee] §aCó phiên bản mới §6" + latestVersion + "§a (bạn đang dùng: " + currentVersion + ")");
            event.getPlayer().sendMessage("§6https://github.com/ShadowZa982/ShopPlugin/releases/latest");
        }
    }
}
