package org.kazamistudio.shopPlugin.gui;

import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kazamistudio.shopPlugin.ShopPlugin;
import org.kazamistudio.shopPlugin.shop.ShopItem;
import org.kazamistudio.shopPlugin.utils.EconomyUtil;

import java.util.*;

public class BuyMenu {

    private static final Map<UUID, ShopItem> currentItems = new HashMap<>();

    public static void openBuyMenu(ShopPlugin plugin, Player p, ItemStack clickedItem) {
        ShopItem shopItem = plugin.getShopManager().findShopItem(clickedItem);
        if (shopItem == null) {
            p.sendMessage("§cKhông tìm thấy thông tin vật phẩm này.");
            return;
        }

        currentItems.put(p.getUniqueId(), shopItem);

        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 9, "Buy - " + shopItem.getDisplayName());

        ItemStack buy1 = new ItemStack(Material.GREEN_WOOL);
        ItemMeta m = buy1.getItemMeta();
        if (m != null) {
            m.setDisplayName("§aMua 1");
            buy1.setItemMeta(m);
        }

        ItemStack buyn = new ItemStack(Material.LIME_WOOL);
        ItemMeta mn = buyn.getItemMeta();
        if (mn != null) {
            mn.setDisplayName("§aMua nhiều");
            buyn.setItemMeta(mn);
        }

        inv.setItem(3, buy1);
        inv.setItem(5, buyn);
        p.openInventory(inv);
    }

    public static ShopItem getCurrentItem(Player p) {
        return currentItems.get(p.getUniqueId());
    }

    public static void openAnvilForAmount(ShopPlugin plugin, Player p, ItemStack itemStack) {
        int max = plugin.getConfig().getInt("max-buy-amount", 128);

        ShopItem shopItem = plugin.getShopManager().findShopItem(itemStack);
        if (shopItem == null) {
            p.sendMessage("§cKhông tìm thấy vật phẩm này trong shop.");
            return;
        }

        new AnvilGUI.Builder()
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String text = state.getText();

                    try {
                        int amount = Integer.parseInt(text);
                        if (amount < 1) {
                            return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("Phải >= 1"));
                        }
                        if (amount > max) {
                            return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("Tối đa " + max));
                        }

                        double totalPrice = shopItem.getPrice() * amount;
                        String currency = shopItem.getCurrency();

                        if (!EconomyUtil.has(state.getPlayer(), totalPrice, currency)) {
                            state.getPlayer().playSound(state.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return Collections.singletonList(
                                    AnvilGUI.ResponseAction.replaceInputText("Không đủ " + currency + "! Tổng: " + totalPrice)
                            );
                        }

                        EconomyUtil.withdraw(state.getPlayer(), totalPrice, currency);
                        plugin.getPendingDeliveries().add(state.getPlayer().getUniqueId());

                        plugin.getDeliveryManager().scheduleDelivery(state.getPlayer(), itemStack, amount);

                        String displayName;
                        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                            displayName = itemStack.getItemMeta().getDisplayName();
                        } else {
                            displayName = itemStack.getType().toString();
                        }

                        state.getPlayer().sendMessage("§aBạn đã đặt mua §e" + amount + "§a " + displayName
                                + "§a với giá §e" + totalPrice + " " + currency + "§a. Hàng sẽ được giao đến bạn!");
                        state.getPlayer().playSound(state.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                        return Collections.singletonList(AnvilGUI.ResponseAction.close());
                    } catch (NumberFormatException ex) {
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("Nhập số hợp lệ"));
                    }
                })
                .text("1")
                .title("Nhập số lượng")
                .plugin(plugin)
                .open(p);
    }

}
