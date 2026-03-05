package me.phuc.stackengine;

import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class StackEngine extends JavaPlugin implements Listener {

    private NamespacedKey storedKey;

    @Override
    public void onEnable() {
        storedKey = new NamespacedKey(this, "stored");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    /*
     * BLOCK BREAK - chỉ để vanilla rơi tự nhiên
     */
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        // Không làm gì → để vanilla drop
    }

    /*
     * PICKUP LOGIC CHÍNH
     */
    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {

        if (!(e.getEntity() instanceof Player player)) return;

        Item itemEntity = e.getItem();
        ItemStack groundItem = itemEntity.getItemStack();

        if (groundItem.getType() == Material.AIR) return;

        int amount = groundItem.getAmount();
        int stored = getStored(groundItem);

        int totalGround = amount + stored;

        // Tìm stack tương tự trong inventory
        for (ItemStack invItem : player.getInventory().getContents()) {

            if (invItem == null) continue;
            if (invItem.getType() != groundItem.getType()) continue;

            if (!isSimilarBase(invItem, groundItem)) continue;

            int invStored = getStored(invItem);
            int invTotal = invItem.getAmount() + invStored;

            int combined = invTotal + totalGround;

            // Nếu <= 64
            if (combined <= 64) {

                e.setCancelled(true);

                invItem.setAmount(combined);
                setStored(invItem, 0);

                itemEntity.remove();
                return;
            }

            // Nếu > 64 → lưu dư vào lore
            if (combined > 64) {

                e.setCancelled(true);

                invItem.setAmount(64);
                setStored(invItem, combined - 64);

                itemEntity.remove();
                return;
            }
        }

        // Nếu không có stack sẵn
        if (totalGround > 64) {

            e.setCancelled(true);

            ItemStack newItem = groundItem.clone();
            newItem.setAmount(64);
            setStored(newItem, totalGround - 64);

            player.getInventory().addItem(newItem);
            itemEntity.remove();
        }
    }

    /*
     * Lấy stored từ PDC
     */
    private int getStored(ItemStack item) {
        if (!item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer()
                .getOrDefault(storedKey, PersistentDataType.INTEGER, 0);
    }

    /*
     * Set stored + update lore
     */
    private void setStored(ItemStack item, int value) {

        ItemMeta meta = item.getItemMeta();

        if (value <= 0) {
            meta.getPersistentDataContainer().remove(storedKey);
            meta.setLore(null);
        } else {
            meta.getPersistentDataContainer()
                    .set(storedKey, PersistentDataType.INTEGER, value);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Stored: " + value);
            lore.add(ChatColor.GRAY + "Total: " + (item.getAmount() + value));
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
    }

    /*
     * So sánh base item (không tính lore)
     */
    private boolean isSimilarBase(ItemStack a, ItemStack b) {

        if (a.getType() != b.getType()) return false;

        ItemMeta ma = a.getItemMeta();
        ItemMeta mb = b.getItemMeta();

        if (ma == null && mb == null) return true;
        if (ma == null || mb == null) return false;

        return Objects.equals(ma.getDisplayName(), mb.getDisplayName());
    }
}
