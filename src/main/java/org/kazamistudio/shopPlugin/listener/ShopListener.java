package org.kazamistudio.shopPlugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kazamistudio.shopPlugin.ShopPlugin;
import org.kazamistudio.shopPlugin.gui.BuyMenu;
import org.kazamistudio.shopPlugin.gui.ShopGUI;
import org.kazamistudio.shopPlugin.shop.ShopItem;
import org.kazamistudio.shopPlugin.utils.EconomyUtil;
import org.kazamistudio.shopPlugin.utils.MoneyFormat;

import java.util.Collections;

public class ShopListener implements Listener {
    private final ShopPlugin plugin;
    private final ShopGUI gui;

    public ShopListener(ShopPlugin plugin) {
        this.plugin = plugin;
        this.gui = new ShopGUI(plugin);
    }

    private ItemStack createSellButton(Inventory inv) {
        double total = 0;
        for (int i = 0; i < 26; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            ShopItem shopItem = plugin.getShopManager().findShopItem(item);
            if (shopItem != null) {
                total += shopItem.getPrice() * 0.01 * item.getAmount();
            }
        }

        ItemStack sellBtn = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = sellBtn.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aBán đồ");
            meta.setLore(Collections.singletonList("§7Tổng tiền nhận được: §e" + MoneyFormat.format(total) + "§7"));
            sellBtn.setItemMeta(meta);
        }
        return sellBtn;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        String title = e.getView().getTitle();
        if (title == null) return;

        // --- SHOP MAIN GUI ---
        if (title.startsWith("Shop - ")) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            String category = title.substring("Shop - ".length());
            int currentPage = gui.getCurrentPage(p);

            if (slot >= 46 && slot <= 49) {
                ItemStack clicked = inv.getItem(slot);
                if (clicked == null || !clicked.hasItemMeta()) return;
                String catName = clicked.getItemMeta().getDisplayName();
                p.openInventory(gui.create(p, catName, 0));
                return;
            }

            if (slot == 51) {
                if (currentPage > 0) {
                    p.openInventory(gui.create(p, category, currentPage - 1));
                } else {
                    p.sendMessage("§cBạn đang ở trang đầu tiên.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                }
                return;
            }

            if (slot == 52) {
                if (gui.hasNextPage(category, currentPage)) {
                    p.openInventory(gui.create(p, category, currentPage + 1));
                } else {
                    p.sendMessage("§cKhông còn trang tiếp theo.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                }
                return;
            }

            if (slot == 53) {
                Inventory sellInv = Bukkit.createInventory(p, 27, "Shop Sell");
                sellInv.setItem(26, createSellButton(sellInv));
                p.openInventory(sellInv);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                return;
            }

            if (slot >= 0 && slot <= 44) {
                ItemStack clicked = inv.getItem(slot);
                if (clicked == null || clicked.getType() == Material.AIR) return;
                BuyMenu.openBuyMenu(plugin, p, clicked);
            }
            return;
        }

        // --- BUY MENU ---
        if (title.startsWith("Buy - ")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String btnName = clicked.getItemMeta().getDisplayName();
            ShopItem shopItem = BuyMenu.getCurrentItem(p);
            if (shopItem == null) return;

            if (btnName.contains("Mua 1")) {
                if (plugin.getPendingDeliveries().contains(p.getUniqueId())) {
                    p.sendMessage("§cBạn đang chờ đơn hàng trước. Vui lòng đợi!");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    return;
                }

                double price = shopItem.getPrice();
                String currency = shopItem.getCurrency();

                if (!EconomyUtil.has(p, price, currency)) {
                    p.sendMessage("§cKhông đủ " + currency + "! Giá: §e" + MoneyFormat.format(price));
                    return;
                }

                EconomyUtil.withdraw(p, price, currency);
                plugin.getPendingDeliveries().add(p.getUniqueId());

                ItemStack deliveryItem = plugin.getShopManager().createDeliveryItem(shopItem);
                plugin.getDeliveryManager().scheduleDelivery(p, deliveryItem, 1);

                p.sendMessage("§aĐã đặt mua 1 " + shopItem.getDisplayName() + ". Hàng sẽ được giao sớm!");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1, 1);
                p.closeInventory();
            }

            else if (btnName.contains("Mua nhiều")) {
                p.closeInventory();
                ItemStack shopStack = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
                ItemMeta meta = shopStack.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(shopItem.getDisplayName());
                    meta.setLore(shopItem.getLore());
                    shopStack.setItemMeta(meta);
                }
                BuyMenu.openAnvilForAmount(plugin, p, shopStack);
            }
            return;
        }

        // --- SHOP SELL ---
        if (title.equals("Shop Sell")) {
            int rawSlot = e.getRawSlot();

            if (rawSlot >= 0 && rawSlot < inv.getSize()) {
                // Nút bán
                if (rawSlot == 26) {
                    e.setCancelled(true);
                    double total = 0;
                    boolean hasValidItem = false;

                    for (int i = 0; i < 26; i++) {
                        ItemStack item = inv.getItem(i);
                        if (item == null || item.getType() == Material.AIR) continue;

                        ShopItem shopItem = plugin.getShopManager().findShopItem(item);
                        if (shopItem != null) {
                            hasValidItem = true;
                            total += shopItem.getPrice() * 0.01 * item.getAmount();
                            inv.setItem(i, null);
                        }
                    }

                    if (hasValidItem && total > 0) {
                        EconomyUtil.deposit(p, total);
                        p.sendMessage("§aBạn đã bán đồ và nhận được §e" + MoneyFormat.format(total));
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
                    } else {
                        p.sendMessage("§cKhông có vật phẩm hợp lệ để bán.");
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    }

                    p.closeInventory();
                    return;
                }

                e.setCancelled(false);
                Bukkit.getScheduler().runTask(plugin, () -> inv.setItem(26, createSellButton(inv)));
            } else {
                e.setCancelled(false); // inventory player
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (title == null || !title.equals("Shop Sell")) return;

        boolean shouldUpdate = e.getRawSlots().stream().anyMatch(slot -> slot >= 0 && slot < 26);
        if (shouldUpdate) {
            Bukkit.getScheduler().runTask(plugin, () -> e.getInventory().setItem(26, createSellButton(e.getInventory())));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        String title = e.getView().getTitle();

        if (title.equals("Shop Sell")) {
            Inventory inv = e.getInventory();
            for (int i = 0; i < 26; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    p.getInventory().addItem(item);
                    inv.setItem(i, null);
                }
            }
        }
    }
}
