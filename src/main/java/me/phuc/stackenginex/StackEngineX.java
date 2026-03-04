package me.phuc.stackenginex;

import org.bukkit.plugin.java.JavaPlugin;

public final class StackEngineX extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Đăng ký sự kiện auto stack khi nhặt item
        getServer().getPluginManager().registerEvents(new StackListener(this), this);

        getLogger().info("StackEngineX đã bật!");
    }

    @Override
    public void onDisable() {
        getLogger().info("StackEngineX đã tắt!");
    }
}
