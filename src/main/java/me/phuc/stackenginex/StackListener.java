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

        ItemStack item = event.getItem().getItemStack();

        // Chỉ chỉnh những item mặc định stack 64
        if (item.getType().getMaxStackSize() != 64) return;

        int maxStack = plugin.getConfig().getInt("max-stack-size", 128);

        item.setMaxStackSize(maxStack);
    }
}
