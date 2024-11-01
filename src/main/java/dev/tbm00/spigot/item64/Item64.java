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

        if (getConfig().getBoolean("hooks.DeluxeCombat.enabled")) {
            if (!setupDeluxeCombat()) {
                getLogger().warning("DeluxeCombat hook failed!");
                disablePlugin();
                return;
            }
        }

        if (getConfig().getBoolean("hooks.GriefDefender.enabled")) {
            if (!setupGriefDefender()) {
                getLogger().warning("GriefDefender hook failed!");
                disablePlugin();
                return;
            }
        }

        if (getConfig().getBoolean("hooks.Vault.enabled")) {
            if (!setupVault()) {
                getLogger().warning("Vault hook failed!");
                disablePlugin();
                return;
            }
        }

        if (getConfig().getBoolean("itemEntries.enabled")) {
            configHandler = new ConfigHandler(this);
            if (configHandler.isEnabled()) {
                getCommand("itm").setExecutor(new ItmCommand(this, configHandler));
                getServer().getPluginManager().registerEvents(new PlayerConnection(this, configHandler, ecoHook, gdHook, dcHook), this);
                getServer().getPluginManager().registerEvents(new ExplosiveArrow(this, configHandler, ecoHook, gdHook, dcHook), this);
                getServer().getPluginManager().registerEvents(new LightningPearl(this, configHandler, ecoHook, gdHook, dcHook), this);
                getServer().getPluginManager().registerEvents(new RandomPotion(this, configHandler, ecoHook, gdHook, dcHook), this);
                getServer().getPluginManager().registerEvents(new ConsumeItem(this, configHandler, ecoHook, gdHook, dcHook), this);
                getServer().getPluginManager().registerEvents(new PreventUsage(this, configHandler, ecoHook, gdHook, dcHook), this);

                // I made 2 flame particle listeners because it was failing to load on servers with DeluxeCombat
                // Not sure why only the flame particle listener fails without a dcHook...
                if (gdHook!=null) getServer().getPluginManager().registerEvents(new FlameParticleDC(this, configHandler, ecoHook, gdHook, dcHook), this);
                else getServer().getPluginManager().registerEvents(new FlameParticle(this, configHandler, ecoHook, gdHook), this);
            } else {
                getLogger().warning("itemEntries disabled in config!");
                disablePlugin();
                return;
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