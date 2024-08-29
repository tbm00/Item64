package dev.tbm00.spigot.item64;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.item64.model.ItemEntry;

public class ItemManager {
    private final Boolean enabled;
    private final List<ItemEntry> itemEntries;
    private final List<Long> cooldowns;
    private final List<Integer> hungers;

    public ItemManager(JavaPlugin javaPlugin) {
        itemEntries = new ArrayList<>();
        cooldowns = new ArrayList<>();
        hungers = new ArrayList<>();
        for (int i=0; i<3; ++i) {
            cooldowns.add(0L);
            hungers.add(0);
        }

        ConfigurationSection entriesSection = javaPlugin.getConfig().getConfigurationSection("itemEntries");
        if (entriesSection != null && entriesSection.getBoolean("enabled")) {
            enabled = true;
            for (String key : entriesSection.getKeys(false)) {
                ConfigurationSection itemEntry = entriesSection.getConfigurationSection(key);
                
                if (itemEntry != null && itemEntry.getBoolean("enabled")) {
                    String givePerm = itemEntry.getString("givePerm");
                    String usePerm = itemEntry.getString("usePerm");
                    String type = itemEntry.getString("type");
                    String KEY = itemEntry.getString("key");
                    int cooldown = itemEntry.getInt("cooldown");
                    int hunger = itemEntry.getInt("hunger");
                    double random = itemEntry.getDouble("shotRandomness");
                    double damage = itemEntry.getDouble("extraDamage");
                    String ammoItem = itemEntry.getString("ammoItem");
                    String item = itemEntry.getString("item");
                    String name = itemEntry.getString("name");
                    Boolean glowing = itemEntry.getBoolean("glowing");
                    List<String> lore = itemEntry.getStringList("lore");

                    switch (type) {
                        case "EXPLOSIVE_ARROW" -> {
                            cooldowns.set(0, Long.valueOf(cooldown));
                            hungers.set(0, hunger);
                        }
                        case "LIGHTNING" -> {
                            cooldowns.set(1, Long.valueOf(cooldown));
                            hungers.set(1, hunger);
                        }
                        case "RANDOM_POTION" -> {
                            cooldowns.set(2, Long.valueOf(cooldown));
                            hungers.set(2, hunger);
                        }
                        default -> {}
                    }
                    
                    if (usePerm != null && givePerm != null && type != null && key != null ) {
                        ItemEntry entry = new ItemEntry(javaPlugin, givePerm, usePerm, type, KEY, cooldown, hunger, random, damage, ammoItem, item, name, glowing, lore);
                        itemEntries.add(entry);
                        javaPlugin.getLogger().info("Loaded itemEntry: " + KEY + " " + type + " " + item + " " + usePerm );
                    } else {
                        javaPlugin.getLogger().warning("Error: Poorly defined itemEntry: " + KEY + " " + type + " " + item + " " + usePerm );
                    }
                }
            }
        } else enabled = false;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public List<ItemEntry> getItemEntries() {
        return itemEntries;
    }

    public ItemEntry getItemEntry(String type) {
        for(ItemEntry entry : itemEntries) {
            if (entry.getType().equals(type)) return entry;
        }
        return null;
    }

    public List<Long> getCooldowns() {
        return cooldowns;
    }

    public List<Integer> getHungers() {
        return hungers;
    }
}
