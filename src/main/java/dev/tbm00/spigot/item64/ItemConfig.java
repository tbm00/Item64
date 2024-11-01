package dev.tbm00.spigot.item64;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.item64.model.ItemEntry;

public class ItemConfig {
    private final Boolean enabled;
    private final List<ItemEntry> itemEntries;
    private final List<String> ignorePlaced;
    private final boolean checkAnchorExplosions;

    public ItemConfig(JavaPlugin javaPlugin) {
        itemEntries = new ArrayList<>();
        ignorePlaced = javaPlugin.getConfig().getConfigurationSection("itemEntries").getStringList("stopBlockPlace");
        checkAnchorExplosions = javaPlugin.getConfig().getConfigurationSection("hooks.DeluxeCombat").getBoolean("anchorExplosionPvpCheck");

        ConfigurationSection entriesSection = javaPlugin.getConfig().getConfigurationSection("itemEntries");
        if (entriesSection != null && entriesSection.getBoolean("enabled")) {
            enabled = true;
            for (String key : entriesSection.getKeys(false)) {
                ConfigurationSection itemEntry = entriesSection.getConfigurationSection(key);
                int id = Integer.parseInt(key);

                if (itemEntry != null && itemEntry.getBoolean("enabled")) {
                    String type = itemEntry.getString("type");
                    String KEY = itemEntry.getString("key"),
                            givePerm = itemEntry.getString("givePerm"),
                            usePerm = itemEntry.getString("usePerm");
                    double money = itemEntry.getDouble("moneyCost");
                    int hunger = itemEntry.getInt("hungerCost"),
                        cooldown = itemEntry.getInt("cooldown");
                    double random = itemEntry.getDouble("shotRandomness"),
                            damage = itemEntry.getDouble("extraDamage");
                    String ammoItem = itemEntry.getString("ammoItem"),
                            item = itemEntry.getString("item"),
                            name = itemEntry.getString("name");
                    List<String> lore = itemEntry.getStringList("lore");
                    Boolean hideEnchants = itemEntry.getBoolean("hideEnchants");
                    List<String> enchants = itemEntry.getStringList("enchantments");
                    boolean removeItem = false;
                    List<String> commands = null;

                    switch (type) {
                        case "CONSUME_COMMANDS" -> {
                            removeItem = itemEntry.getBoolean("removeConsumedItem");
                            commands = itemEntry.getStringList("consoleCommands");
                        }
                        case "CONSUME_EFFECTS" -> {
                            removeItem = itemEntry.getBoolean("removeConsumedItem");
                        }
                        default -> {}
                    }
                    
                    if (usePerm != null && givePerm != null && type != null && key != null ) {
                        ItemEntry entry = new ItemEntry(javaPlugin, id, givePerm, usePerm, type, KEY, money, hunger, cooldown, random, damage, ammoItem, item, name, lore, hideEnchants, enchants, removeItem, commands);
                        itemEntries.add(entry);
                        javaPlugin.getLogger().info("Loaded itemEntry: " + id + " " + KEY + " " + type + " " + item + " " + usePerm );
                    } else {
                        javaPlugin.getLogger().warning("Error: Poorly defined itemEntry: " + id + " " + KEY + " " + type + " " + item + " " + usePerm );
                    }
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

    public ItemEntry getItemEntryByType(String type) {
        for(ItemEntry entry : itemEntries) {
            if (entry.getType().equals(type)) return entry;
        }
        return null;
    }
}
