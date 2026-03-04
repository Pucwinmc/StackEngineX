package me.phuc.stackenginex;

import org.bukkit.entity.Player;

import java.util.Map;

public class LimitManager {

    public static long getMax(Player p) {

        long max = SettingsManager.DEFAULT_MAX;

        for (Map.Entry<String, Long> entry : SettingsManager.PERMISSION_LIMITS.entrySet()) {

            if (p.hasPermission(entry.getKey())) {

                long value = entry.getValue();

                if (value == -1) return Long.MAX_VALUE;

                if (value > max) max = value;
            }
        }

        return max;
    }
}
