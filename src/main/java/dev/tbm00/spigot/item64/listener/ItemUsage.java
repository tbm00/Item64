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

import dev.tbm00.spigot.item64.Item64;
import dev.tbm00.spigot.item64.UsageHelper;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class ItemUsage implements Listener {
    private UsageHelper usageHelper;

    public ItemUsage(Item64 item64, UsageHelper usageHelper) {
        this.usageHelper = usageHelper;
    }

    // TRIGGER: ALL
    private void triggerUsage(Player player, ItemEntry entry, ItemStack item, Action action, Projectile projectile) {
        if (!usageHelper.passUsageChecks(player, entry)) return;

        // Use the item
        switch (entry.getType()) {
            case "USABLE":
                runCmdsApplyFX(player, entry, item);
                break;
            case "CONSUMABLE":
                runCmdsApplyFX(player, entry, item);
                break;
            case "EXPLOSIVE_ARROW":
                if (!usageHelper.passShootingChecks(player, entry)) return;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting explosive arrow..."));
                shootExplosiveArrow(player, entry, projectile);
                break;
            case "LIGHTNING_PEARL":
                if (!usageHelper.passShootingChecks(player, entry)) return;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting lightning pearl..."));
                shootLightningPearl(player, entry);
                break;
            case "FLAME_PARTICLE":
                if (!usageHelper.passShootingChecks(player, entry)) return;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting flames..."));
                shootFlameParticles(player, entry);
                break;
            case "RANDOM_POTION":
                boolean leftClick = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
                if (leftClick && !usageHelper.passShootingChecks(player, entry)) return;
                if (!shootRandomPotion(player, entry, leftClick)) return;
                break;
            default: 
                break;
        }

        // Remove hunger, ammo, and money
        if (usageHelper.getEcoHook() != null && entry.getMoney() > 0 && !usageHelper.removeMoney(player, entry.getMoney()))
            usageHelper.getItem64().logRed("Error: failed to remove money for " + player.getName() + "'s " + entry.getKeyString() + " usage!");
        if (entry.getRemoveAmmo()) usageHelper.removeItem(player, entry.getAmmoItem());
        usageHelper.adjustHunger(player, entry);

        // Set cooldowns
        if (entry.getType().equals("FLAME_PARTICLE")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    usageHelper.adjustCooldown(player, entry);
                }
            }.runTaskLater(usageHelper.getItem64(), entry.getCooldown() * 20);
        } else usageHelper.adjustCooldown(player, entry);
    }

    // USER: CONSUMABLE, USABLE
    private void runCmdsApplyFX(Player player, ItemEntry entry, ItemStack item) {
        if (!entry.getMessage().isBlank() && !entry.getMessage().isEmpty())
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', entry.getMessage())));
        List<String> effects = entry.getEffects();
        List<String> commands = entry.getCommands();
        if (commands!=null) {
            for (String command : commands) {
                String cmd = command.replace("<player>", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
        if (effects!=null)
            usageHelper.applyEffects(player, effects, item);
        
        if (entry.getRemoveItem()) usageHelper.removeItem(player, item);
    }

    // USER: EXPLOSIVE_ARROW
    private void shootExplosiveArrow(Player player, ItemEntry entry, Projectile projectile) {
        double random = entry.getRandom();
        projectile.setMetadata("Item64-keyString", new FixedMetadataValue(usageHelper.getItem64(), entry.getKeyString()));
        usageHelper.getExplosiveArrows().add(projectile);
        if (random > 0) usageHelper.randomizeProjectile(projectile, random);
    }

    // USER: LIGHTNING_PEARL
    private void shootLightningPearl(Player player, ItemEntry entry) {
        double random = entry.getRandom();
        EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        pearl.setMetadata("Item64-keyString", new FixedMetadataValue(usageHelper.getItem64(), entry.getKeyString()));
        usageHelper.getLightningPearls().add(pearl);
        if (random > 0) usageHelper.randomizeProjectile(pearl, random);
    }

    // USER: FLAME_PARTICLE
    private void shootFlameParticles(Player player, ItemEntry entry) {
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

                if (usageHelper.passDamageChecks(player, location, entry)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (targetBlock.getType().isBlock() && targetBlockAbove.getType() == Material.AIR)
                                targetBlockAbove.setType(Material.FIRE);
                            usageHelper.damageEntities(player, targetBlockAbove.getLocation(), 0.9, 1.5, entry.getDamage(), 60);
                        }
                    }.runTaskLater(usageHelper.getItem64(), 12);
                } else usageHelper.refundPlayer(player, entry);
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
                            usageHelper.randomizeLocation(offsetLocation, random);
                            usageHelper.randomizeVelocity(offsetPath, random / 2);
                        }
                        offsetLocation.add(0, 0.3, 0);
                        player.getWorld().spawnParticle(Particle.FLAME, offsetLocation, 0, offsetPath.getX(), offsetPath.getY(), offsetPath.getZ(), 0.1);
                    }
                }
            }.runTaskLater(usageHelper.getItem64(), i);
        }
    }

    // USER: RANDOM_POTION
    private boolean shootRandomPotion(Player player, ItemEntry entry, boolean leftClick) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        String effectLine = leftClick ? entry.getLEffects().get(ThreadLocalRandom.current().nextInt(entry.getLEffects().size()))
                                      : entry.getREffects().get(ThreadLocalRandom.current().nextInt(entry.getREffects().size()));
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
                    if (random > 0) usageHelper.randomizeProjectile(thrownPotion, random);
                    
                    if (leftClick) {
                        thrownPotion.setVisualFire(true);
                        if (entry.getDamage() >= 0)
                            thrownPotion.setMetadata("Item64-randomPotion-left", new FixedMetadataValue(usageHelper.getItem64(), "true"));
                    }
                    thrownPotion.setMetadata("Item64-keyString", new FixedMetadataValue(usageHelper.getItem64(), entry.getKeyString()));
                    thrownPotion.setShooter(player);
                    usageHelper.getMagicPotions().add(thrownPotion);
                });
                return true;
            } else {
                usageHelper.getItem64().logRed("Unknown potion effect type: " + parts[0]);
                return false;
            }
        } catch (Exception e) {
            usageHelper.getItem64().logRed("Exception while throwing potion: ");
            usageHelper.getItem64().getLogger().warning(e.getMessage());
            return false;
        }
    }

    // USE LISTENER: CONSUMABLE
    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || player == null) return;

        ItemEntry entry = usageHelper.getItemEntryByItem(item);
        if (entry == null || !entry.getType().equalsIgnoreCase("CONSUMABLE") || !player.hasPermission(entry.getUsePerm()))
            return;

        event.setCancelled(true);

        triggerUsage(player, entry, item, null, null);
    }

    // USE LISTENER: USABLE, FLAME_PARTICLE, LIGHTNING_PEARL, RANDOM_POTION
    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || player == null) return;
        
        ItemEntry entry = usageHelper.getItemEntryByItem(item);
        if (entry == null || !player.hasPermission(entry.getUsePerm()))
            return;

        String type = entry.getType();
        if (type.equalsIgnoreCase("EXPLOSIVE_ARROW") || type.equalsIgnoreCase("CONSUMABLE") || type.equalsIgnoreCase("NO_ITEM"))
            return;
        
        event.setCancelled(true);

        Action action = event.getAction();
        triggerUsage(player, entry, item, action, null);
    }

    // USE LISTENER: EXPLOSIVE_ARROW
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (player == null || item == null) return;

        ItemEntry entry = usageHelper.getItemEntryByItem(item);
        if (entry == null || !player.hasPermission(entry.getUsePerm()) || 
        (!entry.getType().equalsIgnoreCase("EXPLOSIVE_ARROW"))) 
            return;

        Arrow arrow = (Arrow) event.getProjectile();
        if (arrow == null) return;

        triggerUsage(player, entry, null, null, arrow);
    }

    // LANDING LISTENER: EXPLOSIVE_ARROW, LIGHTNING_PEARL
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile instanceof Arrow) {
            if (!usageHelper.getExplosiveArrows().remove((Arrow) projectile)) return;
        } else if (projectile instanceof EnderPearl) {
            if (!usageHelper.getLightningPearls().remove((EnderPearl) projectile)) return;
            event.setCancelled(true);
        } else return;

        Location location = projectile.getLocation();
        Player player = (Player) projectile.getShooter();
        ItemEntry entry = usageHelper.getConfigHandler().getItemEntryByKeyString(projectile.getMetadata("Item64-keyString").get(0).asString());

        if (usageHelper.passDamageChecks(player, location, entry)) {
            usageHelper.damageEntities(player, location, 1.7, 1.2, entry.getDamage(), 30);
            if (projectile instanceof Arrow) 
                projectile.getWorld().createExplosion(location, (float)entry.getPower(), true, true, player);
            else if (projectile instanceof EnderPearl)
                location.getWorld().strikeLightning(location);
        } else usageHelper.refundPlayer(player, entry);

        projectile.remove();
    }

    // LANDING LISTENER: RANDOM_POTION
    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion thrownPotion = (ThrownPotion) event.getEntity();
        if (!usageHelper.getMagicPotions().contains(thrownPotion)) return;
        usageHelper.getMagicPotions().remove(thrownPotion);

        Location location = thrownPotion.getLocation();
        Player player = (Player) thrownPotion.getShooter();
        ItemEntry entry = usageHelper.getConfigHandler().getItemEntryByKeyString(thrownPotion.getMetadata("Item64-keyString").get(0).asString());

        if (usageHelper.passDamageChecks(player, location, entry)) {
            if (thrownPotion.hasMetadata("Item64-randomPotion-left"))
                usageHelper.damageEntities(player, location, 0.9, 1.3, entry.getDamage(), 20);
        } else {
            event.setCancelled(true);
            thrownPotion.remove();
            usageHelper.refundPlayer(player, entry);
        }
    }
}