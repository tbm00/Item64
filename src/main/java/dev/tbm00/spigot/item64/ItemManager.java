package dev.tbm00.spigot.item64;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.item64.model.ItemEntry;

public class ItemManager {
    private final List<ItemEntry> itemEntries;
    private final Boolean enabled;

    public ItemManager(JavaPlugin javaPlugin) {
        this.itemEntries = new ArrayList<>();

        // Load Item Commands from config.yml
        ConfigurationSection itemCmdSection = javaPlugin.getConfig().getConfigurationSection("itemCommandEntries");
        if (itemCmdSection != null && itemCmdSection.getBoolean("enabled")) {
            this.enabled = true;
            for (String key : itemCmdSection.getKeys(false)) {
                ConfigurationSection itemEntry = itemCmdSection.getConfigurationSection(key);
                
                if (itemEntry != null && itemEntry.getBoolean("enabled")) {
                    String givePerm = itemEntry.getString("givePerm");
                    String usePerm = itemEntry.getString("usePerm");
                    String type = itemEntry.getString("type");
                    String KEY = itemEntry.getString("key");
                    String name = itemEntry.getString("name");
                    String item = itemEntry.getString("item");
                    Boolean glowing = itemEntry.getBoolean("glowing");
                    List<String> lore = itemEntry.getStringList("lore");
                    
                    if (usePerm != null && givePerm != null && type != null && key != null ) {
                        ItemEntry entry = new ItemEntry(javaPlugin, givePerm, usePerm, type, KEY, name, item, glowing, lore);
                        this.itemEntries.add(entry);
                        System.out.println("Loaded itemEntry: " + KEY + " " + type + " " + item + " " + usePerm );
                    } else {
                        System.out.println("Error: Poorly defined itemEntry: " + KEY + " " + type + " " + item + " " + usePerm );
                    }
                }
            }
        } else this.enabled = false;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public List<ItemEntry> getItemEntries() {
        return itemEntries;
    }
}
