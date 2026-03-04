package me.phuc.stackenginex;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class StackEngineX extends JavaPlugin {

    private int maxStack;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPlugin();
        getLogger().info("StackEngineX enabled!");
    }

    private void loadPlugin() {
        reloadConfig();
        maxStack = getConfig().getInt("max-stack-size", 128);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("stackenginex")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadPlugin();
            sender.sendMessage("§aStackEngineX config reloaded!");
            return true;
        }

        sender.sendMessage("§eStackEngineX v1.0");
        return true;
    }
}
