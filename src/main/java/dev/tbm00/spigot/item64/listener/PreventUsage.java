package dev.tbm00.spigot.item64.listener;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class PreventUsage extends ItemLeader {
    private static Set<String> preventedBlocks;
    private static Set<String> inactiveWorlds;
    private final boolean checkAnchorExplosions;

    public PreventUsage(JavaPlugin javaPlugin, ConfigHandler configHandler, DCHook dcHook) {
        super(javaPlugin, configHandler, null, null, dcHook);
        preventedBlocks = configHandler.getPreventedPlacing();
        checkAnchorExplosions = configHandler.getCheckAnchorExplosions();
        inactiveWorlds = configHandler.getInactiveWorlds();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // check if block is in breakEvent.preventBlockPlacing config
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

        // check if block is an active item entry
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) return;
        for (ItemEntry entry : configHandler.getItemEntries()) {
            if (item.getItemMeta().getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.RESPAWN_ANCHOR) return;
        if (!checkAnchorExplosions || dcHook==null) return;
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        Location location = clickedBlock.getLocation();
        boolean passDCPvpPlayerCheck = true, passDCPvpLocCheck = true;

        if (!passDCPvpLocCheck(location, 6.0)) passDCPvpLocCheck = false;
        else if (!passDCPvpPlayerCheck(player)) passDCPvpPlayerCheck = false;

        if (!passDCPvpPlayerCheck || !passDCPvpLocCheck) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Anchor explosion blocked -- pvp protection!"));
            event.setCancelled(true);
        }
    }
}
