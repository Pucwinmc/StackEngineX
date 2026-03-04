package me.phuc.stackenginex;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsManager {

    public static boolean STACK_ENABLED;
    public static boolean ONLY_64;
    public static long DEFAULT_MAX;

    public static boolean STORED_ENABLED;
    public static long MAX_PER_ITEM;
    public static int START_WHEN_EMPTY_BELOW;
    public static boolean GLOWING;

    public static boolean RATE_ENABLED;
    public static int RATE_MAX;
    public static long RATE_INTERVAL;
    public static String RATE_BYPASS;

    public static boolean SILK_BLOCK;

    public static boolean TAX_ENABLED;
    public static LinkedHashMap<Long, Integer> TAX_TIERS = new LinkedHashMap<>();

    public static void load(StackEngineX plugin) {

        FileConfiguration c = plugin.getConfig();

        STACK_ENABLED = c.getBoolean("stack.enabled");
        ONLY_64 = c.getBoolean("stack.only-64-stackable");
        DEFAULT_MAX = c.getLong("stack.default-max");

        STORED_ENABLED = c.getBoolean("stored.enabled");
        MAX_PER_ITEM = c.getLong("stored.max-per-item");
        START_WHEN_EMPTY_BELOW = c.getInt("stored.start-when-empty-slots-below");
        GLOWING = c.getBoolean("stored.glowing-when-stored");

        RATE_ENABLED = c.getBoolean("stored-rate-limit.enabled");
        RATE_MAX = c.getInt("stored-rate-limit.max-per-interval");
        RATE_INTERVAL = c.getLong("stored-rate-limit.interval-milliseconds");
        RATE_BYPASS = c.getString("stored-rate-limit.bypass-permission");

        SILK_BLOCK = c.getBoolean("restrictions.disable-if-silk-touch");

        TAX_ENABLED = c.getBoolean("stored-tax.enabled");

        TAX_TIERS.clear();
        for (Map<?, ?> tier : c.getMapList("stored-tax.tiers")) {
            long max = Long.parseLong(tier.get("max").toString());
            int percent = Integer.parseInt(tier.get("percent").toString());
            TAX_TIERS.put(max, percent);
        }
    }
}
