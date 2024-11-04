package dev.tbm00.spigot.item64.listener.item;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.Item64;
import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;
import dev.tbm00.spigot.item64.listener.ItemLeader;
import dev.tbm00.spigot.item64.listener.InteractHandler;

public class LightningPearl extends ItemLeader implements InteractHandler {

    public LightningPearl(Item64 item64, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        super(item64, configHandler, ecoHook, gdHook, dcHook);
        super.registerHandler(this);
    }

    @Override
    public boolean canHandle(ItemEntry entry) {
        return "LIGHTNING_PEARL".equals(entry.getType());
    }

    @Override
    public void handle(PlayerInteractEvent event, Player player, ItemEntry entry) {
        triggerLightningPearl(event, player, entry);
    }

    private void triggerLightningPearl(PlayerInteractEvent event, Player player, ItemEntry entry) {
        event.setCancelled(true);
        if (!passGDPvpCheck(player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- claim pvp protection!"));
            return;
        }

        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> cooldowns);
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(entry.getID()-1)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(player, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(player, cost)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- not enough money!"));
            return;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting lightning pearl..."));
        EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        pearl.setMetadata("Item64-keyString", new FixedMetadataValue(item64, entry.getKeyString()));
        lightningPearls.add(pearl);
        double random = entry.getRandom();
        if (random > 0) randomizeProjectile(pearl, random);

        // Set cooldowns and remove resources
        adjustCooldown(player, entry);
        adjustHunger(player, entry);
        if (ecoHook != null && cost > 0 && !removeMoney(player, cost)) {
            item64.logRed("Error: failed to remove money for " + player.getName() + "'s " + entry.getKeyString() + " usage!");
        }
        if (hasAmmoItem == 2) removeItem(player, entry.getAmmoItem());
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
        Player player = (Player) pearl.getShooter();
        boolean passDCPvpLocCheck = true, passGDPvpCheck = true;
        ItemEntry entry = configHandler.getItemEntryByKeyString(pearl.getMetadata("Item64-keyString").get(0).asString());
        
        if (dcHook != null && !passDCPvpLocCheck(location, 4.0)) passDCPvpLocCheck = false;
        if (gdHook != null) {
            if (!passGDPvpCheck(location)) passGDPvpCheck = false;
            else if (!passGDPvpCheck(player.getLocation())) passGDPvpCheck = false;
        }        

        if (!passDCPvpLocCheck) {
            pearl.remove();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lightning blocked -- pvp protection!"));
            refundPlayer(player, entry);
        } else if (!passGDPvpCheck) {
            pearl.remove();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lightning blocked -- claim pvp protection!"));
            refundPlayer(player, entry);
        } else {
            damageEntities(player, location, 1.0, 2.5, entry.getDamage(), 0);
            location.getWorld().strikeLightning(location);
            pearl.remove();
        }
    }
}
