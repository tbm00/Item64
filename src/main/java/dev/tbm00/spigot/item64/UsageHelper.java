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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

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
    private final WorldGuard wgHook;
    private final List<ItemEntry> itemEntries;
    private static final List<Long> cooldowns = new ArrayList<>();
    private static final Map<UUID, List<Long>> activeCooldowns = new HashMap<>();
    private static final ArrayList<Projectile> explosiveArrows = new ArrayList<>();
    private static final ArrayList<Projectile> lightningPearls = new ArrayList<>();
    private static final ArrayList<Projectile> magicPotions = new ArrayList<>();
    private static final int[][] CHECK_DIRECTIONS = {
        { 1, 0,  0},
        { -1, 0,  0},
        { 0, 0,  1}, 
        { 0, 0,  -1},
        {0, 1,  0},
        {0, -1, 0},
        {0, 0, 0}
    };

    public UsageHelper(Item64 item64, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook, WorldGuard wgHook) {
        this.item64 = item64;
        this.configHandler = configHandler;
        this.ecoHook = ecoHook;
        this.gdHook = gdHook;
        this.dcHook = dcHook;
        this.wgHook = wgHook;
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

    // HELPER: ALL ITEMS
    public boolean passUsageChecks(Player player, ItemEntry entry) {
        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- you're too hungry!"));
            return false;
        }
        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> cooldowns);
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(entry.getID()-1)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- active cooldown!"));
            return false;
        }
        if (entry.getAmmoItem()!=null && !entry.getAmmoItem().isBlank() && !hasItem(player, entry.getAmmoItem())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toLowerCase()));
            return false;
        }
        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(player, cost)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- not enough money!"));
            return false;
        }
        return true;
    }

    // HELPER: PVP ITEMS
    public boolean passShootingChecks(Player player, ItemEntry entry) {
        if (!passGDClaimPvpCheck(player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- claim pvp protection!"));
            return false;
        }
        if (!passWGRegionPvpCheck(player, player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- region pvp protection!"));
            return false;
        }
        if (!passGDClaimBuildCheck(player, player.getLocation(), 6)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- claim block/entity protection!"));
            return false;
        }
        if (!passWGRegionBuildCheck(player, player.getLocation(), 6)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Usage blocked -- region block/entity protection!"));
            return false;
        }
        return true;
    }

    // HELPER: PVP ITEMS
    public boolean passDamageChecks(Player shooter, Location location, ItemEntry entry) {
        if (!passDCPvpLocCheck(shooter, location, 6)) {
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
        if (!passGDClaimBuildCheck(shooter, location, 6)) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Damage blocked -- claim block/entity protection!"));
            return false;
        }
        if (!passWGRegionBuildCheck(shooter, location, 6)) {
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

        RegionContainer container = wgHook.getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(shooter);

        for (int[] offset : CHECK_DIRECTIONS) {
            Location loc = center.clone().add(radius*offset[0], radius*offset[1], radius*offset[2]);
            com.sk89q.worldedit.util.Location wgLocation = BukkitAdapter.adapt(loc);
            ApplicableRegionSet set = query.getApplicableRegions(wgLocation);

            if (set.isVirtual() || set.testState(localPlayer, Flags.BUILD)) continue;
            return false;
        }
        return true;
    }

    public boolean passWGRegionPvpCheck(Player shooter, Location location) {
        if (wgHook==null) return true;

        com.sk89q.worldedit.util.Location wgLocation = BukkitAdapter.adapt(location);
        RegionContainer container = wgHook.getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(wgLocation);
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(shooter);

        if (set.isVirtual()) return true;
        if (set.testState(localPlayer, Flags.PVP)) return true;
    
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

    public boolean hasItem(Player player, String itemName) {
        if (itemName == null 
            || itemName.equals("none")
            || itemName.isBlank()) return true;

        Material itemMaterial = Material.getMaterial(itemName.toUpperCase());
        if (itemMaterial == null) {
            item64.logRed("Error: Poorly defined item " + itemName);
            return false;
        }
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == itemMaterial && itemStack.getAmount() > 0)
                return true;
        }
        return false;
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