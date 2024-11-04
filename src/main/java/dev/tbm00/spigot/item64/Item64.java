package dev.tbm00.spigot.item64;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.command.ItmCommand;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.listener.*;
import dev.tbm00.spigot.item64.listener.item.*;

public class Item64 extends JavaPlugin {
    private static ConfigHandler configHandler;
    private static GDHook gdHook = null;
    private static DCHook dcHook = null;
    private static Economy ecoHook = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final PluginDescriptionFile pdf = getDescription();

		log(
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
            pdf.getName() + " v" + pdf.getVersion() + " created by tbm00",
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"
		);

        if (getConfig().getBoolean("enabled")) {
            configHandler = new ConfigHandler(this);
            if (configHandler.isEnabled()) {
                new Hooker(this, configHandler, ecoHook, gdHook, dcHook);

                getCommand("itm").setExecutor(new ItmCommand(this, configHandler));
                getServer().getPluginManager().registerEvents(new PlayerConnection(this, configHandler), this);
                getServer().getPluginManager().registerEvents(new ItemLeader(this, configHandler, ecoHook, gdHook, dcHook), this);
                getServer().getPluginManager().registerEvents(new ExplosiveArrow(this, configHandler, ecoHook, gdHook, dcHook), this);
                getServer().getPluginManager().registerEvents(new LightningPearl(this, configHandler, ecoHook, gdHook, dcHook), this);
                getServer().getPluginManager().registerEvents(new RandomPotion(this, configHandler, ecoHook, gdHook, dcHook), this);
                getServer().getPluginManager().registerEvents(new Consumable(this, configHandler, ecoHook, gdHook, dcHook), this);
                getServer().getPluginManager().registerEvents(new Usable(this, configHandler, ecoHook, gdHook, dcHook), this);
                getServer().getPluginManager().registerEvents(new PreventUsage(this, configHandler, dcHook), this);
                getServer().getPluginManager().registerEvents(new PreventGrowth(this, configHandler, gdHook), this);
                getServer().getPluginManager().registerEvents(new RewardBreak(this, configHandler), this);
                getServer().getPluginManager().registerEvents(new FlameParticle(this, configHandler, ecoHook, gdHook, dcHook), this);
            } else {
                getLogger().severe("Either itemEntries is disabled or there was an error in config... disabling plugin!");
                disablePlugin();
            }
        } else {
            getLogger().warning("Plugin disabled in config..!");
            disablePlugin();
        }
    }

    public void log(String... strings) {
		for (String s : strings)
            getServer().getConsoleSender().sendMessage("[Item64] " + ChatColor.LIGHT_PURPLE + s);
	}

    public void logGreen(String... strings) {
		for (String s : strings)
            getServer().getConsoleSender().sendMessage("[Item64] " + ChatColor.GREEN + s);
	}

    public void logYellow(String... strings) {
		for (String s : strings)
            getServer().getConsoleSender().sendMessage("[Item64] " + ChatColor.YELLOW + s);
	}

    public void logRed(String... strings) {
		for (String s : strings)
            getServer().getConsoleSender().sendMessage("[Item64] " + ChatColor.RED + s);
	}

    private void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
        logRed("Item64 disabled..!");
    }
}