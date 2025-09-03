package org.kazamistudio.shopPlugin.delivery;

import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.kazamistudio.shopPlugin.ShopPlugin;
import org.kazamistudio.shopPlugin.utils.ColorUtil;

public class DeliveryManager {
    private final ShopPlugin plugin;

    public DeliveryManager(ShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void scheduleDelivery(Player buyer, ItemStack prototype, int amount) {
        int minMinutes = plugin.getConfig().getInt("delivery-delay-min-minutes", 2);
        int maxMinutes = plugin.getConfig().getInt("delivery-delay-max-minutes", 60);

        int delayMinutes = minMinutes + (int)(Math.random() * (maxMinutes - minMinutes + 1));
        int delayTicks = delayMinutes * 60 * 20;

        buyer.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(
                        ColorUtil.translate(
                                plugin.getConfig().getString("messages.delivery-soon-title", "&aĐơn hàng sẽ đến sau {time}")
                                        .replace("{time}", delayMinutes + " phút")
                        )
                )
        );

        ItemStack toDeliver = prototype.clone();
        toDeliver.setAmount(amount);

        new BukkitRunnable() {
            @Override
            public void run() {
                Location loc = buyer.getLocation().clone();
                loc.setY(Math.min(loc.getWorld().getMaxHeight() - 2, loc.getY() + 30));

                FallingBlock falling = loc.getWorld().spawnFallingBlock(loc, Material.CHEST.createBlockData());
                falling.setDropItem(false);
                falling.setHurtEntities(false);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!falling.isDead() && !falling.isOnGround()) {
                            falling.getWorld().spawnParticle(Particle.CLOUD, falling.getLocation().clone().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0.01);
                        }

                        if (falling.isOnGround()) {
                            Location chestLoc = falling.getLocation().getBlock().getLocation();
                            falling.remove();

                            buyer.sendMessage(ChatColor.GREEN + "Đơn hàng của bạn đã đến nơi tại tọa độ: " +
                                    ChatColor.YELLOW + "X: " + chestLoc.getBlockX() +
                                    " Y: " + chestLoc.getBlockY() +
                                    " Z: " + chestLoc.getBlockZ() +
                                    ChatColor.GREEN + ". Hãy kiểm tra đơn hàng nhận được.");

                            buyer.playSound(buyer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                            chestLoc.getBlock().setType(Material.CHEST);
                            if (chestLoc.getBlock().getState() instanceof Chest chest) {
                                Inventory inv = chest.getBlockInventory();
                                inv.addItem(toDeliver);

                                chest.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, chestLoc.clone().add(0.5, 1, 0.5), 50, 1, 1, 1, 0.05);
                                chest.getWorld().playSound(chestLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);

                                new BukkitRunnable() {
                                    int ticksLeft = 15 * 60 * 20;

                                    @Override
                                    public void run() {
                                        if (isInventoryEmpty(inv)) {
                                            chest.getBlock().setType(Material.AIR);
                                            cancel();
                                            return;
                                        }

                                        ticksLeft -= 20;
                                        if (ticksLeft <= 0) {
                                            for (Player pl : Bukkit.getOnlinePlayers()) {
                                                if (pl.getWorld().equals(chestLoc.getWorld()) &&
                                                        pl.getLocation().distance(chestLoc) <= 20) {
                                                    pl.sendMessage(ChatColor.RED + "Rương giao hàng đã biến mất do hết hạn!");
                                                }
                                            }
                                            spawnFirework(chestLoc);
                                            chest.getBlock().setType(Material.AIR);
                                            cancel();
                                        }
                                    }
                                }.runTaskTimer(plugin, 20L, 20L);
                                plugin.getPendingDeliveries().remove(buyer.getUniqueId());
                            }
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 1L, 1L);
            }
        }.runTaskLater(plugin, delayTicks);
    }


    private boolean isInventoryEmpty(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc.clone().add(0.5, 0.5, 0.5), Firework.class);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.addEffect(FireworkEffect.builder()
                .withColor(Color.RED, Color.ORANGE, Color.YELLOW)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build());
        fwm.setPower(1);
        fw.setFireworkMeta(fwm);
    }
}
