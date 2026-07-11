package com.papergun;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class ReloadingMechanic {

    public static void startReload(Player player, ItemStack weapon, PaperGunPlugin plugin) {
        if (!WeaponData.isWeapon(weapon)) return;

        WeaponType weaponType = WeaponData.getWeaponType(weapon);
        int magazineSize = WeaponData.getMagazineSize(weapon);

        WeaponData.setReloading(weapon, true);
        player.sendMessage("§e正在换弹... Reloading §7(" + weaponType.getChineseName() + ")");

        // 修复废弃 API 警告：将 SLOW 改为 SLOWNESS
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));

        int reloadTime = getReloadTime(weaponType);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    cancel();
                    return;
                }

                ItemStack currentMainHand = player.getInventory().getItemInMainHand();
                ItemStack currentOffHand = player.getInventory().getItemInOffHand();
                boolean foundWeapon = false;
                ItemStack foundItem = null;

                if (isSameWeapon(currentMainHand, weapon)) {
                    foundWeapon = true;
                    foundItem = currentMainHand;
                } else if (isSameWeapon(currentOffHand, weapon)) {
                    foundWeapon = true;
                    foundItem = currentOffHand;
                }

                if (!foundWeapon || (foundItem != null && !WeaponData.isReloading(foundItem))) {
                    WeaponData.setReloading(weapon, false);
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    player.sendMessage("§c换弹已取消！Reload cancelled!");
                    cancel();
                    return;
                }

                WeaponData.setCurrentAmmo(foundItem, magazineSize);
                WeaponData.setReloading(foundItem, false);

                player.removePotionEffect(PotionEffectType.SLOWNESS);
                updateWeaponLore(foundItem, weaponType, magazineSize);
                player.sendMessage("§a换弹完成！Reload complete! §e" + magazineSize + "/" + magazineSize);
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
                player.spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }.runTaskLater(plugin, reloadTime);
    }

    private static boolean isSameWeapon(ItemStack item, ItemStack original) {
        if (item == null || !WeaponData.isWeapon(item)) return false;
        String originalType = WeaponData.getWeaponType(original).name();
        String itemType = WeaponData.getWeaponType(item).name();
        return originalType.equals(itemType);
    }

    private static int getReloadTime(WeaponType weaponType) {
        return switch (weaponType) {
            case PISTOL -> 40;
            case REVOLVER -> 60;
            case RIFLE -> 50;
            case ASSAULT_RIFLE -> 70;
            case SNIPER_RIFLE -> 80;
            case SHOTGUN -> 90;
            // 修复编译错误：添加 default 分支兜底，防止未覆盖的枚举值导致报错
            default -> 60; 
        };
    }

    private static void updateWeaponLore(ItemStack weapon, WeaponType type, int magazineSize) {
        var meta = weapon.getItemMeta();
        var lore = new java.util.ArrayList<String>();
        lore.add("§7类型：" + type.getChineseName());
        lore.add("§7弹匣容量：§e" + magazineSize);
        lore.add("§7当前弹药：§e" + magazineSize + "/" + magazineSize);
        lore.add("§7冷却时间：§e" + (type.getCooldownTicks() / 20.0) + "秒");
        if (type.isAutoFire()) {
            lore.add("§a自动武器");
        }
        meta.setLore(lore);
        weapon.setItemMeta(meta);
    }
}
