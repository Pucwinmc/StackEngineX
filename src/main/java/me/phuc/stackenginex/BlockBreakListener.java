package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;

public class BlockBreakListener implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent event) {

        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool == null || tool.getType().isAir()) return;

        Collection<ItemStack> drops =
                event.getBlock().getDrops(tool);

        if (drops.isEmpty()) return;

        event.setDropItems(false);

        for (ItemStack drop : drops) {

            // Nếu tool có stored system
            StoredManager.addStored(tool, drop.getAmount());

            HashMap<Integer, ItemStack> leftover =
                    player.getInventory().addItem(drop);

            if (!leftover.isEmpty()) {
                for (ItemStack remain : leftover.values()) {
                    player.getWorld().dropItemNaturally(
                            event.getBlock().getLocation(),
                            remain
                    );
                }
            }
        }
    }
}
