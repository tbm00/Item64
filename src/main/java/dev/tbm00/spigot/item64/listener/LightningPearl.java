package dev.tbm00.spigot.item64.listener;

import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.ListenerLeader;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class LightningPearl extends ListenerLeader implements Listener {

    public LightningPearl(JavaPlugin javaPlugin, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        super(javaPlugin, configHandler, ecoHook, gdHook, dcHook);
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player shooter = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || shooter == null) return;

        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null || !entry.getType().equals("LIGHTNING_PEARL") || !shooter.hasPermission(entry.getUsePerm()))
            return;

        triggerLightningPearl(event, shooter, entry);
    }

    private void triggerLightningPearl(PlayerInteractEvent event, Player shooter, ItemEntry entry) {
        event.setCancelled(true);
        if (!passGDPvpCheck(shooter.getLocation())) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- claim pvp protection!"));
            return;
        }

        if (shooter.getFoodLevel() < entry.getHunger()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(shooter.getUniqueId(), k -> cooldowns);
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(entry.getID()-1)) < entry.getCooldown()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(shooter, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(shooter, cost)) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- not enough money!"));
            return;
        }

        shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting lightning pearl..."));
        EnderPearl pearl = shooter.launchProjectile(EnderPearl.class);
        pearl.setMetadata("Item64-keyString", new FixedMetadataValue(javaPlugin, entry.getKeyString()));
        lightningPearls.add(pearl);
        double random = entry.getRandom();
        if (random > 0) randomizeProjectile(pearl, random);

        // Set cooldowns and remove resources
        adjustCooldown(shooter, entry);
        adjustHunger(shooter, entry);
        if (ecoHook != null && cost > 0 && !removeMoney(shooter, cost)) {
            javaPlugin.getLogger().warning("Error: failed to remove money for " + shooter.getName() + "'s " + entry.getKeyString() + " usage!");
        }
        if (hasAmmoItem == 2) removeItem(shooter, entry.getAmmoItem());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof EnderPearl)
            handlePearlHit(event, (EnderPearl) event.getEntity());
    }

    private void handlePearlHit(ProjectileHitEvent event, EnderPearl pearl) {
        if (!lightningPearls.remove(pearl)) return;
        event.setCancelled(true);

        Location location = pearl.getLocation();
        Player shooter = (Player) pearl.getShooter();
        boolean passDCPvpLocCheck = true, passGDPvpCheck = true;
        ItemEntry entry = configHandler.getItemEntryByKeyString(pearl.getMetadata("Item64-keyString").get(0).asString());
        
        if (dcHook != null && !passDCPvpLocCheck(location, 4.0)) passDCPvpLocCheck = false;
        if (gdHook != null) {
            if (!passGDPvpCheck(location)) passGDPvpCheck = false;
            else if (!passGDPvpCheck(shooter.getLocation())) passGDPvpCheck = false;
        }        

        if (!passDCPvpLocCheck) {
            pearl.remove();
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lightning blocked -- pvp protection!"));
            refundPlayer(shooter, entry);
        } else if (!passGDPvpCheck) {
            pearl.remove();
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lightning blocked -- claim pvp protection!"));
            refundPlayer(shooter, entry);
        } else {
            damagePlayers(shooter, location, 1.2, 3.0, entry.getDamage(), 0);
            location.getWorld().strikeLightning(location);
            pearl.remove();
        }
    }
}
