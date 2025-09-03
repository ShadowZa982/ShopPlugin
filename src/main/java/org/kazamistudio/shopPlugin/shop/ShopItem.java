package org.kazamistudio.shopPlugin.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopItem {
    private final String key;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final double price;
    private final int amount;
    private final Integer customModelData;
    private final ItemStack baseItem;
    private final String currency;
    private final String mmoType;
    private final String mmoId;

    public ShopItem(String key, ItemStack baseItem, Material material, String name,
                    List<String> lore, double price, int amount,
                    Integer customModelData, String currency,
                    String mmoType, String mmoId) {
        this.key = key;
        this.baseItem = baseItem;
        this.material = material;
        this.displayName = name;
        this.lore = lore;
        this.price = price;
        this.amount = amount;
        this.customModelData = customModelData;
        this.currency = currency != null ? currency.toLowerCase() : "vault";
        this.mmoType = mmoType;
        this.mmoId = mmoId;
    }

    public String getMmoType() { return mmoType; }
    public String getMmoId() { return mmoId; }
    public boolean isMmoItem() { return mmoType != null && mmoId != null; }
    public String getKey() { return key; }
    public ItemStack getBaseItem() { return baseItem; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public String getName() { return displayName; }
    public List<String> getLore() { return lore; }
    public double getPrice() { return price; }
    public int getAmount() { return amount; }
    public Integer getCustomModelData() { return customModelData; }
    public String getCurrency() { return currency; }
}
