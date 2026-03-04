package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class StackEngine extends JavaPlugin {

    private static StackEngine instance;

    public static StackEngine get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(
                new BlockBreakListener(), this);

        getLogger().info("StackEngineX Enabled.");
    }
}
