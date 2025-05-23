package dev.tbm00.spigot.item64.listener;

import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.chat.TextComponent;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.UsageHandler;

public class PreventPlaceBreak implements Listener {
    private final UsageHandler usageHandler;
    private final ConfigHandler configHandler;
    private final Set<String> preventedBlocks;
    private final Set<String> inactiveWorlds;

    public PreventPlaceBreak(JavaPlugin javaPlugin, UsageHandler usageHandler) {
        this.usageHandler = usageHandler;
        configHandler = usageHandler.getConfigHandler();
        preventedBlocks = configHandler.getPreventedPlacing();
        inactiveWorlds = configHandler.getInactiveWorlds();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // check if block is in blockEvent.preventBlockPlacing config
        if (configHandler.isPreventedPlacingEnabled()) {
            Block block = event.getBlock();
            if (!inactiveWorlds.contains(block.getWorld().getName()) 
            && preventedBlocks.contains(block.getType().name())) {
                if (event.getPlayer().hasPermission("item64.allowplace")) return;
                event.getPlayer().spigot().sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', configHandler.getPreventedPlacingMessage())));
                event.setCancelled(true);
                return;
            }
        }

        // check if block is an active item entry, if so cancel it
        if (usageHandler.getItemEntryByItem(event.getItemInHand()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // check if block is in blockEvent.preventBlockBreaking config
        if (configHandler.isPreventedBreakingEnabled()) {
            Block block = event.getBlock();
            if (!inactiveWorlds.contains(block.getWorld().getName()) 
            && preventedBlocks.contains(block.getType().name())) {
                if (event.getPlayer().hasPermission("item64.allowbreak")) return;
                event.getPlayer().spigot().sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', configHandler.getPreventedBreakingMessage())));
                event.setCancelled(true);
                return;
            }
        }
    }
}
