package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class StackEngine extends JavaPlugin implements Listener {

    private NamespacedKey storedKey;

    @Override
    public void onEnable() {
        storedKey = new NamespacedKey(this, "stored");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("StackEngineX ENABLED!");
    }

    // ===============================
    // STACK khi nhặt item
    // ===============================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        if (e.getCursor() == null) return;

        ItemStack current = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        if (current.getType() != cursor.getType()) return;

        int total = getTotal(current) + getTotal(cursor);

        if (total <= 64) return;

        e.setCancelled(true);

        current.setAmount(64);
        setStored(current, total - 64);
        updateLore(current);

        e.setCurrentItem(current);
        e.setCursor(null);
    }

    // ===============================
    // ĐẶT BLOCK → trừ stored
    // ===============================

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        ItemStack item = e.getItemInHand();
        if (!hasStored(item)) return;

        int stored = getStored(item);

        if (stored <= 0) return;

        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {

                ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
                if (hand == null) return;
                if (hand.getType() == Material.AIR) return;

                if (hand.getAmount() < 64) {
                    hand.setAmount(hand.getAmount() + 1);
                    setStored(hand, stored - 1);
                    updateLore(hand);
                }
            }
        }, 1L);
    }

    // ===============================
    // CHO VÀO RƯƠNG → unstack
    // ===============================

    @EventHandler
    public void onChestClick(InventoryClickEvent e) {

        if (e.getClickedInventory() == null) return;
        if (e.getCurrentItem() == null) return;

        if (!(e.getClickedInventory().getHolder() instanceof org.bukkit.inventory.InventoryHolder)) return;

        ItemStack item = e.getCurrentItem();
        if (!hasStored(item)) return;

        int stored = getStored(item);
        if (stored <= 0) return;

        e.setCancelled(true);

        Inventory inv = e.getClickedInventory();

        item.setAmount(64);
        setStored(item, 0);
        updateLore(item);

        for (int i = 0; i < stored; i++) {
            inv.addItem(new ItemStack(item.getType(), 1));
        }
    }

    // ===============================
    // DATA METHODS
    // ===============================

    private boolean hasStored(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(storedKey, PersistentDataType.INTEGER);
    }

    private int getStored(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(storedKey, PersistentDataType.INTEGER, 0);
    }

    private int getTotal(ItemStack item) {
        return item.getAmount() + getStored(item);
    }

    private void setStored(ItemStack item, int amount) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(storedKey, PersistentDataType.INTEGER, amount);

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
    }

    private void updateLore(ItemStack item) {

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int stored = getStored(item);

        if (stored <= 0) {
            meta.setLore(null);
            item.setItemMeta(meta);
            return;
        }

        meta.setLore(java.util.Arrays.asList(
                ChatColor.YELLOW + "Stored: " + stored
        ));

        item.setItemMeta(meta);
    }
}
