package com.papergun;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class WeaponData {
    private static NamespacedKey weaponTypeKey;
    private static NamespacedKey magazineSizeKey;
    private static NamespacedKey currentAmmoKey;
    private static NamespacedKey lastShotTimeKey;
    private static NamespacedKey isReloadingKey;

    public static void init(Plugin plugin) {
        weaponTypeKey = new NamespacedKey(plugin, "weapon_type");
        magazineSizeKey = new NamespacedKey(plugin, "magazine_size");
        currentAmmoKey = new NamespacedKey(plugin, "current_ammo");
        lastShotTimeKey = new NamespacedKey(plugin, "last_shot_time");
        isReloadingKey = new NamespacedKey(plugin, "is_reloading");
    }

    public static NamespacedKey getWeaponTypeKey() {
        return weaponTypeKey;
    }

    public static NamespacedKey getMagazineSizeKey() {
        return magazineSizeKey;
    }

    public static NamespacedKey getCurrentAmmoKey() {
        return currentAmmoKey;
    }

    public static NamespacedKey getLastShotTimeKey() {
        return lastShotTimeKey;
    }

    public static NamespacedKey getIsReloadingKey() {
        return isReloadingKey;
    }

    public static boolean isWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(weaponTypeKey, PersistentDataType.STRING);
    }

    public static WeaponType getWeaponType(ItemStack item) {
        if (!isWeapon(item)) return null;
        String typeStr = item.getItemMeta().getPersistentDataContainer().get(weaponTypeKey, PersistentDataType.STRING);
        return WeaponType.fromString(typeStr);
    }

    public static int getMagazineSize(ItemStack item) {
        if (!isWeapon(item)) return 0;
        Integer size = item.getItemMeta().getPersistentDataContainer().get(magazineSizeKey, PersistentDataType.INTEGER);
        return size != null ? size : 0;
    }

    public static int getCurrentAmmo(ItemStack item) {
        if (!isWeapon(item)) return 0;
        Integer ammo = item.getItemMeta().getPersistentDataContainer().get(currentAmmoKey, PersistentDataType.INTEGER);
        return ammo != null ? ammo : 0;
    }

    public static void setCurrentAmmo(ItemStack item, int ammo) {
        if (!isWeapon(item)) return;
        item.getItemMeta().getPersistentDataContainer().set(currentAmmoKey, PersistentDataType.INTEGER, ammo);
    }

    public static long getLastShotTime(ItemStack item) {
        if (!isWeapon(item)) return 0;
        Long time = item.getItemMeta().getPersistentDataContainer().get(lastShotTimeKey, PersistentDataType.LONG);
        return time != null ? time : 0;
    }

    public static void setLastShotTime(ItemStack item, long time) {
        if (!isWeapon(item)) return;
        item.getItemMeta().getPersistentDataContainer().set(lastShotTimeKey, PersistentDataType.LONG, time);
    }

    public static boolean isReloading(ItemStack item) {
        if (!isWeapon(item)) return false;
        Boolean reloading = item.getItemMeta().getPersistentDataContainer().get(isReloadingKey, PersistentDataType.BOOLEAN);
        return reloading != null && reloading;
    }

    public static void setReloading(ItemStack item, boolean reloading) {
        if (!isWeapon(item)) return;
        item.getItemMeta().getPersistentDataContainer().set(isReloadingKey, PersistentDataType.BOOLEAN, reloading);
    }

    public static void setWeapon(ItemStack item, WeaponType type, int magazineSize) {
        if (item == null) return;
        if (!item.hasItemMeta()) {
            item.setItemMeta(org.bukkit.Bukkit.createInventory(null, 9).getItem(0).getItemMeta());
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        pdc.set(weaponTypeKey, PersistentDataType.STRING, type.name());
        pdc.set(magazineSizeKey, PersistentDataType.INTEGER, magazineSize);
        pdc.set(currentAmmoKey, PersistentDataType.INTEGER, magazineSize);
        pdc.set(lastShotTimeKey, PersistentDataType.LONG, 0L);
        pdc.set(isReloadingKey, PersistentDataType.BOOLEAN, false);
        
        // Update item name and lore
        var meta = item.getItemMeta();
        meta.setDisplayName("§6" + type.getChineseName());
        var lore = new java.util.ArrayList<String>();
        lore.add("§7类型: " + type.getChineseName());
        lore.add("§7弹匣容量: §e" + magazineSize);
        lore.add("§7当前弹药: §e" + magazineSize + "/" + magazineSize);
        lore.add("§7冷却时间: §e" + (type.getCooldownTicks() / 20.0) + "秒");
        if (type.isAutoFire()) {
            lore.add("§a自动武器");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
