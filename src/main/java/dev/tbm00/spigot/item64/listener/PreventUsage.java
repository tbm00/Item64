package dev.tbm00.spigot.item64.listener;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.ListenerLeader;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class PreventUsage extends ListenerLeader implements Listener {
    private final List<String> ignorePlaced;
    private final boolean checkAnchorExplosions;

    public PreventUsage(JavaPlugin javaPlugin, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        super(javaPlugin, configHandler, ecoHook, gdHook, dcHook);
        ignorePlaced = configHandler.getIgnoredPlaced();
        checkAnchorExplosions = configHandler.getCheckAnchorExplosions();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!ignorePlaced.contains(item.getType().toString())) return;

        ItemMeta itemData = item.getItemMeta();
        if (itemData==null) return;

        for (ItemEntry entry : itemEntries) {
            if (itemData.getPersistentDataContainer().has(entry.getKey(), PersistentDataType.STRING)) {
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
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Explosion blocked -- pvp protection!"));
            event.setCancelled(true);
        }
    }
}
