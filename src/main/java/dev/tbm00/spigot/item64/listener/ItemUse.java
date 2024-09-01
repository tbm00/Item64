package dev.tbm00.spigot.item64.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import nl.marido.deluxecombat.api.DeluxeCombatAPI;

import dev.tbm00.spigot.item64.ItemManager;
import dev.tbm00.spigot.item64.model.ItemEntry;
import dev.tbm00.spigot.item64.hook.GDHook;

public class ItemUse implements Listener {
    private final JavaPlugin javaPlugin;
    private final ItemManager itemManager;
    private final GDHook gdHook;
    private final DeluxeCombatAPI dcHook;
    private final Boolean enabled;
    private final List<ItemEntry> itemCmdEntries;
    private final Map<UUID, List<Long>> activeCooldowns = new HashMap<>();
    private final ArrayList<Projectile> explosiveArrows = new ArrayList<>();
    private final ArrayList<Projectile> extraArrows = new ArrayList<>();
    private final ArrayList<Projectile> lightningPearls = new ArrayList<>();
    private final ArrayList<Projectile> magicPotions = new ArrayList<>();
    private static final PotionEffectType[] positiveEFFECTS = {
        PotionEffectType.INCREASE_DAMAGE, //STRENGTH
        PotionEffectType.HEAL, //INSTANT_HEALTH
        PotionEffectType.JUMP, //JUMP_BOOST
        PotionEffectType.SPEED,
        PotionEffectType.REGENERATION,
        PotionEffectType.FIRE_RESISTANCE,
        PotionEffectType.NIGHT_VISION,
        PotionEffectType.INVISIBILITY,
        PotionEffectType.ABSORPTION,
        PotionEffectType.SATURATION,
        PotionEffectType.SLOW_FALLING,
    };
    private static final PotionEffectType[] negativeEFFECTS = {
        PotionEffectType.SLOW, //SLOWNESS
        PotionEffectType.HARM, //INSTANT_DAMAGE
        PotionEffectType.CONFUSION, //NAUSEA
        PotionEffectType.BLINDNESS,
        PotionEffectType.HUNGER,
        PotionEffectType.WEAKNESS,
        PotionEffectType.POISON,
        PotionEffectType.LEVITATION,
        PotionEffectType.GLOWING
    };

    public ItemUse(JavaPlugin javaPlugin, ItemManager itemManager, GDHook gdHook, DeluxeCombatAPI dcHook) {
        this.javaPlugin = javaPlugin;
        this.itemManager = itemManager;
        this.gdHook = gdHook;
        this.dcHook = dcHook;
        this.enabled = itemManager.isEnabled();
        this.itemCmdEntries = itemManager.getItemEntries();
    }

    private ItemEntry getItemEntry(ItemStack item) {
        if (item == null) return null;
        if (!item.hasItemMeta() || item.getType() == Material.AIR) return null;
        return itemCmdEntries.stream()
            .filter(entry -> item.getItemMeta().getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING))
            .findFirst()
            .orElse(null);
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item==null || player==null) return;

        ItemEntry entry = getItemEntry(item);
        if (entry == null || !player.hasPermission(entry.getUsePerm())) return;

        switch (entry.getType()) {
            /* case "EXPLOSIVE_ARROW":
                // activeCooldowns index: 0    
                // has custom listeners: onBowShoot & onProjectileHit
                break; */
            /* case "EXTRA_ARROW":
                // activeCooldowns index: 1
                // has custom listeners: onBowShoot & onProjectileHit
                break; */
            case "LIGHTNING_PEARL": {
                // activeCooldowns index: 2
                // has custom listener: onProjectileHit
                triggerLightningPearl(event, player, entry);
                break;
            }
            case "RANDOM_POTION": {
                // activeCooldowns index: 3
                // has custom listener: onPotionSplash
                triggerRandomPotion(event, player, entry);
                break;
            }
            case "FLAME_PARTICLE": {
                // activeCooldowns index: 4
                triggerFlameParticle(event, player, entry);
                break;
            }
            default:
                break;
        }
    }

    private void triggerLightningPearl(PlayerInteractEvent event, Player player, ItemEntry entry) {
        if (!passGDPvpCheck(player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- claim pvp protection!"));
            event.setCancelled(true);
            return;
        }

        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- you're too hungry!"));
            event.setCancelled(true);
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(2)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- active cooldown!"));
            event.setCancelled(true);
            return;
        }

        if (removeAmmoItem(player, entry.getType())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting lightning pearl..."));
            EnderPearl pearl = player.launchProjectile(EnderPearl.class);
            lightningPearls.add(pearl);

            double random = entry.getRandom();
            if (random > 0) randomizeVelocity(pearl, random);
            adjustCooldowns(player, itemManager.getHungers(), 2);
        }
        event.setCancelled(true);
    }

    private void triggerRandomPotion(PlayerInteractEvent event, Player player, ItemEntry entry) {
        if (!passGDPvpCheck(player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- claim pvp protection!"));
            event.setCancelled(true);
            return;
        }

        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- you're too hungry!"));
            event.setCancelled(true);
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(3)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- active cooldown!"));
            event.setCancelled(true);
            return;
        }

        if (removeAmmoItem(player, entry.getType())) {
            Action action = event.getAction();
            boolean rightClick = !(action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
            shootPotion(player, rightClick);
            adjustCooldowns(player, itemManager.getHungers(), 3);
        }
        event.setCancelled(true);
    }

    private void triggerFlameParticle(PlayerInteractEvent event, Player player, ItemEntry entry) {
        if (!passGDPvpCheck(player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- claim pvp protection!"));
            event.setCancelled(true);
            return;
        }

        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- you're too hungry!"));
            event.setCancelled(true);
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(4)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- active cooldown!"));
            event.setCancelled(true);
            return;
        }

        if (removeAmmoItem(player, entry.getType())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting flames..."));
            shootFlames(player, entry);
            adjustHunger(player, itemManager.getHungers(), 4);
            new BukkitRunnable() {
                @Override
                public void run() {
                    adjustCooldown(player, 4);
                }
            }.runTaskLater(javaPlugin, entry.getCooldown()*20);
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Arrow arrow = (Arrow) event.getProjectile();
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemEntry entry = getItemEntry(item);
        if (entry == null) return;

        String entryType = entry.getType();
        double random = entry.getRandom();

        if (random > 0) randomizeVelocity(arrow, random);

        if (entryType.equalsIgnoreCase("EXPLOSIVE_ARROW")) {
            handleArrowShoot(player, arrow, entry, 0, "Shooting explosive arrow...", explosiveArrows);
        } else if (entryType.equalsIgnoreCase("EXTRA_ARROW") && entry.getDamage() > 0) {
            handleArrowShoot(player, arrow, entry, 1, "Shooting stronger arrow...", extraArrows);
        }
    }

    private void handleArrowShoot(Player player, Arrow arrow, ItemEntry entry, int cooldownIndex, String message, List<Projectile> arrowList) {
        if (!player.hasPermission(entry.getUsePerm())) return;
        if (!passGDPvpCheck(player.getLocation())) {
            if (entry.getType().equalsIgnoreCase("EXPLOSIVE_ARROW"))
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- claim pvp protection!"));
            else player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Extra damage blocked -- claim pvp protection!"));
            return;
        }

        if (player.getFoodLevel() < entry.getHunger()) {
            if (entry.getType().equalsIgnoreCase("EXPLOSIVE_ARROW"))
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- you're too hungry!"));
            else player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Extra damage blocked -- you're too hungry!!")); 
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(cooldownIndex)) < entry.getCooldown()) {
            if (entry.getType().equalsIgnoreCase("EXPLOSIVE_ARROW"))
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- active cooldown!"));
            else player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Extra damage blocked -- active cooldown!")); 
            return;
        }

        if (removeAmmoItem(player, entry.getType())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + message));
            arrowList.add(arrow);
            adjustCooldowns(player, itemManager.getHungers(), cooldownIndex);
        }
    }

    private void shootPotion(Player player, boolean rightClick) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        PotionEffectType effectType = rightClick ? positiveEFFECTS[ThreadLocalRandom.current().nextInt(positiveEFFECTS.length)]
                                                 : negativeEFFECTS[ThreadLocalRandom.current().nextInt(negativeEFFECTS.length)];
        potionMeta.addCustomEffect(new PotionEffect(effectType, 600, 1), true);
        potion.setItemMeta(potionMeta);

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Casting " + effectType.getName().toLowerCase() + "..."));
        player.getWorld().spawn(player.getLocation().add(0, 1.5, 0), ThrownPotion.class, thrownPotion -> {
            thrownPotion.setItem(potion);
            thrownPotion.setBounce(false);
            thrownPotion.setVelocity(player.getLocation().getDirection().multiply(1.4));
            ItemEntry entry = itemManager.getItemEntry("RANDOM_POTION");

            double random = entry.getRandom();
            if (random > 0) randomizeVelocity(thrownPotion, random);
            
            if (!rightClick) {
                thrownPotion.setVisualFire(true);
                if (entry.getDamage()>=0)
                    thrownPotion.setMetadata("randomPotionLeft", new FixedMetadataValue(javaPlugin, "true"));
            }
            thrownPotion.setShooter((ProjectileSource)player);
            magicPotions.add(thrownPotion);
        });
    }

    private void shootFlames(Player player, ItemEntry entry) {
        double damageChance = 0.42;
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
        for (int i = 0; i < 4; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Vector offsetPath = playerDirection.clone();
                    Location offsetLocation = particleLocation.clone();
                    if (random > 0) {
                        randomizeLocation(offsetLocation, random);
                        randomizeVelocity(offsetPath, random/2);
                    }
                    offsetLocation.add(0, 0.3, 0);
                    player.getWorld().spawnParticle(Particle.FLAME, offsetLocation, 0, offsetPath.getX(), offsetPath.getY(), offsetPath.getZ(), 0.1);
                }
            }.runTaskLater(javaPlugin, 2*i);
        }

        // Set fire & extraDamage
        if (Math.random() < damageChance) {
            Block targetBlock = player.getTargetBlock(null, 20);
            Block targetBlockAbove = targetBlock.getRelative(BlockFace.UP);

            if (targetBlock != null && targetBlockAbove != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location location = targetBlockAbove.getLocation();
                        boolean passGDPvpCheck = true, passDCPvpCheck = true;
                        if (gdHook != null) {
                            if (!passGDPvpCheck(location)) passGDPvpCheck = false;
                        }
                        if (dcHook != null) {
                            if (!passDCPvpLocCheck(location, 3.0)) passDCPvpCheck = false;
                            if (!passDCPvpPlayerCheck(player)) passDCPvpCheck = false;
                        }
                
                        if (!passDCPvpCheck) {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- pvp protection!"));
                        } else if (!passGDPvpCheck) {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- claim pvp protection!"));
                        } else {
                            if (targetBlock.getType().isBlock() && targetBlockAbove.getType() == Material.AIR) {
                                targetBlockAbove.setType(Material.FIRE);
                            }
                            damagePlayers(player, targetBlockAbove.getLocation(), 1.5, 1.6, entry.getDamage(), 60);
                        }
                    }
                }.runTaskLater(javaPlugin, 10);
            }
        }
    }

    private void adjustCooldown(Player player, int index) {
        List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
        playerCooldowns.set(index, System.currentTimeMillis() / 1000);
        activeCooldowns.put(player.getUniqueId(), playerCooldowns);
    }

    private void adjustCooldowns(Player player, List<Integer> hungers, int index) {
        List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
        player.setFoodLevel(Math.max(player.getFoodLevel() - hungers.get(index), 0));
        playerCooldowns.set(index, System.currentTimeMillis() / 1000);
        activeCooldowns.put(player.getUniqueId(), playerCooldowns);
    }

    private void adjustHunger(Player player, List<Integer> hungers, int index) {
        player.setFoodLevel(Math.max(player.getFoodLevel() - hungers.get(index), 0));
    }

    private boolean removeAmmoItem(Player player, String type) {
        ItemEntry entry = itemManager.getItemEntry(type);

        String ammoItemName = entry.getAmmoItem();
        if (ammoItemName.equals("none")) return true;

        Material ammoMaterial = Material.getMaterial(ammoItemName.toUpperCase());
        if (ammoMaterial == null) {
            javaPlugin.getLogger().warning("Error: Poorly defined itemEntry: " + type + " " + ammoItemName);
            return false;
        }
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == ammoMaterial) {
                itemStack.setAmount(Math.max(itemStack.getAmount() - 1, 0));
                if (itemStack.getAmount() <= 0) {
                    player.getInventory().remove(itemStack);
                } return true;
            }
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + ammoItemName.toLowerCase()));
        return false;
    }

    private void randomizeVelocity(Projectile projectile, double random) {
        Vector velocity = projectile.getVelocity();
        velocity.add(new Vector(
            ThreadLocalRandom.current().nextDouble(-random, random),
            ThreadLocalRandom.current().nextDouble(-random / 2, random / 2),
            ThreadLocalRandom.current().nextDouble(-random, random)
        ));
        projectile.setVelocity(velocity);
    }

    private void randomizeVelocity(Vector velocity, double random) {
        velocity.add(new Vector(
            ThreadLocalRandom.current().nextDouble(-random, random),
            ThreadLocalRandom.current().nextDouble(-random / 2, random / 2),
            ThreadLocalRandom.current().nextDouble(-random, random)
        ));
    }

    private void randomizeLocation(Location location, double random) {
        location.add(new Vector(
            ThreadLocalRandom.current().nextDouble(-random, random),
            ThreadLocalRandom.current().nextDouble(-random / 2, random / 2),
            ThreadLocalRandom.current().nextDouble(-random, random)
        ));
    }

    private boolean passGDPvpCheck(Location location) {
        if (gdHook==null) return true;
        String regionID = gdHook.getRegionID(location);
        return gdHook.hasPvPEnabled(regionID);
    }

    private boolean passDCPvpLocCheck(Location location, double radius) {
        return location.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .noneMatch(player -> dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player));
    }

    
    private boolean passDCPvpPlayerCheck(Player player) {
        if (dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player)) return false;
        return true;
    }

    private boolean passGDBuilderCheck(Player sender, Location location, int radius) {
        String[] ids = new String[9];
        Location loc = location.clone();
    
        ids[0] = gdHook.getRegionID(loc);
        ids[1] = gdHook.getRegionID(loc.add(radius, radius, radius));
        ids[2] = gdHook.getRegionID(loc.add(0, 0, (-2*radius)));
        ids[3] = gdHook.getRegionID(loc.add((-2*radius), 0, 0));
        ids[4] = gdHook.getRegionID(loc.add(0, 0, (2*radius)));
        ids[5] = gdHook.getRegionID(loc.add(0, (-2*radius), 0));
        ids[6] = gdHook.getRegionID(loc.add((2*radius), 0, 0));
        ids[7] = gdHook.getRegionID(loc.add(0, 0, (-2*radius)));
        ids[8] = gdHook.getRegionID(loc.add((-2*radius), 0, 0));

        for (String id : ids) {
            if (!gdHook.hasBuilderTrust(sender, id)) return false;
        } return true;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            handleArrowHit((Arrow) event.getEntity());
        } else if (event.getEntity() instanceof EnderPearl) {
            handlePearlHit(event, (EnderPearl) event.getEntity());
        }
    }

    private void handleArrowHit(Arrow arrow) {
        if (explosiveArrows.remove(arrow)) {
            Location location = arrow.getLocation();
            Player player = (Player) arrow.getShooter();
            boolean passDCPvpCheck = true, passGDPvpCheck = true, passGDBuilderCheck = true;

            if (dcHook != null) {
                if (!passDCPvpLocCheck(location, 5.0)) passDCPvpCheck = false;
            }
            if (gdHook != null) {
                if (!passGDPvpCheck(location)) passGDPvpCheck = false;
                else if (!passGDBuilderCheck(player, location, 5)) passGDBuilderCheck = false;
            }

            if (!passDCPvpCheck) {
                arrow.remove();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- pvp protection!"));
            } else if (!passGDPvpCheck) {
                arrow.remove();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- claim pvp protection!"));
            } else if (!passGDBuilderCheck) {
                damagePlayers(player, location, 1.7, 1.2, itemManager.getItemEntry("EXPLOSIVE_ARROW").getDamage(), 0);
                arrow.getWorld().createExplosion(location, 2.0F, true, false, player);
                arrow.remove();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion nerfed -- claim block protection!"));
            } else {
                damagePlayers(player, location, 1.7, 1.2, itemManager.getItemEntry("EXPLOSIVE_ARROW").getDamage(), 0);
                arrow.getWorld().createExplosion(location, 2.0F, true, true, player);
                arrow.remove();
            } return;
        } else if (extraArrows.remove(arrow)) {
            Location location = arrow.getLocation();
            Player player = (Player) arrow.getShooter();
            boolean passDCPvpCheck = true, passGDPvpCheck = true;

            if (dcHook != null) {
                if (!passDCPvpLocCheck(location, 5.0)) passDCPvpCheck = false;
            }
            if (gdHook != null) {
                if (!passGDPvpCheck(location)) passGDPvpCheck = false; 
            }

            if (!passDCPvpCheck) {
                arrow.remove();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Extra damage blocked -- pvp protection!"));
            } else if (!passGDPvpCheck) {
                arrow.remove();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Extra damage blocked -- claim pvp protection!"));
            } else {
                damagePlayers(player, location, 0.8, 1.2, itemManager.getItemEntry("EXTRA_ARROW").getDamage(), 0);
                arrow.remove();
            } return;
        }
    }

    private void handlePearlHit(ProjectileHitEvent event, EnderPearl pearl) {
        if (!lightningPearls.remove(pearl)) return;

        Location location = pearl.getLocation();
        Player player = (Player) pearl.getShooter();
        boolean passDCPvpCheck = true, passGDPvpCheck = true;
        
        if (gdHook != null) {
            if (!passGDPvpCheck(location)) passGDPvpCheck = false;
        }
        if (dcHook != null) {
            if (!passDCPvpLocCheck(location, 4.0)) passDCPvpCheck = false;
        }

        if (!passDCPvpCheck) {
            event.setCancelled(true);
            pearl.remove();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lightning blocked -- pvp protection!"));
        } else if (!passGDPvpCheck) {
            event.setCancelled(true);
            pearl.remove();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lightning blocked -- claim pvp protection!"));
        } else {
            event.setCancelled(true);
            damagePlayers(player, location, 1.2, 3.0, itemManager.getItemEntry("LIGHTNING_PEARL").getDamage(), 0);
            location.getWorld().strikeLightning(location);
            pearl.remove();
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion thrownPotion = (ThrownPotion) event.getEntity();
        if (!magicPotions.contains(thrownPotion)) return;
        magicPotions.remove(thrownPotion);

        Location location = thrownPotion.getLocation();
        Player player = (Player) thrownPotion.getShooter();
        boolean passDCPvpCheck = true, passGDPvpCheck = true;
            
        if (gdHook != null) {
            if (!passGDPvpCheck(location)) passGDPvpCheck = false;
        }
        if (dcHook != null) {
            if (!passDCPvpLocCheck(location, 4.0)) passDCPvpCheck = false;
        }

        if (!passDCPvpCheck) {
            event.setCancelled(true);
            thrownPotion.remove();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- pvp protection!"));
            return;
        } else if (!passGDPvpCheck) {
            event.setCancelled(true);
            thrownPotion.remove();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- claim pvp protection!"));
            return;
        }

        if (thrownPotion.hasMetadata("randomPotionLeft")) {
            damagePlayers(player, location, 0.9, 1.3, itemManager.getItemEntry("RANDOM_POTION").getDamage(), 20);
        }
    }

    private boolean damagePlayers(Player sender, Location location, double hRadius, double vRadius, double damage, int ignite) {
        Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, hRadius, vRadius, hRadius);
        boolean damaged = false;
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                player.damage(damage, sender);
                if (ignite>0) player.setFireTicks(ignite);
                //player.sendMessage(ChatColor.RED + "hit: " + ChatColor.GRAY + damage + ChatColor.RED + ", by: " + ChatColor.GRAY + sender);
                //sender.sendMessage(ChatColor.RED + "hit: " + ChatColor.GRAY + damage + ChatColor.RED + ", on: " + ChatColor.GRAY + player);
                damaged = true;
            }
        }
        return damaged;
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getItemMeta() == null) return;
        for (ItemEntry entry : itemCmdEntries) {
            if (item.getItemMeta().getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}