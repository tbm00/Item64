package dev.tbm00.spigot.item64.listener;

import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.ListenerLeader;
import net.md_5.bungee.api.chat.TextComponent;

public class PlayerConnection extends ListenerLeader implements Listener {

    public PlayerConnection(JavaPlugin javaPlugin, ConfigHandler configHandler) {
        super(javaPlugin, configHandler, null, null, null);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                event.getPlayer().spigot().sendMessage(
                    new TextComponent(ChatColor.translateAlternateColorCodes('&', 
                        configHandler.getRewardedBreakingJoinMessage())
                    )
                );
            }
        }.runTaskLater(javaPlugin, configHandler.getRewardedBreakingJoinMessageDelay());

        // cooldown handling unnecessary on join
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeCooldowns.remove(uuid);
    }


}
