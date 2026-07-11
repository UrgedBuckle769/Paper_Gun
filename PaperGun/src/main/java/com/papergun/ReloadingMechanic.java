package com.papergun;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ReloadingMechanic {

    public static void startReload(Player player, ItemStack weapon) {
        if (!WeaponData.isWeapon(weapon)) return;
        
        WeaponType weaponType = WeaponData.getWeaponType(weapon);
        int magazineSize = WeaponData.getMagazineSize(weapon);
        
        // Set reloading state
        WeaponData.setReloading(weapon, true);
        
        // Send reload message
        player.sendMessage("§e正在换弹... §7(" + weaponType.getChineseName() + ")");
        
        // Reload time based on weapon type (in ticks)
        int reloadTime = getReloadTime(weaponType);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                ItemStack currentWeapon = player.getInventory().getItemInMainHand();
                if (!currentWeapon.isSimilar(weapon) || !WeaponData.isWeapon(currentWeapon)) {
                    // Player switched weapon, cancel reload
                    WeaponData.setReloading(weapon, false);
                    player.sendMessage("§c换弹已取消!");
                    cancel();
                    return;
                }
                
                // Complete reload
                WeaponData.setCurrentAmmo(weapon, magazineSize);
                WeaponData.setReloading(weapon, false);
                
                // Update lore
                updateWeaponLore(weapon, weaponType, magazineSize);
                
                player.sendMessage("§a换弹完成! §e" + magazineSize + "/" + magazineSize);
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
            }
        }.runTaskLater(PaperGunPlugin.getInstance(), reloadTime);
    }
    
    private static int getReloadTime(WeaponType weaponType) {
        return switch (weaponType) {
            case PISTOL -> 40;      // 2 seconds
            case REVOLVER -> 60;    // 3 seconds
            case RIFLE -> 50;       // 2.5 seconds
            case ASSAULT_RIFLE -> 70; // 3.5 seconds
            case SNIPER_RIFLE -> 80;  // 4 seconds
            case SHOTGUN -> 90;     // 4.5 seconds (shell by shell)
        };
    }
    
    private static void updateWeaponLore(ItemStack weapon, WeaponType type, int magazineSize) {
        var meta = weapon.getItemMeta();
        var lore = new java.util.ArrayList<String>();
        lore.add("§7类型: " + type.getChineseName());
        lore.add("§7弹匣容量: §e" + magazineSize);
        lore.add("§7当前弹药: §e" + magazineSize + "/" + magazineSize);
        lore.add("§7冷却时间: §e" + (type.getCooldownTicks() / 20.0) + "秒");
        if (type.isAutoFire()) {
            lore.add("§a自动武器");
        }
        meta.setLore(lore);
        weapon.setItemMeta(meta);
    }
}
