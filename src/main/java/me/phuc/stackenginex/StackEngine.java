package me.phuc.stackenginex;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class StackEngine extends JavaPlugin implements Listener {

    private NamespacedKey storedKey;

    private int defaultMax;
    private boolean only64;
    private boolean glow;
    private boolean loreEnabled;
    private boolean actionbarEnabled;

    private Map<String, Integer> permissionLimits = new HashMap<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();
        loadConfigValues();

        storedKey = new NamespacedKey(this, "stored");

        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("stack") != null) {
            getCommand("stack").setExecutor((sender, cmd, label, args) -> {
                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    loadConfigValues();
                    sender.sendMessage(ChatColor.GREEN + "StackEngine reloaded!");
                    return true;
                }
                return false;
            });
        }

        startActionbar();

        getLogger().info("StackEngine Loaded Safely!");
    }

    private void loadConfigValues() {

        defaultMax = Math.max(1, getConfig().getInt("stack.default-max", 128));
        only64 = getConfig().getBoolean("stack.only-64-stackable", true);
        glow = getConfig().getBoolean("stored.glowing-when-stored", true);
        loreEnabled = getConfig().getBoolean("stored.lore.enabled", true);
        actionbarEnabled = getConfig().getBoolean("actionbar.enabled", true);

        permissionLimits.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("stack.permission-limits");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                permissionLimits.put(key, sec.getInt(key));
            }
        }
    }

    private int getMax(Player p) {

        for (Map.Entry<String, Integer> e : permissionLimits.entrySet()) {
            if (p.hasPermission(e.getKey())) {
                if (e.getValue() == -1) return Integer.MAX_VALUE;
                return Math.max(1, e.getValue());
            }
        }
        return defaultMax;
    }

    // ================== PICKUP ==================
    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {

        if (!(e.getEntity() instanceof Player player)) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {

            ItemStack picked = e.getItem().getItemStack();
            if (picked == null) return;

            Material type = picked.getType();
            if (only64 && type.getMaxStackSize() != 64) return;

            Inventory inv = player.getInventory();

            int total = 0;
            for (ItemStack item : inv.getContents()) {
                if (item == null || item.getType() != type) continue;

                total += item.getAmount();
                Integer stored = getStored(item);
                if (stored != null) total += stored;
            }

            int max = getMax(player);
            if (total <= max) return;

            inv.remove(type);

            int base = Math.min(64, max);
            if (base <= 0) return;

            int stored = total - base;

            ItemStack newItem = new ItemStack(type, base);
            applyStored(newItem, stored);

            inv.addItem(newItem);

        }, 1L);
    }

    // ================== PLACE REFILL ==================
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        ItemStack item = e.getItemInHand();
        Integer stored = getStored(item);
        if (stored == null || stored <= 0) return;

        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(this, () -> {

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {

                ItemStack restore = new ItemStack(item.getType(), 1);
                applyStored(restore, stored - 1);
                p.getInventory().setItemInMainHand(restore);

            } else {
                applyStored(hand, stored - 1);
            }

        }, 1L);
    }

    // ================== CONTAINER AUTO UNSTACK ==================
    @EventHandler
    public void onClick(InventoryClickEvent e) {

        Inventory top = e.getView().getTopInventory();
        if (top == null) return;

        Bukkit.getScheduler().runTaskLater(this,
                () -> unstack(top), 1L);
    }

    private void unstack(Inventory inv) {

        for (int i = 0; i < inv.getSize(); i++) {

            ItemStack item = inv.getItem(i);
            if (item == null) continue;

            Integer stored = getStored(item);
            if (stored == null || stored <= 0) continue;

            Material type = item.getType();
            clearStored(item);

            while (stored > 0) {
                int give = Math.min(64, stored);
                if (give <= 0) break;
                inv.addItem(new ItemStack(type, give));
                stored -= give;
            }
        }
    }

    // ================== STORED ==================
    private Integer getStored(ItemStack item) {

        if (!item.hasItemMeta()) return null;

        return item.getItemMeta().getPersistentDataContainer()
                .get(storedKey, PersistentDataType.INTEGER);
    }

    private void applyStored(ItemStack item, int amount) {

        if (amount <= 0) {
            clearStored(item);
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer()
                .set(storedKey, PersistentDataType.INTEGER, amount);

        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (loreEnabled) {
            int total = item.getAmount() + amount;
            meta.setLore(List.of(
                    ChatColor.GRAY + "Stored: " + ChatColor.GOLD + amount,
                    ChatColor.GRAY + "Total: " + ChatColor.WHITE + total
            ));
        }

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

    // ================== ACTIONBAR ==================
    private void startActionbar() {

        if (!actionbarEnabled) return;

        new BukkitRunnable() {
            @Override
            public void run() {

                for (Player p : Bukkit.getOnlinePlayers()) {

                    ItemStack item = p.getInventory().getItemInMainHand();
                    Integer stored = getStored(item);

                    if (stored == null || stored <= 0) continue;

                    int total = item.getAmount() + stored;

                    String format = getConfig().getString("actionbar.format",
                            "&eStored: {stored} &7| &fTotal: {total}");

                    format = format.replace("{stored}", String.valueOf(stored));
                    format = format.replace("{total}", String.valueOf(total));

                    p.sendActionBar(ChatColor.translateAlternateColorCodes('&', format));
                }
            }
        }.runTaskTimer(this, 20L,
                Math.max(10, getConfig().getInt("actionbar.update-interval-ticks", 20)));
    }
}
