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

    // Lấy số lượng stored
    public static long getStored(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        return item.getItemMeta()
                .getPersistentDataContainer()
                .getOrDefault(KEY, PersistentDataType.LONG, 0L);
    }

    // Thêm stored + tự glow
    public static void addStored(ItemStack item, long amount) {

        if (item == null || item.getType().isAir()) return;

        long current = getStored(item);
        long max = StackEngine.get().getConfig()
                .getLong("stored.max-per-item");

        long total = Math.min(current + amount, max);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Lưu dữ liệu
        meta.getPersistentDataContainer()
                .set(KEY, PersistentDataType.LONG, total);

        // Thêm glow nếu chưa có
        if (!meta.hasEnchant(Enchantment.UNBREAKING)) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
    }

    // Xóa glow nếu stored = 0
    public static void removeGlow(ItemStack item) {

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.removeEnchant(Enchantment.UNBREAKING);
        meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
    }
}
