package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class StackEngine extends JavaPlugin implements Listener {

    private NamespacedKey storedKey;

    @Override
    public void onEnable() {
        storedKey = new NamespacedKey(this, "stored");

        Bukkit.getPluginManager().registerEvents(this, this);

        // Tick auto stack mỗi 20 tick (1s)
        new BukkitRunnable() {
            @Override
            public void run() {
                autoStackTick();
            }
        }.runTaskTimer(this, 20L, 20L);

        getLogger().info("StackEngineX FULL ENABLED");
    }

    // ==============================
    // AUTO STACK & REFILL
    // ==============================
    private void autoStackTick() {

        Bukkit.getOnlinePlayers().forEach(player -> {

            Inventory inv = player.getInventory();

            for (int i = 0; i < inv.getSize(); i++) {

                ItemStack item = inv.getItem(i);
                if (!isValid(item)) continue;

                ItemMeta meta = item.getItemMeta();
                int stored = getStored(meta);
                int amount = item.getAmount();

                // STACK >64 vào stored
                if (amount > 64) {
                    stored += (amount - 64);
                    item.setAmount(64);
                }

                // REFILL nếu <64
                if (amount < 64 && stored > 0) {
                    int need = 64 - amount;
                    int take = Math.min(need, stored);

                    item.setAmount(amount + take);
                    stored -= take;
                }

                applyStored(item, meta, stored);
            }
        });
    }

    // ==============================
    // KHI ĐẶT BLOCK → TRỪ STORED
    // ==============================
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        ItemStack item = e.getItemInHand();
        if (!isValid(item)) return;

        ItemMeta meta = item.getItemMeta();
        int stored = getStored(meta);

        if (stored <= 0) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {

            ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
            if (!isValid(hand)) return;

            ItemMeta newMeta = hand.getItemMeta();
            int newStored = getStored(newMeta);

            if (hand.getAmount() < 64 && newStored > 0) {
                hand.setAmount(hand.getAmount() + 1);
                newStored--;

                applyStored(hand, newMeta, newStored);
            }

        }, 1L);
    }

    // ==============================
    // BỎ VÀO RƯƠNG → UNSTACK
    // ==============================
    @EventHandler
    public void onChestClick(InventoryClickEvent e) {

        if (e.getClickedInventory() == null) return;
        if (!(e.getClickedInventory().getHolder() instanceof InventoryHolder)) return;
        if (e.getCurrentItem() == null) return;

        ItemStack item = e.getCurrentItem();
        if (!isValid(item)) return;

        ItemMeta meta = item.getItemMeta();
        int stored = getStored(meta);
        if (stored <= 0) return;

        e.setCancelled(true);

        Inventory chest = e.getClickedInventory();

        // Set lại item còn 64
        item.setAmount(64);
        applyStored(item, meta, 0);

        // Thêm từng item lẻ vào rương
        for (int i = 0; i < stored; i++) {
            chest.addItem(new ItemStack(item.getType(), 1));
        }
    }

    // ==============================
    // HELPER METHODS
    // ==============================
    private boolean isValid(ItemStack item) {
        return item != null &&
                item.getType() != Material.AIR &&
                item.getMaxStackSize() > 1 &&
                item.hasItemMeta();
    }

    private int getStored(ItemMeta meta) {
        Integer stored = meta.getPersistentDataContainer()
                .get(storedKey, PersistentDataType.INTEGER);
        return stored == null ? 0 : stored;
    }

    private void applyStored(ItemStack item, ItemMeta meta, int stored) {

        if (stored <= 0) {
            meta.getPersistentDataContainer().remove(storedKey);
            meta.setLore(null);
            meta.removeEnchant(Enchantment.UNBREAKING);
            item.setItemMeta(meta);
            return;
        }

        meta.getPersistentDataContainer()
                .set(storedKey, PersistentDataType.INTEGER, stored);

        meta.setLore(java.util.List.of("§eStored: §f" + stored));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
    }
}
