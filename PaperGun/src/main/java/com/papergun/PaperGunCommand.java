package com.papergun;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PaperGunCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行!");
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("setweapon")) {
            sender.sendMessage("§e用法: /papergun setweapon <类型> <弹匣子弹量>");
            sender.sendMessage("§7可用类型: 手枪, 左轮, 步枪, 突击步枪, 狙击步枪, 霰弹枪");
            return true;
        }

        String typeStr = args[1];
        int magazineSize;
        
        try {
            magazineSize = Integer.parseInt(args[2]);
            if (magazineSize <= 0) {
                player.sendMessage("§c弹匣容量必须大于0!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的弹匣容量! 请输入数字.");
            return true;
        }

        WeaponType weaponType = WeaponType.fromString(typeStr);
        if (weaponType == null) {
            player.sendMessage("§c无效的武器类型!");
            player.sendMessage("§7可用类型: 手枪, 左轮, 步枪, 突击步枪, 狙击步枪, 霰弹枪");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == org.bukkit.Material.AIR) {
            player.sendMessage("§c你必须手持一个物品!");
            return true;
        }

        // Set the weapon
        WeaponData.setWeapon(item, weaponType, magazineSize);
        
        player.sendMessage("§a成功设置武器! §6" + weaponType.getChineseName());
        player.sendMessage("§7弹匣容量: §e" + magazineSize);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        return true;
    }
}
