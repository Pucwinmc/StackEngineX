package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class StackEngine extends JavaPlugin implements Listener {

    private NamespacedKey storedKey;

    @Override
    public void onEnable() {
        storedKey = new NamespacedKey(this, "stored");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {

        if (!(e.getEntity() instanceof Player player)) return;

        Item itemEntity = e.getItem();
        ItemStack ground = itemEntity.getItemStack();

        if (ground.getType() == Material.AIR) return;

        int groundAmount = ground.getAmount();
        int groundStored = getStored(ground);
        int groundTotal = groundAmount + groundStored;

        for (ItemStack invItem : player.getInventory().getContents()) {

            if (invItem == null) continue;
            if (invItem.getType() != ground.getType()) continue;

            int invStored = getStored(invItem);
            int invTotal = invItem.getAmount() + invStored;

            int combined = invTotal + groundTotal;

            e.setCancelled(true);

            if (combined <= 64) {
                invItem.setAmount(combined);
                setStored(invItem, 0);
            } else {
                invItem.setAmount(64);
                setStored(invItem, combined - 64);
            }

            itemEntity.remove();
            return;
        }

        if (groundTotal > 64) {
            e.setCancelled(true);

            ItemStack newItem = ground.clone();
            newItem.setAmount(64);
            setStored(newItem, groundTotal - 64);

            player.getInventory().addItem(newItem);
            itemEntity.remove();
        }
    }

    private int getStored(ItemStack item) {
        if (!item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer()
                .getOrDefault(storedKey, PersistentDataType.INTEGER, 0);
    }

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
}
