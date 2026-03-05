package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class StackEngine extends JavaPlugin implements Listener {

    private NamespacedKey storedKey;
    private final int MAX = 64;

    @Override
    public void onEnable() {
        storedKey = new NamespacedKey(this, "stored");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("StackEngineX Enabled!");
    }

    // ================= STORED SYSTEM =================

    private boolean hasStored(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(storedKey, PersistentDataType.INTEGER);
    }

    private int getStored(ItemStack item) {
        if (!hasStored(item)) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .get(storedKey, PersistentDataType.INTEGER);
    }

    private void setStored(ItemStack item, int amount) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(storedKey, PersistentDataType.INTEGER, amount);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Stored: " + amount);
        lore.add(ChatColor.GRAY + "Total: " + (item.getAmount() + amount));
        meta.setLore(lore);

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
    }

    private void clearStored(ItemStack item) {
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(storedKey);
        meta.setLore(null);
        meta.removeEnchant(Enchantment.UNBREAKING);
        item.setItemMeta(meta);
    }

    // ================= PICKUP FIX (PAPER SAFE) =================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickup(EntityPickupItemEvent e) {

        if (!(e.getEntity() instanceof Player player)) return;
        if (e.isCancelled()) return;

        ItemStack picked = e.getItem().getItemStack();

        Bukkit.getScheduler().runTaskLater(this, () -> {

            for (ItemStack inv : player.getInventory().getContents()) {

                if (inv == null) continue;
                if (inv.getType() != picked.getType()) continue;

                int current = inv.getAmount();
                int stored = getStored(inv);

                if (current > MAX) {
                    int overflow = current - MAX;
                    inv.setAmount(MAX);
                    setStored(inv, stored + overflow);
                }

            }

        }, 1L);
    }

    // ================= AUTO REFILL =================

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        Player p = e.getPlayer();
        ItemStack item = e.getItemInHand();

        if (!hasStored(item)) return;

        int stored = getStored(item);
        if (stored <= 0) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {

            ItemStack hand = p.getInventory().getItemInMainHand();

            if (hand.getType() == item.getType()
                    && hand.getAmount() == 0
                    && stored > 0) {

                int refill = Math.min(MAX, stored);

                ItemStack newStack = new ItemStack(item.getType());
                newStack.setAmount(refill);

                int newStored = stored - refill;

                if (newStored > 0) {
                    setStored(newStack, newStored);
                }

                p.getInventory().setItemInMainHand(newStack);
            }

        }, 1L);
    }

    // ================= UNSTACK WHEN PUT INTO CONTAINER =================

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!(e.getWhoClicked() instanceof Player)) return;

        Inventory clicked = e.getClickedInventory();
        if (clicked == null) return;

        ItemStack item = e.getCurrentItem();
        if (item == null) return;

        if (!hasStored(item)) return;

        if (!(clicked.getHolder() instanceof Player)) {

            clearStored(item);
            item.setAmount(Math.min(MAX, item.getAmount()));
            e.setCurrentItem(item);
        }
    }
}
