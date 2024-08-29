package dev.tbm00.spigot.item64.listener;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.event.block.Action;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
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
        //PotionEffectType.FAST_DIGGING, //HASTE
        //PotionEffectType.LUCK,
        //PotionEffectType.WATER_BREATHING,
        //PotionEffectType.DOLPHINS_GRACE
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
        //PotionEffectType.SLOW_DIGGING, //MINING FATIGUE
        //PotionEffectType.UNLUCK, //BAD_LUCK
        //DARKNESS
        //INFESTED
    };

    public ItemUse(JavaPlugin javaPlugin, ItemManager itemManager, GDHook gdHook, DeluxeCombatAPI dcHook) {
        this.javaPlugin = javaPlugin;
        this.itemManager = itemManager;
        this.gdHook = gdHook;
        this.dcHook = dcHook;
        this.enabled = itemManager.isEnabled();
        this.itemCmdEntries = itemManager.getItemEntries();
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item==null || player==null) return;

        ItemEntry entry = getItemEntry(item);
        if (entry == null) return;

        if (!player.hasPermission(entry.getUsePerm())) return;

        String entryType = entry.getType();
        

        switch (entryType) {
            /* case "EXPLOSIVE_ARROW":
                // has custom listeners: onBowShoot & onProjectileHit
                // activeCooldowns index: 0
                break; */
            case "LIGHTNING_PEARL": {
                // activeCooldowns index: 1
                if (!activeCooldowns.containsKey(player.getUniqueId())) activeCooldowns.put(player.getUniqueId(), itemManager.getCooldowns());
                List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
                if (passGDPvpCheck(player.getLocation())) {
                    if (player.getFoodLevel() >= entry.getHunger()) {
                        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(1)) >= entry.getCooldown()) {
                            if (removeAmmoItem(player, entryType)) {
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting lightning pearl..."));
                                EnderPearl pearl = player.launchProjectile(EnderPearl.class);
                                lightningPearls.add(pearl);

                                double random = entry.getRandom();
                                if (random > 0) randomizeVelocity(pearl, random);

                                adjustCooldowns(player, itemManager.getHungers(), 1);
                            }
                        } else {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- active cooldown!"));
                        }
                    } else {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- you're too hungry!"));
                    }
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- claim pvp protection!"));
                }
                event.setCancelled(true);
                break;
            }
            case "RANDOM_POTION": {
                // activeCooldowns index: 2
                if (!activeCooldowns.containsKey(player.getUniqueId())) activeCooldowns.put(player.getUniqueId(), itemManager.getCooldowns());
                List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
                if (passGDPvpCheck(player.getLocation())) {
                    if (player.getFoodLevel() >= entry.getHunger()) {
                        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(2)) >= entry.getCooldown()) {
                            if (removeAmmoItem(player, entryType)) {
                                //player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Using magic..."));
                                Action action = event.getAction();
                                boolean rightClick = true;
                                if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) rightClick = false;
                                shootPotion(player, rightClick);

                                adjustCooldowns(player, itemManager.getHungers(), 2);
                            }
                        } else {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- active cooldown!"));
                        }
                    } else {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- you're too hungry!"));
                    }
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- claim pvp protection!"));
                }
                event.setCancelled(true);
                break;
            }
            default:
                break;
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Arrow arrow = (Arrow) event.getProjectile();
            ItemStack item = player.getInventory().getItemInMainHand();
            ItemEntry entry = getItemEntry(item);
            
            if (entry != null && entry.getType().equalsIgnoreCase("EXPLOSIVE_ARROW") ) {
                double random = entry.getRandom();
                if (random > 0) randomizeVelocity(arrow, random);
                if (player.hasPermission(entry.getUsePerm())) {
                    if (!activeCooldowns.containsKey(player.getUniqueId())) activeCooldowns.put(player.getUniqueId(), itemManager.getCooldowns());
                    List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
                    
                    if (passGDPvpCheck(player.getLocation())) {
                        if (player.getFoodLevel() >= entry.getHunger()) {
                            if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(0)) >= entry.getCooldown()) {
                                if (removeAmmoItem(player, entry.getType())) {
                                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting explosive arrow..."));
                                    explosiveArrows.add(arrow);
                                    adjustCooldowns(player, itemManager.getHungers(), 0);
                                }
                            } else {
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Exploson blocked -- active cooldown!"));
                            }
                        } else {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- you're too hungry!"));
                        }
                    } else {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- claim pvp protection!"));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            if (!explosiveArrows.contains(arrow)) return;
            explosiveArrows.remove(arrow);

            Location location = arrow.getLocation();
            Player player = (Player) arrow.getShooter();
            boolean passDCPvpCheck = true, passGDPvpCheck = true, passGDBuilderCheck = true;

            if (dcHook != null) {
                if (!passDCPvpCheck(player, location, 5.0)) passDCPvpCheck = false;
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
                arrow.getWorld().createExplosion(location, 2.0F, true, false, player);
                arrow.remove();
                damagePlayers(player, location, 1.7, 0.7, itemManager.getItemEntry("EXPLOSIVE_ARROW").getDamage());
            } else {
                arrow.getWorld().createExplosion(location, 2.0F, true, true, player);
                arrow.remove();
                damagePlayers(player, location, 1.7, 0.7, itemManager.getItemEntry("EXPLOSIVE_ARROW").getDamage());
            }
        } else if (event.getEntity() instanceof EnderPearl) {
            EnderPearl pearl = (EnderPearl) event.getEntity();
            if (!lightningPearls.contains(pearl)) return;
            lightningPearls.remove(pearl);
            
            Location location = pearl.getLocation();
            Player player = (Player) pearl.getShooter();
            boolean passDCPvpCheck = true, passGDPvpCheck = true;
            
            if (gdHook != null) {
                if (!passGDPvpCheck(location)) passGDPvpCheck = false;
            }
            if (dcHook != null) {
                if (!passDCPvpCheck(player, location, 4.0)) passDCPvpCheck = false;
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
                location.getWorld().strikeLightning(location);
                damagePlayers(player, location, 1.2, 2.0, itemManager.getItemEntry("LIGHTNING_PEARL").getDamage());
                //location.getWorld().getNearbyEntities();
                pearl.remove();
            }
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
            if (!passDCPvpCheck(player, location, 4.0)) passDCPvpCheck = false;
        }

        if (!passDCPvpCheck) {
            event.setCancelled(true);
            thrownPotion.remove();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- pvp protection!"));
        } else if (!passGDPvpCheck) {
            event.setCancelled(true);
            thrownPotion.remove();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- claim pvp protection!"));
        }

        if (thrownPotion.hasMetadata("randomPotionLeft")) {
            damagePlayers(player, location, 0.8, 1, itemManager.getItemEntry("RANDOM_POTION").getDamage());
        }
    }

    private void shootPotion(Player player, boolean rightClick) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        if (rightClick) {
            int index = ThreadLocalRandom.current().nextInt(0, 11);
            potionMeta.addCustomEffect(new PotionEffect(positiveEFFECTS[index], 600, 1), true);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Casting " + positiveEFFECTS[index].getName().toLowerCase() + "..."));
        } else {
            int index = ThreadLocalRandom.current().nextInt(0, 9);
            potionMeta.addCustomEffect(new PotionEffect(negativeEFFECTS[index], 600, 1), true);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Casting " + negativeEFFECTS[index].getName().toLowerCase() + "..."));
        }
        potion.setItemMeta((ItemMeta)potionMeta);
        player.getWorld().spawn(player.getLocation().add(0, 1.5, 0), ThrownPotion.class, thrownPotion -> {
            thrownPotion.setItem(potion);
            thrownPotion.setBounce(false);

            Vector direction = player.getLocation().getDirection();
            direction.multiply(1.4);
            thrownPotion.setVelocity(direction);
            ItemEntry entry = itemManager.getItemEntry("RANDOM_POTION");

            double random = entry.getRandom();
            if (random > 0) randomizeVelocity(thrownPotion, random);
            
            if (!rightClick) {
                thrownPotion.setVisualFire(true);
                if (entry.getDamage()>=0) {
                    thrownPotion.setMetadata("randomPotionLeft", new FixedMetadataValue(javaPlugin, "true"));
                }
            }

            thrownPotion.setShooter((ProjectileSource)player);
            magicPotions.add(thrownPotion);
        });
    }

    private ItemEntry getItemEntry(ItemStack item) {
        if (item == null) return null;
        if (!item.hasItemMeta() || item.getType() == Material.AIR) return null;
        for (ItemEntry entry : itemCmdEntries) {
            if (item.getItemMeta().getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING)) return entry;
        }
        return null;
    }

    private void adjustCooldowns(Player player, List<Integer> hungers, int index) {
        List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
        player.setFoodLevel(Math.max(player.getFoodLevel() - hungers.get(index), 0));
        playerCooldowns.set(index, System.currentTimeMillis() / 1000);
        activeCooldowns.put(player.getUniqueId(), playerCooldowns);
    }

    private boolean removeAmmoItem(Player player, String type) {
        ItemEntry entry = itemManager.getItemEntry(type);
        String ammoItemName = entry.getAmmoItem();
        if (ammoItemName.equals("none")) return true;
        Material ammoMaterial = Material.getMaterial(ammoItemName.toUpperCase());
    
        if (ammoMaterial != null) {
            ItemStack[] inventoryContents = player.getInventory().getContents();
            for (int i = 0; i < inventoryContents.length; i++) {
                ItemStack itemStack = inventoryContents[i];
                if (itemStack != null && itemStack.getType() == ammoMaterial) {
                    itemStack.setAmount(Math.max((itemStack.getAmount() - 1), 0));
                    if (itemStack.getAmount() <= 0) {
                        player.getInventory().setItem(i, null);
                    } return true;
                }
            }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + ammoItemName.toLowerCase()));
            return false;
        } else {
            javaPlugin.getLogger().warning("Error: Poorly defined itemEntry:  " + type + " " + ammoItemName);
            return false;
        }
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

    private boolean passGDPvpCheck(Location location) {
        String regionID = gdHook.getRegionID(location);
        return gdHook.hasPvPEnabled(regionID);
    }

    private boolean passDCPvpCheck(Player sender, Location location, double radius) {
        Collection<Entity> nearbyEntities = sender.getWorld().getNearbyEntities(location, radius, radius, radius);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player)) return false;
            }
        }
        return true;
    }

    private boolean damagePlayers(Player sender, Location location, double hRadius, double vRadius, double damage) {
        Collection<Entity> nearbyEntities = sender.getWorld().getNearbyEntities(location, hRadius, vRadius, hRadius);
        int count = 0;
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                player.damage(damage, sender);
                //player.sendMessage(ChatColor.BLUE + "damagePlayers: hR-0.8, vR-1, d-" + damage);
                ++count;
                //player.sendMessage(ChatColor.DARK_BLUE + "count: " + count);
            }
        }
        if (count>0) return true;
        return false;
    }

    private void randomizeVelocity(Projectile projectile, double random) {
        Vector velocity = projectile.getVelocity();
        
        double randomX = ThreadLocalRandom.current().nextDouble(-random, random),
                randomY = ThreadLocalRandom.current().nextDouble(-random/2, random/2),
                randomZ = ThreadLocalRandom.current().nextDouble(-random, random);
        
        velocity.setX(velocity.getX() + randomX);
        velocity.setY(velocity.getY() + randomY);
        velocity.setZ(velocity.getZ() + randomZ);

        projectile.setVelocity(velocity);
    }
}