package me.phuc.stackenginex;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class StoredManager {

    private static final NamespacedKey KEY =
            new NamespacedKey(StackEngineX.get(), "stored");

    public static long getStored(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        PersistentDataContainer pdc =
                item.getItemMeta().getPersistentDataContainer();

        return pdc.getOrDefault(KEY, PersistentDataType.LONG, 0L);
    }

    public static void setStored(ItemStack item, long value) {

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (value <= 0) {
            pdc.remove(KEY);
        } else {
            if (value > SettingsManager.MAX_PER_ITEM)
                value = SettingsManager.MAX_PER_ITEM;

            pdc.set(KEY, PersistentDataType.LONG, value);
        }

        item.setItemMeta(meta);
        updateVisual(item);
    }

    public static long getTotal(ItemStack item) {
        return item.getAmount() + getStored(item);
    }

    private static void updateVisual(ItemStack item) {

        long stored = getStored(item);
        ItemMeta meta = item.getItemMeta();

        if (stored > 0) {

            // ===== GLOW =====
            if (SettingsManager.GLOWING) {
                meta.addEnchant(Enchantment.LUCK, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // ===== LORE =====
            if (StackEngineX.get().getConfig().getBoolean("stored.lore.enabled")) {

                List<String> format =
                        StackEngineX.get().getConfig().getStringList("stored.lore.format");

                List<String> lore = new ArrayList<>();

                for (String line : format) {
                    lore.add(line
                            .replace("{stored}", String.valueOf(stored))
                            .replace("{total}", String.valueOf(getTotal(item)))
                            .replace("&", "§"));
                }

                meta.setLore(lore);
            }

        } else {
            // Xoá glow + lore nếu hết stored
            meta.removeEnchant(Enchantment.LUCK);
            meta.setLore(null);
        }

        item.setItemMeta(meta);
    }
}
