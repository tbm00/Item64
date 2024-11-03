package dev.tbm00.spigot.item64.listener.item;

import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Location;
import org.bukkit.ChatColor;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;
import dev.tbm00.spigot.item64.listener.ItemLeader;

public class ExplosiveArrow extends ItemLeader {

    public ExplosiveArrow(JavaPlugin javaPlugin, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        super(javaPlugin, configHandler, ecoHook, gdHook, dcHook);
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Arrow arrow = (Arrow) event.getProjectile();
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null) return;

        if (!entry.getType().equals("EXPLOSIVE_ARROW") || !player.hasPermission(entry.getUsePerm()))
            return;

        double random = entry.getRandom();
        if (random > 0) randomizeProjectile(arrow, random);

        triggerExplosiveArrow(player, arrow, entry);
    }

    private void triggerExplosiveArrow(Player player, Arrow arrow, ItemEntry entry) {
        if (!passGDPvpCheck(player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- claim pvp protection!"));
            return;
        }

        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> cooldowns);
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(entry.getID()-1)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(player, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toLowerCase()));
            return;
        }
        
        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(player, cost)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- not enough money!"));
            return;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting explosive arrow..."));
        arrow.setMetadata("Item64-keyString", new FixedMetadataValue(javaPlugin, entry.getKeyString()));
        explosiveArrows.add(arrow);
        
        // Set cooldowns and remove resources
        adjustCooldown(player, entry);
        adjustHunger(player, entry);
        if (ecoHook != null && cost > 0 && !removeMoney(player, cost)) {
            javaPlugin.getLogger().warning("Error: failed to remove money for " + player.getName() + "'s " + entry.getKeyString() + " usage!");
        }
        if (hasAmmoItem == 2) removeItem(player, entry.getAmmoItem()); // remove ammo item
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow)
            handleArrowHit((Arrow) event.getEntity());
    }

    private void handleArrowHit(Arrow arrow) {
        if (explosiveArrows.remove(arrow)) {
            Location location = arrow.getLocation();
            Player player = (Player) arrow.getShooter();
            boolean passDCPvpLocCheck = true, passGDPvpCheck = true, passGDBuilderCheck = true;
            ItemEntry entry = configHandler.getItemEntryByKeyString(arrow.getMetadata("Item64-keyString").get(0).asString());

            if (dcHook != null && !passDCPvpLocCheck(location, 5.0)) passDCPvpLocCheck = false;
            if (gdHook != null) {
                if (!passGDPvpCheck(player.getLocation())) passGDPvpCheck = false;
                else if (!passGDPvpCheck(location)) passGDPvpCheck = false;
                else if (!passGDBuilderCheck(player, location, 5)) passGDBuilderCheck = false;
            }

            if (!passDCPvpLocCheck) {
                arrow.remove();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- pvp protection!"));
                refundPlayer(player, entry);
            } else if (!passGDPvpCheck) {
                arrow.remove();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- claim pvp protection!"));
                refundPlayer(player, entry);
            } else if (!passGDBuilderCheck) {
                damagePlayers(player, location, 1.7, 1.2, entry.getDamage(), 30);
                arrow.getWorld().createExplosion(location, (float)entry.getPower(), true, false, player);
                arrow.remove();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion nerfed -- claim block protection!"));
            } else {
                damagePlayers(player, location, 1.7, 1.2, entry.getDamage(), 30);
                arrow.getWorld().createExplosion(location, (float)entry.getPower(), true, true, player);
                arrow.remove();
            }
        }
    }
}