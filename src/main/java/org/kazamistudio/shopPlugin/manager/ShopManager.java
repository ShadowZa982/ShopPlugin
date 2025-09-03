package org.kazamistudio.shopPlugin.manager;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kazamistudio.shopPlugin.ShopPlugin;
import org.kazamistudio.shopPlugin.shop.ShopItem;
import org.kazamistudio.shopPlugin.utils.ColorUtil;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

public class ShopManager {
    private final ShopPlugin plugin;
    private final Map<String, List<ShopItem>> categories = new HashMap<>();
    private static final Map<String, String> CURRENCY_DISPLAY = new HashMap<>();

    public ShopManager(ShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAllCategories() {
        categories.clear();

        File dataFolder = plugin.getDataFolder();
        File dir = new File(dataFolder, "data");

        if (!dir.exists()) {
            dir.mkdirs();
            plugin.saveResource("data/hatgiong.yml", false);
            plugin.saveResource("data/thucan.yml", false);
            plugin.saveResource("data/vatlieu.yml", false);
            plugin.saveResource("data/trangtri.yml", false);
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("Không tìm thấy file .yml nào trong thư mục data/.");
            return;
        }

        for (File f : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            String catName = cfg.getString("category-name", f.getName().replace(".yml", ""));
            List<ShopItem> items = new ArrayList<>();

            if (cfg.isConfigurationSection("items")) {
                ConfigurationSection itemsSec = cfg.getConfigurationSection("items");
                for (String key : itemsSec.getKeys(false)) {
                    ConfigurationSection section = itemsSec.getConfigurationSection(key);
                    ShopItem shopItem = parseItemFromSection(key, section, f.getName());
                    if (shopItem != null) items.add(shopItem);
                }
            } else if (cfg.isList("items")) {
                List<?> raw = cfg.getList("items");
                for (Object o : raw) {
                    if (o instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) o;
                        ShopItem shopItem = parseItemFromMap(map, f.getName());
                        if (shopItem != null) items.add(shopItem);
                    }
                }
            }

            categories.put(catName, items);
            plugin.getLogger().info("Loaded category: " + catName + " (" + items.size() + " items)");
        }

        plugin.getLogger().info("Tổng số category đã load: " + categories.keySet().size());
    }

    private ShopItem parseItemFromSection(String key, ConfigurationSection section, String fileName) {
        String mmoType = section.getString("mmoitem-type", null);
        String mmoId = section.getString("mmoitem-id", null);

        String name = ColorUtil.translate(section.getString("name", "Item"));
        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(ColorUtil.translate(line));
        }

        double price = section.getDouble("price", 0);
        int amount = section.getInt("amount", 1);
        int cmd = section.contains("custom-model-data") ? section.getInt("custom-model-data") : 0;
        String currency = section.getString("currency", "vault");

        Material mat = Material.STONE;
        ItemStack base = null;

        if (mmoType != null && mmoId != null) {
            Type type = Type.get(mmoType);
            if (type != null) {
                MMOItem mItem = MMOItems.plugin.getMMOItem(type, mmoId);
                if (mItem != null) {
                    base = mItem.newBuilder().build();
                    base.setAmount(amount);
                }
            }
        }

        if (base == null) {
            String matName = section.getString("material", "STONE").toUpperCase();
            try {
                mat = Material.valueOf(matName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Material không hợp lệ trong " + fileName + ": " + matName);
                return null;
            }

            base = new ItemStack(mat, amount);
            ItemMeta meta = base.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(lore);
                if (cmd > 0) meta.setCustomModelData(cmd);
                base.setItemMeta(meta);
            }
        }

        return new ShopItem(key, base, mat, name, lore, price, amount,
                cmd > 0 ? cmd : null, currency, mmoType, mmoId);
    }

    private ShopItem parseItemFromMap(Map<String, Object> map, String fileName) {
        String mmoType = map.containsKey("mmoitem-type") ? String.valueOf(map.get("mmoitem-type")) : null;
        String mmoId = map.containsKey("mmoitem-id") ? String.valueOf(map.get("mmoitem-id")) : null;

        String name = ColorUtil.translate(String.valueOf(map.getOrDefault("name", "Item")));
        @SuppressWarnings("unchecked")
        List<String> rawLore = (List<String>) map.getOrDefault("lore", Collections.emptyList());
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            lore.add(ColorUtil.translate(line));
        }

        double price = Double.parseDouble(map.getOrDefault("price", 0).toString());
        int amount = Integer.parseInt(map.getOrDefault("amount", 1).toString());
        int cmd = map.containsKey("custom-model-data")
                ? Integer.parseInt(map.get("custom-model-data").toString())
                : 0;
        String currency = String.valueOf(map.getOrDefault("currency", "vault"));

        Material mat = Material.STONE;
        ItemStack base = null;

        if (mmoType != null && mmoId != null) {
            Type type = Type.get(mmoType);
            if (type != null) {
                MMOItem mItem = MMOItems.plugin.getMMOItem(type, mmoId);
                if (mItem != null) {
                    base = mItem.newBuilder().build();
                    base.setAmount(amount);
                }
            }
        }

        if (base == null) {
            String matName = String.valueOf(map.getOrDefault("material", "STONE")).toUpperCase();
            try {
                mat = Material.valueOf(matName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Material không hợp lệ trong " + fileName + ": " + matName);
                return null;
            }

            base = new ItemStack(mat, amount);
            ItemMeta meta = base.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(lore);
                if (cmd > 0) meta.setCustomModelData(cmd);
                base.setItemMeta(meta);
            }
        }

        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        return new ShopItem(key, base, mat, name, lore, price, amount,
                cmd > 0 ? cmd : null, currency, mmoType, mmoId);
    }

    public Set<String> getCategoryNames() {
        return categories.keySet();
    }

    public List<ShopItem> getItemsForCategory(String category) {
        return categories.getOrDefault(category, Collections.emptyList());
    }

    public ShopItem findShopItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;

        String itemName = null;
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            itemName = stack.getItemMeta().getDisplayName();
        }

        for (List<ShopItem> itemList : categories.values()) {
            for (ShopItem shopItem : itemList) {
                if (shopItem.isMmoItem()) {
                    if (stack.hasItemMeta() && MMOItems.getID(stack) != null) {
                        if (MMOItems.getID(stack).equalsIgnoreCase(shopItem.getMmoId())) {
                            return shopItem;
                        }
                    }
                } else {
                    if (shopItem.getMaterial() != stack.getType()) continue;
                    if (itemName != null && !itemName.equalsIgnoreCase(shopItem.getName())) continue;
                    return shopItem;
                }
            }
        }
        return null;
    }

    public ItemStack createDisplayItem(ShopItem shopItem) {
        ItemStack is;
        if (shopItem.isMmoItem()) {
            Type type = Type.get(shopItem.getMmoType());
            if (type != null) {
                MMOItem mItem = MMOItems.plugin.getMMOItem(type, shopItem.getMmoId());
                if (mItem != null) {
                    is = mItem.newBuilder().build();
                    is.setAmount(shopItem.getAmount());
                } else {
                    is = new ItemStack(Material.BARRIER);
                }
            } else {
                is = new ItemStack(Material.BARRIER);
            }
        } else {
            is = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(shopItem.getDisplayName());
                if (shopItem.getLore() != null) meta.setLore(new ArrayList<>(shopItem.getLore()));
                if (shopItem.getCustomModelData() != null) {
                    meta.setCustomModelData(shopItem.getCustomModelData());
                }
                is.setItemMeta(meta);
            }
        }

        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            double price = shopItem.getPrice();
            String formattedPrice = new DecimalFormat("#,###").format(price);
            String currencyKey = shopItem.getCurrency().toLowerCase();
            String displayCurrency = CURRENCY_DISPLAY.getOrDefault(currencyKey, currencyKey);
            lore.add("§7Giá: §e" + formattedPrice + " " + displayCurrency);

            meta.setLore(lore);
            is.setItemMeta(meta);
        }
        return is;
    }

    public ItemStack createDeliveryItem(ShopItem shopItem) {
        if (shopItem.isMmoItem()) {
            Type type = Type.get(shopItem.getMmoType());
            if (type != null) {
                MMOItem mItem = MMOItems.plugin.getMMOItem(type, shopItem.getMmoId());
                if (mItem != null) {
                    ItemStack built = mItem.newBuilder().build();
                    built.setAmount(shopItem.getAmount());
                    return built;
                }
            }
        }

        ItemStack base = shopItem.getBaseItem();
        if (base == null) {
            base = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
        }
        ItemStack is = base.clone();
        is.setAmount(shopItem.getAmount());
        return is;
    }

    static {
        CURRENCY_DISPLAY.put("vault", "xu");
        CURRENCY_DISPLAY.put("playerpoints", "$");
        CURRENCY_DISPLAY.put("tokenmanager", "BTC");
        CURRENCY_DISPLAY.put("vneconomy", "VND");
    }
}
