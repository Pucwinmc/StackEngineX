package me.phuc.stackenginex;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
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
        getCommand("stack").setExecutor((sender, cmd, label, args) -> {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                loadConfigValues();
                sender.sendMessage(ChatColor.GREEN + "StackEngine reloaded!");
                return true;
            }
            return false;
        });

        startActionbarTask();

        getLogger().info("StackEngine FULL Loaded!");
    }

    private void loadConfigValues() {

        defaultMax = getConfig().getInt("stack.default-max");
        only64 = getConfig().getBoolean("stack.only-64-stackable");
        glow = getConfig().getBoolean("stored.glowing-when-stored");
        loreEnabled = getConfig().getBoolean("stored.lore.enabled");
        actionbarEnabled = getConfig().getBoolean("actionbar.enabled");

        permissionLimits.clear();
        ConfigurationSection section =
                getConfig().getConfigurationSection("stack.permission-limits");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                permissionLimits.put(key, section.getInt(key));
            }
        }
    }

    private int getMax(Player player) {

        for (Map.Entry<String, Integer> e : permissionLimits.entrySet()) {
            if (player.hasPermission(e.getKey())) {
                if (e.getValue() == -1) return Integer.MAX_VALUE;
                return e.getValue();
            }
        }
        return defaultMax;
    }

    // ===============================
    // PICKUP STACK
    // ===============================
    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {

        if (!(e.getEntity() instanceof Player player)) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {

            ItemStack picked = e.getItem().getItemStack();
            Material type = picked.getType();

            if (only64 && type.getMaxStackSize() != 64) return;

            int total = 0;
            Inventory inv = player.getInventory();

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
            int stored = total - base;

            ItemStack newItem = new ItemStack(type, base);
            applyStored(newItem, stored);

            inv.addItem(newItem);

        }, 1L);
    }

    // ===============================
    // REFILL ON PLACE
    // ===============================
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        ItemStack item = e.getItemInHand();
        Integer stored = getStored(item);
        if (stored == null || stored <= 0) return;

        Player player = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(this, () -> {

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR && stored > 0) {

                ItemStack restore = new ItemStack(item.getType(), 64);
                applyStored(restore, stored - 1);
                player.getInventory().setItemInMainHand(restore);
            } else {
                applyStored(hand, stored - 1);
            }

        }, 1L);
    }

    // ===============================
    // CONTAINER AUTO UNSTACK
    // ===============================
    @EventHandler
    public void onClick(InventoryClickEvent e) {

        Inventory top = e.getView().getTopInventory();
        if (top == null) return;

        Bukkit.getScheduler().runTaskLater(this,
                () -> unstackContainer(top), 1L);
    }

    private void unstackContainer(Inventory inv) {

        for (int i = 0; i < inv.getSize(); i++) {

            ItemStack item = inv.getItem(i);
            if (item == null) continue;

            Integer stored = getStored(item);
            if (stored == null || stored <= 0) continue;

            Material type = item.getType();
            clearStored(item);

            while (stored > 0) {
                int give = Math.min(64, stored);
                inv.addItem(new ItemStack(type, give));
                stored -= give;
            }
        }
    }

    // ===============================
    // STORED HANDLING
    // ===============================
    private Integer getStored(ItemStack item) {

        if (item == null || !item.hasItemMeta()) return null;

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

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(storedKey);
        meta.setLore(null);
        meta.removeEnchant(Enchantment.UNBREAKING);
        item.setItemMeta(meta);
    }

    // ===============================
    // ACTIONBAR
    // ===============================
    private void startActionbarTask() {

        if (!actionbarEnabled) return;

        new BukkitRunnable() {
            @Override
            public void run() {

                for (Player player : Bukkit.getOnlinePlayers()) {

                    ItemStack item = player.getInventory().getItemInMainHand();
                    Integer stored = getStored(item);

                    if (stored == null || stored <= 0) continue;

                    int total = item.getAmount() + stored;

                    String format = getConfig().getString("actionbar.format");
                    format = format.replace("{stored}", String.valueOf(stored));
                    format = format.replace("{total}", String.valueOf(total));

                    player.sendActionBar(ChatColor.translateAlternateColorCodes('&', format));
                }
            }
        }.runTaskTimer(this, 20L, getConfig().getInt("actionbar.update-interval-ticks"));
    }
}
