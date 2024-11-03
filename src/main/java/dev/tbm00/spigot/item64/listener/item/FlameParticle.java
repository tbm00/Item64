package dev.tbm00.spigot.item64.listener.item;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
import org.bukkit.event.player.PlayerInteractEvent;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.*;
import dev.tbm00.spigot.item64.listener.ItemLeader;
import dev.tbm00.spigot.item64.listener.InteractHandler;

public class FlameParticle extends ItemLeader implements InteractHandler {
    private final static Queue<Pair<Boolean, Boolean>> queue = new LinkedList<>();

    public FlameParticle(JavaPlugin javaPlugin, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        super(javaPlugin, configHandler, ecoHook, gdHook, dcHook);
        super.registerHandler(this);
    }

    @Override
    public boolean canHandle(ItemEntry entry) {
        return "FLAME_PARTICLE".equals(entry.getType());
    }

    @Override
    public void handle(PlayerInteractEvent event, Player player, ItemEntry entry) {
        triggerFlameParticle(event, player, entry);
    }

    private void triggerFlameParticle(PlayerInteractEvent event, Player player, ItemEntry entry) {
        event.setCancelled(true);
        if (!passGDPvpCheck(player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- claim pvp protection!"));
            return;
        }

        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> cooldowns);
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(entry.getID()-1)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(player, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No fuel: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(player, cost)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- not enough money!"));
            return;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting flames..."));
        shootFlames(player, entry);

        // Set cooldowns and remove resources
        if (ecoHook != null && cost > 0 && !removeMoney(player, cost)) {
            javaPlugin.getLogger().warning("Error: failed to remove money for " + player.getName() + "'s " + entry.getKeyString() + " usage!");
        }
        if (hasAmmoItem == 2) removeItem(player, entry.getAmmoItem());
        adjustHunger(player, entry);

        new BukkitRunnable() {
            @Override
            public void run() {
                adjustCooldown(player, entry);
            }
        }.runTaskLater(javaPlugin, entry.getCooldown() * 20);
    }

    private void shootFlames(Player player, ItemEntry entry) {
        double random = entry.getRandom();

        // Initialize particle direction
        Vector playerDirection = player.getLocation().getDirection();
        Vector particleVector = playerDirection.clone();
        playerDirection.multiply(16);
        double temp = particleVector.getX();
        particleVector.setX(-particleVector.getZ());
        particleVector.setZ(temp);
        particleVector.divide(new Vector(3, 3, 3));
        Location particleLocation = particleVector.toLocation(player.getWorld()).add(player.getLocation()).add(0, 1.05, 0);

        // Shoot flames
        sendFlameHit(player, entry); // Set blocks on fire and damage players
        // visuals:
        for (int i = 0; i < 5; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i=0; i<3; ++i) {
                        Vector offsetPath = playerDirection.clone();
                        Location offsetLocation = particleLocation.clone();
                        if (random > 0) {
                            randomizeLocation(offsetLocation, random);
                            randomizeVelocity(offsetPath, random / 2);
                        }
                        offsetLocation.add(0, 0.3, 0);
                        player.getWorld().spawnParticle(Particle.FLAME, offsetLocation, 0, offsetPath.getX(), offsetPath.getY(), offsetPath.getZ(), 0.1);
                    }
                }
            }.runTaskLater(javaPlugin, i);
        }
    }

    private void sendFlameHit(Player player, ItemEntry entry) {
        if (Math.random() < 0.64) {
            Block targetBlock = player.getTargetBlock(null, 24);
            Block targetBlockAbove = targetBlock.getRelative(BlockFace.UP);

            if (targetBlock != null && targetBlockAbove != null) {
                Location location = targetBlockAbove.getLocation();
                boolean passGDPvpCheckResult = true, passDCPvpLocCheckResult = true;

                if (dcHook != null) {
                    if (!passDCPvpLocCheck(location, 3.0)) passDCPvpLocCheckResult = false;
                    else if (!passDCPvpPlayerCheck(player)) passDCPvpLocCheckResult = false;
                }
                if (gdHook != null) {
                    if (!passGDPvpCheck(location)) passGDPvpCheckResult = false;
                    else if (!passGDPvpCheck(player.getLocation())) passGDPvpCheckResult = false;
                }
                
                Pair<Boolean, Boolean> passage = new Pair<>(passDCPvpLocCheckResult, passGDPvpCheckResult);
                queue.add(passage);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Pair<Boolean, Boolean> check = queue.poll();

                        if (check!=null) {
                            boolean first = check.getFirst();
                            boolean second = check.getSecond();
                            
                            if (!first) {
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- pvp protection!"));
                                refundPlayer(player, entry);
                            } else if (!second) {
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- claim pvp protection!"));
                                refundPlayer(player, entry);
                            } else {
                                if (targetBlock.getType().isBlock() && targetBlockAbove.getType() == Material.AIR)
                                    targetBlockAbove.setType(Material.FIRE);
                                damagePlayers(player, targetBlockAbove.getLocation(), 1.5, 1.6, entry.getDamage(), 60);
                            }
                        }
                    }
                }.runTaskLater(javaPlugin, 10);
            }
        }
    }
}