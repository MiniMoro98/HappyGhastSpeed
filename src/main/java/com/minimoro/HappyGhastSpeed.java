package com.minimoro;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class HappyGhastSpeed extends JavaPlugin implements Listener {

    private double defaultSpeed;
    private double ridingSpeed;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("HappyGhastSpeed plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("HappyGhastSpeed plugin disabled!");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        double speedValue = config.getDouble("speedValue.default", 0.075);
        if (speedValue < 0.01) speedValue = 0.01;
        if (speedValue > 0.5) speedValue = 0.5;
        defaultSpeed = speedValue;
        double speed = config.getDouble("speedValue.riding", 0.075);
        if (speed < 0.01) speed = 0.01;
        if (speed > 0.5) speed = 0.5;
        ridingSpeed = speed;
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (isHappyGhast(entity)) {
            setEntitySpeed(entity, defaultSpeed);
        }
    }

    @EventHandler
    public void onEntityMount(EntityMountEvent event) {
        Entity vehicle = event.getMount();
        Entity rider = event.getEntity();
        if (isHappyGhast(vehicle) && rider instanceof Player && !event.isCancelled()) {
            setEntitySpeed(vehicle, ridingSpeed);
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        Entity vehicle = event.getVehicle();
        Entity rider = event.getExited();
        if (isHappyGhast(vehicle) && rider instanceof Player) {
            setEntitySpeed(vehicle, defaultSpeed);
        }
    }

    private boolean isHappyGhast(Entity entity) {
        try {
            return entity.getType() == EntityType.valueOf("HAPPY_GHAST");
        } catch (IllegalArgumentException e) {
            String entityName = entity.getType().name();
            return entityName.contains("HAPPY") && entityName.contains("GHAST");
        }
    }

    private void setEntitySpeed(Entity entity, double speed) {
        if (!(entity instanceof LivingEntity livingEntity)) return;
        try {
            AttributeInstance movementSpeed = livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (movementSpeed != null) {
                movementSpeed.setBaseValue(speed);
            }
            AttributeInstance flyingSpeed = livingEntity.getAttribute(Attribute.GENERIC_FLYING_SPEED);
            if (flyingSpeed != null) {
                flyingSpeed.setBaseValue(speed);
            }
        } catch (Exception e) {
            applySpeedWithPotions(livingEntity, speed);
        }
    }

    private void applySpeedWithPotions(LivingEntity entity, double speed) {
        entity.removePotionEffect(PotionEffectType.SPEED);
        entity.removePotionEffect(PotionEffectType.SLOWNESS);
        int amplifier = calculateSpeedAmplifier(speed);
        if (amplifier > 0) {
            PotionEffect speedEffect = new PotionEffect(
                    PotionEffectType.SPEED,
                    Integer.MAX_VALUE,
                    amplifier - 1,
                    false,
                    false
            );
            entity.addPotionEffect(speedEffect);
        } else if (amplifier < 0) {
            PotionEffect slowEffect = new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    Integer.MAX_VALUE,
                    Math.abs(amplifier) - 1,
                    false,
                    false
            );
            entity.addPotionEffect(slowEffect);
        }
    }

    private int calculateSpeedAmplifier(double speed) {
        if (speed >= 0.5) {
            return (int) Math.round((speed - 0.5) / 0.2) + 1;
        } else {
            return -((int) Math.round((0.5 - speed) / 0.1) + 1);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("ghast")) return false;
        if (!sender.hasPermission("happyghastspeed.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e=== HappyGhastSpeed Commands ===");
            sender.sendMessage("§7/ghast setspeed default <value> - Set speed");
            sender.sendMessage("§7/ghast setspeed riding <value> - Set speed");
            sender.sendMessage("§7/ghast reload - Reload the config");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            loadConfig();
            sender.sendMessage("§aConfiguration reloaded!");
            return true;
        }
        if (args[0].equalsIgnoreCase("setspeed")) {
            if (args.length != 3) {
                sender.sendMessage("§cCorrect usage: /ghast setspeed <default|riding> <value>");
                return true;
            }
            try {
                double newSpeed = Double.parseDouble(args[2]);
                if (newSpeed < 0.0 || newSpeed > 0.5) {
                    sender.sendMessage("§cSpeed must be between 0.0 and 0.5!");
                    return true;
                }
                FileConfiguration config = getConfig();
                if (args[1].equalsIgnoreCase("default")) {
                    config.set("speedValue.default", newSpeed);
                    defaultSpeed = newSpeed;
                    saveConfig();
                    sender.sendMessage("§aDefault speed set to: " + newSpeed);
                } else if (args[1].equalsIgnoreCase("riding")) {
                    config.set("speedValue.riding", newSpeed);
                    ridingSpeed = newSpeed;
                    saveConfig();
                    sender.sendMessage("§aRiding speed set to: " + newSpeed);
                } else {
                    sender.sendMessage("§cUse 'default' or 'riding' as speed type!");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cPlease enter a valid number for speed!");
            }
            return true;
        }
        sender.sendMessage("§cUnknown command. Use /ghast for a list of commands.");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("ghast")) return null;
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("setspeed", "reload");
            for (String sub : subs) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setspeed")) {
            List<String> types = Arrays.asList("default", "riding");
            for (String type : types) {
                if (type.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(type);
                }
            }
            return completions;
        }
        return completions;
    }

}
