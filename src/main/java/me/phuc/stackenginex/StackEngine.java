package me.phuc.stackenginex;

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
        getLogger().info("StackEngineX Enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("StackEngineX Disabled.");
    }
}
