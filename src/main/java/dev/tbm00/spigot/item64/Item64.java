package dev.tbm00.spigot.item64;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

import nl.marido.deluxecombat.api.DeluxeCombatAPI;

import dev.tbm00.spigot.item64.command.ItmCommand;
import dev.tbm00.spigot.item64.hook.GDHook;
import dev.tbm00.spigot.item64.listener.ItemUse;

public class Item64 extends JavaPlugin {
    private static ItemManager itemManager;
    private static GDHook gdHook;
    private static DeluxeCombatAPI dcHook;
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

        if (getConfig().getBoolean("hooks.DeluxeCombat.enabled"))
            if (!setupDeluxeCombat()) {
                getLogger().warning("DeluxeCombat hook failed!");
                return;
            }

        if (getConfig().getBoolean("hooks.GriefDefender.enabled"))
            if (!setupGriefDefender()) {
                getLogger().warning("GriefDefender hook failed!");
                return;
            }

        if (getConfig().getBoolean("hooks.Vault.enabled"))
            if (!setupVault()) {
                getLogger().warning("Vault hook failed!");
                return;
            }

        if (getConfig().getBoolean("itemEntries.enabled")) {
            itemManager = new ItemManager(this);
            getCommand("itm").setExecutor(new ItmCommand(this, itemManager));
            getServer().getPluginManager().registerEvents(new ItemUse(this, itemManager, ecoHook, gdHook, dcHook), this);
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

        dcHook = new DeluxeCombatAPI();
        
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
}