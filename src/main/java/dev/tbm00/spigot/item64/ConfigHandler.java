package dev.tbm00.spigot.item64;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import dev.tbm00.spigot.item64.model.ItemEntry;

public class ConfigHandler {
    private final Item64 item64;
    private boolean enabled = false;
    
    private boolean vaultEnabled = false;
    private boolean deluxeCombatEnabled = false;
    private boolean griefDefenderEnabled = false;
    private boolean worldGuardEnabled = false;

    public final int PROTECTION_RADIUS = 6;

    private List<String> ignoredClaims = null;

    private Set<String> inactiveWorlds = new HashSet<>();
    private boolean rewardedBreakingEnabled = false;
    private String rewardedBreakingJoinMessage = "";
    private int rewardedBreakingJoinMessageDelay = 0;
    private double rewardedBreakingChance = 0.0;
    private Set<String> rewardedBreaking = new HashSet<>();
    private boolean preventedPlacingEnabled = false;
    private String preventedPlacingMessage = "";
    private Set<String> preventedPlacing = new HashSet<>();
    private boolean preventedGrowingEnabled = false;
    private boolean preventedGrowingLog = false;
    private Set<String> preventedGrowing = new HashSet<>();
    
    private List<ItemEntry> itemEntries = new ArrayList<>();

    public ConfigHandler(Item64 item64) {
        this.item64 = item64;
        try {
            // Load Hooks
            ConfigurationSection hookSection = item64.getConfig().getConfigurationSection("hooks");
            if (hookSection != null) {
                loadHooks(hookSection);
            }

            // Load BreakEvent
            ConfigurationSection eventSection = item64.getConfig().getConfigurationSection("breakEvent");
            if (eventSection != null) {
                loadBreakEvent(eventSection);
            }
            
            // Load ItemEntries
            ConfigurationSection entriesSection = item64.getConfig().getConfigurationSection("itemEntries");
            if (entriesSection != null) {
                loadItemEntries(entriesSection);
            } else enabled = false;
        } catch (Exception e) {
            item64.logRed("Caught exception loading config: ");
            item64.getLogger().warning(e.getMessage());
            enabled = false;
        }
    }

    private void loadHooks(ConfigurationSection hookSection) {
        if (hookSection.getBoolean("Vault.enabled")) {
            vaultEnabled = true;
        }
        if (hookSection.getBoolean("DeluxeCombat.enabled")) {
            deluxeCombatEnabled = true;
        }
        if (hookSection.getBoolean("GriefDefender.enabled")) {
            griefDefenderEnabled = true;
            ignoredClaims = hookSection.getStringList("GriefDefender.ignoredClaims");
        }
        if (hookSection.getBoolean("WorldGuard.enabled")) {
            worldGuardEnabled = true;
        }
    }

    private void loadBreakEvent(ConfigurationSection eventSection) {
        // active worlds
        List<String> worldsHolder = eventSection.getStringList("inactiveWorlds");
        inactiveWorlds.addAll(worldsHolder);

        // reward block breaking listener
        rewardedBreakingEnabled = eventSection.getBoolean("rewardBlockBreaking.enabled");
        if (rewardedBreakingEnabled) {
            rewardedBreakingJoinMessage = eventSection.getString("rewardBlockBreaking.joinMessage");
            rewardedBreakingJoinMessageDelay = eventSection.getInt("rewardBlockBreaking.joinMessageDelay");
            rewardedBreakingChance = eventSection.getDouble("rewardBlockBreaking.chance");
            List<String> rewardBlockHolder = eventSection.getStringList("rewardBlockBreaking.blocks");
            rewardedBreaking.addAll(rewardBlockHolder);
            item64.logGreen("rewardBlockBreaking is enabled. Rewarding breaking of: " + rewardBlockHolder);
        } else {
            item64.logYellow("rewardBlockBreaking is disabled.");
        }

        // prevent block placing listener
        preventedPlacingEnabled = eventSection.getBoolean("preventBlockPlacing.enabled");
        if (preventedPlacingEnabled) {
            List<String> preventBlockHolder = eventSection.getStringList("preventBlockPlacing.blocks");
            preventedPlacing.addAll(preventBlockHolder);
            preventedPlacingMessage = eventSection.getString("preventBlockPlacing.message");
            item64.logGreen("preventBlockPlacing is enabled. Preventing placement of: " + preventBlockHolder);
        } else {
            item64.logYellow("preventBlockPlacing is disabled.");
        }

        // prevent block growth listener
        preventedGrowingEnabled = eventSection.getBoolean("preventBlockGrowth.enabled");
        if (preventedGrowingEnabled) {
            preventedGrowingLog = eventSection.getBoolean("preventBlockGrowth.logInConsole");
            List<String> preventGrowHolder = eventSection.getStringList("preventBlockGrowth.blocks");
            preventedGrowing.addAll(preventGrowHolder);
            item64.logGreen("preventBlockGrowth is enabled. Preventing growth of: " + preventGrowHolder);
        } else {
            item64.logYellow("preventBlockGrowth is disabled.");
        }
    }

    private void loadItemEntries(ConfigurationSection entriesSection) {
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
                item64.logRed("Skipping invalid itemEntry key: " + key);
                continue;
            }
        }
    }

    private void loadEntry(ConfigurationSection itemEntrySec, int id) {
        List<String> rEffects = null, lEffects = null, commands = null, effects = null;
        double power = 0.0;
        int radius = 0;
        boolean removeItem = false;
        String message = null, breakType = null;

        String KEY = itemEntrySec.contains("key") ? itemEntrySec.getString("key") : null;
        String type = itemEntrySec.contains("type") ? itemEntrySec.getString("type") : "NULL";
        String givePerm = itemEntrySec.contains("givePerm") ? itemEntrySec.getString("givePerm") : "item64.na";
        String usePerm = itemEntrySec.contains("usePerm") ? itemEntrySec.getString("usePerm") : "item64.na";
        String materialStr = itemEntrySec.contains("item.mat") ? itemEntrySec.getString("item.mat") : null;
        Material material = Material.getMaterial(materialStr);
        if (material == null && !type.equalsIgnoreCase("NO_ITEM")) {
            item64.logRed("Error loading item: Unknown material: " + materialStr);
            return;
        }
        String name = itemEntrySec.contains("item.name") ? itemEntrySec.getString("item.name") : null;
        List<String> lore = itemEntrySec.contains("item.lore") ? itemEntrySec.getStringList("item.lore") : null;
        boolean hideEnchants = itemEntrySec.contains("item.hideEnchants") ? itemEntrySec.getBoolean("item.hideEnchants") : false;
        List<String> enchants = itemEntrySec.contains("item.enchantments") ? itemEntrySec.getStringList("item.enchantments") : null;
        double money = itemEntrySec.contains("usage.moneyCost") ? itemEntrySec.getDouble("usage.moneyCost") : 0.0;
        double hungerChance = itemEntrySec.contains("usage.hungerChance") ? itemEntrySec.getDouble("usage.hungerChance") : 100.0;
        int hunger = itemEntrySec.contains("usage.hungerCost") ? itemEntrySec.getInt("usage.hungerCost") : 0;
        int cooldown = itemEntrySec.contains("usage.cooldown") ? itemEntrySec.getInt("usage.cooldown") : 0;
        String ammoItemStr = itemEntrySec.contains("usage.ammoItem.mat") ? itemEntrySec.getString("usage.ammoItem.mat") : null;
        Material ammoItem = Material.getMaterial(ammoItemStr);
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
        } else if (type.equals("AREA_BREAK")) {
            radius = itemEntrySec.contains("usage.breakage.radius") ? itemEntrySec.getInt("usage.breakage.radius") : 0;
            breakType = itemEntrySec.contains("usage.breakage.type") ? itemEntrySec.getString("usage.breakage.type") : "2D";
        } 
        double rewardChance = itemEntrySec.contains("breakEvent.rewardChance") ? itemEntrySec.getDouble("breakEvent.rewardChance") : 0.0;
        String rewardMessage = itemEntrySec.contains("breakEvent.rewardMessage") ? itemEntrySec.getString("breakEvent.rewardMessage") : null;
        boolean giveItem = itemEntrySec.contains("breakEvent.giveRewardItem") ? itemEntrySec.getBoolean("breakEvent.giveRewardItem") : false;
        List<String> rewardCommands = itemEntrySec.contains("breakEvent.rewardCommands") ? itemEntrySec.getStringList("breakEvent.rewardCommands") : null;

        //item64.logYellow("entries["+ (id-1) + "] -> " + KEY + " @ " + rewardChance);
        if (type != null && KEY != null) {
            ItemEntry entry = new ItemEntry(item64, id, givePerm, usePerm, type, KEY, money, hunger, hungerChance, cooldown, random, damage, 
                                        ammoItem, removeAmmo, material, name, lore, hideEnchants, enchants, removeItem, commands, message, effects, 
                                        rEffects, lEffects, power, rewardChance, rewardMessage, giveItem, rewardCommands, radius, breakType);
            itemEntries.add(entry);
            item64.logGreen("Loaded itemEntry: " + id + ") " + KEY + ", " + type + ", " + rewardChance + "%");
        } else {
            item64.logRed("Errored itemEntry: " + id + ") " + KEY + ", " + type + ", " + rewardChance + "%");
        }
    }

    public ItemStack getConfiguredItemStack(ItemEntry entry) {
        if (entry.getMaterial()==null) return null;

        ItemStack item = new ItemStack(entry.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return null;

        // Apply name
        if (entry.getName()!=null && !entry.getName().isBlank()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', entry.getName()));
        }

        // Apply lore
        if (!entry.getLore().isEmpty()) {
            meta.setLore(entry.getLore().stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        }
        
        // Apply enchanctments
        if (entry.getHideEnchants()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (!entry.getEnchants().isEmpty() && entry.getEnchants()!=null) {
            for (String line : entry.getEnchants()) {
                String[] parts = line.split(":");
                String name = parts[0];
                int level = Integer.parseInt(parts[1]);
    
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
                if (enchantment != null) {
                    meta.addEnchant(enchantment, level, true);
                } else {
                    item64.logRed("Unknown enchantment '" + name + "' in " + entry.getKeyString());
                }
            }
        }

        // Apply key
        if (entry.getKeyString()!=null && !entry.getKeyString().isBlank()) {
            meta.getPersistentDataContainer().set(new NamespacedKey(item64, entry.getKeyString()), PersistentDataType.STRING, "true");
        } else return null;

        // Apply meta and return
        item.setItemMeta(meta);
        return item;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isVaultEnabled() {
        return vaultEnabled;
    }
    
    public boolean isDeluxeCombatEnabled() {
        return deluxeCombatEnabled;
    }
    
    public boolean isGriefDefenderEnabled() {
        return griefDefenderEnabled;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
    
    public List<String> getIgnoredClaims() {
        return ignoredClaims;
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

    public boolean isPreventedGrowingLogged() {
        return preventedGrowingLog;
    }

    public Set<String> getPreventedGrowing() {
        return preventedGrowing;
    }
}
