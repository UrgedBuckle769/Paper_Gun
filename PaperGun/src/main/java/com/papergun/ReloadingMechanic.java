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

        // Set reloading state
        WeaponData.setReloading(weapon, true);

        // Send reload message (Chinese-English mixed)
        player.sendMessage("§e正在换弹... Reloading §7(" + weaponType.getChineseName() + ")");

        // Apply Slowness I effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 0, false, false));

        // Reload time based on weapon type (in ticks)
        int reloadTime = getReloadTime(weaponType);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    // Remove slowness if player offline
                    player.removePotionEffect(PotionEffectType.SLOW);
                    cancel();
                    return;
                }

                // Check main hand AND offhand for the weapon
                ItemStack currentMainHand = player.getInventory().getItemInMainHand();
                ItemStack currentOffHand = player.getInventory().getItemInOffHand();
                
                boolean foundWeapon = false;
                boolean isInOffHand = false;
                
                if (isSameWeapon(currentMainHand, weapon)) {
                    foundWeapon = true;
                } else if (isSameWeapon(currentOffHand, weapon)) {
                    foundWeapon = true;
                    isInOffHand = true;
                }
                
                if (!foundWeapon || !WeaponData.isReloading(weapon)) {
                    // Player switched weapon, cancel reload
                    WeaponData.setReloading(weapon, false);
                    player.removePotionEffect(PotionEffectType.SLOW);
                    player.sendMessage("§c换弹已取消! Reload cancelled!");
                    cancel();
                    return;
                }

                // Complete reload
                WeaponData.setCurrentAmmo(weapon, magazineSize);
                WeaponData.setReloading(weapon, false);

                // Remove slowness effect
                player.removePotionEffect(PotionEffectType.SLOW);

                // Update lore
                updateWeaponLore(weapon, weaponType, magazineSize);

                player.sendMessage("§a换弹完成！Reload complete! §e" + magazineSize + "/" + magazineSize);
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);

                // Spawn reload complete particles
                player.spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0.1);
                
                // If weapon was in offhand, move it back to main hand after reload
                if (isInOffHand) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                ItemStack offHand = player.getInventory().getItemInOffHand();
                                if (isSameWeapon(offHand, weapon)) {
                                    player.getInventory().setItemInMainHand(offHand.clone());
                                    player.getInventory().setItemInOffHand(new ItemStack(org.bukkit.Material.AIR));
                                }
                            }
                        }
                    }.runTaskLater(plugin, 5L);
                }
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
