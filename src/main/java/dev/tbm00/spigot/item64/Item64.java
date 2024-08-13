package dev.tbm00.spigot.item64;

import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.item64.command.ItmCommand;
import dev.tbm00.spigot.item64.listener.ItemUse;

public class Item64 extends JavaPlugin {
    private ItemManager itemManager;

    @Override
    public void onEnable() {
        // Load Config  
        this.saveDefaultConfig();

        // Startup Message
        final PluginDescriptionFile pdf = this.getDescription();
		log(
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
            pdf.getName() + " v" + pdf.getVersion() + " created by tbm00",
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"
		);

        if (this.getConfig().getBoolean("itemCommandEntries.enabled")
                || this.getConfig().getBoolean("customCommandEntries.enabled")) {
            // Create itemManager and load config
            itemManager = new ItemManager(this);

            // Register Commands
            getCommand("itm").setExecutor(new ItmCommand(this, itemManager));
            
            // Register ItemUse listener
            if (this.getConfig().getBoolean("itemCommandEntries.enabled")) {
                getServer().getPluginManager().registerEvents(new ItemUse(this, itemManager), this);
            }
        }
    }

    private void log(String... strings) {
		for (String s : strings)
            getServer().getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + s);
	}
}