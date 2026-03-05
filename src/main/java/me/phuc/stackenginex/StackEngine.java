package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class StackEngine extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("StackEngine enabled!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.getPlayer().sendMessage("§a[StackEngine] Plugin đang hoạt động!");
    }

    private boolean isStackItem(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasLore();
    }

    private int getStoredAmount(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return 0;
        String line = meta.getLore().get(0);
        return Integer.parseInt(line.replace("§7Stored: ", ""));
    }

    private void setStoredAmount(ItemStack item, int amount) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName("§6Stack Block");
        meta.setLore(List.of("§7Stored: " + amount));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player player)) return;

        // Delay 1 tick để Minecraft xử lý xong
        Bukkit.getScheduler().runTaskLater(this, () -> {

            Material type = event.getItem().getItemStack().getType();
            int total = 0;

            // gom tất cả item cùng loại
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null) continue;
                if (item.getType() != type) continue;

                if (isStackItem(item)) {
                    total += getStoredAmount(item);
                } else {
                    total += item.getAmount();
                }
            }

            if (total <= 64) return;

            // xoá hết item cùng loại
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null) continue;
                if (item.getType() == type) {
                    player.getInventory().setItem(i, null);
                }
            }

            // tạo stack block mới
            ItemStack stackBlock = new ItemStack(type, 1);
            setStoredAmount(stackBlock, total);
            player.getInventory().addItem(stackBlock);

        }, 1L);
    }
}
