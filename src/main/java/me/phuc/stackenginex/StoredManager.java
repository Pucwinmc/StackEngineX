package me.phuc.stackenginex;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
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

        if (item == null || item.getType().isAir()) return;

        long current = getStored(item);
        long max = StackEngine.get().getConfig()
                .getLong("stored.max-per-item");

        long total = Math.min(current + amount, max);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer()
                .set(KEY, PersistentDataType.LONG, total);

        // Glow
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
    }
}
