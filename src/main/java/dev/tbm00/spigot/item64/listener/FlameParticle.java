package dev.tbm00.spigot.item64.listener;

import java.util.List;

import org.bukkit.util.Vector;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.ListenerLeader;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class FlameParticle extends ListenerLeader implements Listener {

    public FlameParticle(JavaPlugin javaPlugin, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook) {
        super(javaPlugin, configHandler, ecoHook, gdHook, null);
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player shooter = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || shooter == null) return;

        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null || !entry.getType().equals("FLAME_PARTICLE") || !shooter.hasPermission(entry.getUsePerm()))
            return;

        triggerFlameParticle(event, shooter, entry);
    }

    private void triggerFlameParticle(PlayerInteractEvent event, Player shooter, ItemEntry entry) {
        event.setCancelled(true);
        if (!passGDPvpCheck(shooter.getLocation())) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- claim pvp protection!"));
            return;
        }

        if (shooter.getFoodLevel() < entry.getHunger()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(shooter.getUniqueId(), k -> cooldowns);
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(entry.getID()-1)) < entry.getCooldown()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(shooter, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No fuel: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(shooter, cost)) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- not enough money!"));
            return;
        }

        shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting flames..."));
        shootFlames(shooter, entry);

        // Set cooldowns and remove resources
        if (ecoHook != null && cost > 0 && !removeMoney(shooter, cost)) {
            javaPlugin.getLogger().warning("Error: failed to remove money for " + shooter.getName() + "'s " + entry.getKeyString() + " usage!");
        }
        if (hasAmmoItem == 2) removeItem(shooter, entry.getAmmoItem());
        adjustHunger(shooter, entry);

        new BukkitRunnable() {
            @Override
            public void run() {
                adjustCooldown(shooter, entry);
            }
        }.runTaskLater(javaPlugin, entry.getCooldown() * 20);
    }

    private void shootFlames(Player shooter, ItemEntry entry) {
        double random = entry.getRandom();

        // Initialize particle direction
        Vector shooterDirection = shooter.getLocation().getDirection();
        Vector particleVector = shooterDirection.clone();
        shooterDirection.multiply(16);
        double temp = particleVector.getX();
        particleVector.setX(-particleVector.getZ());
        particleVector.setZ(temp);
        particleVector.divide(new Vector(3, 3, 3));
        Location particleLocation = particleVector.toLocation(shooter.getWorld()).add(shooter.getLocation()).add(0, 1.05, 0);

        // Shoot flames
        sendFlameHit(shooter, entry); // Set blocks on fire and damage players
        // visuals:
        for (int i = 0; i < 5; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i=0; i<3; ++i) {
                        Vector offsetPath = shooterDirection.clone();
                        Location offsetLocation = particleLocation.clone();
                        if (random > 0) {
                            randomizeLocation(offsetLocation, random);
                            randomizeVelocity(offsetPath, random / 2);
                        }
                        offsetLocation.add(0, 0.3, 0);
                        shooter.getWorld().spawnParticle(Particle.FLAME, offsetLocation, 0, offsetPath.getX(), offsetPath.getY(), offsetPath.getZ(), 0.1);
                    }
                }
            }.runTaskLater(javaPlugin, i);
        }
    }

    private void sendFlameHit(Player shooter, ItemEntry entry) {
        if (Math.random() < 0.64) {
            Block targetBlock = shooter.getTargetBlock(null, 24);
            Block targetBlockAbove = targetBlock.getRelative(BlockFace.UP);

            if (targetBlock != null && targetBlockAbove != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location location = targetBlockAbove.getLocation();
                        boolean passGDPvpCheckResult = true;

                        if (gdHook != null) {
                            if (!passGDPvpCheck(location)) passGDPvpCheckResult = false;
                            else if (!passGDPvpCheck(shooter.getLocation())) passGDPvpCheckResult = false;
                        }

                        if (!passGDPvpCheckResult) {
                            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- claim pvp protection!"));
                            refundPlayer(shooter, entry);
                        } else {
                            if (targetBlock.getType().isBlock() && targetBlockAbove.getType() == Material.AIR)
                                targetBlockAbove.setType(Material.FIRE);
                            damagePlayers(shooter, targetBlockAbove.getLocation(), 1.5, 1.6, entry.getDamage(), 60);
                        }
                    }
                }.runTaskLater(javaPlugin, 10);
            }
        }
    }
}