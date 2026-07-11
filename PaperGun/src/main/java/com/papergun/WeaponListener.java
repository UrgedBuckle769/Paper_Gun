package com.papergun;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;

public class WeaponListener implements Listener {
    
    private final PaperGunPlugin plugin;
    
    public WeaponListener(PaperGunPlugin plugin) {
        this.plugin = plugin;
        startActionBarUpdater();
        registerSoundPacketListener();
    }
    
    private void registerSoundPacketListener() {
        // Use ProtocolLib to intercept sound packets and change blast to blast_far for other players
        PacketAdapter soundPacketListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, 
                com.comphenix.protocol.PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                // Get the shooter UUID from packet context or skip if it's the shooter
                // We need to check if this is our firework blast sound
                String soundEffect = event.getPacket().getStrings().read(0);
                
                if ("minecraft:entity.firework_rocket.blast".equals(soundEffect)) {
                    Player shooter = getShooterFromContext(event.getPlayer());
                    
                    // If the receiving player is NOT the shooter, change to blast_far
                    if (shooter != null && !event.getPlayer().getUniqueId().equals(shooter.getUniqueId())) {
                        event.getPacket().getStrings().write(0, "minecraft:entity.firework_rocket.blast_far");
                    }
                }
            }
        };
        
        ProtocolLibrary.getProtocolManager().addPacketListener(soundPacketListener);
    }
    
    // Helper method to track recent shooters
    private static final java.util.Map<java.util.UUID, Long> recentShooters = new java.util.concurrent.ConcurrentHashMap<>();
    
    public static void markPlayerAsShooter(Player player) {
        recentShooters.put(player.getUniqueId(), System.currentTimeMillis());
        // Clean up old entries after 1 second
        long now = System.currentTimeMillis();
        recentShooters.entrySet().removeIf(e -> now - e.getValue() > 1000);
    }
    
    public static Player getShooterFromContext(Player receiver) {
        long now = System.currentTimeMillis();
        // Find the most recent shooter within 500ms
        return recentShooters.entrySet().stream()
            .filter(e -> now - e.getValue() < 500)
            .findFirst()
            .map(e -> receiver.getServer().getPlayer(e.getKey()))
            .orElse(null);
    }

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
            player.sendMessage("§c弹药耗尽！按 F 键换弹.");
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1.0f, 2.0f);
            return;
        }
        
        // Mark player as shooter for sound packet handling
        markPlayerAsShooter(player);
        
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
                ReloadingMechanic.startReload(player, mainHand, plugin);
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
        if (meta == null) return;
        
        var lore = new java.util.ArrayList<String>();
        lore.add("§7类型：" + type.getChineseName());
        lore.add("§7弹匣容量：§e" + magazineSize);
        lore.add("§7当前弹药：§e" + currentAmmo + "/" + magazineSize);
        lore.add("§7冷却时间：§e" + (type.getCooldownTicks() / 20.0) + "秒");
        if (type.isAutoFire()) {
            lore.add("§a自动武器");
        }
        if (WeaponData.isReloading(weapon)) {
            lore.add("§e正在换弹...");
        }
        meta.setLore(lore);
        weapon.setItemMeta(meta);
    }
    
    private void startActionBarUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (WeaponData.isWeapon(item)) {
                        WeaponType type = WeaponData.getWeaponType(item);
                        if (type != null) {
                            int currentAmmo = WeaponData.getCurrentAmmo(item);
                            int magazineSize = WeaponData.getMagazineSize(item);
                            String message = "§eAMMO: §c[" + currentAmmo + "/" + magazineSize + "] §7类型:§b" + type.getChineseName();
                            player.sendActionBar(message);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // Update every 5 ticks (0.25 seconds)
    }
}
