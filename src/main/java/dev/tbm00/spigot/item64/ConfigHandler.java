package dev.tbm00.spigot.item64;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.item64.model.ItemEntry;

public class ConfigHandler {
    private static boolean enabled;
    private static List<ItemEntry> itemEntries;
    private static Set<String> inactiveWorlds = new HashSet<>();
    private static boolean rewardedBreakingEnabled = false;
    private static String rewardedBreakingJoinMessage = "";
    private static int rewardedBreakingJoinMessageDelay = 0;
    private static double rewardedBreakingChance = 0.0;
    private static Set<String> rewardedBreaking = new HashSet<>();
    private static boolean preventedPlacingEnabled = false;
    private static String preventedPlacingMessage = "";
    private static Set<String> preventedPlacing = new HashSet<>();
    private static boolean preventedGrowingEnabled = false;
    private static Set<String> preventedGrowing = new HashSet<>();
    private static boolean checkAnchorExplosions = false;

    public ConfigHandler(JavaPlugin javaPlugin) {
        try {
            itemEntries = new ArrayList<>();
            
            // Load Hook: respawn anchor PVP check
            checkAnchorExplosions = javaPlugin.getConfig().getConfigurationSection("hooks.DeluxeCombat").getBoolean("anchorExplosionPvpCheck");

            // Load BreakEvent: inactive worlds
            ConfigurationSection eventSection = javaPlugin.getConfig().getConfigurationSection("breakEvent");
            List<String> worldsHolder = eventSection.getStringList("inactiveWorlds");
            inactiveWorlds.addAll(worldsHolder);

            // Load BreakEvent: block breaking
            rewardedBreakingEnabled = eventSection.getBoolean("rewardBlockBreaking.enabled");
            if (rewardedBreakingEnabled) {
                rewardedBreakingJoinMessage = eventSection.getString("rewardBlockBreaking.joinMessage");
                rewardedBreakingJoinMessageDelay = 20*eventSection.getInt("rewardBlockBreaking.joinMessageDelay");
                rewardedBreakingChance = eventSection.getDouble("rewardBlockBreaking.chance");
                List<String> rewardBlockHolder = eventSection.getStringList("rewardBlockBreaking.blocks");
                rewardedBreaking.addAll(rewardBlockHolder);
            }

            // Load BreakEvent: block placing
            preventedPlacingEnabled = eventSection.getBoolean("preventBlockPlacing.enabled");
            if (preventedPlacingEnabled) {
                List<String> preventBlockHolder = eventSection.getStringList("preventBlockPlacing.blocks");
                preventedPlacing.addAll(preventBlockHolder);
                preventedPlacingMessage = eventSection.getString("preventBlockPlacing.message");
            }

            // Load BreakEvent: block growth
            preventedGrowingEnabled = eventSection.getBoolean("preventBlockGrowth.enabled");
            if (preventedGrowingEnabled) {
                List<String> preventGrowHolder = eventSection.getStringList("preventBlockGrowth.blocks");
                preventedGrowing.addAll(preventGrowHolder);
            }

            // Load ItemEntries: custom items
            ConfigurationSection entriesSection = javaPlugin.getConfig().getConfigurationSection("itemEntries");
            if (entriesSection != null && entriesSection.getBoolean("enabled")) {
                enabled = true;
                for (String key : entriesSection.getKeys(false)) {
                    if (key.equalsIgnoreCase("enabled")) 
                        continue;
                    try {
                        int id = Integer.parseInt(key);
                        ConfigurationSection itemEntry = entriesSection.getConfigurationSection(key);
        
                        if (itemEntry != null && itemEntry.getBoolean("enabled")) {
                            List<String> rEffects = null, lEffects = null, commands = null, effects = null;
                            double power = 0.0;
                            boolean removeItem = false;
                            String message = null;

                            String KEY = itemEntry.contains("key") ? itemEntry.getString("key") : null;
                            String type = itemEntry.contains("type") ? itemEntry.getString("type") : "NULL";
                            String givePerm = itemEntry.contains("givePerm") ? itemEntry.getString("givePerm") : null;
                            String usePerm = itemEntry.contains("usePerm") ? itemEntry.getString("usePerm") : null;
                            String material = itemEntry.contains("item.mat") ? itemEntry.getString("item.mat") : null;
                            String name = itemEntry.contains("item.name") ? itemEntry.getString("item.name") : null;
                            List<String> lore = itemEntry.contains("item.lore") ? itemEntry.getStringList("item.lore") : null;
                            boolean hideEnchants = itemEntry.contains("item.hideEnchants") ? itemEntry.getBoolean("item.hideEnchants") : false;
                            List<String> enchants = itemEntry.contains("item.enchantments") ? itemEntry.getStringList("item.enchantments") : null;
                            double money = itemEntry.contains("usage.moneyCost") ? itemEntry.getDouble("usage.moneyCost") : 0.0;
                            int hunger = itemEntry.contains("usage.hungerCost") ? itemEntry.getInt("usage.hungerCost") : 0;
                            int cooldown = itemEntry.contains("usage.cooldown") ? itemEntry.getInt("usage.cooldown") : 0;
                            String ammoItem = itemEntry.contains("usage.ammoItem.mat") ? itemEntry.getString("usage.ammoItem.mat") : null;
                            boolean removeAmmo = itemEntry.contains("usage.ammoItem.removeAmmoItemOnUse") ? itemEntry.getBoolean("usage.ammoItem.removeAmmoItemOnUse") : false;
                            double random = itemEntry.contains("usage.projectile.shotRandomness") ? itemEntry.getDouble("usage.projectile.shotRandomness") : 0.0;
                            double damage = itemEntry.contains("usage.projectile.extraPlayerDamage") ? itemEntry.getDouble("usage.projectile.extraPlayerDamage") : 0.0;
                            if (type.equals("RANDOM_POTION")) {
                                rEffects = itemEntry.contains("usage.projectile.randomPotion.rightClickEffects") ? itemEntry.getStringList("usage.projectile.randomPotion.rightClickEffects") : null;
                                lEffects = itemEntry.contains("usage.projectile.randomPotion.leftClickEffects") ? itemEntry.getStringList("usage.projectile.randomPotion.leftClickEffects") : null;
                            } else if (type.equals("EXPLOSIVE_ARROW")) {
                                power = itemEntry.contains("usage.projectile.explosiveArrow.power") ? itemEntry.getInt("usage.projectile.explosiveArrow.power") : 0.0;
                            } else if (type.equals("USABLE") || type.equals("CONSUMABLE")) {
                                removeItem = itemEntry.contains("usage.triggers.removeItemOnUse") ? itemEntry.getBoolean("usage.triggers.removeItemOnUse") : false;
                                commands = itemEntry.contains("usage.triggers.consoleCommands") ? itemEntry.getStringList("usage.triggers.consoleCommands") : null;
                                effects = itemEntry.contains("usage.triggers.effects") ? itemEntry.getStringList("usage.triggers.effects") : null;
                                message = itemEntry.contains("usage.triggers.actionBarMessage") ? itemEntry.getString("usage.triggers.actionBarMessage") : null;
                            }
                            double rewardChance = itemEntry.contains("breakEvent.rewardChance") ? itemEntry.getDouble("breakEvent.rewardChance") : 0.0;
                            String rewardMessage = itemEntry.contains("breakEvent.rewardMessage") ? itemEntry.getString("breakEvent.rewardMessage") : null;
                            boolean giveItem = itemEntry.contains("breakEvent.giveRewardItem") ? itemEntry.getBoolean("breakEvent.giveRewardItem") : false;
                            List<String> rewardCommands = itemEntry.contains("breakEvent.rewardCommands") ? itemEntry.getStringList("breakEvent.rewardCommands") : null;

                            //javaPlugin.getLogger().info("entries["+ (id-1) + "] -> " + KEY + " @ " + rewardChance);
                            if (type != null && KEY != null) {
                                ItemEntry entry = new ItemEntry(javaPlugin, id, givePerm, usePerm, type, KEY, money, hunger, cooldown, random, damage, 
                                                            ammoItem, removeAmmo, material, name, lore, hideEnchants, enchants, removeItem, commands, message, effects, 
                                                            rEffects, lEffects, power, rewardChance, rewardMessage, giveItem, rewardCommands);
                                itemEntries.add(entry);
                                javaPlugin.getLogger().info("Loaded itemEntry: " + id + " " + KEY + " " + type + " " + material + " " + usePerm + " " + rewardChance);
                            } else {
                                javaPlugin.getLogger().warning("Error: Poorly defined itemEntry: " + id + " " + KEY + " " + type + " " + material + " " + usePerm + " " + rewardChance);
                            }
                        }
                    } catch (NumberFormatException e) {
                        javaPlugin.getLogger().warning("Skipping invalid itemEntry key: " + key);
                        continue;
                    }
                }
            } else enabled = false;
        } catch (Exception e) {
            javaPlugin.getLogger().warning("Caught exception loading config: " + e.getMessage());
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }


    public boolean getCheckAnchorExplosions() {
        return checkAnchorExplosions;
    }

    public List<ItemEntry> getItemEntries() {
        return itemEntries;
    }

    public ItemEntry getItemEntryByKeyString(String key) {
        for(ItemEntry entry : itemEntries) {
            if (entry.getKeyString().equals(key.toUpperCase())) return entry;
        }
        return null;
    }

    public Set<String> getInactiveWorlds() {
        return inactiveWorlds;
    }

    public boolean isRewardedBreakingEnabled() {
        return rewardedBreakingEnabled;
    }

    public String getRewardedBreakingJoinMessage() {
        return rewardedBreakingJoinMessage;
    }

    public int getRewardedBreakingJoinMessageDelay() {
        return rewardedBreakingJoinMessageDelay;
    }

    public double getRewardedBreakingChance() {
        return rewardedBreakingChance;
    }

    public Set<String> getRewardedBreaking() {
        return rewardedBreaking;
    }

    public boolean isPreventedPlacingEnabled() {
        return preventedPlacingEnabled;
    }

    public String getPreventedPlacingMessage() {
        return preventedPlacingMessage;
    }

    public Set<String> getPreventedPlacing() {
        return preventedPlacing;
    }

    public boolean isPreventedGrowingEnabled() {
        return preventedGrowingEnabled;
    }

    public Set<String> getPreventedGrowing() {
        return preventedGrowing;
    }
}
