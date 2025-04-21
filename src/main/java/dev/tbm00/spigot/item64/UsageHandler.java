package dev.tbm00.spigot.item64;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.protection.flags.StateFlag.State;

import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class UsageHandler {
    private final Item64 item64;
    private final ConfigHandler configHandler;
    private final Economy ecoHook;
    private final GDHook gdHook;
    private final DCHook dcHook;
    private final WorldGuard wgHook;
    private final List<ItemEntry> itemEntries;
    private final List<Long> cooldowns = new ArrayList<>();
    private final Map<UUID, List<Long>> activeCooldowns = new HashMap<>();
    private final ArrayList<Projectile> explosiveArrows = new ArrayList<>();
    private final ArrayList<Projectile> lightningPearls = new ArrayList<>();
    private final ArrayList<Projectile> magicPotions = new ArrayList<>();
    private final int[][] CHECK_DIRECTIONS = {
        {1, 0, 0},
        {-1, 0, 0},
        {0, 0, 1}, 
        {0, 0, -1},
        {0, 1, 0},
        {0, -1, 0},
        {0, 0, 0}
    };

    private RegionQuery regionQuery;

    public UsageHandler(Item64 item64, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook, WorldGuard wgHook) {
        this.item64 = item64;
        this.configHandler = configHandler;
        this.ecoHook = ecoHook;
        this.gdHook = gdHook;
        this.dcHook = dcHook;
        this.wgHook = wgHook;
        itemEntries = configHandler.getItemEntries();

        regionQuery = wgHook.getPlatform().getRegionContainer().createQuery();

        // only set cooldowns on first initization of ListenerLeader
        if (cooldowns.size()==0) {
            // get cooldown size
            int cooldownSize = 0;
            for (ItemEntry entry : itemEntries) {
                if (entry.getID()>cooldownSize) cooldownSize = entry.getID();
            }

            // initialize `cooldowns`
            for (int i = 0; i<cooldownSize; ++i) {
                if (cooldowns.size()<cooldownSize)
                    cooldowns.add(0L);
                else break;
            }

            // load `cooldowns`
            for (ItemEntry entry : itemEntries) {
                int index = entry.getID()-1;
                cooldowns.set(index, Long.valueOf(entry.getCooldown()));
            }
        }
    }

    // TRIGGER: ALL
    public void triggerUsage(Player player, ItemEntry entry, ItemStack item, Action action, Projectile projectile, Block block) {
        if (!passUsageInitialChecks(player, entry)) return;

        // Use the item
        switch (entry.getType()) {
            case "USABLE":
                runCmdsApplyFX(player, entry, item);
                break;
            case "CONSUMABLE":
                runCmdsApplyFX(player, entry, item);
                break;
            case "EXPLOSIVE_ARROW":
                if (!passUsagePVPChecks(player, entry) || !passUsageBuildChecks(player, entry, configHandler.PROTECTION_RADIUS)) return;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting explosive arrow..."));
                shootExplosiveArrow(player, entry, projectile);
                break;
            case "LIGHTNING_PEARL":
                if (!passUsagePVPChecks(player, entry) || !passUsageBuildChecks(player, entry, configHandler.PROTECTION_RADIUS)) return;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting lightning pearl..."));
                shootLightningPearl(player, entry);
                break;
            case "FLAME_PARTICLE":
                if (!passUsagePVPChecks(player, entry) || !passUsageBuildChecks(player, entry, configHandler.PROTECTION_RADIUS)) return;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting flames..."));
                shootFlameParticles(player, entry);
                break;
            case "RANDOM_POTION":
                if (!passUsageBuildChecks(player, entry, configHandler.PROTECTION_RADIUS)) return;
                boolean leftClick = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
                if (leftClick && !passUsagePVPChecks(player, entry)) return;
                if (!shootRandomPotion(player, entry, leftClick)) return;
                break;
            case "AREA_BREAK":
                if (!passUsageBuildChecks(player, entry, configHandler.PROTECTION_RADIUS)) return;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Breaking blocks..."));
                breakBlocks(player, entry, block);
                break;
            default: 
                break;
        }

        // Remove hunger, ammo, and money
        if (getEcoHook() != null && entry.getMoney() > 0 && !removeMoney(player, entry.getMoney()))
            getItem64().logRed("Error: failed to remove money for " + player.getName() + "'s " + entry.getKeyString() + " usage!");
        if (entry.getRemoveAmmo()) removeItem(player, entry.getAmmoItem());
        adjustHunger(player, entry);

        // Set cooldowns
        if (entry.getType().equals("FLAME_PARTICLE")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    adjustCooldown(player, entry);
                }
            }.runTaskLater(getItem64(), entry.getCooldown() * 20);
        } else adjustCooldown(player, entry);
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
            applyEffects(player, effects, item);
        
        if (entry.getRemoveItem()) removeItem(player, item);
    }

    // USER: EXPLOSIVE_ARROW
    private void shootExplosiveArrow(Player player, ItemEntry entry, Projectile projectile) {
        double random = entry.getRandom();
        projectile.setMetadata("Item64-keyString", new FixedMetadataValue(getItem64(), entry.getKeyString()));
        getExplosiveArrows().add(projectile);
        if (random > 0) randomizeProjectile(projectile, random);
    }

    // USER: LIGHTNING_PEARL
    private void shootLightningPearl(Player player, ItemEntry entry) {
        double random = entry.getRandom();
        EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        pearl.setMetadata("Item64-keyString", new FixedMetadataValue(getItem64(), entry.getKeyString()));
        getLightningPearls().add(pearl);
        if (random > 0) randomizeProjectile(pearl, random);
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

                if (passDamageChecks(player, location, entry, configHandler.PROTECTION_RADIUS)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (targetBlock.getType().isBlock() && targetBlockAbove.getType() == Material.AIR)
                                targetBlockAbove.setType(Material.FIRE);
                            damageEntities(player, targetBlockAbove.getLocation(), 0.9, 1.5, entry.getDamage(), 60);
                        }
                    }.runTaskLater(getItem64(), 12);
                } else refundPlayer(player, entry);
            }
        }

        // Shot flame visuals 
        for (int i = 0; i < 6; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i=0; i<6; ++i) {
                        Vector offsetPath = playerDirection.clone();
                        Location offsetLocation = particleLocation.clone();
                        if (random > 0) {
                            randomizeLocation(offsetLocation, random);
                            randomizeVelocity(offsetPath, random / 2);
                        }
                        Particle particle = getRandomFireParticle();
                        offsetLocation.add(0, 0.3, 0);
                        player.getWorld().spawnParticle(particle, offsetLocation, 0, offsetPath.getX(), offsetPath.getY(), offsetPath.getZ(), 0.1);
                    }
                }
            }.runTaskLater(getItem64(), i);
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
                    if (random > 0) randomizeProjectile(thrownPotion, random);
                    
                    if (leftClick) {
                        thrownPotion.setVisualFire(true);
                        if (entry.getDamage() >= 0)
                            thrownPotion.setMetadata("Item64-randomPotion-left", new FixedMetadataValue(getItem64(), "true"));
                    }
                    thrownPotion.setMetadata("Item64-keyString", new FixedMetadataValue(getItem64(), entry.getKeyString()));
                    thrownPotion.setShooter(player);
                    getMagicPotions().add(thrownPotion);
                });
                return true;
            } else {
                getItem64().logRed("Unknown potion effect type: " + parts[0]);
                return false;
            }
        } catch (Exception e) {
            getItem64().logRed("Exception while throwing potion: ");
            getItem64().getLogger().warning(e.getMessage());
            return false;
        }
    }

    // USER: AREA_BREAK
    private boolean breakBlocks(Player player, ItemEntry entry, Block brokenBlock) {



        try {

        } catch (Exception e) {
            getItem64().logRed("Exception while breaking blocks: ");
            getItem64().getLogger().warning(e.getMessage());
            return false;
        }

        return true;
    }

    // HELPER: ALL ITEMS
    public boolean passUsageInitialChecks(Player player, ItemEntry entry) {
        if (entry.getAmmoItem()!=null && !hasItem(player, entry.getAmmoItem())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toString().toLowerCase()));
            return false;
        }
        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- you're too hungry!"));
            return false;
        }
        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>(cooldowns));
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(entry.getID()-1)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- active cooldown!"));
            return false;
        }
        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(player, cost)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- not enough money!"));
            return false;
        }
        return true;
    }

    // HELPER: PVP ITEMS & BREAK ITEMS
    public boolean passUsageBuildChecks(Player player, ItemEntry entry, int radius) {
        if (!passGDClaimBuildCheck(player, player.getLocation(), radius)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- claim block/entity protection!"));
            return false;
        }
        if (!passWGRegionBuildCheck(player, player.getLocation(), radius)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- region block/entity protection!"));
            return false;
        }
        return true;
    }

    // HELPER: PVP ITEMS
    public boolean passUsagePVPChecks(Player player, ItemEntry entry) {
        if (!passGDClaimPvpCheck(player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- claim pvp protection!"));
            return false;
        }
        if (!passWGRegionPvpCheck(player, player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- region pvp protection!"));
            return false;
        }
        return true;
    }

    // HELPER: PVP ITEMS
    public boolean passDamageChecks(Player shooter, Location location, ItemEntry entry, int radius) {
        if (!passDCPvpLocCheck(shooter, location, radius)) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Damage blocked -- pvp protection!"));
            return false;
        }
        if (!passGDClaimPvpCheck(location)) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Damage blocked -- claim pvp protection!"));
            return false;
        }
        if (!passWGRegionPvpCheck(shooter, location)) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Damage blocked -- region pvp protection!"));
            return false;
        }
        if (!passGDClaimBuildCheck(shooter, location, radius)) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Damage blocked -- claim block/entity protection!"));
            return false;
        }
        if (!passWGRegionBuildCheck(shooter, location, radius)) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Damage blocked -- region block/entity protection!"));
            return false;
        }
        return true;
    }

    public boolean passDCPvpLocCheck(Player shooter, Location location, double radius) {
        if (dcHook==null) return true;

        boolean shooterEnabled = passDCPvpToggleCheck(shooter);

        return location.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .noneMatch(player -> !passDCPvpToggleCheck(player) || !shooterEnabled);
    }
    
    public boolean passDCPvpToggleCheck(Player player) {
        if (dcHook==null) return true;
        else if (dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player)) return false;
        return true;
    }

    public boolean passGDClaimPvpCheck(Location location) {
        if (gdHook==null) return true;
        String regionID = gdHook.getRegionID(location);
        return gdHook.hasPvPEnabled(regionID);
    }

    public boolean passGDClaimBuildCheck(Player shooter, Location center, int radius) {
        if (gdHook==null) return true;

        for (int i = 0; i < CHECK_DIRECTIONS.length; ++i) {
            Location loc = center.clone().add(radius*CHECK_DIRECTIONS[i][0], radius*CHECK_DIRECTIONS[i][1], radius*CHECK_DIRECTIONS[i][2]);

            String claimId = gdHook.getRegionID(loc);
            if (claimId == null) continue;
            if (!gdHook.hasBuilderTrust(shooter, claimId)) {
                return false;
            }
        }
        return true;
    }

    public boolean passWGRegionBuildCheck(Player shooter, Location center, int radius) {
        if (wgHook==null) return true;

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(shooter);

        for (int[] offset : CHECK_DIRECTIONS) {
            Location loc = center.clone().add(radius*offset[0], radius*offset[1], radius*offset[2]);
            com.sk89q.worldedit.util.Location wgLocation = BukkitAdapter.adapt(loc);
            ApplicableRegionSet set = regionQuery.getApplicableRegions(wgLocation);

            if (set.isVirtual()) continue;

            State state = set.queryState(localPlayer, Flags.BUILD);
            if (state==null || state.equals(State.ALLOW)) continue;

            return false;
        }
        return true;
    }

    public boolean passWGRegionPvpCheck(Player shooter, Location location) {
        if (wgHook==null) return true;

        com.sk89q.worldedit.util.Location wgLocation = BukkitAdapter.adapt(location);
        ApplicableRegionSet set = regionQuery.getApplicableRegions(wgLocation);
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(shooter);

        if (set.isVirtual()) return true;
        
        State state = set.queryState(localPlayer, Flags.BUILD);
        if (state==null || state.equals(State.ALLOW)) return true;;
    
        return false;
    }

    // HELPER: PVP ITEMS
    public void damageEntities(Player shooter, Location location, double hRadius, double vRadius, double damage, int ignite) {
        Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, hRadius, vRadius, hRadius);
        List<String> damagedPlayers = new ArrayList<>(3);
        for (Entity entity : nearbyEntities) {
            if (damagedPlayers.size()>=3) break;
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.damage(damage, shooter);
                if (ignite>0) livingEntity.setFireTicks(ignite);
                if (livingEntity instanceof Player player) 
                    damagedPlayers.add(player.getDisplayName());
            }

            if (!damagedPlayers.isEmpty()) {
                String message = String.join(", ", damagedPlayers);
                shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "Damaged " + message));
            }
        }
    }

    // HELPER: CONSUMABLE, USABLE
    public void applyEffects(Player player, List<String> effects, ItemStack item) {
        for (String line : effects) {
            try {
                String[] parts = line.split(":");
                PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                int amplifier = Integer.parseInt(parts[1]);
                int duration = Integer.parseInt(parts[2])*20;
                if (effectType != null) {
                    player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, true));
                } else {
                    item64.logRed("Unknown potion effect type: " + parts[0]);
                    return;
                }
            } catch (Exception e) {
                item64.logRed("Error parsing effect: " + line + " - ");
                item64.getLogger().warning(e.getMessage());
            }
        }
    }

    public void adjustCooldown(Player player, ItemEntry entry) {
        if (entry.getCooldown()<=0) return;
        int index = entry.getID()-1;
        List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
        playerCooldowns.set(index, System.currentTimeMillis() / 1000);
        activeCooldowns.put(player.getUniqueId(), playerCooldowns);
    }

    public void adjustHunger(Player player, ItemEntry entry) {
        if (entry.getHunger()<=0) return;
        player.setFoodLevel(Math.max(player.getFoodLevel() - entry.getHunger(), 0));
    }

    public ItemEntry getItemEntryByItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getType() == Material.AIR)
            return null;
        return itemEntries.stream()
            .filter(entry -> item.getItemMeta().getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING))
            .findFirst()
            .orElse(null);
    }

    public boolean giveItem(Player player, Material material) {
        ItemStack item = new ItemStack(material);
        item.setAmount(1);
        player.getInventory().addItem(item);
        return true;
    }

    public boolean hasItem(Player player, Material material) {
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == material && itemStack.getAmount() > 0)
                return true;
        }
        return false;
    }

    // doesn't require checks as hasItem should always get called before
    public boolean removeItem(Player player, Material material) {
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == material) {
                if (itemStack.getAmount() <= 1) player.getInventory().remove(itemStack);
                else itemStack.setAmount(Math.max(itemStack.getAmount() - 1, 0));
                return true;
            }
        }
        return false;
    }

    // doesn't require checks as passed itemStack should exist
    public boolean removeItem(Player player, ItemStack itemStack) {
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

    public boolean hasMoney(Player player, double amount) {
        if (ecoHook==null) return true;
        double bal = ecoHook.getBalance(player);
        if (bal>=amount) return true;
        else return false;
    }

    public boolean removeMoney(Player player, double amount) {
        if (ecoHook==null && amount <= 0) return true;

        int iAmount = (int) amount;
        EconomyResponse r = ecoHook.withdrawPlayer(player, amount);
        if (r.transactionSuccess()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Charged $" + iAmount));
            return true;
        } else return false;
    }

    public boolean giveMoney(Player player, double amount) {
        if (ecoHook==null || amount <= 0) return true;

        EconomyResponse r = ecoHook.depositPlayer(player, amount);
        if (r.transactionSuccess()) {
            return true;
        } else return false;
    }

    public boolean refundPlayer(Player player, ItemEntry entry) {
        boolean failed = false;
        if (entry.getMoney() > 0){
            if (!giveMoney(player, entry.getMoney())) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Money refund failed!"));
                player.sendMessage(ChatColor.RED + "Money refund failed!");
                failed = true;
            }
        }
        if (entry.getRemoveAmmo() && entry.getAmmoItem()!=null) {
            if (!giveItem(player, entry.getAmmoItem())) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Ammo refund failed!"));
                failed = true;
            }
        }
        return failed;
    }

    public void randomizeProjectile(Projectile projectile, double random) {
        Vector velocity = projectile.getVelocity();
        randomizeVelocity(velocity, random);
        projectile.setVelocity(velocity);
    }

    public void randomizeVelocity(Vector velocity, double random) {
        velocity.add(new Vector(
            ThreadLocalRandom.current().nextDouble(-random, random),
            ThreadLocalRandom.current().nextDouble(-random / 2, random / 2),
            ThreadLocalRandom.current().nextDouble(-random, random)
        ));
    }

    public void randomizeLocation(Location location, double random) {
        location.add(new Vector(
            ThreadLocalRandom.current().nextDouble(-random, random),
            ThreadLocalRandom.current().nextDouble(-random / 2, random / 2),
            ThreadLocalRandom.current().nextDouble(-random, random)
        ));
    }

    public Particle getRandomFireParticle() {
        int rInt = ThreadLocalRandom.current().nextInt(0, 3);

        switch (rInt%4) {
            case 0:
                return Particle.FLAME;
            case 1:
                return Particle.SMOKE_NORMAL;
            case 2:
                return Particle.SMOKE_LARGE;
            case 3:
                return Particle.FLAME;
            default:
                return Particle.FLAME;
        }
    }

    public Item64 getItem64() {
        return item64;
    }

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public Economy getEcoHook() {
        return ecoHook;
    }

    public GDHook getGdHook() {
        return gdHook;
    }

    public DCHook getDcHook() {
        return dcHook;
    }

    public List<ItemEntry> getItemEntries() {
        return Collections.unmodifiableList(itemEntries);
    }

    public List<Long> getCooldowns() {
        return cooldowns;
    }
    
    public Map<UUID, List<Long>> getActiveCooldowns() {
        return activeCooldowns;
    }
    
    public ArrayList<Projectile> getExplosiveArrows() {
        return explosiveArrows;
    }
    
    public ArrayList<Projectile> getLightningPearls() {
        return lightningPearls;
    }
    
    public ArrayList<Projectile> getMagicPotions() {
        return magicPotions;
    } 
}