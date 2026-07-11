package com.papergun;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public enum WeaponType {
    PISTOL("手枪", 8, 100, 1, false),
    REVOLVER("左轮", 6, 200, 1, false),
    RIFLE("步枪", 10, 150, 1, false),
    ASSAULT_RIFLE("突击步枪", 30, 80, 1, true),
    SNIPER_RIFLE("狙击步枪", 5, 500, 1, false),
    SHOTGUN("霰弹枪", 8, 300, 5, false);

    private final String chineseName;
    private final int defaultMagazineSize;
    private final int cooldownTicks;
    private final int pelletCount;
    private final boolean autoFire;

    WeaponType(String chineseName, int defaultMagazineSize, int cooldownTicks, int pelletCount, boolean autoFire) {
        this.chineseName = chineseName;
        this.defaultMagazineSize = defaultMagazineSize;
        this.cooldownTicks = cooldownTicks;
        this.pelletCount = pelletCount;
        this.autoFire = autoFire;
    }

    public String getChineseName() {
        return chineseName;
    }

    public int getDefaultMagazineSize() {
        return defaultMagazineSize;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getPelletCount() {
        return pelletCount;
    }

    public boolean isAutoFire() {
        return autoFire;
    }

    public static WeaponType fromString(String type) {
        try {
            return WeaponType.valueOf(type.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
