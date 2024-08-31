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
            /* case "EXPLOSIVE_ARROW":
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
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- claim pvp protection!"));
            return;
        }

        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(cooldownIndex)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- active cooldown!"));
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

    private boolean passGDPvpCheck(Location location) {
        if (gdHook==null) return true;
        String regionID = gdHook.getRegionID(location);
        return gdHook.hasPvPEnabled(regionID);
    }

    private boolean passDCPvpCheck(Player sender, Location location, double radius) {
        return sender.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .noneMatch(player -> dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player));
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
            handlePearlHit((EnderPearl) event.getEntity());
        }
    }

    private void handleArrowHit(Arrow arrow) {
        if (explosiveArrows.remove(arrow)) {
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
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion nerfed -- claim block protection!"));
                damagePlayers(player, location, 1.7, 0.7, itemManager.getItemEntry("EXPLOSIVE_ARROW").getDamage());
            } else {
                arrow.getWorld().createExplosion(location, 2.0F, true, true, player);
                arrow.remove();
                damagePlayers(player, location, 1.7, 0.7, itemManager.getItemEntry("EXPLOSIVE_ARROW").getDamage());
            } return;
        } else if (extraArrows.remove(arrow)) {
            Location location = arrow.getLocation();
            Player player = (Player) arrow.getShooter();
            boolean passDCPvpCheck = true, passGDPvpCheck = true;

            if (dcHook != null) {
                if (!passDCPvpCheck(player, location, 5.0)) passDCPvpCheck = false;
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
                arrow.remove();
                damagePlayers(player, location, 0.7, 0.9, itemManager.getItemEntry("EXTRA_ARROW").getDamage());
            } return;
        }
    }

    private void handlePearlHit(EnderPearl pearl) {
        if (!lightningPearls.remove(pearl)) return;

        Location location = pearl.getLocation();
        Player player = (Player) pearl.getShooter();

        if (!passDCPvpCheck(player, location, 4.0) || !passGDPvpCheck(location)) {
            pearl.remove();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lightning blocked -- protection active!"));
            return;
        }

        location.getWorld().strikeLightning(location);
        damagePlayers(player, location, 1.2, 2.0, itemManager.getItemEntry("LIGHTNING_PEARL").getDamage());
        pearl.remove();
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

    private boolean damagePlayers(Player sender, Location location, double hRadius, double vRadius, double damage) {
        Collection<Entity> nearbyEntities = sender.getWorld().getNearbyEntities(location, hRadius, vRadius, hRadius);
        boolean damaged = false;
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                player.damage(damage, sender);
                damaged = true;
            }
        }
        return damaged;
    }
}