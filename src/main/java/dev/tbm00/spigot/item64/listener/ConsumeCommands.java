package dev.tbm00.spigot.item64.listener;

import java.util.List;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;
import nl.marido.deluxecombat.api.DeluxeCombatAPI;

import dev.tbm00.spigot.item64.ItemManager;
import dev.tbm00.spigot.item64.ListenerLeader;
import dev.tbm00.spigot.item64.hook.GDHook;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class ConsumeCommands extends ListenerLeader implements Listener {

    public ConsumeCommands(JavaPlugin javaPlugin, ItemManager itemManager, Economy ecoHook, GDHook gdHook, DeluxeCombatAPI dcHook) {
        super(javaPlugin, itemManager, ecoHook, gdHook, dcHook);
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player consumer = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || consumer == null) return;

        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null || !entry.getType().equalsIgnoreCase("CONSUME_COMMANDS") || !consumer.hasPermission(entry.getUsePerm()))
            return;

        event.setCancelled(true);

        if (consumer.getFoodLevel() < entry.getHunger()) {
            consumer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Use blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(consumer.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(4)) < entry.getCooldown()) {
            consumer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Use blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(consumer, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            consumer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No fuel: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(consumer, cost)) {
            consumer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Use blocked -- not enough money!"));
            return;
        }

        consumer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Healing..."));

        // Run commands
        List<String> commands = entry.getCommands();
        for (String command : commands) {
            String cmd = command.replace("<player>", consumer.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        // Set cooldowns and remove resources
        adjustCooldowns(consumer, itemManager.getHungers(), 4);
        if (ecoHook != null && cost > 0 && !removeMoney(consumer, cost)) {
            javaPlugin.getLogger().warning("Error: failed to remove money for " + consumer.getName() + "'s " + entry.getKeyString() + " usage!");
        }
        if (hasAmmoItem == 2) removeItem(consumer, entry.getAmmoItem());
        if (entry.getRemoveItem()) removeItem(consumer, item);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeCooldowns.remove(uuid);
    }
}
