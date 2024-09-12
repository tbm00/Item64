package dev.tbm00.spigot.item64.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
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
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.ItemMeta;
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
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import nl.marido.deluxecombat.api.DeluxeCombatAPI;

import dev.tbm00.spigot.item64.ItemManager;
import dev.tbm00.spigot.item64.model.ItemEntry;
import dev.tbm00.spigot.item64.hook.GDHook;

public class ItemUse implements Listener {
    private final JavaPlugin javaPlugin;
    private final ItemManager itemManager;
    private final Economy ecoHook;
    private final GDHook gdHook;
    private final DeluxeCombatAPI dcHook;
    private final Boolean enabled;
    private final List<ItemEntry> itemCmdEntries;
    private final List<String> ignorePlaced;
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

    public ItemUse(JavaPlugin javaPlugin, ItemManager itemManager, Economy ecoHook, GDHook gdHook, DeluxeCombatAPI dcHook) {
        this.javaPlugin = javaPlugin;
        this.itemManager = itemManager;
        this.ecoHook = ecoHook;
        this.gdHook = gdHook;
        this.dcHook = dcHook;
        this.enabled = itemManager.isEnabled();
        this.itemCmdEntries = itemManager.getItemEntries();
        this.ignorePlaced = javaPlugin.getConfig().getConfigurationSection("itemEntries").getStringList("stopBlockPlace");
    }

    private ItemEntry getItemEntryByItem(ItemStack item) {
        if (item == null) return null;
        if (!item.hasItemMeta() || item.getType() == Material.AIR) 
            return null;
        return itemCmdEntries.stream()
            .filter(entry -> item.getItemMeta().getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING))
            .findFirst()
            .orElse(null);
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        if (!enabled) return;

        Player shooter = event.getPlayer();
        ItemStack item = event.getItem();
        if (item==null || shooter==null) return;

        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null || !shooter.hasPermission(entry.getUsePerm()))
            return;

        switch (entry.getType()) {
            /* case "EXPLOSIVE_ARROW":
                // activeCooldowns index: 0    
                // has custom listeners: onBowShoot & onProjectileHit
                break; */
            case "LIGHTNING_PEARL": {
                // activeCooldowns index: 1
                // has custom listener: onProjectileHit
                triggerLightningPearl(event, shooter, entry);
                break;
            }
            case "RANDOM_POTION": {
                // activeCooldowns index: 2
                // has custom listener: onPotionSplash
                triggerRandomPotion(event, shooter, entry);
                break;
            }
            case "FLAME_PARTICLE": {
                // activeCooldowns index: 3
                triggerFlameParticle(event, shooter, entry);
                break;
            }
            /* case "CONSUME_COMMANDS": {
                // activeCooldowns index: 4
                // has custom listener: onItemConsume
                break;
            } */
            default:
                break;
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player shooter = (Player) event.getEntity();
        Arrow arrow = (Arrow) event.getProjectile();
        ItemStack item = shooter.getInventory().getItemInMainHand();
        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null) return;

        double random = entry.getRandom();
        if (random > 0) randomizeVelocity(arrow, random);

        triggerExplosiveArrow(shooter, arrow, entry, explosiveArrows);
    }

    private void triggerExplosiveArrow(Player shooter, Arrow arrow, ItemEntry entry, List<Projectile> arrowList) {
        if (!shooter.hasPermission(entry.getUsePerm())) return;
        if (!passGDPvpCheck(shooter.getLocation())) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- claim pvp protection!"));
            return;
        }

        if (shooter.getFoodLevel() < entry.getHunger()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(shooter.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(0)) < entry.getCooldown()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(shooter, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toLowerCase()));
            return;
        }
        
        double cost = entry.getMoney();
        if (ecoHook != null)
            if (cost > 0 && !hasMoney(shooter, cost)) {
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- not enough money!"));
                return;
            }

        // Passed all checks!
        shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting explosive arrow..."));

        // Add arrow to data, projectile hit handled by listener
        arrowList.add(arrow);
        
        // Set cooldowns, etc.
        adjustCooldowns(shooter, itemManager.getHungers(), 0); // remove hunger and set cooldown
        if (ecoHook != null)
            if (cost>0 && !removeMoney(shooter, cost)) { // remove money
                javaPlugin.getLogger().warning("Error: failed to remove money for " + shooter.getName() + "'s " + entry.getKeyString() + " usage!");
            }
        if (hasAmmoItem==2) removeItem(shooter, entry.getAmmoItem()); // remove ammo item
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

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(shooter.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(1)) < entry.getCooldown()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(shooter, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null)
            if (cost > 0 && !hasMoney(shooter, cost)) {
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked -- not enough money!"));
                return;
            }

        // Passed all checks!
        shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting lightning pearl..."));
        
        // Shoot projectile & add to data
        EnderPearl pearl = shooter.launchProjectile(EnderPearl.class);
        lightningPearls.add(pearl);
        double random = entry.getRandom();
        if (random > 0) randomizeVelocity(pearl, random);

        // Set cooldowns, etc.
        adjustCooldowns(shooter, itemManager.getHungers(), 1); // remove hunger and set cooldown
        if (ecoHook != null)
            if (cost>0 && !removeMoney(shooter, cost)) { // remove money
                javaPlugin.getLogger().warning("Error: failed to remove money for " + shooter.getName() + "'s " + entry.getKeyString() + " usage!");
            }
        if (hasAmmoItem==2) removeItem(shooter, entry.getAmmoItem()); // remove ammo item
    }

    private void triggerRandomPotion(PlayerInteractEvent event, Player shooter, ItemEntry entry) {
        event.setCancelled(true);
        if (!passGDPvpCheck(shooter.getLocation())) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- claim pvp protection!"));
            return;
        }

        if (shooter.getFoodLevel() < entry.getHunger()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(shooter.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(2)) < entry.getCooldown()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(shooter, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null)
            if (cost > 0 && !hasMoney(shooter, cost)) {
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- not enough money!"));
                return;
            }
        
        // Passed all checks!
        
        // Shoot projectile, will get added to data in shootPotion()
        Action action = event.getAction();
        boolean rightClick = !(action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        shootPotion(shooter, rightClick);
        
        // Set cooldowns, etc.
        adjustCooldowns(shooter, itemManager.getHungers(), 2); // remove hunger & set cooldown
        if (ecoHook != null)
            if (cost>0 && !removeMoney(shooter, cost)) // remove money
                javaPlugin.getLogger().warning("Error: failed to remove money for " + shooter.getName() + "'s " + entry.getKeyString() + " usage!");
        if (hasAmmoItem==2)
            removeItem(shooter, entry.getAmmoItem()); // remove ammo item
    }

    private void triggerFlameParticle(PlayerInteractEvent event, Player shooter, ItemEntry entry) {
        event.setCancelled(true);
        if (!passGDPvpCheck(shooter.getLocation())) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- claim pvp protection!"));
            return;
        }

        if (shooter.getFoodLevel() < entry.getHunger()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(shooter.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(3)) < entry.getCooldown()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(shooter, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No fuel: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null)
            if (cost > 0 && !hasMoney(shooter, cost)) {
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- not enough money!"));
                return;
            }

        // Passed all checks!
        shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting flames..."));

        // Shoot particles, no data associated
        shootFlames(shooter, entry);

        // Set cooldowns, etc.
        if (ecoHook != null)
            if (cost>0 && !removeMoney(shooter, cost)) { // remove money
                javaPlugin.getLogger().warning("Error: failed to remove money for " + shooter.getName() + "'s " + entry.getKeyString() + " usage!");
            }
        if (hasAmmoItem==2) removeItem(shooter, entry.getAmmoItem()); // remove ammo item
        adjustHunger(shooter, itemManager.getHungers(), 3); // remove hunger
        new BukkitRunnable() {
            @Override
            public void run() {
                adjustCooldown(shooter, 3); // set cooldown
            }
        }.runTaskLater(javaPlugin, entry.getCooldown()*20);
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player consumer = event.getPlayer();
        ItemStack item = event.getItem();
        if (item==null || consumer==null) return;

        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null) return;

        event.setCancelled(true);

        if (!entry.getType().equalsIgnoreCase("CONSUME_COMMANDS") || !consumer.hasPermission(entry.getUsePerm()))
            return;

        if (consumer.getFoodLevel() < entry.getHunger()) {
            consumer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Healing blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(consumer.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(4)) < entry.getCooldown()) {
            consumer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Healing blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(consumer, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            consumer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No fuel: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null)
            if (cost > 0 && !hasMoney(consumer, cost)) {
                consumer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Healing blocked -- not enough money!"));
                return;
            }

        // Passed all checks!
        consumer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Healing..."));
        
        
        // Run commands
        List<String> commands = entry.getCommands();
        for (String command : commands) {
            String cmd = command.replace("<player>", consumer.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        // Set cooldowns, etc.
        adjustCooldowns(consumer, itemManager.getHungers(), 4); // remove hunger & set cooldown
        if (ecoHook != null)
            if (cost>0 && !removeMoney(consumer, cost)) { // remove money
                javaPlugin.getLogger().warning("Error: failed to remove money for " + consumer.getName() + "'s " + entry.getKeyString() + " usage!");
            }
        if (hasAmmoItem==2) removeItem(consumer, entry.getAmmoItem()); // remove ammo item
        if (entry.getRemoveItem()) removeItem(consumer, item); // remove consumed item
    }

    private void shootPotion(Player shooter, boolean rightClick) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        PotionEffectType effectType = rightClick ? positiveEFFECTS[ThreadLocalRandom.current().nextInt(positiveEFFECTS.length)]
                                                 : negativeEFFECTS[ThreadLocalRandom.current().nextInt(negativeEFFECTS.length)];
        potionMeta.addCustomEffect(new PotionEffect(effectType, 600, 1), true);
        potion.setItemMeta(potionMeta);

        shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting " + effectType.getName().toLowerCase() + "..."));
        shooter.getWorld().spawn(shooter.getLocation().add(0, 1.5, 0), ThrownPotion.class, thrownPotion -> {
            thrownPotion.setItem(potion);
            thrownPotion.setBounce(false);
            thrownPotion.setVelocity(shooter.getLocation().getDirection().multiply(1.4));
            ItemEntry entry = itemManager.getItemEntryByType("RANDOM_POTION");

            double random = entry.getRandom();
            if (random > 0) randomizeVelocity(thrownPotion, random);
            
            if (!rightClick) {
                thrownPotion.setVisualFire(true);
                if (entry.getDamage()>=0)
                    thrownPotion.setMetadata("randomPotionLeft", new FixedMetadataValue(javaPlugin, "true"));
            }
            thrownPotion.setShooter((ProjectileSource)shooter);
            magicPotions.add(thrownPotion);
        });
    }

    private void shootFlames(Player shooter, ItemEntry entry) {
        double random = entry.getRandom();

        // Initialize particle direction
        Vector shooterDirection = shooter.getLocation().getDirection();
        Vector particleVector = shooterDirection.clone();
        shooterDirection.multiply(16);
        double temp = particleVector.getX();
        particleVector.setX(-particleVector.getZ());
        particleVector.setZ(temp);
        particleVector.divide(new Vector(3, 3, 3));
        Location particleLocation = particleVector.toLocation(shooter.getWorld()).add(shooter.getLocation()).add(0, 1.05, 0);
        
        // Shoot flames
        for (int i = 0; i < 8; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Vector offsetPath = shooterDirection.clone();
                    Location offsetLocation = particleLocation.clone();
                    if (random > 0) {
                        randomizeLocation(offsetLocation, random);
                        randomizeVelocity(offsetPath, random/2);
                    }
                    offsetLocation.add(0, 0.3, 0);
                    shooter.getWorld().spawnParticle(Particle.FLAME, offsetLocation, 0, offsetPath.getX(), offsetPath.getY(), offsetPath.getZ(), 0.1);
                }
            }.runTaskLater(javaPlugin, i);
        }

        // Set fire & extraDamage
        handleFlameHit(shooter, entry);
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

    // 0 - doesnt have ammo & action blocked
    // 1 - doesnt have ammo & action permitted
    // 2 - has ammo & action permitted
    private int hasItem(Player player, String itemName) {
        if (itemName.equals("none")
            || itemName.isBlank()
            || itemName.isEmpty()
            || itemName == null) return 1;

        Material itemMaterial = Material.getMaterial(itemName.toUpperCase());
        if (itemMaterial == null) {
            javaPlugin.getLogger().warning("Error: Poorly defined item " + itemName);
            return 1;
        }
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == itemMaterial && itemStack.getAmount() > 0)
                return 2;
        }
        return 0;
    }

    // doesn't require checks as hasItem should always get called before
    private boolean removeItem(Player player, String itemName) {
        Material itemMaterial = Material.getMaterial(itemName.toUpperCase());
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == itemMaterial) {
                if (itemStack.getAmount() <= 1) player.getInventory().remove(itemStack);
                else itemStack.setAmount(Math.max(itemStack.getAmount() - 1, 0));
                return true;
            }
        }
        return false;
    }

    // doesn't require checks as passed itemStack should exist
    private boolean removeItem(Player player, ItemStack itemStack) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(itemStack))  {
                if (getItemEntryByItem(item) != null) {
                    if (item.getAmount() <= 1) player.getInventory().remove(item);
                    else item.setAmount(Math.max(item.getAmount() - 1, 0));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean giveItem(Player player, String itemName) {
        if (itemName.equals("none")
            || itemName.isBlank()
            || itemName.isEmpty()
            || itemName == null) return true;

        Material itemMaterial = Material.getMaterial(itemName.toUpperCase());
        if (itemMaterial == null) {
            javaPlugin.getLogger().warning("Error: Unknown item " + itemMaterial);
            return false;
        }

        ItemStack item = new ItemStack(itemMaterial);
        item.setAmount(1);
        player.getInventory().addItem(item);
        return true;
    }

    private boolean hasMoney(Player player, double amount) {
        if (ecoHook==null) return true;
        double bal = ecoHook.getBalance(player);
        if (bal>=amount) return true;
        else return false;
    }

    private boolean removeMoney(Player player, double amount) {
        if (ecoHook==null && amount <= 0) return true;

        int iAmount = (int) amount;
        EconomyResponse r = ecoHook.withdrawPlayer(player, amount);
        if (r.transactionSuccess()) {
            player.sendMessage(ChatColor.DARK_GREEN + "$$$: " + ChatColor.YELLOW + "Charged $" + iAmount + "");
            return true;
        } else return false;
    }

    private boolean giveMoney(Player player, double amount) {
        if (ecoHook==null || amount <= 0) return true;

        int iAmount = (int) amount;
        EconomyResponse r = ecoHook.depositPlayer(player, amount);
        if (r.transactionSuccess()) {
            player.sendMessage(ChatColor.DARK_GREEN + "$$$: " + ChatColor.YELLOW + "Refunded $" + iAmount + "");
            return true;
        } else return false;
    }

    private boolean refundPlayer(Player player, ItemEntry entry) {
        boolean failed = false;
        if (!giveMoney(player, entry.getMoney())) {
            player.sendMessage(ChatColor.RED + "Money refund failed!");
            failed = true;
        }
        if (!giveItem(player, entry.getAmmoItem())) {
            player.sendMessage(ChatColor.RED + "Ammo refund failed!");
            failed = true;
        }
        return failed;
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

    private boolean passGDBuilderCheck(Player shooter, Location location, int radius) {
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
            if (!gdHook.hasBuilderTrust(shooter, id)) return false;
        }
        return true;
    }

    private void handleFlameHit(Player shooter, ItemEntry entry) {
        if (Math.random() < 0.42) {
            Block targetBlock = shooter.getTargetBlock(null, 20);
            Block targetBlockAbove = targetBlock.getRelative(BlockFace.UP);

            if (targetBlock != null && targetBlockAbove != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location location = targetBlockAbove.getLocation();
                        boolean passGDPvpCheck = true, passDCPvpLocCheck = true;
                        
                        if (dcHook != null) {
                            if (!passDCPvpLocCheck(location, 3.0)) passDCPvpLocCheck = false;
                            else if (!passDCPvpPlayerCheck(shooter)) passDCPvpLocCheck = false;
                        }
                        if (gdHook != null) {
                            if (!passGDPvpCheck(location)) passGDPvpCheck = false;
                            else if (!passGDPvpCheck(shooter.getLocation())) passGDPvpCheck = false;
                        }
                
                        if (!passDCPvpLocCheck) {
                            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- pvp protection!"));
                            refundPlayer(shooter, entry);
                        } else if (!passGDPvpCheck) {
                            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Flames blocked -- claim pvp protection!"));
                            refundPlayer(shooter, entry);
                        } else {
                            if (targetBlock.getType().isBlock() && targetBlockAbove.getType() == Material.AIR)
                                targetBlockAbove.setType(Material.FIRE);
                            damagePlayers(shooter, targetBlockAbove.getLocation(), 1.5, 1.6, entry.getDamage(), 60);
                        }
                    }
                }.runTaskLater(javaPlugin, 10);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow)
            handleArrowHit((Arrow) event.getEntity());
        else if (event.getEntity() instanceof EnderPearl)
            handlePearlHit(event, (EnderPearl) event.getEntity());
    }

    private void handleArrowHit(Arrow arrow) {
        if (explosiveArrows.remove(arrow)) {
            Location location = arrow.getLocation();
            Player shooter = (Player) arrow.getShooter();
            boolean passDCPvpLocCheck = true, passGDPvpCheck = true, passGDBuilderCheck = true;

            if (dcHook != null)
                if (!passDCPvpLocCheck(location, 5.0)) passDCPvpLocCheck = false;
            if (gdHook != null) {
                if (!passGDPvpCheck(shooter.getLocation())) passGDPvpCheck = false;
                else if (!passGDPvpCheck(location)) passGDPvpCheck = false;
                else if (!passGDBuilderCheck(shooter, location, 5)) passGDBuilderCheck = false;
            }

            if (!passDCPvpLocCheck) {
                arrow.remove();
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- pvp protection!"));
                refundPlayer(shooter, itemManager.getItemEntryByType("EXPLOSIVE_ARROW"));
            } else if (!passGDPvpCheck) {
                arrow.remove();
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- claim pvp protection!"));
                refundPlayer(shooter, itemManager.getItemEntryByType("EXPLOSIVE_ARROW"));
            } else if (!passGDBuilderCheck) {
                damagePlayers(shooter, location, 1.7, 1.2, itemManager.getItemEntryByType("EXPLOSIVE_ARROW").getDamage(), 0);
                arrow.getWorld().createExplosion(location, 2.0F, true, false, shooter);
                arrow.remove();
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion nerfed -- claim block protection!"));
            } else {
                damagePlayers(shooter, location, 1.7, 1.2, itemManager.getItemEntryByType("EXPLOSIVE_ARROW").getDamage(), 20);
                arrow.getWorld().createExplosion(location, 2.0F, true, true, shooter);
                arrow.remove();
            } return;
        }
    }

    private void handlePearlHit(ProjectileHitEvent event, EnderPearl pearl) {
        if (!lightningPearls.remove(pearl)) return;
        event.setCancelled(true);

        Location location = pearl.getLocation();
        Player shooter = (Player) pearl.getShooter();
        boolean passDCPvpLocCheck = true, passGDPvpCheck = true;
        
        if (dcHook != null)
            if (!passDCPvpLocCheck(location, 4.0)) passDCPvpLocCheck = false;
        if (gdHook != null) {
            if (!passGDPvpCheck(location)) passGDPvpCheck = false;
            else if (!passGDPvpCheck(shooter.getLocation())) passGDPvpCheck = false;
        }        

        if (!passDCPvpLocCheck) {
            pearl.remove();
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lightning blocked -- pvp protection!"));
            refundPlayer(shooter, itemManager.getItemEntryByType("LIGHTNING_PEARL"));
        } else if (!passGDPvpCheck) {
            pearl.remove();
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lightning blocked -- claim pvp protection!"));
            refundPlayer(shooter, itemManager.getItemEntryByType("LIGHTNING_PEARL"));
        } else {
            damagePlayers(shooter, location, 1.2, 3.0, itemManager.getItemEntryByType("LIGHTNING_PEARL").getDamage(), 0);
            location.getWorld().strikeLightning(location);
            pearl.remove();
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion thrownPotion = (ThrownPotion) event.getEntity();
        if (!magicPotions.contains(thrownPotion)) return;
        magicPotions.remove(thrownPotion);

        handlePotionHit(event, thrownPotion);
    }

    private void handlePotionHit(PotionSplashEvent event, ThrownPotion thrownPotion) {
        Location location = thrownPotion.getLocation();
        Player shooter = (Player) thrownPotion.getShooter();
        boolean passDCPvpLocCheck = true, passGDPvpCheck = true;
        
        if (dcHook != null)
            if (!passDCPvpLocCheck(location, 4.0)) passDCPvpLocCheck = false;
        if (gdHook != null) {
            if (!passGDPvpCheck(location)) passGDPvpCheck = false;
            else if (!passGDPvpCheck(shooter.getLocation())) passGDPvpCheck = false;
        }

        if (!passDCPvpLocCheck) {
            event.setCancelled(true);
            thrownPotion.remove();
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- pvp protection!"));
            refundPlayer(shooter, itemManager.getItemEntryByType("RANDOM_POTION"));
        } else if (!passGDPvpCheck) {
            event.setCancelled(true);
            thrownPotion.remove();
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- claim pvp protection!"));
            refundPlayer(shooter, itemManager.getItemEntryByType("RANDOM_POTION"));
        } else if (thrownPotion.hasMetadata("randomPotionLeft"))
            damagePlayers(shooter, location, 0.9, 1.3, itemManager.getItemEntryByType("RANDOM_POTION").getDamage(), 20);
    }

    private boolean damagePlayers(Player shooter, Location location, double hRadius, double vRadius, double damage, int ignite) {
        Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, hRadius, vRadius, hRadius);
        boolean damaged = false;
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                player.damage(damage, shooter);
                if (ignite>0) player.setFireTicks(ignite);
                //player.sendMessage(ChatColor.RED + "hit: " + ChatColor.GRAY + damage + ChatColor.RED + ", by: " + ChatColor.GRAY + shooter);
                //shooter.sendMessage(ChatColor.RED + "hit: " + ChatColor.GRAY + damage + ChatColor.RED + ", on: " + ChatColor.GRAY + player);
                damaged = true;
            }
        }
        return damaged;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!enabled) return;
        
        ItemStack item = event.getItemInHand();
        if (!ignorePlaced.contains(item.getType().toString())) return;

        ItemMeta itemData = item.getItemMeta();
        if (itemData==null) return;

        for (ItemEntry entry : itemCmdEntries) {
            if (itemData.getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeCooldowns.remove(uuid);
    }
}