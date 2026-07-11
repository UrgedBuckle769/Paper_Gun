package com.papergun;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class ShootingMechanic {

    public static void shoot(Player player, WeaponType weaponType) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        
        // Play shoot sound - shooter hears blast, others hear blast_far
        // Shooter hears minecraft:entity.firework_rocket.blast
        player.playSound(eyeLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
        
        // Others will hear minecraft:entity.firework_rocket.blast_far via PacketListener
        // This is handled in WeaponListener using ProtocolLib
        
        // Handle RPG separately with virtual projectile
        if (weaponType == WeaponType.RPG) {
            shootRPG(player, eyeLoc, direction);
            return;
        }
        
        // Spawn particles
        player.spawnParticle(Particle.FLAME, eyeLoc.add(direction.clone().multiply(2)), 10, 0.1, 0.1, 0.1, 0.05);
        
        // Ray trace for hit detection
        for (int i = 0; i < weaponType.getPelletCount(); i++) {
            // Add slight spread for shotguns and some weapons
            Vector spreadDir = direction.clone();
            if (weaponType.getPelletCount() > 1 || weaponType == WeaponType.SHOTGUN) {
                spreadDir.add(new Vector(
                    (Math.random() - 0.5) * 0.1,
                    (Math.random() - 0.5) * 0.1,
                    (Math.random() - 0.5) * 0.1
                )).normalize();
            }
            
            RayTraceResult result = player.getWorld().rayTraceEntities(
                eyeLoc,
                spreadDir,
                100.0, // Max range
                1.0,   // Bounding box size
                entity -> entity != player && entity instanceof LivingEntity
            );
            
            if (result != null && result.getHitEntity() != null) {
                Entity hitEntity = result.getHitEntity();
                if (hitEntity instanceof LivingEntity livingEntity) {
                    // Apply damage based on weapon type
                    double damage = getDamage(weaponType);
                    
                    // Headshot multiplier for sniper rifles
                    if (weaponType == WeaponType.SNIPER_RIFLE && isHeadshot(player, livingEntity)) {
                        damage *= 2.5;
                    }
                    
                    livingEntity.damage(damage, player);
                    
                    // Hit particles
                    player.spawnParticle(Particle.DAMAGE_INDICATOR, hitEntity.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
                }
            } else {
                // Block hit - spawn particles at max range or block hit location
                RayTraceResult blockResult = player.getWorld().rayTraceBlocks(
                    eyeLoc,
                    spreadDir,
                    100.0,
                    org.bukkit.FluidCollisionMode.NEVER,
                    true
                );
                
                if (blockResult != null && blockResult.getHitBlock() != null) {
                    Location hitLoc = blockResult.getHitPosition().toLocation(player.getWorld());
                    player.spawnParticle(Particle.BLOCK_CRACK, hitLoc, 15, 0.2, 0.2, 0.2, 0.05, 
                        org.bukkit.Material.STONE.createBlockData());
                }
            }
        }
    }
    
    private static void shootRPG(Player player, Location eyeLoc, Vector direction) {
        // RPG uses a virtual projectile that travels in a straight line
        // and explodes on impact with entities, dealing area damage without block damage
        
        Location projectileLoc = eyeLoc.clone().add(direction.clone().multiply(3));
        
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                // Move projectile forward
                projectileLoc.add(direction.clone().multiply(2));
                
                // Check for entity collision
                for (Entity entity : projectileLoc.getWorld().getNearbyEntities(projectileLoc, 1.5, 1.5, 1.5)) {
                    if (entity != player && entity instanceof LivingEntity livingEntity) {
                        // Hit an entity - explode!
                        explodeRPG(projectileLoc, player);
                        cancel();
                        return;
                    }
                }
                
                // Check for block collision
                if (projectileLoc.getBlock().getType().isSolid()) {
                    // Hit a block - explode anyway but no block damage
                    explodeRPG(projectileLoc, player);
                    cancel();
                    return;
                }
                
                // Spawn trail particles
                player.spawnParticle(Particle.SMOKE_NORMAL, projectileLoc, 3, 0.1, 0.1, 0.1, 0.02);
            }
        }.runTaskTimer(player.getServer().getPluginManager().getPlugin("PaperGun"), 0L, 1L);
    }
    
    private static void explodeRPG(Location loc, Player shooter) {
        // Explosion effect without block damage
        loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
        
        // Deal damage to all players within radius (no block damage, only damage players)
        double radius = 6.0;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof Player player && entity != shooter) {
                double distance = loc.distance(entity.getLocation());
                if (distance <= radius) {
                    // Damage falls off with distance
                    double damage = 20.0 * (1.0 - (distance / radius));
                    player.damage(damage, shooter);
                    
                    // Knockback
                    Vector knockback = entity.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.0);
                    player.setVelocity(player.getVelocity().add(knockback));
                }
            }
        }
        
        // Fire particles
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 50, 2, 2, 2, 0.1);
    }
    
    private static double getDamage(WeaponType weaponType) {
        return switch (weaponType) {
            case PISTOL -> 4.0;
            case REVOLVER -> 6.0;
            case RIFLE -> 5.0;
            case ASSAULT_RIFLE -> 4.5;
            case SNIPER_RIFLE -> 15.0;
            case SHOTGUN -> 3.0; // Per pellet, total can be high at close range
            case RPG -> 20.0; // Base damage for RPG explosion
        };
    }
    
    private static boolean isHeadshot(Player shooter, LivingEntity target) {
        Location headLoc = target.getEyeLocation();
        Location shooterLoc = shooter.getEyeLocation();
        
        // Simple check: if the vertical difference is small, consider it a headshot
        return Math.abs(headLoc.getY() - shooterLoc.getY()) < 0.5;
    }
}
