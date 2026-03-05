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

    private final Map<String, Integer> permissionLimits = new HashMap<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();
        loadConfigValues();

        storedKey = new NamespacedKey(this, "stored");

        Bukkit.getPluginManager().registerEvents(this, this);

        startActionbar();
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

    // ================= FIXED PICKUP =================
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
                if (item == null) continue;
                if (item.getType() != type) continue;

                total += item.getAmount();

                Integer stored = getStored(item);
                if (stored != null) total += stored;
            }

            int max = getMax(player);
            if (total <= max) return;

            // XÓA TỪNG SLOT thay vì remove(type)
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item == null) continue;
                if (item.getType() == type) {
                    inv.clear(i);
                }
            }

            int base = 64;
            int storedAmount = total - base;

            if (storedAmount < 0) storedAmount = 0;

            ItemStack newItem = new ItemStack(type, base);

            if (storedAmount > 0) {
                applyStored(newItem, storedAmount);
            }

            inv.addItem(newItem);

        }, 1L);
    }

    // ================= REFILL =================
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        Player p = e.getPlayer();
        ItemStack item = e.getItemInHand();

        Integer stored = getStored(item);
        if (stored == null || stored <= 0) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) return;

            int current = hand.getAmount();
            int missing = 64 - current;

            if (missing <= 0) return;

            int refill = Math.min(missing, stored);
            int newStored = stored - refill;

            hand.setAmount(current + refill);

            if (newStored <= 0) {
                clearStored(hand);
            } else {
                applyStored(hand, newStored);
            }

        }, 1L);
    }

    // ================= CONTAINER =================
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
                inv.addItem(new ItemStack(type, give));
                stored -= give;
            }
        }
    }

    // ================= STORED =================
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
        meta.getPersistentDataContainer().set(storedKey, PersistentDataType.INTEGER, amount);

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

    // ================= ACTIONBAR =================
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
        }.runTaskTimer(this, 20L, 20L);
    }
}
