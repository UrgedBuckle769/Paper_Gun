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
            sender.sendMessage("§e用法 Usage:");
            sender.sendMessage("§7/papergun setweapon <类型 Type> <弹匣子弹量 MagazineSize> - 设置武器 Set weapon");
            sender.sendMessage("§7/papergun setceweapon <craftengine:id:xxx:xxx> <类型 Type> <弹匣容量 MagazineSize> - 设置 CraftEngine 自定义物品为武器 Set CraftEngine item as weapon");
            sender.sendMessage("§7/papergun listguns - 列出所有已注册的枪 List all registered guns");
            sender.sendMessage("§7/papergun delgun <craftengine:id:xxx:xxx> - 删除已注册的枪配置 Delete gun config");
            sender.sendMessage("§7/papergun addrpg <弹匣容量 MagazineSize> - 添加 RPG Add RPG launcher");
            sender.sendMessage("§7可用类型 Available types: 手枪 Pistol, 左轮 Revolver, 步枪 Rifle, 突击步枪 Assault Rifle, 狙击步枪 Sniper Rifle, 霰弹枪 Shotgun, RPG 火箭筒");
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
        } else if (subCommand.equals("addrpg")) {
            return handleAddRPG(player, args);
        } else {
            sender.sendMessage("§cUnknown subcommand! Use /papergun for help");
            return true;
        }
    }

    private boolean handleSetWeapon(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§e用法 Usage: /papergun setweapon <类型 Type> <弹匣子弹量 MagazineSize>");
            player.sendMessage("§7可用类型 Available: 手枪 Pistol, 左轮 Revolver, 步枪 Rifle, 突击步枪 Assault Rifle, 狙击步枪 Sniper Rifle, 霰弹枪 Shotgun");
            return true;
        }

        String typeStr = args[1];
        int magazineSize;
        
        try {
            magazineSize = Integer.parseInt(args[2]);
            if (magazineSize <= 0) {
                player.sendMessage("§c弹匣容量必须大于 0! Magazine size must be > 0!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的弹匣容量！请输入数字 Invalid magazine size! Please enter a number.");
            return true;
        }

        WeaponType weaponType = WeaponType.fromString(typeStr);
        if (weaponType == null) {
            player.sendMessage("§c无效的武器类型 Invalid weapon type!");
            player.sendMessage("§7可用类型 Available: 手枪 Pistol, 左轮 Revolver, 步枪 Rifle, 突击步枪 Assault Rifle, 狙击步枪 Sniper Rifle, 霰弹枪 Shotgun");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == org.bukkit.Material.AIR) {
            player.sendMessage("§c你必须手持一个物品 You must hold an item!");
            return true;
        }

        // Set the weapon
        WeaponData.setWeapon(item, weaponType, magazineSize);
        
        player.sendMessage("§a成功设置武器 Success! §6" + weaponType.getChineseName() + " (" + weaponType.name() + ")");
        player.sendMessage("§7弹匣容量 Magazine Size: §e" + magazineSize);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        return true;
    }

    private boolean handleSetCEWeapon(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§e用法 Usage: /papergun setceweapon <craftengine:id:xxx:xxx> <类型 Type> <弹匣容量 MagazineSize>");
            player.sendMessage("§7示例 Example: /papergun setceweapon craftengine:id:mygun:pistol 手枪 Pistol 15");
            player.sendMessage("§7可用类型 Available: 手枪 Pistol, 左轮 Revolver, 步枪 Rifle, 突击步枪 Assault Rifle, 狙击步枪 Sniper Rifle, 霰弹枪 Shotgun, RPG");
            return true;
        }

        String craftEngineId = args[1];
        String typeStr = args[2];
        int magazineSize;
        
        try {
            magazineSize = Integer.parseInt(args[3]);
            if (magazineSize <= 0) {
                player.sendMessage("§c弹匣容量必须大于 0! Magazine size must be > 0!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的弹匣容量！请输入数字 Invalid magazine size! Please enter a number.");
            return true;
        }

        WeaponType weaponType = WeaponType.fromString(typeStr);
        if (weaponType == null) {
            player.sendMessage("§c无效的武器类型 Invalid weapon type!");
            player.sendMessage("§7可用类型 Available: 手枪 Pistol, 左轮 Revolver, 步枪 Rifle, 突击步枪 Assault Rifle, 狙击步枪 Sniper Rifle, 霰弹枪 Shotgun, RPG");
            return true;
        }

        // Register the gun config for CraftEngine items
        WeaponData.registerGunConfig(craftEngineId, weaponType, magazineSize);
        
        player.sendMessage("§a成功注册 CraftEngine 武器配置 Success!");
        player.sendMessage("§7CraftEngine ID: §e" + craftEngineId);
        player.sendMessage("§7武器类型 Weapon Type: §6" + weaponType.getChineseName() + " (" + weaponType.name() + ")");
        player.sendMessage("§7弹匣容量 Magazine Size: §e" + magazineSize);
        player.sendMessage("§e当玩家拿起对应 ID 的 CraftEngine 物品时，将自动应用此配置 When players pick up the CraftEngine item with this ID, the config will be applied automatically.");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        return true;
    }

    private boolean handleListGuns(Player player) {
        ConcurrentHashMap<String, WeaponData.GunConfig> configs = WeaponData.getAllGunConfigs();
        
        if (configs.isEmpty()) {
            player.sendMessage("§c当前没有注册任何枪配置 No gun configs registered.");
            return true;
        }

        player.sendMessage("§a=== 已注册的枪配置 Registered Guns ===");
        for (WeaponData.GunConfig config : configs.values()) {
            player.sendMessage("§7ID: §e" + config.craftEngineId);
            player.sendMessage("  §7类型 Type: §6" + config.weaponType.getChineseName() + " (" + config.weaponType.name() + ")");
            player.sendMessage("  §7弹匣容量 Magazine Size: §e" + config.magazineSize);
        }
        player.sendMessage("§a共 Total: §e" + configs.size() + " §a个配置 configs");

        return true;
    }

    private boolean handleDelGun(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§e用法 Usage: /papergun delgun <craftengine:id:xxx:xxx>");
            return true;
        }

        String craftEngineId = args[1];
        WeaponData.GunConfig removed = WeaponData.getAllGunConfigs().remove(craftEngineId);
        
        if (removed != null) {
            player.sendMessage("§a成功删除枪配置 Success!");
            player.sendMessage("§7已删除 Deleted: §e" + craftEngineId);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        } else {
            player.sendMessage("§c未找到该 ID 的枪配置 Config not found!");
            player.sendMessage("§7使用 /papergun listguns 查看所有配置 Use /papergun listguns to view all configs");
        }

        return true;
    }

    private boolean handleAddRPG(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§e用法 Usage: /papergun addrpg <弹匣容量 MagazineSize>");
            player.sendMessage("§7示例 Example: /papergun addrpg 1");
            return true;
        }

        int magazineSize;
        
        try {
            magazineSize = Integer.parseInt(args[1]);
            if (magazineSize <= 0) {
                player.sendMessage("§c弹匣容量必须大于 0! Magazine size must be > 0!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的弹匣容量！请输入数字 Invalid magazine size! Please enter a number.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == org.bukkit.Material.AIR) {
            player.sendMessage("§c你必须手持一个物品 You must hold an item!");
            return true;
        }

        // Set the RPG weapon
        WeaponData.setWeapon(item, WeaponType.RPG, magazineSize);
        
        player.sendMessage("§a成功设置 RPG! Success! §6火箭筒 (RPG)");
        player.sendMessage("§7弹匣容量 Magazine Size: §e" + magazineSize);
        player.sendMessage("§eRPG 发射虚拟弹射物，碰炸造成范围伤害，不会破坏方块");
        player.sendMessage("§eRPG fires virtual projectile, explodes on impact with AoE damage, no block damage");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

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
            completions.add("addrpg");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setweapon") || args[0].equalsIgnoreCase("setceweapon")) {
                completions.add("手枪");
                completions.add("左轮");
                completions.add("步枪");
                completions.add("突击步枪");
                completions.add("狙击步枪");
                completions.add("霰弹枪");
                completions.add("RPG");
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
                completions.add("RPG");
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
