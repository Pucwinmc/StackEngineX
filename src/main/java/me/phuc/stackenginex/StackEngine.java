package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class StackEngine extends JavaPlugin implements Listener {

    private NamespacedKey storedKey;

    @Override
    public void onEnable() {
        storedKey = new NamespacedKey(this, "stored");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("StackEngine Survival Ready");
    }

    // ===============================
    // STACK SAU KHI NHẶT ITEM
    // ===============================
    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {

        if (!(e.getEntity() instanceof Player player)) return;

        // Delay 1 tick để Minecraft cho item vào inventory trước
        Bukkit.getScheduler().runTaskLater(this, () -> {

            Material type = e.getItem().getItemStack().getType();
            Inventory inv = player.getInventory();

            int total = 0;

            // Cộng tổng tất cả stack cùng loại
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

            // Xoá tất cả stack cùng loại
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item == null) continue;
                if (item.getType() == type) {
                    inv.setItem(i, null);
                }
            }

            // Tạo stack mới
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
    // REFILL KHI ĐẶT BLOCK
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

        Bukkit.getScheduler().runTaskLater(this, () -> {

            ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
            if (hand == null) return;

            ItemMeta m = hand.getItemMeta();
            if (m == null) return;

            int newStored = stored - 1;

            if (newStored <= 0) {
                m.getPersistentDataContainer().remove(storedKey);
                m.setLore(null);
                m.removeEnchant(Enchantment.UNBREAKING);
            } else {
                m.getPersistentDataContainer()
                        .set(storedKey, PersistentDataType.INTEGER, newStored);
                m.setLore(List.of("§eStored: §f" + newStored));
            }

            hand.setItemMeta(m);

        }, 1L);
    }

    // ===============================
    // BỎ VÀO RƯƠNG → UNSTACK
    // ===============================
    @EventHandler
    public void onChestClick(InventoryClickEvent e) {

        if (e.getClickedInventory() == null) return;
        if (!(e.getClickedInventory().getHolder() instanceof InventoryHolder)) return;
        if (e.getCurrentItem() == null) return;

        ItemStack item = e.getCurrentItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Integer stored = meta.getPersistentDataContainer()
                .get(storedKey, PersistentDataType.INTEGER);

        if (stored == null || stored <= 0) return;

        e.setCancelled(true);

        Inventory chest = e.getClickedInventory();

        // reset item
        meta.getPersistentDataContainer().remove(storedKey);
        meta.setLore(null);
        meta.removeEnchant(Enchantment.UNBREAKING);
        item.setItemMeta(meta);

        // thêm item dư vào rương
        for (int i = 0; i < stored; i++) {
            chest.addItem(new ItemStack(item.getType(), 1));
        }
    }
}
