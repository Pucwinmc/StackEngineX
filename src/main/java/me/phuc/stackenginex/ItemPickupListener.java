package me.phuc.stackenginex;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemPickupListener implements Listener {

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        ItemStack pickup = event.getItem().getItemStack();

        int amount = pickup.getAmount();

        for (ItemStack invItem : player.getInventory().getStorageContents()) {

            if (invItem == null) continue;
            if (!invItem.isSimilar(pickup)) continue;

            if (invItem.getAmount() >= 64) {

                event.setCancelled(true);

                StoredManager.addStored(invItem, amount);

                event.getItem().remove();
                return;
            }
        }
    }
}
