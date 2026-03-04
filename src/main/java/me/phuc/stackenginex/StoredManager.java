package me.phuc.stackenginex;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class StoredManager {

    private static final NamespacedKey KEY =
            new NamespacedKey(StackEngine.get(), "stored");

    public static long getStored(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        return item.getItemMeta()
                .getPersistentDataContainer()
                .getOrDefault(KEY, PersistentDataType.LONG, 0L);
    }

    public static void addStored(ItemStack item, long amount) {

        long current = getStored(item);
        long max = StackEngine.get().getConfig()
                .getLong("stored.max-per-item");

        long total = Math.min(current + amount, max);

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer()
                .set(KEY, PersistentDataType.LONG, total);

        var luck = Registry.ENCHANTMENT.get(
                NamespacedKey.minecraft("luck"));

        if (luck != null) {
            meta.addEnchant(luck, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
    }
}
