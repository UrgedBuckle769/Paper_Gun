package com.papergun;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;

public class PaperGunPlugin extends JavaPlugin {

    private static PaperGunPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize WeaponData with plugin
        WeaponData.init(this);
        
        // Register command
        PaperGunCommand commandExecutor = new PaperGunCommand();
        getCommand("papergun").setExecutor(commandExecutor);
        getCommand("papergun").setTabCompleter(commandExecutor);
        
        // Register listeners with plugin instance
        getServer().getPluginManager().registerEvents(new WeaponListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftEngineListener(this), this);
        
        getLogger().info("PaperGun 已启用!");
        getLogger().info("使用 /papergun setweapon <类型> <弹匣容量> 来设置武器");
        getLogger().info("使用 /papergun setceweapon <craftengine:id:xxx> <类型> <弹匣容量> 来设置 CraftEngine 自定义武器");
        getLogger().info("使用 /papergun listguns 列出所有枪配置");
        getLogger().info("使用 /papergun delgun <id> 删除枪配置");
        getLogger().info("支持类型：手枪，左轮，步枪，突击步枪，狙击步枪，霰弹枪");
    }

    @Override
    public void onDisable() {
        getLogger().info("PaperGun 已禁用!");
    }

    public static PaperGunPlugin getInstance() {
        return instance;
    }
}
