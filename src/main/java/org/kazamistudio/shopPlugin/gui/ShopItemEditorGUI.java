package org.kazamistudio.shopPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopItemEditorGUI {

    public Inventory createEditor(String category, ItemStack pending,
                                  Double price, String name, Integer cmd, List<String> loreLines, String currency) {
        Inventory inv = Bukkit.createInventory(null, 27, "§a[Thêm item] " + category);

        ItemStack blackPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = blackPane.getItemMeta();
        bm.setDisplayName(" ");
        blackPane.setItemMeta(bm);

        int[] borderSlots = {0,1,2,3,5,6,7,8,9,17,18,19,21,22,23,24,25};
        for (int slot : borderSlots) inv.setItem(slot, blackPane);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§7Đặt vật phẩm vào slot số 4");
        info.setItemMeta(im);
        inv.setItem(4, pending != null ? pending : info);

        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta ex = exit.getItemMeta();
        ex.setDisplayName("§cThoát");
        exit.setItemMeta(ex);
        inv.setItem(18, exit);

        inv.setItem(10, createPriceButton(price));
        inv.setItem(12, createNameButton(name));
        inv.setItem(14, createCmdButton(cmd));
        inv.setItem(16, createLoreButton(loreLines));

        inv.setItem(20, createCurrencyButton(currency != null ? currency : "vault"));

        ItemStack save = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta sm = save.getItemMeta();
        sm.setDisplayName("§aLưu");
        save.setItemMeta(sm);
        inv.setItem(26, save);

        return inv;
    }

    private ItemStack createPriceButton(Double price) {
        ItemStack is = new ItemStack(Material.GOLD_INGOT);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName("§eNhập giá (qua chat)");
        List<String> lore = new ArrayList<>();
        lore.add("§7Giá hiện tại: §a" + (price != null ? price : 0));
        m.setLore(lore);
        is.setItemMeta(m);
        return is;
    }

    private ItemStack createNameButton(String name) {
        ItemStack is = new ItemStack(Material.OAK_SIGN);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName("§dĐặt tên item (qua chat)");
        List<String> lore = new ArrayList<>();
        lore.add("§7Tên hiện tại: " + (name != null ? name : "§cChưa đặt"));
        m.setLore(lore);
        is.setItemMeta(m);
        return is;
    }

    private ItemStack createCmdButton(Integer cmd) {
        ItemStack is = new ItemStack(Material.NAME_TAG);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName("§bNhập CustomModelData (qua chat)");
        List<String> lore = new ArrayList<>();
        lore.add("§7CMD hiện tại: " + (cmd != null && cmd > 0 ? cmd : 0));
        m.setLore(lore);
        is.setItemMeta(m);
        return is;
    }

    private ItemStack createLoreButton(List<String> loreLines) {
        ItemStack is = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName("§6Thêm lore (qua chat)");
        List<String> lore = new ArrayList<>();
        if (loreLines != null && !loreLines.isEmpty()) {
            lore.add("§7Lore hiện tại:");
            for (String l : loreLines) {
                lore.add(" §f- " + l);
            }
        } else {
            lore.add("§cChưa có lore");
        }
        m.setLore(lore);
        is.setItemMeta(m);
        return is;
    }

    public static ItemStack createCurrencyButton(String current) {
        ItemStack cur = new ItemStack(Material.PAPER);
        ItemMeta cm = cur.getItemMeta();
        cm.setDisplayName("§6Chọn loại tiền");

        List<String> lore = new ArrayList<>();
        lore.add("§7Hiện tại: §e" + current);
        lore.add("§7Chuột phải để chuyển đổi");

        cm.setLore(lore);
        cur.setItemMeta(cm);
        return cur;
    }

    public void handleSetItem(Player p, Inventory inv, ItemStack placedItem,
                              double price, int cmd, String displayName, List<String> loreLines) {
        if (placedItem == null || placedItem.getType() == Material.AIR) return;

        ItemStack display = placedItem.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return;

        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(displayName);
        }

        List<String> finalLore = new ArrayList<>();
        if (meta.hasLore()) finalLore.addAll(meta.getLore());
        if (loreLines != null && !loreLines.isEmpty()) finalLore.addAll(loreLines);

        meta.setLore(finalLore);
        display.setItemMeta(meta);

        inv.setItem(4, display);

        p.openInventory(inv);
    }
}
