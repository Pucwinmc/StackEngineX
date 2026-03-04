package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class BlockBreakListener implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent e) {

        if (!SettingsManager.STACK_ENABLED) return;
        if (!SettingsManager.STORED_ENABLED) return;

        ItemStack tool = e.getPlayer().getInventory().getItemInMainHand();

        // ===== SILK TOUCH CHECK =====
        if (SettingsManager.SILK_BLOCK) {
            if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
                return;
            }
        }

        Material type = e.getBlock().getType();

        // ===== ONLY 64 STACKABLE =====
        if (SettingsManager.ONLY_64) {
            if (type.getMaxStackSize() != 64) return;
        }

        ItemStack drop = new ItemStack(type);

        int emptySlots = countEmptySlots(e.getPlayer());

        // ===== CHỈ STORED KHI GẦN ĐẦY =====
        if (emptySlots > SettingsManager.START_WHEN_EMPTY_BELOW) {
            return;
        }

        // ===== RATE LIMIT =====
        if (!RateLimitManager.allow(e.getPlayer().getUniqueId())) {
            return;
        }

        e.setDropItems(false);

        ItemStack hand = findSimilar(e.getPlayer().getInventory().getContents(), drop);

        if (hand != null) {

            long stored = StoredManager.getStored(hand);

            if (stored >= SettingsManager.MAX_PER_ITEM) {
                return;
            }

            StoredManager.setStored(hand, stored + 1);

        } else {
            e.getPlayer().getInventory().addItem(drop);
        }
    }

    private int countEmptySlots(org.bukkit.entity.Player player) {
        int empty = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null) empty++;
        }
        return empty;
    }

    private ItemStack findSimilar(ItemStack[] items, ItemStack target) {
        for (ItemStack item : items) {
            if (item == null) continue;
            if (item.isSimilar(target)) return item;
        }
        return null;
    }
}
