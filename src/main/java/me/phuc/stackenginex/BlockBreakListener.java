package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BlockBreakListener implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent e) {

        if (!StackEngine.get().getConfig().getBoolean("stored.enabled"))
            return;

        var player = e.getPlayer();
        PlayerInventory inv = player.getInventory();

        int empty = 0;
        for (ItemStack item : inv.getStorageContents()) {
            if (item == null || item.getType() == Material.AIR)
                empty++;
        }

        int threshold = StackEngine.get().getConfig()
                .getInt("stored.start-when-empty-slots-below");

        if (empty > threshold)
            return;

        e.setDropItems(false);

        ItemStack drop = new ItemStack(e.getBlock().getType(), 1);

        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (inHand == null || inHand.getType() == Material.AIR)
            return;

        StoredManager.addStored(inHand, 1);
    }
}
