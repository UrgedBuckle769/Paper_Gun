package com.papergun;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class WeaponListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !WeaponData.isWeapon(item)) return;
        
        // Only handle main hand
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        
        // Cancel block interaction if right clicking a block
        if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
        
        WeaponType weaponType = WeaponData.getWeaponType(item);
        if (weaponType == null) return;
        
        // Check if reloading
        if (WeaponData.isReloading(item)) {
            player.sendMessage("§c正在换弹中，无法射击!");
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long lastShotTime = WeaponData.getLastShotTime(item);
        int cooldownTicks = weaponType.getCooldownTicks();
        
        // Check cooldown
        if (currentTime - lastShotTime < cooldownTicks * 50L) { // 50ms per tick
            return;
        }
        
        // Check ammo
        int currentAmmo = WeaponData.getCurrentAmmo(item);
        if (currentAmmo <= 0) {
            player.sendMessage("§c弹药耗尽! 按 F 键换弹.");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1.0f, 2.0f);
            return;
        }
        
        // Shoot
        ShootingMechanic.shoot(player, weaponType);
        
        // Decrease ammo
        WeaponData.setCurrentAmmo(item, currentAmmo - 1);
        WeaponData.setLastShotTime(item, currentTime);
        
        // Update lore to show current ammo
        updateWeaponLore(item, weaponType);
        
        // For non-auto weapons, consume the click
        if (!weaponType.isAutoFire()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = event.getMainHandItem();
        
        // If holding a weapon in main hand, prevent swap (F key)
        if (WeaponData.isWeapon(mainHand)) {
            // Check if it's a reload attempt
            if (!WeaponData.isReloading(mainHand)) {
                // Start reload instead of swapping
                event.setCancelled(true);
                ReloadingMechanic.startReload(player, mainHand);
            } else {
                // Already reloading, just cancel
                event.setCancelled(true);
            }
        }
    }
    
    private void updateWeaponLore(ItemStack weapon, WeaponType type) {
        int magazineSize = WeaponData.getMagazineSize(weapon);
        int currentAmmo = WeaponData.getCurrentAmmo(weapon);
        
        var meta = weapon.getItemMeta();
        var lore = new java.util.ArrayList<String>();
        lore.add("§7类型: " + type.getChineseName());
        lore.add("§7弹匣容量: §e" + magazineSize);
        lore.add("§7当前弹药: §e" + currentAmmo + "/" + magazineSize);
        lore.add("§7冷却时间: §e" + (type.getCooldownTicks() / 20.0) + "秒");
        if (type.isAutoFire()) {
            lore.add("§a自动武器");
        }
        if (WeaponData.isReloading(weapon)) {
            lore.add("§e正在换弹...");
        }
        meta.setLore(lore);
        weapon.setItemMeta(meta);
    }
}
