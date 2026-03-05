package me.phuc.stackenginex;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

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

        updateLore(meta, item.getAmount(), total);

        // Glow nếu có stored
        if (total > 0) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
    }

    private static void updateLore(ItemMeta meta, int amount, long stored) {

        List<String> lore = new ArrayList<>();

        lore.add("§8§m----------------");
        lore.add("§eStored: §6" + stored);
        lore.add("§7Total: §f" + (amount + stored));
        lore.add("§8§m----------------");

        meta.setLore(lore);
    }
}
