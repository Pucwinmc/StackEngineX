package me.phuc.stackenginex;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class StackListener implements Listener {

    private final StackEngineX plugin;

    public StackListener(StackEngineX plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack picked = event.getItem().getItemStack();
        int maxStack = plugin.getConfig().getInt("max-stack-size", 128);

        // Chỉ xử lý item mặc định stack 64
        if (picked.getType().getMaxStackSize() != 64) return;

        for (ItemStack content : player.getInventory().getContents()) {
            if (content == null) continue;

            if (!content.isSimilar(picked)) continue;

            if (content.getAmount() >= maxStack) continue;

            int space = maxStack - content.getAmount();
            int transfer = Math.min(space, picked.getAmount());

            content.setAmount(content.getAmount() + transfer);
            picked.setAmount(picked.getAmount() - transfer);

            if (picked.getAmount() <= 0) {
                event.getItem().remove();
                event.setCancelled(true);
                return;
            }
        }
    }
}
