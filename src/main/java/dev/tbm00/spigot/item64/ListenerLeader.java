package dev.tbm00.spigot.item64;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.util.Vector;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class ListenerLeader {
    protected final JavaPlugin javaPlugin;
    protected final ConfigHandler configHandler;
    protected final Economy ecoHook;
    protected final GDHook gdHook;
    protected final DCHook dcHook;
    protected static List<ItemEntry> itemEntries = null;
    protected static final List<Long> cooldowns = new ArrayList<>();
    protected static final Map<UUID, List<Long>> activeCooldowns = new HashMap<>();
    protected static final ArrayList<Projectile> explosiveArrows = new ArrayList<>();
    protected static final ArrayList<Projectile> lightningPearls = new ArrayList<>();
    protected static final ArrayList<Projectile> magicPotions = new ArrayList<>();

    public ListenerLeader(JavaPlugin javaPlugin, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        this.javaPlugin = javaPlugin;
        this.configHandler = configHandler;
        this.ecoHook = ecoHook;
        this.gdHook = gdHook;
        this.dcHook = dcHook;
        itemEntries = configHandler.getItemEntries();

        // only set cooldowns on first initization of ListenerLeader
        if (cooldowns.size()!=0) return;

        // get cooldown size
        int cooldownSize = 0;
        for (ItemEntry entry : itemEntries) {
            if (entry.getID()>cooldownSize) cooldownSize = entry.getID();
        }

        // initialize `cooldowns`
        for (int i = 0; i<cooldownSize; ++i) {
            if (cooldowns.size()<cooldownSize)
                cooldowns.add(0L);
            else return;
        }

        // load `cooldowns`
        for (ItemEntry entry : itemEntries) {
            int index = entry.getID()-1;
            cooldowns.set(index, Long.valueOf(entry.getCooldown()));
        }
    }

    protected ItemEntry getItemEntryByItem(ItemStack item) {
        if (item == null) return null;
        if (!item.hasItemMeta() || item.getType() == Material.AIR || item.getType() == Material.RESPAWN_ANCHOR) 
            return null;
        return itemEntries.stream()
            .filter(entry -> item.getItemMeta().getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING))
            .findFirst()
            .orElse(null);
    }

    protected void adjustCooldown(Player player, ItemEntry entry) {
        int index = entry.getID()-1;
        List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
        playerCooldowns.set(index, System.currentTimeMillis() / 1000);
        activeCooldowns.put(player.getUniqueId(), playerCooldowns);
    }

    protected void adjustHunger(Player player, ItemEntry entry) {
        player.setFoodLevel(Math.max(player.getFoodLevel() - entry.getHunger(), 0));
    }

    // 0 - doesnt have ammo & action blocked
    // 1 - doesnt have ammo & action permitted
    // 2 - has ammo & action permitted
    protected int hasItem(Player player, String itemName) {
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
    protected boolean removeItem(Player player, String itemName) {
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
    protected boolean removeItem(Player player, ItemStack itemStack) {
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

    protected boolean giveItem(Player player, String itemName) {
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

    protected boolean hasMoney(Player player, double amount) {
        if (ecoHook==null) return true;
        double bal = ecoHook.getBalance(player);
        if (bal>=amount) return true;
        else return false;
    }

    protected boolean removeMoney(Player player, double amount) {
        if (ecoHook==null && amount <= 0) return true;

        int iAmount = (int) amount;
        EconomyResponse r = ecoHook.withdrawPlayer(player, amount);
        if (r.transactionSuccess()) {
            player.sendMessage(ChatColor.DARK_GREEN + "$$$: " + ChatColor.YELLOW + "Charged $" + iAmount + "");
            return true;
        } else return false;
    }

    protected boolean giveMoney(Player player, double amount) {
        if (ecoHook==null || amount <= 0) return true;

        int iAmount = (int) amount;
        EconomyResponse r = ecoHook.depositPlayer(player, amount);
        if (r.transactionSuccess()) {
            player.sendMessage(ChatColor.DARK_GREEN + "$$$: " + ChatColor.YELLOW + "Refunded $" + iAmount + "");
            return true;
        } else return false;
    }

    protected boolean refundPlayer(Player player, ItemEntry entry) {
        boolean failed = false;
        if (entry.getMoney() > 0){
            if (!giveMoney(player, entry.getMoney())) {
                player.sendMessage(ChatColor.RED + "Money refund failed!");
                failed = true;
            }
        }
        if (entry.getRemoveAmmo()) {
            if (!giveItem(player, entry.getAmmoItem())) {
                player.sendMessage(ChatColor.RED + "Ammo refund failed!");
                failed = true;
            }
        }
        return failed;
    }

    protected void randomizeProjectile(Projectile projectile, double random) {
        Vector velocity = projectile.getVelocity();
        randomizeVelocity(velocity, random);
        projectile.setVelocity(velocity);
    }

    protected void randomizeVelocity(Vector velocity, double random) {
        velocity.add(new Vector(
            ThreadLocalRandom.current().nextDouble(-random, random),
            ThreadLocalRandom.current().nextDouble(-random / 2, random / 2),
            ThreadLocalRandom.current().nextDouble(-random, random)
        ));
    }

    protected void randomizeLocation(Location location, double random) {
        location.add(new Vector(
            ThreadLocalRandom.current().nextDouble(-random, random),
            ThreadLocalRandom.current().nextDouble(-random / 2, random / 2),
            ThreadLocalRandom.current().nextDouble(-random, random)
        ));
    }

    protected boolean passGDPvpCheck(Location location) {
        if (gdHook==null) return true;
        String regionID = gdHook.getRegionID(location);
        return gdHook.hasPvPEnabled(regionID);
    }

    protected boolean passDCPvpLocCheck(Location location, double radius) {
        if (dcHook==null) return true;
        return location.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .noneMatch(player -> dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player));
    }
    
    protected boolean passDCPvpPlayerCheck(Player player) {
        if (dcHook==null) return true;
        else if (dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player)) return false;
        return true;
    }

    protected boolean passGDBuilderCheck(Player shooter, Location location, int radius) {
        if (gdHook==null) return true;
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

    protected boolean damagePlayers(Player shooter, Location location, double hRadius, double vRadius, double damage, int ignite) {
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
}