package dev.tbm00.spigot.item64.listener;

import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
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
import dev.tbm00.spigot.item64.ListenerLeader;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class ExplosiveArrow extends ListenerLeader implements Listener {

    public ExplosiveArrow(JavaPlugin javaPlugin, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        super(javaPlugin, configHandler, ecoHook, gdHook, dcHook);
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player shooter = (Player) event.getEntity();
        Arrow arrow = (Arrow) event.getProjectile();
        ItemStack item = shooter.getInventory().getItemInMainHand();
        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null) return;

        if (!entry.getType().equals("EXPLOSIVE_ARROW") || !shooter.hasPermission(entry.getUsePerm()))
            return;

        double random = entry.getRandom();
        if (random > 0) randomizeProjectile(arrow, random);

        triggerExplosiveArrow(shooter, arrow, entry);
    }

    private void triggerExplosiveArrow(Player shooter, Arrow arrow, ItemEntry entry) {
        if (!passGDPvpCheck(shooter.getLocation())) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- claim pvp protection!"));
            return;
        }

        if (shooter.getFoodLevel() < entry.getHunger()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(shooter.getUniqueId(), k -> cooldowns);
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(entry.getID()-1)) < entry.getCooldown()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(shooter, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toLowerCase()));
            return;
        }
        
        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(shooter, cost)) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- not enough money!"));
            return;
        }

        shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting explosive arrow..."));
        arrow.setMetadata("Item64-keyString", new FixedMetadataValue(javaPlugin, entry.getKeyString()));
        explosiveArrows.add(arrow);
        
        // Set cooldowns and remove resources
        adjustCooldown(shooter, entry);
        adjustHunger(shooter, entry);
        if (ecoHook != null && cost > 0 && !removeMoney(shooter, cost)) {
            javaPlugin.getLogger().warning("Error: failed to remove money for " + shooter.getName() + "'s " + entry.getKeyString() + " usage!");
        }
        if (hasAmmoItem == 2) removeItem(shooter, entry.getAmmoItem()); // remove ammo item
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow)
            handleArrowHit((Arrow) event.getEntity());
    }

    private void handleArrowHit(Arrow arrow) {
        if (explosiveArrows.remove(arrow)) {
            Location location = arrow.getLocation();
            Player shooter = (Player) arrow.getShooter();
            boolean passDCPvpLocCheck = true, passGDPvpCheck = true, passGDBuilderCheck = true;
            ItemEntry entry = configHandler.getItemEntryByKeyString(arrow.getMetadata("Item64-keyString").get(0).asString());

            if (dcHook != null && !passDCPvpLocCheck(location, 5.0)) passDCPvpLocCheck = false;
            if (gdHook != null) {
                if (!passGDPvpCheck(shooter.getLocation())) passGDPvpCheck = false;
                else if (!passGDPvpCheck(location)) passGDPvpCheck = false;
                else if (!passGDBuilderCheck(shooter, location, 5)) passGDBuilderCheck = false;
            }

            if (!passDCPvpLocCheck) {
                arrow.remove();
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- pvp protection!"));
                refundPlayer(shooter, entry);
            } else if (!passGDPvpCheck) {
                arrow.remove();
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- claim pvp protection!"));
                refundPlayer(shooter, entry);
            } else if (!passGDBuilderCheck) {
                damagePlayers(shooter, location, 1.7, 1.2, entry.getDamage(), 30);
                arrow.getWorld().createExplosion(location, entry.getPower(), true, false, shooter);
                arrow.remove();
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion nerfed -- claim block protection!"));
            } else {
                damagePlayers(shooter, location, 1.7, 1.2, entry.getDamage(), 30);
                arrow.getWorld().createExplosion(location, entry.getPower(), true, true, shooter);
                arrow.remove();
            }
        }
    }
}