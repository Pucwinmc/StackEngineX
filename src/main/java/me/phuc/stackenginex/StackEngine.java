package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
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
        getLogger().info("StackEngine FULL FIXED");
    }

    // ===============================
    // STACK SAU KHI NHẶT
    // ===============================
    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {

        if (!(e.getEntity() instanceof Player player)) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {

            Material type = e.getItem().getItemStack().getType();
            Inventory inv = player.getInventory();

            int total = 0;

            for (ItemStack item : inv.getContents()) {
                if (item == null) continue;
                if (item.getType() != type) continue;

                total += item.getAmount();

                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    Integer stored = meta.getPersistentDataContainer()
                            .get(storedKey, PersistentDataType.INTEGER);
                    if (stored != null) total += stored;
                }
            }

            if (total <= 64) return;

            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item == null) continue;
                if (item.getType() == type) {
                    inv.setItem(i, null);
                }
            }

            ItemStack newStack = new ItemStack(type, 64);
            int storedAmount = total - 64;

            ItemMeta meta = newStack.getItemMeta();
            meta.getPersistentDataContainer()
                    .set(storedKey, PersistentDataType.INTEGER, storedAmount);

            meta.setLore(List.of("§eStored: §f" + storedAmount));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            newStack.setItemMeta(meta);
            inv.addItem(newStack);

        }, 1L);
    }

    // ===============================
    // FIX REFILL KHÔNG MẤT ITEM
    // ===============================
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        ItemStack item = e.getItemInHand();
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Integer stored = meta.getPersistentDataContainer()
                .get(storedKey, PersistentDataType.INTEGER);

        if (stored == null || stored <= 0) return;

        // REFILL NGAY TRONG EVENT
        int newStored = stored - 1;

        meta.getPersistentDataContainer()
                .set(storedKey, PersistentDataType.INTEGER, newStored);

        meta.setLore(List.of("§eStored: §f" + newStored));

        item.setItemMeta(meta);

        // Đảm bảo không mất item khi amount về 0
        Bukkit.getScheduler().runTaskLater(this, () -> {
            ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {

                ItemStack restore = new ItemStack(item.getType(), 64);
                ItemMeta m = restore.getItemMeta();

                if (newStored > 0) {
                    m.getPersistentDataContainer()
                            .set(storedKey, PersistentDataType.INTEGER, newStored);
                    m.setLore(List.of("§eStored: §f" + newStored));
                    m.addEnchant(Enchantment.UNBREAKING, 1, true);
                    m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                restore.setItemMeta(m);
                e.getPlayer().getInventory().setItemInMainHand(restore);
            }

        }, 1L);
    }

    // ===============================
    // UNSTACK ĐÚNG TRONG RƯƠNG
    // ===============================
    @EventHandler
    public void onChestClick(InventoryClickEvent e) {

        if (!(e.getWhoClicked() instanceof Player)) return;

        Inventory top = e.getView().getTopInventory();
        if (top == null) return;

        // Chỉ khi click vào rương (top inventory)
        if (e.getClickedInventory() != top) return;

        ItemStack item = e.getCurrentItem();
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Integer stored = meta.getPersistentDataContainer()
                .get(storedKey, PersistentDataType.INTEGER);

        if (stored == null || stored <= 0) return;

        e.setCancelled(true);

        // Reset stack trong rương
        meta.getPersistentDataContainer().remove(storedKey);
        meta.setLore(null);
        meta.removeEnchant(Enchantment.UNBREAKING);
        item.setItemMeta(meta);

        // Thêm item dư vào chính RƯƠNG
        for (int i = 0; i < stored; i++) {
            top.addItem(new ItemStack(item.getType(), 1));
        }
    }
}
