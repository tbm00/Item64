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
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import nl.marido.deluxecombat.api.DeluxeCombatAPI;
import dev.tbm00.spigot.item64.hook.GDHook;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class ListenerLeader {
    protected final JavaPlugin javaPlugin;
    protected final ItemManager itemManager;
    protected final Economy ecoHook;
    protected final GDHook gdHook;
    protected final DeluxeCombatAPI dcHook;
    protected final List<ItemEntry> itemCmdEntries;
    protected final List<String> ignorePlaced;
    protected final boolean checkAnchorExplosions;
    protected static final Map<UUID, List<Long>> activeCooldowns = new HashMap<>();
    protected static final ArrayList<Projectile> explosiveArrows = new ArrayList<>();
    protected static final ArrayList<Projectile> lightningPearls = new ArrayList<>();
    protected static final ArrayList<Projectile> magicPotions = new ArrayList<>();
    protected static final PotionEffectType[] positiveEFFECTS = {
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
    protected static final PotionEffectType[] negativeEFFECTS = {
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

    public ListenerLeader(JavaPlugin javaPlugin, ItemManager itemManager, Economy ecoHook, GDHook gdHook, DeluxeCombatAPI dcHook) {
        this.javaPlugin = javaPlugin;
        this.itemManager = itemManager;
        this.ecoHook = ecoHook;
        this.gdHook = gdHook;
        this.dcHook = dcHook;
        this.itemCmdEntries = itemManager.getItemEntries();
        this.ignorePlaced = javaPlugin.getConfig().getConfigurationSection("itemEntries").getStringList("stopBlockPlace");
        this.checkAnchorExplosions = javaPlugin.getConfig().getConfigurationSection("hooks.DeluxeCombat").getBoolean("anchorExplosionPvpCheck");
    }

    protected ItemEntry getItemEntryByItem(ItemStack item) {
        if (item == null) return null;
        if (!item.hasItemMeta() || item.getType() == Material.AIR || item.getType() == Material.RESPAWN_ANCHOR) 
            return null;
        return itemCmdEntries.stream()
            .filter(entry -> item.getItemMeta().getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING))
            .findFirst()
            .orElse(null);
    }

    protected void adjustCooldown(Player player, int index) {
        List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
        playerCooldowns.set(index, System.currentTimeMillis() / 1000);
        activeCooldowns.put(player.getUniqueId(), playerCooldowns);
    }

    protected void adjustCooldowns(Player player, List<Integer> hungers, int index) {
        player.setFoodLevel(Math.max(player.getFoodLevel() - hungers.get(index), 0));
        List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
        playerCooldowns.set(index, System.currentTimeMillis() / 1000);
        activeCooldowns.put(player.getUniqueId(), playerCooldowns);
    }

    protected void adjustHunger(Player player, List<Integer> hungers, int index) {
        player.setFoodLevel(Math.max(player.getFoodLevel() - hungers.get(index), 0));
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

    protected void randomizeVelocity(Projectile projectile, double random) {
        Vector velocity = projectile.getVelocity();
        velocity.add(new Vector(
            ThreadLocalRandom.current().nextDouble(-random, random),
            ThreadLocalRandom.current().nextDouble(-random / 2, random / 2),
            ThreadLocalRandom.current().nextDouble(-random, random)
        ));
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
        return location.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .noneMatch(player -> dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player));
    }
    
    protected boolean passDCPvpPlayerCheck(Player player) {
        if (dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player)) return false;
        return true;
    }

    protected boolean passGDBuilderCheck(Player shooter, Location location, int radius) {
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
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

    public void checkAnchorExplosion(PlayerInteractEvent event) {
        //Player player = event.getPlayer();
        //player.sendMessage("" + ChatColor.YELLOW + "test: anchor detected");

        if (!checkAnchorExplosions) return;
        //player.sendMessage("" + ChatColor.YELLOW + "test: checkAnchorExplosions on");

        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        //player.sendMessage("" + ChatColor.YELLOW + "test: RIGHT_CLICK_AIR || RIGHT_CLICK_BLOCK");
        
        //RespawnAnchor anchor = (RespawnAnchor) event.getClickedBlock().getBlockData();
        //if (anchor.getCharges() <= 3) return;

        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation();
        boolean passDCPvpPlayerCheck = true, passDCPvpLocCheck = true;
        
        if (dcHook != null) {
            if (!passDCPvpLocCheck(location, 6.0)) passDCPvpLocCheck = false;
            else if (!passDCPvpPlayerCheck(player)) passDCPvpPlayerCheck = false;
        }
        
        if (!passDCPvpPlayerCheck || !passDCPvpLocCheck) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- pvp protection!"));
            event.setCancelled(true);
        }/* else {
            player.sendMessage("" + ChatColor.YELLOW + "test: passed checks");
        }*/
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeCooldowns.remove(uuid);
    }
}