package com.papergun;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class CraftEngineListener implements Listener {
    
    private final PaperGunPlugin plugin;
    
    public CraftEngineListener(PaperGunPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        
        if (newItem != null && newItem.hasItemMeta()) {
            checkAndApplyCraftEngineGun(player, newItem);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item != null && item.hasItemMeta()) {
            checkAndApplyCraftEngineGun(player, item);
        }
    }
    
    private void checkAndApplyCraftEngineGun(Player player, ItemStack item) {
        if (!item.hasItemMeta()) return;
        
        // Try to get CraftEngine ID from item's custom data
        String craftEngineId = getCraftEngineIdFromItem(item);
        
        if (craftEngineId != null) {
            WeaponData.GunConfig config = WeaponData.getGunConfig(craftEngineId);
            
            if (config != null) {
                // Check if item already has weapon data
                if (!WeaponData.isWeapon(item)) {
                    // Apply the gun config to this item
                    WeaponData.applyGunConfigToItem(item, config);
                    player.sendMessage("§e已自动应用武器配置：" + config.weaponType.getChineseName());
                }
            }
        }
    }
    
    private String getCraftEngineIdFromItem(ItemStack item) {
        // Method 1: Check our stored PDC key
        var meta = item.getItemMeta();
        if (meta != null) {
            var pdc = meta.getPersistentDataContainer();
            NamespacedKey ceKey = new NamespacedKey(plugin, "craftengine_id");
            String storedId = pdc.get(ceKey, PersistentDataType.STRING);
            if (storedId != null) {
                return storedId;
            }
        }
        
        // Method 2: Try to get from lore for CraftEngine ID pattern like "custom_data/craftengine:id:xxxx:xxxxx"
        if (meta != null && meta.hasLore()) {
            var lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.contains("craftengine:id:")) {
                        int idx = line.indexOf("craftengine:id:");
                        String fullId = line.substring(idx);
                        // Extract just the ID part after "craftengine:id:"
                        if (fullId.startsWith("craftengine:id:")) {
                            return fullId.substring("craftengine:id:".length()).split("\\s")[0];
                        }
                    }
                }
            }
        }
        
        return null;
    }
}
