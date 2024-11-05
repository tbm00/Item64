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
import dev.tbm00.spigot.item64.UsageHelper;

public class PlayerConnection implements Listener {
    private final UsageHelper usageHelper;

    public PlayerConnection(Item64 item64, UsageHelper usageHelper) {
        this.usageHelper = usageHelper;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getPlayer(uuid)!=null) {
                    Bukkit.getPlayer(uuid).spigot().sendMessage(
                        new TextComponent(ChatColor.translateAlternateColorCodes('&', 
                            usageHelper.getConfigHandler().getRewardedBreakingJoinMessage())
                        )
                    );
                }
            }
        }.runTaskLater(usageHelper.getItem64(), 20*usageHelper.getConfigHandler().getRewardedBreakingJoinMessageDelay());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getPlayer(uuid)==null)
                    UsageHelper.activeCooldowns.remove(uuid);
            }
        }.runTaskLater(usageHelper.getItem64(), 6000);
    }
}