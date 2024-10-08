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
        for (int i=0; i<6; ++i) {
            cooldowns.add(0L);
            hungers.add(0);
        }

        ConfigurationSection entriesSection = javaPlugin.getConfig().getConfigurationSection("itemEntries");
        if (entriesSection != null && entriesSection.getBoolean("enabled")) {
            enabled = true;
            for (String key : entriesSection.getKeys(false)) {
                ConfigurationSection itemEntry = entriesSection.getConfigurationSection(key);
                
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
                        case "EXPLOSIVE_ARROW" -> {
                            cooldowns.set(0, Long.valueOf(cooldown));
                            hungers.set(0, hunger);
                        }
                        case "LIGHTNING_PEARL" -> {
                            cooldowns.set(1, Long.valueOf(cooldown));
                            hungers.set(1, hunger);
                        }
                        case "RANDOM_POTION" -> {
                            cooldowns.set(2, Long.valueOf(cooldown));
                            hungers.set(2, hunger);
                        }
                        case "FLAME_PARTICLE" -> {
                            cooldowns.set(3, Long.valueOf(cooldown));
                            hungers.set(3, hunger);
                        }
                        case "CONSUME_COMMANDS" -> {
                            removeItem = itemEntry.getBoolean("removeConsumedItem");
                            commands = itemEntry.getStringList("consoleCommands");
                            cooldowns.set(4, Long.valueOf(cooldown));
                            hungers.set(4, hunger);
                        }
                        case "CONSUME_EFFECTS" -> {
                            removeItem = itemEntry.getBoolean("removeConsumedItem");
                            cooldowns.set(5, Long.valueOf(cooldown));
                            hungers.set(5, hunger);
                        }
                        default -> {}
                    }
                    
                    if (usePerm != null && givePerm != null && type != null && key != null ) {
                        ItemEntry entry = new ItemEntry(javaPlugin, givePerm, usePerm, type, KEY, money, hunger, cooldown, random, damage, ammoItem, item, name, lore, hideEnchants, enchants, removeItem, commands);
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

    public ItemEntry getItemEntryByType(String type) {
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
