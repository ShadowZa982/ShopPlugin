package org.kazamistudio.shopPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kazamistudio.shopPlugin.ShopPlugin;
import org.kazamistudio.shopPlugin.manager.ShopManager;
import org.kazamistudio.shopPlugin.shop.ShopItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopGUI {
    private final ShopPlugin plugin;
    private final ShopManager manager;

    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public ShopGUI(ShopPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getShopManager();
    }

    public Inventory create(Player p, String category, int page) {
        playerPages.put(p.getUniqueId(), page); // Lưu page hiện tại

        Inventory inv = Bukkit.createInventory(null, 54, "Shop - " + category);
        List<ShopItem> items = manager.getItemsForCategory(category);
        int perPage = 46;
        int start = page * perPage;

        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= items.size()) break;
            ShopItem s = items.get(idx);

            ItemStack is = manager.createDisplayItem(s);

            inv.setItem(i, is);
        }

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        int slot = 46;
        for (String cat : manager.getCategoryNames()) {
            if (slot > 49) break;
            ItemStack b = new ItemStack(Material.PAPER);
            ItemMeta meta = b.getItemMeta();
            meta.setDisplayName(cat);
            b.setItemMeta(meta);
            inv.setItem(slot++, b);
        }

        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        pm.setDisplayName("Trang trước");
        prev.setItemMeta(pm);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        nm.setDisplayName("Trang sau");
        next.setItemMeta(nm);

        inv.setItem(51, prev);
        inv.setItem(52, next);

        ItemStack sell = new ItemStack(Material.CHEST);
        ItemMeta sm = sell.getItemMeta();
        sm.setDisplayName("Bán vật phẩm");
        sell.setItemMeta(sm);
        inv.setItem(53, sell);

        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);

        return inv;
    }

    public int getCurrentPage(Player p) {
        return playerPages.getOrDefault(p.getUniqueId(), 0);
    }

    public boolean hasNextPage(String category, int currentPage) {
        int totalItems = manager.getItemsForCategory(category).size();
        int maxPage = (int) Math.ceil(totalItems / 46.0) - 1;
        return currentPage < maxPage;
    }
}