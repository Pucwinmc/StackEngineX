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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
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

    @Override
    public void onDisable() {
        getLogger().info("StackEngine disabled!");
    }

    // TEST EVENT - để biết plugin có hoạt động không
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.getPlayer().sendMessage("§a[StackEngine] Plugin đang hoạt động!");
    }

    private boolean isStackItem(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.hasLore();
    }

    private int getStoredAmount(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        if (!meta.hasLore()) return 0;

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

        Item itemEntity = event.getItem();
        ItemStack picked = itemEntity.getItemStack();

        if (picked == null) return;
        if (picked.getType() == Material.AIR) return;

        for (ItemStack content : player.getInventory().getContents()) {

            if (content == null) continue;
            if (content.getType() != picked.getType()) continue;

            if (isStackItem(content)) {

                int stored = getStoredAmount(content);
                stored += picked.getAmount();

                setStoredAmount(content, stored);

                event.setCancelled(true);
                itemEntity.remove();
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (!isStackItem(current)) return;

        if (event.getClickedInventory() == null) return;

        Inventory inv = event.getClickedInventory();

        if (!(inv.getHolder() instanceof Player)) {

            int stored = getStoredAmount(current);

            inv.addItem(new ItemStack(current.getType(), stored));
            event.setCurrentItem(null);
        }
    }
}
