package com.papergun;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PaperGunCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be executed by players!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§eUsage:");
            sender.sendMessage("§7/papergun setweapon <type> <magazineSize> - Set weapon");
            sender.sendMessage("§7/papergun setceweapon <craftengine:id:xxx:xxx> <type> <magazineSize> - Set CraftEngine item as weapon");
            sender.sendMessage("§7/papergun listguns - List all registered guns");
            sender.sendMessage("§7/papergun delgun <craftengine:id:xxx:xxx> - Delete gun config");
            sender.sendMessage("§7/papergun addrpg <magazineSize> - Add RPG launcher");
            sender.sendMessage("§7Available types: Pistol, Revolver, Rifle, Assault Rifle, Sniper Rifle, Shotgun, RPG");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("setweapon")) {
            return handleSetWeapon(player, args);
        } else if (subCommand.equals("setceweapon")) {
            return handleSetCEWeapon(player, args);
        } else if (subCommand.equals("listguns")) {
            return handleListGuns(player);
        } else if (subCommand.equals("delgun")) {
            return handleDelGun(player, args);
        } else if (subCommand.equals("addrpg")) {
            return handleAddRPG(player, args);
        } else {
            sender.sendMessage("§cUnknown subcommand! Use /papergun for help");
            return true;
        }
    }

    private boolean handleSetWeapon(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§eUsage: /papergun setweapon <type> <magazineSize>");
            player.sendMessage("§7Available: Pistol, Revolver, Rifle, Assault Rifle, Sniper Rifle, Shotgun");
            return true;
        }

        String typeStr = args[1];
        int magazineSize;
        
        try {
            magazineSize = Integer.parseInt(args[2]);
            if (magazineSize <= 0) {
                player.sendMessage("§cMagazine size must be > 0!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid magazine size! Please enter a number.");
            return true;
        }

        WeaponType weaponType = WeaponType.fromString(typeStr);
        if (weaponType == null) {
            player.sendMessage("§cInvalid weapon type!");
            player.sendMessage("§7Available: Pistol, Revolver, Rifle, Assault Rifle, Sniper Rifle, Shotgun");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == org.bukkit.Material.AIR) {
            player.sendMessage("§cYou must hold an item!");
            return true;
        }

        // Set the weapon
        WeaponData.setWeapon(item, weaponType, magazineSize);
        
        player.sendMessage("§aSuccess! §6" + weaponType.getChineseName() + " (" + weaponType.name() + ")");
        player.sendMessage("§7Magazine Size: §e" + magazineSize);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        return true;
    }

    private boolean handleSetCEWeapon(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§eUsage: /papergun setceweapon <craftengine:id:xxx:xxx> <type> <magazineSize>");
            player.sendMessage("§7Example: /papergun setceweapon craftengine:id:mygun:pistol Pistol 15");
            player.sendMessage("§7Available: Pistol, Revolver, Rifle, Assault Rifle, Sniper Rifle, Shotgun, RPG");
            return true;
        }

        String craftEngineId = args[1];
        String typeStr = args[2];
        int magazineSize;
        
        try {
            magazineSize = Integer.parseInt(args[3]);
            if (magazineSize <= 0) {
                player.sendMessage("§cMagazine size must be > 0!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid magazine size! Please enter a number.");
            return true;
        }

        WeaponType weaponType = WeaponType.fromString(typeStr);
        if (weaponType == null) {
            player.sendMessage("§cInvalid weapon type!");
            player.sendMessage("§7Available: Pistol, Revolver, Rifle, Assault Rifle, Sniper Rifle, Shotgun, RPG");
            return true;
        }

        // Register the gun config for CraftEngine items
        WeaponData.registerGunConfig(craftEngineId, weaponType, magazineSize);
        
        player.sendMessage("§aSuccess!");
        player.sendMessage("§7CraftEngine ID: §e" + craftEngineId);
        player.sendMessage("§7Weapon Type: §6" + weaponType.getChineseName() + " (" + weaponType.name() + ")");
        player.sendMessage("§7Magazine Size: §e" + magazineSize);
        player.sendMessage("§eWhen players pick up the CraftEngine item with this ID, the config will be applied automatically.");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        return true;
    }

    private boolean handleListGuns(Player player) {
        ConcurrentHashMap<String, WeaponData.GunConfig> configs = WeaponData.getAllGunConfigs();
        
        if (configs.isEmpty()) {
            player.sendMessage("§cNo gun configs registered.");
            return true;
        }

        player.sendMessage("§a=== Registered Guns ===");
        for (WeaponData.GunConfig config : configs.values()) {
            player.sendMessage("§7ID: §e" + config.craftEngineId);
            player.sendMessage("  §7Type: §6" + config.weaponType.getChineseName() + " (" + config.weaponType.name() + ")");
            player.sendMessage("  §7Magazine Size: §e" + config.magazineSize);
        }
        player.sendMessage("§aTotal: §e" + configs.size() + " §aconfigs");

        return true;
    }

    private boolean handleDelGun(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§eUsage: /papergun delgun <craftengine:id:xxx:xxx>");
            return true;
        }

        String craftEngineId = args[1];
        WeaponData.GunConfig removed = WeaponData.getAllGunConfigs().remove(craftEngineId);
        
        if (removed != null) {
            player.sendMessage("§aSuccess!");
            player.sendMessage("§7Deleted: §e" + craftEngineId);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        } else {
            player.sendMessage("§cConfig not found!");
            player.sendMessage("§7Use /papergun listguns to view all configs");
        }

        return true;
    }

    private boolean handleAddRPG(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§eUsage: /papergun addrpg <magazineSize>");
            player.sendMessage("§7Example: /papergun addrpg 1");
            return true;
        }

        int magazineSize;
        
        try {
            magazineSize = Integer.parseInt(args[1]);
            if (magazineSize <= 0) {
                player.sendMessage("§cMagazine size must be > 0!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid magazine size! Please enter a number.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == org.bukkit.Material.AIR) {
            player.sendMessage("§cYou must hold an item!");
            return true;
        }

        // Set the RPG weapon
        WeaponData.setWeapon(item, WeaponType.RPG, magazineSize);
        
        player.sendMessage("§aSuccess! §6RPG Launcher");
        player.sendMessage("§7Magazine Size: §e" + magazineSize);
        player.sendMessage("§eRPG fires virtual projectile, explodes on impact with AoE damage, no block damage");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("setweapon");
            completions.add("setceweapon");
            completions.add("listguns");
            completions.add("delgun");
            completions.add("addrpg");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setweapon") || args[0].equalsIgnoreCase("setceweapon")) {
                completions.add("Pistol");
                completions.add("Revolver");
                completions.add("Rifle");
                completions.add("Assault Rifle");
                completions.add("Sniper Rifle");
                completions.add("Shotgun");
                completions.add("RPG");
            } else if (args[0].equalsIgnoreCase("delgun")) {
                for (WeaponData.GunConfig config : WeaponData.getAllGunConfigs().values()) {
                    completions.add(config.craftEngineId);
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setceweapon")) {
                completions.add("Pistol");
                completions.add("Revolver");
                completions.add("Rifle");
                completions.add("Assault Rifle");
                completions.add("Sniper Rifle");
                completions.add("Shotgun");
                completions.add("RPG");
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("setceweapon")) {
                completions.add("10");
                completions.add("15");
                completions.add("20");
                completions.add("30");
            }
        }
        
        return completions;
    }
}
