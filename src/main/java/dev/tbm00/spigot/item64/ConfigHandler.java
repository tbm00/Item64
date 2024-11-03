package dev.tbm00.spigot.item64;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.item64.model.ItemEntry;

public class ConfigHandler {
    private final JavaPlugin javaPlugin;
    private static boolean enabled = false;
    private static List<ItemEntry> itemEntries = new ArrayList<>();
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
        this.javaPlugin = javaPlugin;
        try {
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
            if (entriesSection != null) {
                enabled = true;
                for (String key : entriesSection.getKeys(false)) {
                    if (key.equalsIgnoreCase("enabled")) 
                        continue;
                    try {
                        int id = Integer.parseInt(key);
                        ConfigurationSection itemEntrySec = entriesSection.getConfigurationSection(key);
        
                        if (itemEntrySec != null && itemEntrySec.getBoolean("enabled"))
                            loadEntry(itemEntrySec, id);
                    } catch (NumberFormatException e) {
                        javaPlugin.getLogger().warning("Skipping invalid itemEntrySec key: " + key);
                        continue;
                    }
                }
            } else enabled = false;
        } catch (Exception e) {
            javaPlugin.getLogger().warning("Caught exception loading config: " + e.getMessage());
            enabled = false;
        }
    }

    private void loadEntry(ConfigurationSection itemEntrySec, int id) {
        List<String> rEffects = null, lEffects = null, commands = null, effects = null;
        double power = 0.0;
        boolean removeItem = false;
        String message = null;

        String KEY = itemEntrySec.contains("key") ? itemEntrySec.getString("key") : null;
        String type = itemEntrySec.contains("type") ? itemEntrySec.getString("type") : "NULL";
        String givePerm = itemEntrySec.contains("givePerm") ? itemEntrySec.getString("givePerm") : null;
        String usePerm = itemEntrySec.contains("usePerm") ? itemEntrySec.getString("usePerm") : null;
        String material = itemEntrySec.contains("item.mat") ? itemEntrySec.getString("item.mat") : null;
        String name = itemEntrySec.contains("item.name") ? itemEntrySec.getString("item.name") : null;
        List<String> lore = itemEntrySec.contains("item.lore") ? itemEntrySec.getStringList("item.lore") : null;
        boolean hideEnchants = itemEntrySec.contains("item.hideEnchants") ? itemEntrySec.getBoolean("item.hideEnchants") : false;
        List<String> enchants = itemEntrySec.contains("item.enchantments") ? itemEntrySec.getStringList("item.enchantments") : null;
        double money = itemEntrySec.contains("usage.moneyCost") ? itemEntrySec.getDouble("usage.moneyCost") : 0.0;
        int hunger = itemEntrySec.contains("usage.hungerCost") ? itemEntrySec.getInt("usage.hungerCost") : 0;
        int cooldown = itemEntrySec.contains("usage.cooldown") ? itemEntrySec.getInt("usage.cooldown") : 0;
        String ammoItem = itemEntrySec.contains("usage.ammoItem.mat") ? itemEntrySec.getString("usage.ammoItem.mat") : null;
        boolean removeAmmo = itemEntrySec.contains("usage.ammoItem.removeAmmoItemOnUse") ? itemEntrySec.getBoolean("usage.ammoItem.removeAmmoItemOnUse") : false;
        double random = itemEntrySec.contains("usage.projectile.shotRandomness") ? itemEntrySec.getDouble("usage.projectile.shotRandomness") : 0.0;
        double damage = itemEntrySec.contains("usage.projectile.extraPlayerDamage") ? itemEntrySec.getDouble("usage.projectile.extraPlayerDamage") : 0.0;
        if (type.equals("RANDOM_POTION")) {
            rEffects = itemEntrySec.contains("usage.projectile.randomPotion.rightClickEffects") ? itemEntrySec.getStringList("usage.projectile.randomPotion.rightClickEffects") : null;
            lEffects = itemEntrySec.contains("usage.projectile.randomPotion.leftClickEffects") ? itemEntrySec.getStringList("usage.projectile.randomPotion.leftClickEffects") : null;
        } else if (type.equals("EXPLOSIVE_ARROW")) {
            power = itemEntrySec.contains("usage.projectile.explosiveArrow.power") ? itemEntrySec.getInt("usage.projectile.explosiveArrow.power") : 0.0;
        } else if (type.equals("USABLE") || type.equals("CONSUMABLE")) {
            removeItem = itemEntrySec.contains("usage.triggers.removeItemOnUse") ? itemEntrySec.getBoolean("usage.triggers.removeItemOnUse") : false;
            commands = itemEntrySec.contains("usage.triggers.consoleCommands") ? itemEntrySec.getStringList("usage.triggers.consoleCommands") : null;
            effects = itemEntrySec.contains("usage.triggers.effects") ? itemEntrySec.getStringList("usage.triggers.effects") : null;
            message = itemEntrySec.contains("usage.triggers.actionBarMessage") ? itemEntrySec.getString("usage.triggers.actionBarMessage") : null;
        }
        double rewardChance = itemEntrySec.contains("breakEvent.rewardChance") ? itemEntrySec.getDouble("breakEvent.rewardChance") : 0.0;
        String rewardMessage = itemEntrySec.contains("breakEvent.rewardMessage") ? itemEntrySec.getString("breakEvent.rewardMessage") : null;
        boolean giveItem = itemEntrySec.contains("breakEvent.giveRewardItem") ? itemEntrySec.getBoolean("breakEvent.giveRewardItem") : false;
        List<String> rewardCommands = itemEntrySec.contains("breakEvent.rewardCommands") ? itemEntrySec.getStringList("breakEvent.rewardCommands") : null;

        //javaPlugin.getLogger().info("entries["+ (id-1) + "] -> " + KEY + " @ " + rewardChance);
        if (type != null && KEY != null) {
            ItemEntry entry = new ItemEntry(javaPlugin, id, givePerm, usePerm, type, KEY, money, hunger, cooldown, random, damage, 
                                        ammoItem, removeAmmo, material, name, lore, hideEnchants, enchants, removeItem, commands, message, effects, 
                                        rEffects, lEffects, power, rewardChance, rewardMessage, giveItem, rewardCommands);
            itemEntries.add(entry);
            javaPlugin.getLogger().info("Loaded itemEntrySec: " + id + " " + KEY + " " + type + " " + material + " " + usePerm + " " + rewardChance);
        } else {
            javaPlugin.getLogger().warning("Error: Poorly defined itemEntry: " + id + " " + KEY + " " + type + " " + material + " " + usePerm + " " + rewardChance);
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
