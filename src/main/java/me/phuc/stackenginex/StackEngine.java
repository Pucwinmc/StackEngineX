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
    private boolean storedTitleEnabled;

    private String storedTitleMain, storedTitleSub;
    private int storedFadeIn, storedStay, storedFadeOut;

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

        // Stored title
        storedTitleEnabled = getConfig().getBoolean("effects.stored.title.enabled", true);
        storedTitleMain = getConfig().getString("effects.stored.title.main", "&6Bắt đầu nén!");
        storedTitleSub = getConfig().getString("effects.stored.title.sub", "&eItem đã được lưu trữ");
        storedFadeIn = getConfig().getInt("effects.stored.title.fade-in", 10);
        storedStay = getConfig().getInt("effects.stored.title.stay", 30);
        storedFadeOut = getConfig().getInt("effects.stored.title.fade-out", 10);

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

            if (max == -1 || total <= 64) return;
            if (total > max) total = max;

            int storedAmount = total - 64;

            inv.remove(type);

            ItemStack result = new ItemStack(type, 64);

            if (storedAmount > 0) {
                applyStored(result, storedAmount, player);

                if (storedTitleEnabled) {
                    player.sendTitle(
                            ChatColor.translateAlternateColorCodes('&', storedTitleMain),
                            ChatColor.translateAlternateColorCodes('&', storedTitleSub),
                            storedFadeIn, storedStay, storedFadeOut
                    );
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

    private void startActionbar() {

        if (!actionbarEnabled) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {

                    ItemStack item = p.getInventory().getItemInMainHand();
                    Integer stored = getStored(item);

                    if (stored == null || stored <= 0) {
                        p.sendActionBar("");
                        continue;
                    }

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
        }.runTaskTimer(this, 2L, 2L); // chạy mỗi 2 tick cho mượt
    }
}
