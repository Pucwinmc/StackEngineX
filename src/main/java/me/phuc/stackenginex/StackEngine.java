package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class StackEngine extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("StackEngine enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("StackEngine disabled!");
    }

    private boolean isStackItem(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.hasLore();
    }

    private ItemStack createStackItem(Material material, int amount) {
        ItemStack item = new ItemStack(material, Math.min(amount, 64));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Stack Block");
            meta.setLore(java.util.List.of("§7Stored: " + amount));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;

        Item itemEntity = event.getItem();
        ItemStack picked = itemEntity.getItemStack();

        if (picked == null) return;

        // merge vào stack có sẵn
        for (ItemStack content : player.getInventory().getContents()) {
            if (content == null) continue;
            if (content.getType() != picked.getType()) continue;
            if (!isStackItem(content)) continue;

            ItemMeta meta = content.getItemMeta();
            int stored = Integer.parseInt(meta.getLore().get(0).replace("§7Stored: ", ""));
            stored += picked.getAmount();

            meta.setLore(java.util.List.of("§7Stored: " + stored));
            content.setItemMeta(meta);

            event.setCancelled(true);
            itemEntity.remove();
            return;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (!isStackItem(current)) return;

        if (event.getClickedInventory() == null) return;

        Inventory inv = event.getClickedInventory();

        // khi đặt vào rương thì unstack
        if (!(inv.getHolder() instanceof Player)) {

            ItemMeta meta = current.getItemMeta();
            int stored = Integer.parseInt(meta.getLore().get(0).replace("§7Stored: ", ""));

            inv.addItem(new ItemStack(current.getType(), stored));
            event.setCurrentItem(null);
        }
    }
}
