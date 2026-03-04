package me.phuc.stackenginex;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class StackEngineX extends JavaPlugin {

    private static StackEngineX instance;
    private NamespacedKey storedKey;

    @Override
    public void onEnable() {
        instance = this;

        storedKey = new NamespacedKey(this, "stored");

        Bukkit.getPluginManager().registerEvents(
                new StackListener(this),
                this
        );

        getLogger().info("StackEngineX enabled.");
    }

    public static StackEngineX getInstance() {
        return instance;
    }

    public NamespacedKey getStoredKey() {
        return storedKey;
    }
}
