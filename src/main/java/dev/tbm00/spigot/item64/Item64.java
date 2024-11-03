package dev.tbm00.spigot.item64;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.command.ItmCommand;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.listener.*;
import dev.tbm00.spigot.item64.listener.item.Consumable;
import dev.tbm00.spigot.item64.listener.item.ExplosiveArrow;
import dev.tbm00.spigot.item64.listener.item.FlameParticle;
import dev.tbm00.spigot.item64.listener.item.LightningPearl;
import dev.tbm00.spigot.item64.listener.item.RandomPotion;
import dev.tbm00.spigot.item64.listener.item.Usable;

public class Item64 extends JavaPlugin {
    private static ConfigHandler configHandler;
    private static GDHook gdHook;
    private static DCHook dcHook;
    private static Economy ecoHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final PluginDescriptionFile pdf = getDescription();

		log(
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
            pdf.getName() + " v" + pdf.getVersion() + " created by tbm00",
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"
		);

        if (getConfig().getBoolean("itemEntries.enabled")) {
            configHandler = new ConfigHandler(this);
            if (configHandler.isEnabled()) {
                if (getConfig().getBoolean("hooks.DeluxeCombat.enabled") && !setupDeluxeCombat()) {
                    getLogger().warning("DeluxeCombat hook failed!");
                    disablePlugin();
                    return;
                }
        
                if (getConfig().getBoolean("hooks.GriefDefender.enabled") && !setupGriefDefender()) {
                    getLogger().warning("GriefDefender hook failed!");
                    disablePlugin();
                    return;
                }
        
                if (getConfig().getBoolean("hooks.Vault.enabled") && !setupVault()) {
                    getLogger().warning("Vault hook failed!");
                    disablePlugin();
                    return;
                }

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
                getLogger().warning("Either itemEntries is disabled or there was an error in config... disabling plugin!");
                disablePlugin();
            }
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

    private boolean setupDeluxeCombat() {
        if (Bukkit.getPluginManager().getPlugin("DeluxeCombat")==null) return false;

        dcHook = new DCHook();
        
        getLogger().info("DeluxeCombat hooked.");
        return true;
    }

    private boolean setupGriefDefender() {
        if (!isPluginAvailable("GriefDefender")) return false;

        gdHook = new GDHook(this);

        getLogger().info("GriefDefender hooked.");
        return true;
    }

    private boolean setupVault() {
        if (!isPluginAvailable("Vault")) return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        ecoHook = rsp.getProvider();
        if (ecoHook == null) return false;

        getLogger().info("Vault hooked.");
        return true;
    }

    private void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
        getLogger().info("Item64 disabled.");
    }
}