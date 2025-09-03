package org.kazamistudio.shopPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kazamistudio.shopPlugin.ShopPlugin;
import org.kazamistudio.shopPlugin.listener.ShopEditListener;
import org.kazamistudio.shopPlugin.manager.ShopManager;
import org.kazamistudio.shopPlugin.shop.ShopItem;

import java.util.List;

public class ShopAdminGUI {
    private final ShopPlugin plugin;
    private final ShopManager manager;

    public ShopAdminGUI(ShopPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getShopManager();
    }

    public void openCategoryMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§c[ShopAdmin] Chọn category");
        int slot = 0;
        for (String cat : manager.getCategoryNames()) {
            ItemStack paper = new ItemStack(Material.BOOK);
            ItemMeta meta = paper.getItemMeta();
            meta.setDisplayName("§e" + cat);
            paper.setItemMeta(meta);
            inv.setItem(slot++, paper);
        }

        ItemStack addCat = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta addMeta = addCat.getItemMeta();
        addMeta.setDisplayName("§a➕ Thêm mục mới");
        addCat.setItemMeta(addMeta);
        inv.setItem(26, addCat);

        p.openInventory(inv);
    }

    public void openCategoryEditor(Player p, String category, int page) {
        p.openInventory(createCategoryEditor(category, page));
    }

    public Inventory createCategoryEditor(String category, int page) {
        List<ShopItem> items = manager.getItemsForCategory(category);
        Inventory inv = Bukkit.createInventory(null, 54, "§c[Edit] " + category + " §7(Page " + (page+1) + ")");

        int perPage = 45;
        int start = page * perPage;

        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= items.size()) break;
            ShopItem item = items.get(idx);

            ItemStack is = manager.createDisplayItem(item);

            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(
                        ShopEditListener.SHOP_KEY,
                        org.bukkit.persistence.PersistentDataType.STRING,
                        item.getKey()
                );
                is.setItemMeta(meta);
            }

            inv.setItem(i, is);
        }

        ItemStack add = new ItemStack(Material.ANVIL);
        ItemMeta am = add.getItemMeta();
        am.setDisplayName("§a➕ Thêm vật phẩm mới");
        add.setItemMeta(am);
        inv.setItem(49, add);

        ItemStack del = new ItemStack(Material.BARRIER);
        ItemMeta dm = del.getItemMeta();
        dm.setDisplayName("§c❌ Xóa vật phẩm");
        del.setItemMeta(dm);
        inv.setItem(50, del);

        ItemStack edit = new ItemStack(Material.NAME_TAG);
        ItemMeta em = edit.getItemMeta();
        em.setDisplayName("§e✏ Chỉnh sửa giá");
        edit.setItemMeta(em);
        inv.setItem(51, edit);

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            pm.setDisplayName("§e◀ Trang trước");
            prev.setItemMeta(pm);
            inv.setItem(45, prev);
        }

        int maxPage = (int) Math.ceil(items.size() / (double) perPage) - 1;
        if (page < maxPage) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName("§eTrang sau ▶");
            next.setItemMeta(nm);
            inv.setItem(53, next);
        }

        return inv;
    }
}
