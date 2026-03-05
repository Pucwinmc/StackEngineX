package me.phuc.stackenginex;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
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

    private List<String> loreFormat;

    private boolean effectsEnabled;

    private String storedSound;
    private float storedVolume;
    private float storedPitch;
    private String storedParticle;
    private int storedParticleCount;

    private String refillSound;
    private float refillVolume;
    private float refillPitch;

    private boolean refillTitleEnabled;
    private String titleMain;
    private String titleSub;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;

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
        actionbarEnabled = getConfig().getBoolean("actionbar.enabled", true);

        loreEnabled = getConfig().getBoolean("stored.lore.enabled", true);
        loreFormat = getConfig().getStringList("stored.lore.format");

        effectsEnabled = getConfig().getBoolean("effects.enabled", true);

        storedSound = getConfig().getString("effects.stored.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        storedVolume = (float) getConfig().getDouble("effects.stored.volume", 1.0);
        storedPitch = (float) getConfig().getDouble("effects.stored.pitch", 1.2);
        storedParticle = getConfig().getString("effects.stored.particle", "VILLAGER_HAPPY");
        storedParticleCount = getConfig().getInt("effects.stored.particle-count", 20);

        refillSound = getConfig().getString("effects.refill.sound", "BLOCK_AMETHYST_BLOCK_CHIME");
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

    private int getMax(Player p) {
        for (Map.Entry<String, Integer> e : permissionLimits.entrySet()) {
            if (p.hasPermission(e.getKey())) {
                if (e.getValue() == -1) return -1;
                return Math.max(1, e.getValue());
            }
        }
        return defaultMax;
    }

    // ================= APPLY STORED =================
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
            String percentString;

            if (max == -1) {
                percentString = "∞";
            } else {
                double percent = (double) total / max * 100;
                percentString = String.format("%.0f", percent);
            }

            List<String> lore = new ArrayList<>();

            for (String line : loreFormat) {
                line = line.replace("{stored}", String.valueOf(amount));
                line = line.replace("{total}", String.valueOf(total));
                line = line.replace("{max}", maxString);
                line = line.replace("{percent}", percentString);
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }

            meta.setLore(lore);
        }

        item.setItemMeta(meta);
    }

    private Integer getStored(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(storedKey, PersistentDataType.INTEGER);
    }

    private void clearStored(ItemStack item) {
        if (!item.hasItemMeta()) return;
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
                    int max = getMax(p);

                    String format = getConfig().getString("actionbar.format",
                            "&eStored: {stored} &7| &fTotal: {total}/{max}");

                    format = format.replace("{stored}", String.valueOf(stored));
                    format = format.replace("{total}", String.valueOf(total));
                    format = format.replace("{max}", max == -1 ? "∞" : String.valueOf(max));

                    p.sendActionBar(ChatColor.translateAlternateColorCodes('&', format));
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }
}
