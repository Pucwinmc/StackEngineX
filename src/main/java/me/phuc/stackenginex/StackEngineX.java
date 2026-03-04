package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class StackEngineX extends JavaPlugin {

    private int maxStack;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPlugin();
        getLogger().info("StackEngineX enabled!");
    }

    private void loadPlugin() {
        reloadConfig();
        maxStack = getConfig().getInt("max-stack-size");

        Bukkit.getWorlds().forEach(world ->
                world.setMaxStackSize(maxStack)
        );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("stackenginex")) {
            return false;
        }

        if (!sender.hasPermission("stackenginex.reload")) {
            sender.sendMessage(color(getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadPlugin();
            sender.sendMessage(color(getConfig().getString("messages.reload-success")));
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "/stackenginex reload");
        return true;
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
