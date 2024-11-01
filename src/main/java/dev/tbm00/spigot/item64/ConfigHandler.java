package dev.tbm00.spigot.item64;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.item64.model.ItemEntry;

public class ConfigHandler {
    private final Boolean enabled;
    private final List<ItemEntry> itemEntries;
    private final List<String> ignorePlaced;
    private final boolean checkAnchorExplosions;

    public ConfigHandler(JavaPlugin javaPlugin) {
        itemEntries = new ArrayList<>();
        ignorePlaced = javaPlugin.getConfig().getConfigurationSection("itemEntries").getStringList("stopBlockPlace");
        checkAnchorExplosions = javaPlugin.getConfig().getConfigurationSection("hooks.DeluxeCombat").getBoolean("anchorExplosionPvpCheck");

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
                        String KEY = itemEntry.getString("key"),
                            type = itemEntry.getString("type"),
                            givePerm = itemEntry.getString("givePerm"),
                            usePerm = itemEntry.getString("usePerm"),
                            ammoItem = itemEntry.getString("usage.ammoItem.mat");
                        boolean removeAmmo = itemEntry.getBoolean("usage.ammoItem.removeAmmoItemOnUse");
                        double money = itemEntry.getDouble("usage.moneyCost");
                        int hunger = itemEntry.getInt("usage.hungerCost"),
                            cooldown = itemEntry.getInt("usage.cooldown");
                        double random = itemEntry.getDouble("usage.projectile.shotRandomness"),
                            damage = itemEntry.getDouble("usage.projectile.extraPlayerDamage");
                        String material = itemEntry.getString("item.mat"),
                            name = itemEntry.getString("item.name");
                        List<String> lore = itemEntry.getStringList("item.lore");
                        Boolean hideEnchants = itemEntry.getBoolean("item.hideEnchants");
                        List<String> enchants = itemEntry.getStringList("item.enchantments");
                        boolean removeItem = false;
                        List<String> commands = null, effects = null, rEffects = null, lEffects = null;
                        float power = 0;
    
                        switch (type) {
                            case "CONSUMABLE" -> {
                                removeItem = itemEntry.getBoolean("usage.consumable.removeConsumableOnUse");
                                commands = itemEntry.getStringList("usage.consumable.consoleCommands");
                                effects = itemEntry.getStringList("usage.consumable.effects");
                            }
                            case "RANDOM_POTION" -> {
                                rEffects = itemEntry.getStringList("usage.projectile.randomPotion.rightClickEffects");
                                lEffects = itemEntry.getStringList("usage.projectile.randomPotion.leftClickEffects");
                            }
                            case "EXPLOSIVE_ARROW" -> {
                                power = itemEntry.getInt("usage.projectile.explosiveArrow.power");
                            }
                            default -> {}
                        }
    
                        if (usePerm != null && givePerm != null && type != null && KEY != null) {
                            ItemEntry entry = new ItemEntry(javaPlugin, id, givePerm, usePerm, type, KEY, money, hunger, cooldown, random, damage, ammoItem, removeAmmo, material, name, lore, hideEnchants, enchants, removeItem, commands, effects, rEffects, lEffects, power);
                            itemEntries.add(entry);
                            javaPlugin.getLogger().info("Loaded itemEntry: " + id + " " + KEY + " " + type + " " + material + " " + usePerm);
                        } else {
                            javaPlugin.getLogger().warning("Error: Poorly defined itemEntry: " + id + " " + KEY + " " + type + " " + material + " " + usePerm);
                        }
                    }
                } catch (NumberFormatException e) {
                    javaPlugin.getLogger().warning("Skipping invalid itemEntry key: " + key);
                    continue;
                }
            }
        } else enabled = false;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public List<String> getIgnoredPlaced() {
        return ignorePlaced;
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
}
