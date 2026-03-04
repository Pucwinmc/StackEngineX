package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;

public class StackEngineX extends JavaPlugin implements Listener {

    private int maxStack;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("StackEngineX enabled.");
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        maxStack = config.getInt("max-stack", 128);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        ItemStack stack = item.getItemStack();
        stack.setAmount(Math.min(stack.getAmount(), maxStack));
        item.setItemStack(stack);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        ItemStack item = event.getCurrentItem();
        if (item.getAmount() > maxStack) {
            item.setAmount(maxStack);
        }
    }

    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        Item item = event.getItem();
        ItemStack stack = item.getItemStack();
        stack.setAmount(Math.min(stack.getAmount(), maxStack));
        item.setItemStack(stack);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("stackenginex.reload")) {
            sender.sendMessage("§cBạn không có quyền dùng lệnh này.");
            return true;
        }

        reloadConfig();
        loadConfigValues();
        sender.sendMessage("§aReload config thành công.");
        return true;
    }
}
