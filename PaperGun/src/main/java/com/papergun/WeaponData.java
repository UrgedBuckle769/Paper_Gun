package com.papergun;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WeaponData {
    private static NamespacedKey weaponTypeKey;
    private static NamespacedKey magazineSizeKey;
    private static NamespacedKey currentAmmoKey;
    private static NamespacedKey lastShotTimeKey;
    private static NamespacedKey isReloadingKey;
    private static NamespacedKey craftEngineIdKey;
    
    // Store gun configurations by CraftEngine ID
    private static final ConcurrentHashMap<String, GunConfig> gunConfigs = new ConcurrentHashMap<>();
    
    public static class GunConfig {
        public final String craftEngineId;
        public final WeaponType weaponType;
        public final int magazineSize;
        
        public GunConfig(String craftEngineId, WeaponType weaponType, int magazineSize) {
            this.craftEngineId = craftEngineId;
            this.weaponType = weaponType;
            this.magazineSize = magazineSize;
        }
    }
    
    public static void init(Plugin plugin) {
        weaponTypeKey = new NamespacedKey(plugin, "weapon_type");
        magazineSizeKey = new NamespacedKey(plugin, "magazine_size");
        currentAmmoKey = new NamespacedKey(plugin, "current_ammo");
        lastShotTimeKey = new NamespacedKey(plugin, "last_shot_time");
        isReloadingKey = new NamespacedKey(plugin, "is_reloading");
        craftEngineIdKey = new NamespacedKey(plugin, "craftengine_id");
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

    public static String getCraftEngineId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(craftEngineIdKey, PersistentDataType.STRING);
    }

    public static void setCraftEngineId(ItemStack item, String id) {
        if (item == null) return;
        var meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(craftEngineIdKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
    }

    public static void registerGunConfig(String craftEngineId, WeaponType type, int magazineSize) {
        gunConfigs.put(craftEngineId, new GunConfig(craftEngineId, type, magazineSize));
    }

    public static GunConfig getGunConfig(String craftEngineId) {
        return gunConfigs.get(craftEngineId);
    }

    public static ConcurrentHashMap<String, GunConfig> getAllGunConfigs() {
        return gunConfigs;
    }

    public static void applyGunConfigToItem(ItemStack item, GunConfig config) {
        if (item == null || config == null) return;
        
        var meta = item.getItemMeta();
        if (meta == null) {
            meta = org.bukkit.Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) return;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(weaponTypeKey, PersistentDataType.STRING, config.weaponType.name());
        pdc.set(magazineSizeKey, PersistentDataType.INTEGER, config.magazineSize);
        pdc.set(currentAmmoKey, PersistentDataType.INTEGER, config.magazineSize);
        pdc.set(lastShotTimeKey, PersistentDataType.LONG, 0L);
        pdc.set(isReloadingKey, PersistentDataType.BOOLEAN, false);
        
        // Update lore only, keep original display name
        var lore = new java.util.ArrayList<String>();
        lore.add("§7类型：" + config.weaponType.getChineseName());
        lore.add("§7弹匣容量：§e" + config.magazineSize);
        lore.add("§7当前弹药：§e" + config.magazineSize + "/" + config.magazineSize);
        lore.add("§7冷却时间：§e" + (config.weaponType.getCooldownTicks() / 20.0) + "秒");
        if (config.weaponType.isAutoFire()) {
            lore.add("§a自动武器");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public static void setWeapon(ItemStack item, WeaponType type, int magazineSize) {
        setWeapon(item, type, magazineSize, null);
    }

    public static void setWeapon(ItemStack item, WeaponType type, int magazineSize, String craftEngineId) {
        if (item == null) return;
        
        // Ensure item has meta, create one if needed
        var meta = item.getItemMeta();
        if (meta == null) {
            meta = org.bukkit.Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) return;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(weaponTypeKey, PersistentDataType.STRING, type.name());
        pdc.set(magazineSizeKey, PersistentDataType.INTEGER, magazineSize);
        pdc.set(currentAmmoKey, PersistentDataType.INTEGER, magazineSize);
        pdc.set(lastShotTimeKey, PersistentDataType.LONG, 0L);
        pdc.set(isReloadingKey, PersistentDataType.BOOLEAN, false);
        
        // Store CraftEngine ID if provided
        if (craftEngineId != null) {
            pdc.set(craftEngineIdKey, PersistentDataType.STRING, craftEngineId);
        }
        
        // Update lore only, keep original display name
        var lore = new java.util.ArrayList<String>();
        lore.add("§7类型：" + type.getChineseName());
        lore.add("§7弹匣容量：§e" + magazineSize);
        lore.add("§7当前弹药：§e" + magazineSize + "/" + magazineSize);
        lore.add("§7冷却时间：§e" + (type.getCooldownTicks() / 20.0) + "秒");
        if (type.isAutoFire()) {
            lore.add("§a自动武器");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
