package dev.tbm00.spigot.item64.listener;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;

import dev.tbm00.spigot.item64.model.ItemEntry;

public interface InteractHandler {
    
    /**
     * Determines if this handler can process the given ItemEntry.
     *
     * @param entry The ItemEntry to check.
     * @return True if this handler can process the entry, false otherwise.
     */
    boolean canHandle(ItemEntry entry);

    /**
     * Handles the PlayerInteractEvent for the given ItemEntry.
     *
     * @param event The PlayerInteractEvent.
     * @param player The player involved in the event.
     * @param entry The ItemEntry associated with the item.
     */
    void handle(PlayerInteractEvent event, Player player, ItemEntry entry);
}