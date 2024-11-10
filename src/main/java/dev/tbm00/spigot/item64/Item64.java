package dev.tbm00.spigot.item64;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.command.ItmCommand;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.listener.*;

public class Item64 extends JavaPlugin {
    private ConfigHandler configHandler;
    private UsageHelper usageHelper;
    private GDHook gdHook;
    private DCHook dcHook;
    private Economy ecoHook;

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
                setupHooks();
                usageHelper = new UsageHelper(this, configHandler, ecoHook, gdHook, dcHook);

                getCommand("itm").setExecutor(new ItmCommand(this, configHandler));
                getServer().getPluginManager().registerEvents(new ItemUsage(this, usageHelper), this);
                getServer().getPluginManager().registerEvents(new PlayerConnection(this, usageHelper), this);
                getServer().getPluginManager().registerEvents(new PreventUsage(this, configHandler, dcHook), this);
                getServer().getPluginManager().registerEvents(new PreventGrowth(this, configHandler, gdHook), this);
                getServer().getPluginManager().registerEvents(new RewardBreak(this, configHandler), this);
            } else {
                getLogger().severe("Either itemEntries is disabled or there was an error in config... disabling plugin!");
                disablePlugin();
            }
        } else {
            getLogger().warning("Plugin disabled in config..!");
            disablePlugin();
        }
    }

    private void setupHooks() {
        if (configHandler.isVaultEnabled() && !setupVault()) {
            getLogger().severe("Vault hook failed -- disabling plugin!");
            disablePlugin();
            return;
        }

        if (configHandler.isGriefDefenderEnabled() && !setupGriefDefender()) {
            getLogger().severe("GriefDefender hook failed -- disabling plugin!");
            disablePlugin();
            return;
        }

        if (configHandler.isDeluxeCombatEnabled() && !setupDeluxeCombat()) {
            getLogger().severe("DeluxeCombat hook failed -- disabling plugin!");
            disablePlugin();
            return;
        }
    }

    private boolean setupDeluxeCombat() {
        if (getServer().getPluginManager().getPlugin("DeluxeCombat")==null) return false;

        dcHook = new DCHook();
        
        logGreen("DeluxeCombat hooked.");
        return true;
    }

    private boolean setupGriefDefender() {
        if (!isPluginAvailable("GriefDefender")) return false;

        gdHook = new GDHook(this, configHandler.getIgnoredClaims());

        logGreen("GriefDefender hooked.");
        return true;
    }

    private boolean setupVault() {
        if (!isPluginAvailable("Vault")) return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        ecoHook = rsp.getProvider();
        if (ecoHook == null) return false;

        logGreen("Vault hooked.");
        return true;
    }

    private boolean isPluginAvailable(String pluginName) {
		final Plugin plugin = getServer().getPluginManager().getPlugin(pluginName);
		return plugin != null && plugin.isEnabled();
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