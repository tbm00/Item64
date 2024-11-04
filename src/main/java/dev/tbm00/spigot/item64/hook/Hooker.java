package dev.tbm00.spigot.item64.hook;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.Item64;
import dev.tbm00.spigot.item64.ConfigHandler;

public class Hooker {
    private final Item64 item64;
    private final ConfigHandler configHandler;

    public Hooker(Item64 item64, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        this.item64 = item64;
        this.configHandler = configHandler;

        if (configHandler.isVaultEnabled() && !setupVault(ecoHook)) {
            item64.getLogger().severe("Vault hook failed -- disabling plugin!");
            disablePlugin();
            return;
        }

        if (configHandler.isGriefDefenderEnabled() && !setupGriefDefender(gdHook)) {
            item64.getLogger().severe("GriefDefender hook failed -- disabling plugin!");
            disablePlugin();
            return;
        }

        if (configHandler.isDeluxeCombatEnabled() && !setupDeluxeCombat(dcHook)) {
            item64.getLogger().severe("DeluxeCombat hook failed -- disabling plugin!");
            disablePlugin();
            return;
        }
    }

    private boolean setupDeluxeCombat(DCHook dcHook) {
        if (item64.getServer().getPluginManager().getPlugin("DeluxeCombat")==null)
            return false;

        dcHook = new DCHook();
        
        item64.logGreen("DeluxeCombat hooked.");
        return true;
    }

    private boolean setupGriefDefender(GDHook gdHook) {
        if (!isPluginAvailable("GriefDefender")) return false;

        gdHook = new GDHook(item64, configHandler.getIgnoredClaims());

        item64.logGreen("GriefDefender hooked.");
        return true;
    }

    private boolean setupVault(Economy ecoHook) {
        if (!isPluginAvailable("Vault")) return false;

        RegisteredServiceProvider<Economy> rsp = item64.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        ecoHook = rsp.getProvider();
        if (ecoHook == null) return false;

        item64.logGreen("Vault hooked.");
        return true;
    }

    private boolean isPluginAvailable(String pluginName) {
		final Plugin plugin = item64.getServer().getPluginManager().getPlugin(pluginName);
		return plugin != null && plugin.isEnabled();
	}

    private void disablePlugin() {
        item64.getServer().getPluginManager().disablePlugin(item64);
        item64.logRed("Item64 disabled..!");
    }
}
