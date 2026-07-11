package com.papergun;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PaperGunCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§e用法:");
            sender.sendMessage("§7/papergun setweapon <类型> <弹匣子弹量> - 设置武器");
            sender.sendMessage("§7/papergun setceweapon <craftengine:id:xxx:xxx> <类型> <弹匣容量> - 设置 CraftEngine 自定义物品为武器");
            sender.sendMessage("§7/papergun listguns - 列出所有已注册的枪");
            sender.sendMessage("§7/papergun delgun <craftengine:id:xxx:xxx> - 删除已注册的枪配置");
            sender.sendMessage("§7可用类型：手枪，左轮，步枪，突击步枪，狙击步枪，霰弹枪");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("setweapon")) {
            return handleSetWeapon(player, args);
        } else if (subCommand.equals("setceweapon")) {
            return handleSetCEWeapon(player, args);
        } else if (subCommand.equals("listguns")) {
            return handleListGuns(player);
        } else if (subCommand.equals("delgun")) {
            return handleDelGun(player, args);
        } else {
            sender.sendMessage("§c未知的子命令！使用 /papergun 查看帮助");
            return true;
        }
    }

    private boolean handleSetWeapon(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§e用法：/papergun setweapon <类型> <弹匣子弹量>");
            player.sendMessage("§7可用类型：手枪，左轮，步枪，突击步枪，狙击步枪，霰弹枪");
            return true;
        }

        String typeStr = args[1];
        int magazineSize;
        
        try {
            magazineSize = Integer.parseInt(args[2]);
            if (magazineSize <= 0) {
                player.sendMessage("§c弹匣容量必须大于 0!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的弹匣容量！请输入数字.");
            return true;
        }

        WeaponType weaponType = WeaponType.fromString(typeStr);
        if (weaponType == null) {
            player.sendMessage("§c无效的武器类型!");
            player.sendMessage("§7可用类型：手枪，左轮，步枪，突击步枪，狙击步枪，霰弹枪");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == org.bukkit.Material.AIR) {
            player.sendMessage("§c你必须手持一个物品!");
            return true;
        }

        // Set the weapon
        WeaponData.setWeapon(item, weaponType, magazineSize);
        
        player.sendMessage("§a成功设置武器！§6" + weaponType.getChineseName());
        player.sendMessage("§7弹匣容量：§e" + magazineSize);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        return true;
    }

    private boolean handleSetCEWeapon(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§e用法：/papergun setceweapon <craftengine:id:xxx:xxx> <类型> <弹匣容量>");
            player.sendMessage("§7示例：/papergun setceweapon craftengine:id:mygun:pistol 手枪 15");
            player.sendMessage("§7可用类型：手枪，左轮，步枪，突击步枪，狙击步枪，霰弹枪");
            return true;
        }

        String craftEngineId = args[1];
        String typeStr = args[2];
        int magazineSize;
        
        try {
            magazineSize = Integer.parseInt(args[3]);
            if (magazineSize <= 0) {
                player.sendMessage("§c弹匣容量必须大于 0!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的弹匣容量！请输入数字.");
            return true;
        }

        WeaponType weaponType = WeaponType.fromString(typeStr);
        if (weaponType == null) {
            player.sendMessage("§c无效的武器类型!");
            player.sendMessage("§7可用类型：手枪，左轮，步枪，突击步枪，狙击步枪，霰弹枪");
            return true;
        }

        // Register the gun config for CraftEngine items
        WeaponData.registerGunConfig(craftEngineId, weaponType, magazineSize);
        
        player.sendMessage("§a成功注册 CraftEngine 武器配置!");
        player.sendMessage("§7CraftEngine ID: §e" + craftEngineId);
        player.sendMessage("§7武器类型：§6" + weaponType.getChineseName());
        player.sendMessage("§7弹匣容量：§e" + magazineSize);
        player.sendMessage("§e当玩家拿起对应 ID 的 CraftEngine 物品时，将自动应用此配置。");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        return true;
    }

    private boolean handleListGuns(Player player) {
        ConcurrentHashMap<String, WeaponData.GunConfig> configs = WeaponData.getAllGunConfigs();
        
        if (configs.isEmpty()) {
            player.sendMessage("§c当前没有注册任何枪配置。");
            return true;
        }

        player.sendMessage("§a=== 已注册的枪配置 ===");
        for (WeaponData.GunConfig config : configs.values()) {
            player.sendMessage("§7ID: §e" + config.craftEngineId);
            player.sendMessage("  §7类型：§6" + config.weaponType.getChineseName());
            player.sendMessage("  §7弹匣容量：§e" + config.magazineSize);
        }
        player.sendMessage("§a共 §e" + configs.size() + " §a个配置");

        return true;
    }

    private boolean handleDelGun(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§e用法：/papergun delgun <craftengine:id:xxx:xxx>");
            return true;
        }

        String craftEngineId = args[1];
        WeaponData.GunConfig removed = WeaponData.getAllGunConfigs().remove(craftEngineId);
        
        if (removed != null) {
            player.sendMessage("§a成功删除枪配置！");
            player.sendMessage("§7已删除：§e" + craftEngineId);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        } else {
            player.sendMessage("§c未找到该 ID 的枪配置！");
            player.sendMessage("§7使用 /papergun listguns 查看所有配置");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("setweapon");
            completions.add("setceweapon");
            completions.add("listguns");
            completions.add("delgun");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setweapon") || args[0].equalsIgnoreCase("setceweapon")) {
                completions.add("手枪");
                completions.add("左轮");
                completions.add("步枪");
                completions.add("突击步枪");
                completions.add("狙击步枪");
                completions.add("霰弹枪");
            } else if (args[0].equalsIgnoreCase("delgun")) {
                for (WeaponData.GunConfig config : WeaponData.getAllGunConfigs().values()) {
                    completions.add(config.craftEngineId);
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setceweapon")) {
                completions.add("手枪");
                completions.add("左轮");
                completions.add("步枪");
                completions.add("突击步枪");
                completions.add("狙击步枪");
                completions.add("霰弹枪");
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("setceweapon")) {
                completions.add("10");
                completions.add("15");
                completions.add("20");
                completions.add("30");
            }
        }
        
        return completions;
    }
}
