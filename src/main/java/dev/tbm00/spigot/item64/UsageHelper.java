package dev.tbm00.spigot.item64;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class UsageHelper {
    private final Item64 item64;
    private final ConfigHandler configHandler;
    private final Economy ecoHook;
    private final GDHook gdHook;
    private final DCHook dcHook;
    private final List<ItemEntry> itemEntries;
    public static final List<Long> cooldowns = new ArrayList<>();
    public static final Map<UUID, List<Long>> activeCooldowns = new HashMap<>();
    public static final ArrayList<Projectile> explosiveArrows = new ArrayList<>();
    public static final ArrayList<Projectile> lightningPearls = new ArrayList<>();
    public static final ArrayList<Projectile> magicPotions = new ArrayList<>();

    public UsageHelper(Item64 item64, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        this.item64 = item64;
        this.configHandler = configHandler;
        this.ecoHook = ecoHook;
        this.gdHook = gdHook;
        this.dcHook = dcHook;
        itemEntries = configHandler.getItemEntries();

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
                else return;
            }

            // load `cooldowns`
            for (ItemEntry entry : itemEntries) {
                int index = entry.getID()-1;
                cooldowns.set(index, Long.valueOf(entry.getCooldown()));
            }
        }
    }

    public ItemEntry getItemEntryByItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getType() == Material.AIR)
            return null;
        return itemEntries.stream()
            .filter(entry -> item.getItemMeta().getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING))
            .findFirst()
            .orElse(null);
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

    // 0 - doesnt have ammo & action blocked
    // 1 - doesnt have ammo & action permitted
    // 2 - has ammo & action permitted
    public int hasItem(Player player, String itemName) {
        if (itemName == null 
            || itemName.equals("none")
            || itemName.isBlank()) return 1;

        Material itemMaterial = Material.getMaterial(itemName.toUpperCase());
        if (itemMaterial == null) {
            item64.logRed("Error: Poorly defined item " + itemName);
            return 1;
        }
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == itemMaterial && itemStack.getAmount() > 0)
                return 2;
        }
        return 0;
    }

    // doesn't require checks as hasItem should always get called before
    public boolean removeItem(Player player, String itemName) {
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

    public boolean giveItem(Player player, String itemName) {
        if (itemName.equals("none")
            || itemName.isBlank()
            || itemName == null) return true;

        Material itemMaterial = Material.getMaterial(itemName.toUpperCase());
        if (itemMaterial == null) {
            item64.logRed("Error: Unknown item " + itemMaterial);
            return false;
        }

        ItemStack item = new ItemStack(itemMaterial);
        item.setAmount(1);
        player.getInventory().addItem(item);
        return true;
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
            player.sendMessage(ChatColor.DARK_GREEN + "$$$: " + ChatColor.YELLOW + "Charged $" + iAmount + "");
            return true;
        } else return false;
    }

    public boolean giveMoney(Player player, double amount) {
        if (ecoHook==null || amount <= 0) return true;

        int iAmount = (int) amount;
        EconomyResponse r = ecoHook.depositPlayer(player, amount);
        if (r.transactionSuccess()) {
            player.sendMessage(ChatColor.DARK_GREEN + "$$$: " + ChatColor.YELLOW + "Refunded $" + iAmount + "");
            return true;
        } else return false;
    }

    public boolean refundPlayer(Player player, ItemEntry entry) {
        boolean failed = false;
        if (entry.getMoney() > 0){
            if (!giveMoney(player, entry.getMoney())) {
                player.sendMessage(ChatColor.RED + "Money refund failed!");
                failed = true;
            }
        }
        if (entry.getRemoveAmmo() && !entry.getAmmoItem().isBlank() && entry.getAmmoItem()!=null) {
            if (!giveItem(player, entry.getAmmoItem())) {
                player.sendMessage(ChatColor.RED + "Ammo refund failed!");
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

    public boolean passGDPvpCheck(Location location) {
        if (gdHook==null) return true;
        String regionID = gdHook.getRegionID(location);
        return gdHook.hasPvPEnabled(regionID);
    }

    public boolean passDCPvpLocCheck(Location location, double radius) {
        if (dcHook==null) return true;
        return location.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .noneMatch(player -> dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player));
    }
    
    public boolean passDCPvpPlayerCheck(Player player) {
        if (dcHook==null) return true;
        else if (dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player)) return false;
        return true;
    }

    public boolean passGDBuilderCheck(Player shooter, Location location, int radius) {
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

    public boolean damageEntities(Player shooter, Location location, double hRadius, double vRadius, double damage, int ignite) {
        Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, hRadius, vRadius, hRadius);
        boolean damaged = false;
        int count = 0;
        for (Entity entity : nearbyEntities) {
            if (count>=4) return damaged;
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.damage(damage, shooter);
                if (ignite>0) livingEntity.setFireTicks(ignite);
                //player.sendMessage(ChatColor.RED + "hit: " + ChatColor.GRAY + damage + ChatColor.RED + ", by: " + ChatColor.GRAY + shooter);
                //shooter.sendMessage(ChatColor.RED + "hit: " + ChatColor.GRAY + damage + ChatColor.RED + ", on: " + ChatColor.GRAY + player);
                damaged = true;
                count++;
            }
        }
        return damaged;
    }

    // HELPER: CONSUMABLE, USABLE
    public boolean applyEffects(Player player, List<String> effects, ItemStack item, boolean removeItem) {
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
                    return false;
                }
            } catch (Exception e) {
                item64.logRed("Error parsing effect: " + line + " - ");
                item64.getLogger().warning(e.getMessage());
                return false;
            }
        }
        return true;
    }

    // HELPER: ALL
    // if it doesnt pass, returns 0, if it does, it returns hasAmmoItem
    public int passUsageChecks(Player player, ItemEntry entry, Projectile projectile) {
        // Do claim-pvp, hunger, cooldown, ammo, and curreny checks & charges
        if (!passGDPvpCheck(player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- claim pvp protection!"));
            return 0;
        }

        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- you're too hungry!"));
            return 0;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> cooldowns);
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(entry.getID()-1)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- active cooldown!"));
            return 0;
        }

        int hasAmmoItem = hasItem(player, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toLowerCase()));
            return 0;
        }

        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(player, cost)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- not enough money!"));
            return 0;
        }
        return hasAmmoItem;
    }

    // HELPER: EXPLOSIVE_ARROW, LIGHTNING_PEARL, FLAME_PARTICLE
    public boolean passDamageChecks(Player player, Location location, ItemEntry entry) {
        boolean passDCPvpLocCheck = true, passGDPvpCheck = true, passGDBuilderCheck = true;

        if (dcHook != null && !passDCPvpLocCheck(location, 4.0)) passDCPvpLocCheck = false;
        if (gdHook != null) {
            if (!passGDPvpCheck(location)) passGDPvpCheck = false;
            else if (!passGDPvpCheck(location)) passGDPvpCheck = false;
            else if (!passGDBuilderCheck(player, location, 6)) passGDBuilderCheck = false;
        }

        if (!passDCPvpLocCheck) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Damage blocked -- pvp protection!"));
            return false;
        } else if (!passGDPvpCheck) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Damage blocked -- claim pvp protection!"));
            return false;
        } else if (!passGDBuilderCheck && entry.getType().equalsIgnoreCase("EXPLOSIVE_ARROW")) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Damage blocked -- claim block protection!"));
            return false;
        }
        return true;
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
}