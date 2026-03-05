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

        getServer().getPluginManager().registerEvents(
                new ItemPickupListener(), this);

        getLogger().info("StackEngine enabled!");
    }
}
