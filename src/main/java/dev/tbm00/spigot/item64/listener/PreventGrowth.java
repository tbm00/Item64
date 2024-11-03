package dev.tbm00.spigot.item64.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.hook.GDHook;

public class PreventGrowth implements Listener {
    private final JavaPlugin javaPlugin;
    private final ConfigHandler configHandler;
    private final boolean enabled;
    private final GDHook gdHook;

    public PreventGrowth(JavaPlugin javaPlugin, ConfigHandler configHandler, GDHook gdHook) {
        this.javaPlugin = javaPlugin;
        this.configHandler = configHandler;
        this.gdHook = gdHook;
        if (configHandler.isPreventedGrowingEnabled()) enabled = true;
        else enabled = false;
    }
    
    @EventHandler
    public void onBlockGrowth(BlockGrowEvent event) {
        if (!enabled) return;
        Material mat = event.getNewState().getType();
        Block block = event.getBlock();
        if (!configHandler.getInactiveWorlds().contains(block.getWorld().getName()) 
        && configHandler.getPreventedGrowing().contains(mat.toString())) {
            event.setCancelled(true);
            if (gdHook!=null) {
                String name = gdHook.getClaimOwner(block.getLocation());
                javaPlugin.getLogger().info("Canceled block growth in " + name + "'s claim: " + event.getBlock().getWorld() + " @ " + event.getBlock().getLocation().getX() + ", " + event.getBlock().getLocation().getY() + ", " + event.getBlock().getLocation().getZ());
            }
            else javaPlugin.getLogger().info("Canceled block growth in " + event.getBlock().getWorld() + " @ " + event.getBlock().getLocation().getX() + ", " + event.getBlock().getLocation().getY() + ", " + event.getBlock().getLocation().getZ());
        }
    }
}
