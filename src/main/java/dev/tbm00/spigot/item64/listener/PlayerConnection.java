package dev.tbm00.spigot.item64.listener;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.chat.TextComponent;

import dev.tbm00.spigot.item64.Item64;
import dev.tbm00.spigot.item64.UsageHandler;

public class PlayerConnection implements Listener {
    private final UsageHandler usageHandler;

    public PlayerConnection(Item64 item64, UsageHandler usageHandler) {
        this.usageHandler = usageHandler;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (usageHandler.getConfigHandler().getRewardedBreakingJoinMessage()==null || usageHandler.getConfigHandler().getRewardedBreakingJoinMessage().isBlank()) return;
        UUID uuid = event.getPlayer().getUniqueId();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getPlayer(uuid)!=null) {
                    Bukkit.getPlayer(uuid).spigot().sendMessage(
                        new TextComponent(ChatColor.translateAlternateColorCodes('&', 
                            usageHandler.getConfigHandler().getRewardedBreakingJoinMessage())
                        )
                    );
                }
            }
        }.runTaskLater(usageHandler.getItem64(), 20*usageHandler.getConfigHandler().getRewardedBreakingJoinMessageDelay());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getPlayer(uuid)==null)
                    usageHandler.getActiveCooldowns().remove(uuid);
            }
        }.runTaskLater(usageHandler.getItem64(), 6000);
    }
}