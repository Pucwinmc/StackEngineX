package me.phuc.stackenginex;

import org.bukkit.plugin.java.JavaPlugin;

public class StackEngineX extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("StackEngineX enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("StackEngineX disabled!");
    }
}
