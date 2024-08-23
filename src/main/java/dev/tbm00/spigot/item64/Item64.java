package dev.tbm00.spigot.item64;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import nl.marido.deluxecombat.api.DeluxeCombatAPI;

import dev.tbm00.spigot.item64.command.ItmCommand;
import dev.tbm00.spigot.item64.hook.GDHook;
import dev.tbm00.spigot.item64.listener.ItemUse;

public class Item64 extends JavaPlugin {
    private ItemManager itemManager;
    private GDHook gdHook;
    private DeluxeCombatAPI dcHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final PluginDescriptionFile pdf = getDescription();

		log(
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
            pdf.getName() + " v" + pdf.getVersion() + " created by tbm00",
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"
		);

        if (getConfig().getBoolean("hooks.GriefDefender.enabled")) {
            if (isPluginAvailable("GriefDefender")) {
			    gdHook = new GDHook(this);
			    getLogger().info("GriefDefender hooked.");
            }
        }

        if (getConfig().getBoolean("hooks.DeluxeCombat.enabled")) {
            dcHook = new DeluxeCombatAPI();
            getLogger().info("DeluxeCombat hooked.");
        }

        if (getConfig().getBoolean("itemEntries.enabled")) {
            itemManager = new ItemManager(this);
            getCommand("itm").setExecutor(new ItmCommand(this, itemManager));
            getServer().getPluginManager().registerEvents(new ItemUse(this, itemManager, gdHook, dcHook), this);
        }
    }

    private void log(String... strings) {
		for (String s : strings)
            getServer().getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + s);
	}

    private boolean isPluginAvailable(String pluginName) {
		final Plugin plugin = getServer().getPluginManager().getPlugin(pluginName);
		return plugin != null && plugin.isEnabled();
	}
}