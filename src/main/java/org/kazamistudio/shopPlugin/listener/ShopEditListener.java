package org.kazamistudio.shopPlugin.listener;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.kazamistudio.shopPlugin.ShopPlugin;
import org.kazamistudio.shopPlugin.gui.ShopAdminGUI;
import org.kazamistudio.shopPlugin.gui.ShopItemEditorGUI;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;

public class ShopEditListener implements Listener {
    private final ShopPlugin plugin;

    private final Map<UUID, String> editingCategory = new HashMap<>();
    private final Map<UUID, ItemStack> pendingItem = new HashMap<>();
    private final Map<UUID, String> editMode = new HashMap<>(); // ADD hoặc EDIT
    private final Map<UUID, String> editingItemKey = new HashMap<>();
    private final Map<String, String> categoryFileMap = new HashMap<>();

    private final Map<UUID, Double> pendingPrice = new HashMap<>();
    private final Map<UUID, Integer> pendingCmd = new HashMap<>();

    public static NamespacedKey SHOP_KEY;
    private final Map<UUID, String> pendingName = new HashMap<>();
    private final Map<UUID, List<String>> pendingLore = new HashMap<>();

    private final Map<UUID, String> pendingNewCategory = new HashMap<>();

    private final Map<UUID, String> pendingCurrency = new HashMap<>();

    public ShopEditListener(ShopPlugin plugin) {
        this.plugin = plugin;
        SHOP_KEY = new NamespacedKey(plugin, "shop_key");
    }

    private File getCategoryFile(String cat) {
        String fileName = categoryFileMap.get(cat);
        if (fileName == null) {
            fileName = normalizeCategory(cat); // fallback
        }
        return new File(plugin.getDataFolder(), "data/" + fileName + ".yml");
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getView().getTitle() == null) return;

        String title = e.getView().getTitle();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // ================= MENU CHỌN CATEGORY =================
        if (title.equals("§c[ShopAdmin] Chọn category")) {
            e.setCancelled(true);
            if (!clicked.hasItemMeta()) return;

            String displayName = clicked.getItemMeta().getDisplayName();

            if (displayName.equals("§a➕ Thêm mục mới")) {
                pendingNewCategory.put(p.getUniqueId(), "");
                p.closeInventory();
                p.sendMessage("§eNhập tên mục mới qua chat:");
                return;
            }

            String cat = displayName.replace("§e", "");
            new ShopAdminGUI(plugin).openCategoryEditor(p, cat, 0);
        }


        // ================= MENU EDIT CATEGORY =================
        else if (title.startsWith("§c[Edit] ")) {
            e.setCancelled(true);

            String raw = title.replace("§c[Edit] ", "");
            String cat = raw;
            int page = 0;

            if (raw.contains("§7(Page ")) {
                cat = raw.substring(0, raw.indexOf("§7(Page ")).trim();
                String numStr = raw.substring(raw.indexOf("Page ") + 5, raw.indexOf(")"));
                page = Integer.parseInt(numStr) - 1;
            }

            if (clicked.getType() == Material.ARROW && clicked.hasItemMeta()) {
                String dn = clicked.getItemMeta().getDisplayName();
                if (dn.contains("Trang trước")) {
                    new ShopAdminGUI(plugin).openCategoryEditor(p, cat, page - 1);
                } else if (dn.contains("Trang sau")) {
                    new ShopAdminGUI(plugin).openCategoryEditor(p, cat, page + 1);
                }
                return;
            }

            if (clicked.getType() == Material.ANVIL) {
                p.openInventory(plugin.getEditorGUI().createEditor(
                        cat,
                        null,
                        0.0,
                        null,
                        0,
                        null,
                        "vault"
                ));

            } else if (clicked.getType() == Material.BARRIER) {
                p.sendMessage("§cChế độ xóa: click item muốn xóa.");
                e.getInventory().setItem(53, createIndicator("§c[CHẾ ĐỘ XÓA]"));

            } else if (clicked.getType() == Material.NAME_TAG) {
                p.sendMessage("§eChế độ sửa giá: click item muốn sửa.");
                e.getInventory().setItem(53, createIndicator("§e[CHẾ ĐỘ SỬA GIÁ]"));

            } else {
                ItemStack indicator = e.getInventory().getItem(53);
                if (indicator != null && indicator.hasItemMeta()) {
                    String mode = indicator.getItemMeta().getDisplayName();

                    if (mode.contains("XÓA")) {
                        removeItemFromCategory(cat, clicked);
                        p.sendMessage("§aĐã xóa item khỏi " + cat);
                        new ShopAdminGUI(plugin).openCategoryEditor(p, cat, page - 1);

                    } else if (mode.contains("SỬA GIÁ")) {
                        editingCategory.put(p.getUniqueId(), cat);
                        editingItemKey.put(p.getUniqueId(), getItemKey(clicked));
                        editMode.put(p.getUniqueId(), "EDIT");

                        p.closeInventory();
                        p.sendMessage("§eNhập giá mới qua chat:");
                    }
                }
            }
        }

        // ================= MENU THÊM ITEM =================
        else if (title.startsWith("§a[Thêm item] ")) {
            String cat = title.replace("§a[Thêm item] ", "");
            int slot = e.getRawSlot();

            int[] borderSlots = {0,1,2,3,5,6,7,8,9,17,18,19,21,22,23,24,25};
            for (int s : borderSlots) {
                if (slot == s) {
                    e.setCancelled(true);
                    return;
                }
            }

            // ===== Slot 4: đặt vật phẩm =====
            if (slot == 4) {
                ItemStack cursorItem = e.getCursor();
                if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                    e.setCancelled(true);

                    double price = pendingPrice.getOrDefault(p.getUniqueId(), 0.0);
                    int cmd = pendingCmd.getOrDefault(p.getUniqueId(), 0);
                    String name = pendingName.getOrDefault(p.getUniqueId(), null);
                    List<String> lore = pendingLore.getOrDefault(p.getUniqueId(), null);
                    String currency = pendingCurrency.getOrDefault(p.getUniqueId(), "vault");

                    if (cursorItem.hasItemMeta() && cursorItem.getItemMeta().hasLore()) {
                        pendingLore.put(p.getUniqueId(), new ArrayList<>(cursorItem.getItemMeta().getLore()));
                    }

                    Inventory inv = new ShopItemEditorGUI().createEditor(cat, cursorItem, price, name, cmd, lore, currency);
                    new ShopItemEditorGUI().handleSetItem(p, inv, cursorItem, price, cmd, name, lore);

                    p.setItemOnCursor(null);
                    pendingItem.put(p.getUniqueId(), cursorItem);
                }

                return;
            }

            if (slot == 18) {
                e.setCancelled(true);
                p.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () ->
                        new ShopAdminGUI(plugin).openCategoryEditor(p, cat, 0)
                );
            }
            else if (slot == 10) {
                e.setCancelled(true);
                pendingItem.put(p.getUniqueId(), e.getInventory().getItem(4));
                editingCategory.put(p.getUniqueId(), cat);
                editMode.put(p.getUniqueId(), "ADD");

                p.closeInventory();
                p.sendMessage("§eNhập giá cho item vừa đặt:");
            }
            else if (slot == 12) {
                e.setCancelled(true);
                pendingItem.put(p.getUniqueId(), e.getInventory().getItem(4));
                editingCategory.put(p.getUniqueId(), cat);
                editMode.put(p.getUniqueId(), "NAME");

                p.closeInventory();
                p.sendMessage("§dNhập tên hiển thị cho item vừa đặt:");
            }
            else if (slot == 14) {
                e.setCancelled(true);
                pendingItem.put(p.getUniqueId(), e.getInventory().getItem(4));
                editingCategory.put(p.getUniqueId(), cat);
                editMode.put(p.getUniqueId(), "CMD");

                p.closeInventory();
                p.sendMessage("§bNhập CustomModelData (số) cho item vừa đặt:");
            }
            else if (slot == 16) {
                e.setCancelled(true);
                pendingItem.put(p.getUniqueId(), e.getInventory().getItem(4));
                editingCategory.put(p.getUniqueId(), cat);
                editMode.put(p.getUniqueId(), "LORE");

                p.closeInventory();
                p.sendMessage("§6Nhập lore cho item (gõ 'huy' để hủy, gõ 'xong' để kết thúc).");
            }
            else if (slot == 20) {
                e.setCancelled(true);

                String current = pendingCurrency.getOrDefault(p.getUniqueId(), "vault");
                List<String> currencies = Arrays.asList("vault", "playerpoints", "tokenmanager", "vneconomy");

                int idx = currencies.indexOf(current);
                if (idx == -1) idx = 0;

                if (e.getClick() == ClickType.RIGHT) {
                    idx = (idx + 1) % currencies.size();
                    current = currencies.get(idx);
                    pendingCurrency.put(p.getUniqueId(), current);
                    p.sendMessage("§aĐã chuyển loại tiền sang: §e" + current);
                } else {
                    p.sendMessage("§7Loại tiền hiện tại: §e" + current);
                }

                e.getInventory().setItem(20, ShopItemEditorGUI.createCurrencyButton(current));
            }
            else if (slot == 26) {
                e.setCancelled(true);
                ItemStack it = e.getInventory().getItem(4);

                if (it == null || it.getType() == Material.AIR) {
                    p.sendMessage("§cBạn chưa đặt item!");
                    return;
                }

                double price = pendingPrice.getOrDefault(p.getUniqueId(), 0.0);
                int cmd = pendingCmd.getOrDefault(p.getUniqueId(), 0);
                String currency = pendingCurrency.getOrDefault(p.getUniqueId(), "vault");

                ItemMeta meta = it.getItemMeta();
                if (meta != null && cmd > 0) {
                    meta.setCustomModelData(cmd);
                    it.setItemMeta(meta);
                }

                saveItemToCategory(cat, it, price, currency, p);
                p.sendMessage("§aĐã lưu item vào " + cat + " với giá " + price + " " + currency + " và CMD = " + cmd);

                pendingItem.remove(p.getUniqueId());
                pendingPrice.remove(p.getUniqueId());
                pendingCmd.remove(p.getUniqueId());
                pendingCurrency.remove(p.getUniqueId());

                new ShopAdminGUI(plugin).openCategoryEditor(p, cat, 0);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();

        UUID uuid = p.getUniqueId();
        boolean isCategoryInput = pendingNewCategory.containsKey(uuid);
        boolean isEditingItem = editingCategory.containsKey(uuid);

        if (!isCategoryInput && !isEditingItem) return;

        e.setCancelled(true);

        // ======= Nhập category mới =======
        if (isCategoryInput) {
            String input = e.getMessage();
            pendingNewCategory.remove(uuid);

            File dataFolder = new File(plugin.getDataFolder(), "data");
            if (!dataFolder.exists()) dataFolder.mkdirs();

            File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            int currentCount = (files != null) ? files.length : 0;

            if (currentCount >= 4) {
                p.sendMessage("§cChỉ có thể tạo tối đa 4 mục category!");
                Bukkit.getScheduler().runTask(plugin, () -> new ShopAdminGUI(plugin).openCategoryMenu(p));
                return;
            }

            String fileName = normalizeCategory(input);
            String displayName = input;

            File file = new File(dataFolder, fileName + ".yml");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                    cfg.set("category-name", displayName);
                    cfg.set("items", null);
                    cfg.save(file);
                    plugin.getShopManager().loadAllCategories();
                    p.sendMessage("§aĐã tạo mục mới: " + displayName);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    p.sendMessage("§cKhông thể tạo mục mới!");
                }
            } else {
                p.sendMessage("§cMục đã tồn tại!");
            }

            Bukkit.getScheduler().runTask(plugin, () -> new ShopAdminGUI(plugin).openCategoryMenu(p));
            return;
        }

        // ======= Các chế độ chỉnh sửa item =======
        if (!isEditingItem) return;

        String cat = editingCategory.get(uuid);
        String mode = editMode.get(uuid);

        try {
            switch (mode) {
                case "ADD" -> {
                    double price = Double.parseDouble(e.getMessage());
                    handleAddMode(p, cat, price);
                }
                case "EDIT" -> {
                    double price = Double.parseDouble(e.getMessage());
                    handleEditMode(p, cat, price);
                }
                case "CMD" -> {
                    int cmd = Integer.parseInt(e.getMessage());
                    handleCmdMode(p, cat, cmd);
                }
                case "NAME" -> {
                    String name = e.getMessage();
                    handleNameMode(p, cat, name);
                }
                case "LORE" -> {
                    String msg = e.getMessage();

                    if (msg.equalsIgnoreCase("huy")) {
                        pendingLore.remove(uuid);
                        p.sendMessage("§cĐã hủy nhập lore.");
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Inventory inv = plugin.getEditorGUI().createEditor(
                                    cat,
                                    pendingItem.get(uuid),
                                    pendingPrice.getOrDefault(uuid, 0.0),
                                    pendingName.getOrDefault(uuid, null),
                                    pendingCmd.getOrDefault(uuid, 0),
                                    null,
                                    pendingCurrency.getOrDefault(uuid, "vault")
                            );
                            p.openInventory(inv);
                        });
                        return;
                    }

                    if (msg.equalsIgnoreCase("xong")) {
                        p.sendMessage("§aĐã hoàn tất nhập lore. Hãy ấn Lưu để hoàn tất.");
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Inventory inv = plugin.getEditorGUI().createEditor(
                                    cat,
                                    pendingItem.get(uuid),
                                    pendingPrice.getOrDefault(uuid, 0.0),
                                    pendingName.getOrDefault(uuid, null),
                                    pendingCmd.getOrDefault(uuid, 0),
                                    pendingLore.getOrDefault(uuid, null),
                                    pendingCurrency.getOrDefault(uuid, "vault")
                            );
                            p.openInventory(inv);
                        });
                        return;
                    }

                    String line = org.kazamistudio.shopPlugin.utils.ColorUtil.translate(msg);
                    pendingLore.computeIfAbsent(uuid, k -> new ArrayList<>()).add(line);
                    p.sendMessage("§aĐã thêm lore: " + line);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Inventory inv = plugin.getEditorGUI().createEditor(
                                cat,
                                pendingItem.get(uuid),
                                pendingPrice.getOrDefault(uuid, 0.0),
                                pendingName.getOrDefault(uuid, null),
                                pendingCmd.getOrDefault(uuid, 0),
                                pendingLore.getOrDefault(uuid, null),
                                pendingCurrency.getOrDefault(uuid, "vault")
                        );
                        plugin.getEditorGUI().handleSetItem(
                                p,
                                inv,
                                pendingItem.get(uuid),
                                pendingPrice.getOrDefault(uuid, 0.0),
                                pendingCmd.getOrDefault(uuid, 0),
                                pendingName.getOrDefault(uuid, null),
                                pendingLore.getOrDefault(uuid, null)
                        );
                    });
                }
            }
        } catch (NumberFormatException ex) {
            p.sendMessage("§cGiá không hợp lệ, hãy nhập số!");
        }
    }


    // ========== Xử lý riêng cho ADD ==========
    private void handleAddMode(Player p, String cat, double price) {
        UUID uuid = p.getUniqueId();
        pendingPrice.put(uuid, price);
        p.sendMessage("§aĐã nhập giá: " + price + ". Hãy ấn Lưu để hoàn tất.");

        Bukkit.getScheduler().runTask(plugin, () -> {
            Inventory inv = plugin.getEditorGUI().createEditor(
                    cat,
                    pendingItem.get(uuid),
                    price,
                    pendingName.getOrDefault(uuid, null),
                    pendingCmd.getOrDefault(uuid, 0),
                    pendingLore.getOrDefault(uuid, null),
                    pendingCurrency.getOrDefault(uuid, "vault")
            );
            plugin.getEditorGUI().handleSetItem(
                    p,
                    inv,
                    pendingItem.get(uuid),
                    price,
                    pendingCmd.getOrDefault(uuid, 0),
                    pendingName.getOrDefault(uuid, null),
                    pendingLore.getOrDefault(uuid, null)
            );
        });
    }

    private void handleCmdMode(Player p, String cat, int cmd) {
        UUID uuid = p.getUniqueId();
        pendingCmd.put(uuid, cmd);
        p.sendMessage("§aĐã nhập CMD = " + cmd + ". Hãy ấn §eLưu §ađể hoàn tất.");

        ItemStack item = pendingItem.get(uuid);
        if (item != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = plugin.getEditorGUI().createEditor(
                        cat,
                        item,
                        pendingPrice.getOrDefault(uuid, 0.0),
                        pendingName.getOrDefault(uuid, null),
                        cmd,
                        pendingLore.getOrDefault(uuid, null),
                        pendingCurrency.getOrDefault(uuid, "vault")
                );
                plugin.getEditorGUI().handleSetItem(
                        p,
                        inv,
                        item,
                        pendingPrice.getOrDefault(uuid, 0.0),
                        cmd,
                        pendingName.getOrDefault(uuid, null),
                        pendingLore.getOrDefault(uuid, null)
                );
            });
        }
    }

    private void handleNameMode(Player p, String cat, String name) {
        UUID uuid = p.getUniqueId();
        String colored = org.kazamistudio.shopPlugin.utils.ColorUtil.translate(name);
        pendingName.put(uuid, colored);
        p.sendMessage("§aĐã đặt tên: " + colored + "§a. Hãy ấn §eLưu §ađể hoàn tất.");

        ItemStack item = pendingItem.get(uuid);
        if (item != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = plugin.getEditorGUI().createEditor(
                        cat,
                        item,
                        pendingPrice.getOrDefault(uuid, 0.0),
                        colored,
                        pendingCmd.getOrDefault(uuid, 0),
                        pendingLore.getOrDefault(uuid, null),
                        pendingCurrency.getOrDefault(uuid, "vault")
                );
                plugin.getEditorGUI().handleSetItem(
                        p,
                        inv,
                        item,
                        pendingPrice.getOrDefault(uuid, 0.0),
                        pendingCmd.getOrDefault(uuid, 0),
                        colored,
                        pendingLore.getOrDefault(uuid, null)
                );
            });
        }
    }


    // ========== Xử lý riêng cho EDIT ==========
    private void handleEditMode(Player p, String cat, double price) {
        File file = getCategoryFile(cat);
        String key = editingItemKey.remove(p.getUniqueId());
        if (key == null) {
            p.sendMessage("§cKhông tìm thấy key của item cần sửa!");
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String path = "items." + key + ".price";

        if (cfg.contains(path)) {
            cfg.set(path, price);
            try {
                cfg.save(file);
                plugin.getShopManager().loadAllCategories();
                p.sendMessage("§aĐã chỉnh giá item trong " + cat + " thành " + price);
            } catch (IOException e) {
                e.printStackTrace();
                p.sendMessage("§cKhông thể cập nhật giá mới!");
            }
        } else {
            p.sendMessage("§cKhông tìm thấy item trong file!");
        }

        Bukkit.getScheduler().runTask(plugin, () ->
                new ShopAdminGUI(plugin).openCategoryEditor(p, cat, 0)
        );
    }

    // ================= HÀM HỖ TRỢ =================

    private void saveItemToCategory(String cat, ItemStack item, double price, String currency, Player p) {
        File file = getCategoryFile(cat);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) meta = clone.getItemMeta();

        String displayName;
        if (pendingName.containsKey(p.getUniqueId())) {
            displayName = pendingName.remove(p.getUniqueId());
        } else if (meta != null && meta.hasDisplayName()) {
            displayName = meta.getDisplayName();
        } else {
            displayName = item.getType().name();
        }

        List<String> lore = new ArrayList<>();
        if (pendingLore.containsKey(p.getUniqueId())) {
            lore.addAll(pendingLore.remove(p.getUniqueId()));
        } else if (meta != null && meta.hasLore()) {
            lore.addAll(meta.getLore());
        }

        setItemKey(clone, key);
        if (meta != null) {
            meta.setLore(lore);
            clone.setItemMeta(meta);
        }

        String mmoId = MMOItems.getID(item);
        Type mmoType = MMOItems.getType(item);

        cfg.set("items." + key + ".name", displayName);
        cfg.set("items." + key + ".lore", lore);
        cfg.set("items." + key + ".price", price);
        cfg.set("items." + key + ".amount", item.getAmount());
        cfg.set("items." + key + ".currency", currency);

        if (mmoId != null && mmoType != null) {
            cfg.set("items." + key + ".mmoitem-type", mmoType.getId());
            cfg.set("items." + key + ".mmoitem-id", mmoId);
        } else {
            cfg.set("items." + key + ".material", item.getType().name());
            if (meta != null && meta.hasCustomModelData()) {
                cfg.set("items." + key + ".custom-model-data", meta.getCustomModelData());
            }
        }

        try {
            cfg.save(file);
            plugin.getShopManager().loadAllCategories();
        } catch (IOException e) {
            e.printStackTrace();
            p.sendMessage("§cKhông thể lưu item vào category!");
        }
    }

    private void removeItemFromCategory(String cat, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        NamespacedKey dataKey = new NamespacedKey(plugin, "shop_key");
        String shopKey = meta.getPersistentDataContainer().get(dataKey, PersistentDataType.STRING);
        if (shopKey == null) return;

        File file = getCategoryFile(cat);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        if (cfg.isConfigurationSection("items")) {
            if (cfg.contains("items." + shopKey)) {
                cfg.set("items." + shopKey, null);
                try {
                    cfg.save(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                plugin.getShopManager().loadAllCategories();
            }
        }
    }

    private void setItemKey(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(SHOP_KEY, PersistentDataType.STRING, key);
        item.setItemMeta(meta);
    }

    private String getItemKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(SHOP_KEY, PersistentDataType.STRING);
    }


    private ItemStack createIndicator(String name) {
        ItemStack is = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta m = is.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            is.setItemMeta(m);
        }
        return is;
    }

    private String normalizeCategory(String cat) {
        String temp = Normalizer.normalize(cat, Normalizer.Form.NFD);
        temp = temp.replaceAll("\\p{M}", "");
        return temp.toLowerCase()
                .replace(" ", "")
                .replaceAll("[^a-z0-9_]", "");
    }

}
