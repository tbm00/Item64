package dev.tbm00.spigot.item64.listener;

import java.util.UUID;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.md_5.bungee.api.chat.TextComponent;

import dev.tbm00.spigot.item64.Item64;
import dev.tbm00.spigot.item64.ConfigHandler;

public class PlayerConnection extends ItemLeader {

    public PlayerConnection(Item64 item64, ConfigHandler configHandler) {
        super(item64, configHandler, null, null, null);
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
        }.runTaskLater(item64, configHandler.getRewardedBreakingJoinMessageDelay());

        // cooldown handling unnecessary on join
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeCooldowns.remove(uuid);
    }
}
