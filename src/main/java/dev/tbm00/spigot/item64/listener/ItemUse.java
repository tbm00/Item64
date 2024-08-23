package dev.tbm00.spigot.item64.listener;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

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
    private final ArrayList<Entity> explosiveArrows = new ArrayList<>();
    private final ArrayList<Entity> lightningPearls = new ArrayList<>();

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
        ItemEntry entry = getItemEntry(item);
        if (entry == null) return;

        String entryType = entry.getType();
        if (entryType.equals("EXPLOSIVE_ARROW")) return;

        if (player.hasPermission(entry.getUsePerm())) {
            switch (entryType) {
                /* case "EXPLOSIVE_ARROW":
                    // has custom listeners: onBowShoot & onProjectileHit
                    // activeCooldowns index: 0
                    break; */
                case "LIGHTNING_PEARL": {
                    // activeCooldowns index: 1
                    if (!activeCooldowns.containsKey(player.getUniqueId())) activeCooldowns.put(player.getUniqueId(), itemManager.getCooldowns());
                    List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
                    if (player.getFoodLevel() >= entry.getHunger()) {
                        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(1)) >= entry.getCooldown()) {
                            if (removeAmmoItem(player, entryType)) {
                                //player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting lightning..."));
                                EnderPearl pearl = player.launchProjectile(EnderPearl.class);
                                pearl.setMetadata("lightning", new FixedMetadataValue(javaPlugin, "pearl"));
                                lightningPearls.add(pearl);

                                adjustCooldowns(player, itemManager.getHungers(), 1);
                            }
                        } else {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked: active cooldown!"));
                        }
                    } else {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Shot blocked: you're too hungry!"));
                    }
                    break;
                }
                case "FLAME": {
                    // activeCooldowns index: 2
                    break;
                }
                case "RANDOM_POTION": {
                    // activeCooldowns index: 3
                    break;
                }
                default:
                    break;
                }
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "You lack the ability to use this item!"));
            event.setCancelled(true);
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
                if (player.hasPermission(entry.getUsePerm())) {
                    if (!activeCooldowns.containsKey(player.getUniqueId())) activeCooldowns.put(player.getUniqueId(), itemManager.getCooldowns());
                    List<Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
                    
                    if (player.getFoodLevel() >= entry.getHunger()) {
                        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(0)) >= entry.getCooldown()) {
                            //player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting explosive arrow..."));
                            explosiveArrows.add(arrow);
                            adjustCooldowns(player, itemManager.getHungers(), 2);
                        } else {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Exploson blocked -- active cooldown!"));
                        }
                    } else {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- you're too hungry!"));
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Explosion blocked -- skill issue!");
                    event.setCancelled(true);
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
            boolean passPvpCheck = true, passClaimBuilderCheck = true;

            if (dcHook != null) {
                if (!passPvpCheck(location, player, 5)) passPvpCheck = false;
            }
            if (gdHook != null) {
                if (!passClaimBuilderCheck(location, player, 5)) passClaimBuilderCheck = false;
            }

            if (!passPvpCheck) {
                arrow.remove();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- togglepvp protection!"));
            } else if (!passClaimBuilderCheck) {
                //arrow.getWorld().createExplosion(location, 1.5F, true, false);
                arrow.remove();
                //player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "Explosion nerfed -- claim protection!"));
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "Explosion blocked -- claim protection!"));
            } else {
                arrow.getWorld().createExplosion(location, 2.5F, true, true);
                arrow.remove();
            }
        } else if (event.getEntity() instanceof EnderPearl) {
            EnderPearl pearl = (EnderPearl) event.getEntity();
            if (!lightningPearls.contains(pearl)) return;
            lightningPearls.remove(pearl);
            
            Location location = pearl.getLocation();
            Player player = (Player) pearl.getShooter();
            boolean passPvpCheck = true;
            
            /*//javaPlugin.getLogger().info("if... " + gdHook);
            if (gdHook != null) {
                if (!passClaimCheck(location, player)) passClaimCheck = false;
            }*/
            if (dcHook != null) {
                if (!passPvpCheck(location, player, 4)) passPvpCheck = false;
            }

            if (!passPvpCheck) {
                event.setCancelled(true);
                pearl.remove();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lightning blocked -- togglepvp protection!"));
            } else {
                event.setCancelled(true);
                location.getWorld().strikeLightning(location);
                pearl.remove();
            }
        } 
    }

    private ItemEntry getItemEntry(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getType() == Material.AIR) return null;
        else {
            for (ItemEntry entry : itemCmdEntries) {
                if (item.getItemMeta().getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING)) return entry;
            }
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

    private boolean passClaimBuilderCheck(Location location, Player player, int radius) {
        int negRadius = -1*radius;
        Location newLocation = location.clone();
        String newRegionID = gdHook.getRegionID(newLocation);
        if (!gdHook.hasBuilderTrust(player, newRegionID)) return false;

        newLocation.add(radius, radius, radius);
        newRegionID = gdHook.getRegionID(newLocation);
        if (!gdHook.hasBuilderTrust(player, newRegionID)) return false;

        newLocation.add(0, 0, (2*negRadius));
        newRegionID = gdHook.getRegionID(newLocation);
        if (!gdHook.hasBuilderTrust(player, newRegionID)) return false;

        newLocation.add((2*negRadius), 0, 0);
        newRegionID = gdHook.getRegionID(newLocation);
        if (!gdHook.hasBuilderTrust(player, newRegionID)) return false;

        newLocation.add(0, 0, (2*radius));
        newRegionID = gdHook.getRegionID(newLocation);
        if (!gdHook.hasBuilderTrust(player, newRegionID)) return false;

        newLocation.add(0, (2*negRadius), 0);
        newRegionID = gdHook.getRegionID(newLocation);
        if (!gdHook.hasBuilderTrust(player, newRegionID)) return false;

        newLocation.add((2*radius), 0, 0);
        newRegionID = gdHook.getRegionID(newLocation);
        if (!gdHook.hasBuilderTrust(player, newRegionID)) return false;

        newLocation.add(0, 0, (2*negRadius));
        newRegionID = gdHook.getRegionID(newLocation);
        if (!gdHook.hasBuilderTrust(player, newRegionID)) return false;

        newLocation.add((2*negRadius), 0, 0);
        newRegionID = gdHook.getRegionID(newLocation);
        if (!gdHook.hasBuilderTrust(player, newRegionID)) return false;

        return true;
    }

    private boolean passPvpCheck(Location location, Player player, int radius) {
        if (dcHook.hasProtection(player) || !dcHook.hasPvPEnabled(player)) return false;
        int chunkRadius = radius < 16 ? 1 : (radius - (radius % 16)) / 16;
        
        for (int chX = 0 - chunkRadius; chX <= chunkRadius; chX++) {
            for (int chZ = 0 - chunkRadius; chZ <= chunkRadius; chZ++) {
                int x = (int) location.getX(), y = (int) location.getY(), z = (int) location.getZ();
                for (Entity entity : new Location(location.getWorld(), x + (chX * 16), y, z + (chZ * 16)).getChunk().getEntities()) {
                    if (entity.getLocation().distance(location) <= radius && entity instanceof Player) {
                        Player p = (Player) entity;
                        if (dcHook.hasProtection(p) || !dcHook.hasPvPEnabled(p)) return false;
                    }
                }
            }
        }
        return true;
    }
}