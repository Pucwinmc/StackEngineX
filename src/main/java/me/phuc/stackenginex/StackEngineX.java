package me.phuc.stackenginex;

import org.bukkit.plugin.java.JavaPlugin;

public final class StackEngineX extends JavaPlugin {

    private static StackEngineX instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        SettingsManager.load(this);

        getLogger().info("StackEngineX enabled!");
    }

    public static StackEngineX get() {
        return instance;
    }
}
