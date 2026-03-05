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
    private boolean actionbarEnabled;
    private boolean loreEnabled;
    private boolean effectsEnabled;
    private boolean refillTitleEnabled;

    private List<String> loreFormat;
    private final Map<String, Integer> permissionLimits = new HashMap<>();

    private String storedSound, storedParticle;
    private float storedVolume, storedPitch;
    private int storedParticleCount;

    private String refillSound;
    private float refillVolume, refillPitch;

    private String titleMain, titleSub;
    private int titleFadeIn, titleStay, titleFadeOut;

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
        loreFormat = getConfig().getStringList("stored.lore.format");

        actionbarEnabled = getConfig().getBoolean("actionbar.enabled", true);
        effectsEnabled = getConfig().getBoolean("effects.enabled", true);

        storedSound = getConfig().getString("effects.stored.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        storedVolume = (float) getConfig().getDouble("effects.stored.volume", 1.0);
        storedPitch = (float) getConfig().getDouble("effects.stored.pitch", 1.2);
        storedParticle = getConfig().getString("effects.stored.particle", "VILLAGER_HAPPY");
        storedParticleCount = getConfig().getInt("effects.stored.particle-count", 20);

        refillSound = getConfig().getString("effects.refill.sound", "UI_TOAST_CHALLENGE_COMPLETE");
        refillVolume = (float) getConfig().getDouble("effects.refill.volume", 1.0);
        refillPitch = (float) getConfig().getDouble("effects.refill.pitch", 1.0);

        refillTitleEnabled = getConfig().getBoolean("effects.refill.title.enabled", true);
        titleMain = getConfig().getString("effects.refill.title.main", "&aStored đã hết!");
        titleSub = getConfig().getString("effects.refill.title.sub", "&eStack trở về bình thường");
        titleFadeIn = getConfig().getInt("effects.refill.title.fade-in", 10);
        titleStay = getConfig().getInt("effects.refill.title.stay", 40);
        titleFadeOut = getConfig().getInt("effects.refill.title.fade-out", 10);

        permissionLimits.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("stack.permission-limits");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                permissionLimits.put(key, sec.getInt(key));
            }
        }
    }

    // ✅ FIXED getMax
    private int getMax(Player p) {

        int highest = defaultMax;

        for (Map.Entry<String, Integer> e : permissionLimits.entrySet()) {
            if (p.hasPermission(e.getKey())) {
                int value = e.getValue();
                if (value == -1) return -1;
                if (value > highest) highest = value;
            }
        }

        return highest;
    }

    // ✅ FIXED PICKUP
    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {

        if (!(e.getEntity() instanceof Player player)) return;

        ItemStack picked = e.getItem().getItemStack();
        if (picked == null) return;

        Material type = picked.getType();
        if (only64 && type.getMaxStackSize() != 64) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {

            Inventory inv = player.getInventory();
            int max = getMax(player);

            int total = 0;

            for (ItemStack item : inv.getContents()) {
                if (item == null || item.getType() != type) continue;

                total += item.getAmount();

                Integer stored = getStored(item);
                if (stored != null) total += stored;
            }

            if (max == -1) return;
            if (total <= 64) return;

            if (total > max) total = max;

            int base = 64;
            int storedAmount = total - base;

            inv.remove(type);

            ItemStack result = new ItemStack(type, base);

            if (storedAmount > 0) {
                applyStored(result, storedAmount, player);

                if (effectsEnabled) {
                    try {
                        player.playSound(player.getLocation(),
                                Sound.valueOf(storedSound), storedVolume, storedPitch);
                    } catch (Exception ignored) {}

                    try {
                        player.spawnParticle(
                                Particle.valueOf(storedParticle),
                                player.getLocation().add(0, 1, 0),
                                storedParticleCount, 0.5, 0.5, 0.5
                        );
                    } catch (Exception ignored) {}
                }
            }

            inv.addItem(result);

        }, 1L);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        Player p = e.getPlayer();
        ItemStack item = e.getItemInHand();
        Integer stored = getStored(item);
        if (stored == null || stored <= 0) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null) return;

            int missing = 64 - hand.getAmount();
            if (missing <= 0) return;

            int refill = Math.min(missing, stored);
            int newStored = stored - refill;

            hand.setAmount(hand.getAmount() + refill);

            if (newStored <= 0) {

                clearStored(hand);

                if (effectsEnabled) {
                    p.playSound(p.getLocation(),
                            Sound.valueOf(refillSound), refillVolume, refillPitch);
                }

                if (refillTitleEnabled) {
                    p.sendTitle(
                            ChatColor.translateAlternateColorCodes('&', titleMain),
                            ChatColor.translateAlternateColorCodes('&', titleSub),
                            titleFadeIn, titleStay, titleFadeOut
                    );
                }

            } else {
                applyStored(hand, newStored, p);
            }

        }, 1L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {

        if (e.getView().getTopInventory() == null) return;
        Inventory top = e.getView().getTopInventory();
        if (top.getHolder() instanceof Player) return;

        Bukkit.getScheduler().runTaskLater(this, () -> autoUnstack(top), 1L);
    }

    private void autoUnstack(Inventory inv) {

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

    private Integer getStored(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(storedKey, PersistentDataType.INTEGER);
    }

    private void applyStored(ItemStack item, int amount, Player player) {

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(storedKey, PersistentDataType.INTEGER, amount);

        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (loreEnabled && loreFormat != null && !loreFormat.isEmpty()) {

            int total = item.getAmount() + amount;
            int max = getMax(player);

            String maxString = (max == -1) ? "∞" : String.valueOf(max);
            String percentString = (max == -1) ? "∞"
                    : String.format("%.0f", ((double) total / max) * 100);

            List<String> lore = new ArrayList<>();

            for (String line : loreFormat) {
                line = line.replace("{stored}", String.valueOf(amount))
                        .replace("{total}", String.valueOf(total))
                        .replace("{max}", maxString)
                        .replace("{percent}", percentString);

                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }

            meta.setLore(lore);
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
                    int max = getMax(p);

                    String format = getConfig().getString("actionbar.format",
                            "&eStored: {stored} &7| &fTotal: {total}/{max}");

                    format = format.replace("{stored}", String.valueOf(stored))
                            .replace("{total}", String.valueOf(total))
                            .replace("{max}", max == -1 ? "∞" : String.valueOf(max));

                    p.sendActionBar(ChatColor.translateAlternateColorCodes('&', format));
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }
}
