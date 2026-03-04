package me.phuc.stackenginex;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RateLimitManager {

    private static final Map<UUID, Long> lastReset = new HashMap<>();
    private static final Map<UUID, Integer> counter = new HashMap<>();

    public static boolean allow(UUID uuid) {

        if (!SettingsManager.RATE_ENABLED) return true;

        long now = System.currentTimeMillis();

        if (!lastReset.containsKey(uuid)
                || now - lastReset.get(uuid) > SettingsManager.RATE_INTERVAL) {

            lastReset.put(uuid, now);
            counter.put(uuid, 0);
        }

        int count = counter.getOrDefault(uuid, 0);

        if (count >= SettingsManager.RATE_MAX) {
            return false;
        }

        counter.put(uuid, count + 1);
        return true;
    }
}
