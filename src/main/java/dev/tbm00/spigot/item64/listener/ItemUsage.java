package dev.tbm00.spigot.item64.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import dev.tbm00.spigot.item64.Item64;
import dev.tbm00.spigot.item64.UsageHandler;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class ItemUsage implements Listener {
    private UsageHandler usageHandler;

    public ItemUsage(Item64 item64, UsageHandler usageHandler) {
        this.usageHandler = usageHandler;
    }

    // USE LISTENER: CONSUMABLE
    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || player == null) return;

        ItemEntry entry = usageHandler.getItemEntryByItem(item);
        if (entry == null || !entry.getType().equalsIgnoreCase("CONSUMABLE") || !player.hasPermission(entry.getUsePerm()))
            return;

        event.setCancelled(true);

        usageHandler.triggerUsage(player, entry, item, null, null, null);
    }

    // USE LISTENER: USABLE, FLAME_PARTICLE, LIGHTNING_PEARL, RANDOM_POTION
    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || player == null) return;
        
        ItemEntry entry = usageHandler.getItemEntryByItem(item);
        if (entry == null || !player.hasPermission(entry.getUsePerm()))
            return;

        String type = entry.getType();
        if (type.equalsIgnoreCase("EXPLOSIVE_ARROW") || type.equalsIgnoreCase("CONSUMABLE") || type.equalsIgnoreCase("NO_ITEM"))
            return;
        switch (type) {
            case "EXPLOSIVE_ARROW":
            case "AREA_BREAK":
            case "NO_ITEM":
            case "CONSUMABLE":
                return;
            default:
                break;
        }
        
        event.setCancelled(true);

        Action action = event.getAction();
        usageHandler.triggerUsage(player, entry, item, action, null, null);
    }

    // USE LISTENER: EXPLOSIVE_ARROW
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (player == null || item == null) return;

        ItemEntry entry = usageHandler.getItemEntryByItem(item);
        if (entry == null || !player.hasPermission(entry.getUsePerm()) || 
        (!entry.getType().equalsIgnoreCase("EXPLOSIVE_ARROW"))) 
            return;

        Arrow arrow = (Arrow) event.getProjectile();
        if (arrow == null) return;

        usageHandler.triggerUsage(player, entry, null, null, arrow, null);
    }

    // USE LISTENER: AREA PICKAXE
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        
        // check if block is an active item entry
        Player player = event.getPlayer();
        ItemEntry entry = usageHandler.getItemEntryByItem(player.getItemInUse());
        if (entry == null || !player.hasPermission(entry.getUsePerm())
            || !entry.getType().equalsIgnoreCase("AREA_BREAK")) {
            return;
        }

        if (event.getBlock()==null) return;

        usageHandler.triggerUsage(player, entry, null, null, null, event.getBlock());
    }

    // LANDING LISTENER: EXPLOSIVE_ARROW, LIGHTNING_PEARL
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile instanceof Arrow) {
            if (!usageHandler.getExplosiveArrows().remove((Arrow) projectile)) return;
        } else if (projectile instanceof EnderPearl) {
            if (!usageHandler.getLightningPearls().remove((EnderPearl) projectile)) return;
            event.setCancelled(true);
        } else return;

        Location location = projectile.getLocation();
        Player player = (Player) projectile.getShooter();
        ItemEntry entry = usageHandler.getConfigHandler().getItemEntryByKeyString(projectile.getMetadata("Item64-keyString").get(0).asString());

        if (usageHandler.passDamageChecks(player, location, usageHandler.getConfigHandler().PROTECTION_RADIUS)) {
            usageHandler.damageEntities(player, location, 1.7, 1.2, entry.getDamage(), 30);
            if (projectile instanceof Arrow) 
                projectile.getWorld().createExplosion(location, (float)entry.getPower(), true, true, player);
            else if (projectile instanceof EnderPearl)
                location.getWorld().strikeLightning(location);
        } else usageHandler.refundPlayer(player, entry);

        projectile.remove();
    }

    // LANDING LISTENER: RANDOM_POTION
    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion thrownPotion = (ThrownPotion) event.getEntity();
        if (!usageHandler.getMagicPotions().contains(thrownPotion)) return;
        usageHandler.getMagicPotions().remove(thrownPotion);

        Location location = thrownPotion.getLocation();
        Player player = (Player) thrownPotion.getShooter();
        ItemEntry entry = usageHandler.getConfigHandler().getItemEntryByKeyString(thrownPotion.getMetadata("Item64-keyString").get(0).asString());

        if (usageHandler.passDamageChecks(player, location, usageHandler.getConfigHandler().PROTECTION_RADIUS)) {
            if (thrownPotion.hasMetadata("Item64-randomPotion-left"))
                usageHandler.damageEntities(player, location, 0.9, 1.3, entry.getDamage(), 20);
        } else {
            event.setCancelled(true);
            thrownPotion.remove();
            usageHandler.refundPlayer(player, entry);
        }
    }
}