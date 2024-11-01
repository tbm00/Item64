package dev.tbm00.spigot.item64.listener;

import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.ListenerLeader;
import dev.tbm00.spigot.item64.hook.*;

public class PlayerConnection extends ListenerLeader implements Listener {

    public PlayerConnection(JavaPlugin javaPlugin, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        super(javaPlugin, configHandler, ecoHook, gdHook, dcHook);
    }

    // join listener unnecessary

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeCooldowns.remove(uuid);
    }
}
