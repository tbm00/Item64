package dev.tbm00.spigot.item64.listener;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.Item64;
import dev.tbm00.spigot.item64.hook.DCHook;
import dev.tbm00.spigot.item64.hook.GDHook;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class ItemUsage extends UsageHelper implements Listener {

    public ItemUsage(Item64 item64, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
            super(item64, configHandler, ecoHook, gdHook, dcHook);
    }

    // USE LISTENER: USABLE, FLAME_PARTICLE, LIGHTNING_PEARL, RANDOM_POTION
    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || player == null) return;
        
        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null || !entry.getType().equalsIgnoreCase("USABLE") || !player.hasPermission(entry.getUsePerm()))
            return;

        event.setCancelled(true);

        Action action = event.getAction();
        triggerUsage(player, entry, action, null, null);
    }

    // USE LISTENER: CONSUMABLE
    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || player == null) return;

        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null || !entry.getType().equalsIgnoreCase("CONSUMABLE") || !player.hasPermission(entry.getUsePerm()))
            return;

        event.setCancelled(true);

        triggerUsage(player, entry, null, item, null);
    }

    // USE LISTENER: EXPLOSIVE_ARROW
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Arrow arrow = (Arrow) event.getProjectile();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (player == null || arrow == null || item == null) return;


        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null || !entry.getType().equalsIgnoreCase("EXPLOSIVE_ARROW") || !player.hasPermission(entry.getUsePerm()))
            return;

        triggerUsage(player, entry, null, null, arrow);
    }

    // TRIGGER: ALL
    private void triggerUsage(Player player, ItemEntry entry, Action action, ItemStack item, Projectile projectile) {
        int takeAmmo = passUsageChecks(player, entry, projectile);
        if (takeAmmo<1) return;

        // Use the item
        double random = entry.getRandom();
        switch (entry.getType()) {
            case "USABLE":
                runCmdsApplyFX(player, entry, item);
                break;
            case "CONSUMABLE":
                runCmdsApplyFX(player, entry, item);
                break;
            case "EXPLOSIVE_ARROW":
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting explosive arrow..."));
                projectile.setMetadata("Item64-keyString", new FixedMetadataValue(item64, entry.getKeyString()));
                explosiveArrows.add(projectile);
                if (random > 0) randomizeProjectile(projectile, random);
                break;
            case "FLAME_PARTICLE":
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting flames..."));
                shootFlames(player, entry);
                break;
            case "LIGHTNING_PEARL":
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting lightning pearl..."));
                EnderPearl pearl = player.launchProjectile(EnderPearl.class);
                pearl.setMetadata("Item64-keyString", new FixedMetadataValue(item64, entry.getKeyString()));
                lightningPearls.add(pearl);
                if (random > 0) randomizeProjectile(pearl, random);
                break;
            case "RANDOM_POTION":
                shootPotion(player, entry, action);
                break;
            default: 
                break;
        }

        // Remove hunger, ammo, and money
        if (ecoHook != null && entry.getMoney() > 0 && !removeMoney(player, entry.getMoney()))
            item64.logRed("Error: failed to remove money for " + player.getName() + "'s " + entry.getKeyString() + " usage!");
        if (takeAmmo == 2) removeItem(player, entry.getAmmoItem());
        adjustHunger(player, entry);

        // Set cooldowns
        if (entry.getType().equals("FLAME_PARTICLE")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    adjustCooldown(player, entry);
                }
            }.runTaskLater(item64, entry.getCooldown() * 20);
        } else adjustCooldown(player, entry);
    }

    // USER: CONSUMABLE, USABLE
    private void runCmdsApplyFX(Player player, ItemEntry entry, ItemStack item) {
        if (!entry.getMessage().isBlank() && !entry.getMessage().isEmpty())
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', entry.getMessage())));
        List<String> effects = entry.getEffects();
        List<String> commands = entry.getCommands();
        boolean removeItem = entry.getRemoveItem();
        if (commands!=null) { 
            for (String command : commands) {
                String cmd = command.replace("<player>", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
        if (effects!=null) {
            boolean appliedEffects = applyEffects(player, effects, item, removeItem);
            if (appliedEffects && removeItem) removeItem(player, item);
        } else if (removeItem) removeItem(player, item);
    }

    // USER: FLAME_PARTICLE
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

        // Shoot flame damage & set blocks on fire
        if (Math.random() < 0.64) {
            Block targetBlock = player.getTargetBlock(null, 24);
            Block targetBlockAbove = targetBlock.getRelative(BlockFace.UP);

            if (targetBlock != null && targetBlockAbove != null) {
                Location location = targetBlockAbove.getLocation();

                if (passDamageChecks(player, location, entry)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (targetBlock.getType().isBlock() && targetBlockAbove.getType() == Material.AIR)
                                targetBlockAbove.setType(Material.FIRE);
                            damageEntities(player, targetBlockAbove.getLocation(), 0.9, 1.5, entry.getDamage(), 60);
                        }
                    }.runTaskLater(item64, 12);
                } else refundPlayer(player, entry);
            }
        }

        // Shot flame visuals 
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
            }.runTaskLater(item64, i);
        }
    }

    // USER: RANDOM_POTION
    private void shootPotion(Player player, ItemEntry entry, Action action) {
        boolean rightClick = !(action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        String effectLine = rightClick ? entry.getREffects().get(ThreadLocalRandom.current().nextInt(entry.getREffects().size()))
                                        : entry.getLEffects().get(ThreadLocalRandom.current().nextInt(entry.getLEffects().size()));
        try {
            String[] parts = effectLine.split(":");
            PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
            int amplifier = Integer.parseInt(parts[1]);
            int duration = Integer.parseInt(parts[2])*20;
            if (effectType != null) {
                potionMeta.addCustomEffect(new PotionEffect(effectType, duration, amplifier), true);
                potion.setItemMeta(potionMeta);
        
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting " + effectType.getName().toLowerCase() + "..."));
                player.getWorld().spawn(player.getLocation().add(0, 1.5, 0), ThrownPotion.class, thrownPotion -> {
                    thrownPotion.setItem(potion);
                    thrownPotion.setBounce(false);
                    thrownPotion.setVelocity(player.getLocation().getDirection().multiply(1.4));
        
                    double random = entry.getRandom();
                    if (random > 0) randomizeProjectile(thrownPotion, random);
                    
                    if (!rightClick) {
                        thrownPotion.setVisualFire(true);
                        if (entry.getDamage() >= 0)
                            thrownPotion.setMetadata("Item64-randomPotion-left", new FixedMetadataValue(item64, "true"));
                    }
                    thrownPotion.setMetadata("Item64-keyString", new FixedMetadataValue(item64, entry.getKeyString()));
                    thrownPotion.setShooter(player);
                    magicPotions.add(thrownPotion);
                });
            } else {
                item64.logRed("Unknown potion effect type: " + parts[0]);
            }
        } catch (Exception e) {
            item64.logRed("Exception while throwing potion: ");
            item64.getLogger().warning(e.getMessage());
        }
    }

    // LANDING LISTENER: EXPLOSIVE_ARROW, LIGHTNING_PEARL
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile instanceof Arrow) {
            if (!explosiveArrows.remove((Arrow) projectile)) return;
        } else if (projectile instanceof EnderPearl) {
            if (!lightningPearls.remove((EnderPearl) projectile)) return;
            event.setCancelled(true);
        } else return;

        Location location = projectile.getLocation();
        Player player = (Player) projectile.getShooter();
        ItemEntry entry = configHandler.getItemEntryByKeyString(projectile.getMetadata("Item64-keyString").get(0).asString());

        if (passDamageChecks(player, location, entry)) {
            damageEntities(player, location, 1.7, 1.2, entry.getDamage(), 30);
            if (projectile instanceof Arrow) 
                projectile.getWorld().createExplosion(location, (float)entry.getPower(), true, true, player);
            else if (projectile instanceof EnderPearl)
                location.getWorld().strikeLightning(location);
        } else refundPlayer(player, entry);

        projectile.remove();
    }

    // LANDING LISTENER: RANDOM_POTION
    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion thrownPotion = (ThrownPotion) event.getEntity();
        if (!magicPotions.contains(thrownPotion)) return;
        magicPotions.remove(thrownPotion);

        Location location = thrownPotion.getLocation();
        Player player = (Player) thrownPotion.getShooter();
        ItemEntry entry = configHandler.getItemEntryByKeyString(thrownPotion.getMetadata("Item64-keyString").get(0).asString());

        if (passDamageChecks(player, location, entry)) {
            if (thrownPotion.hasMetadata("Item64-randomPotion-left"))
                damageEntities(player, location, 0.9, 1.3, entry.getDamage(), 20);
        } else {
            event.setCancelled(true);
            thrownPotion.remove();
            refundPlayer(player, entry);
        }
    }
}