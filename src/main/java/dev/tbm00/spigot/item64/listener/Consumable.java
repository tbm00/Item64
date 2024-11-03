package dev.tbm00.spigot.item64.listener;

import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.ListenerLeader;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class Consumable extends ListenerLeader implements Listener {

    public Consumable(JavaPlugin javaPlugin, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        super(javaPlugin, configHandler, ecoHook, gdHook, dcHook);
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || player == null) return;

        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null || !entry.getType().equalsIgnoreCase("CONSUMABLE") || !player.hasPermission(entry.getUsePerm()))
            return;

        event.setCancelled(true);

        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Use blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> cooldowns);
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(entry.getID()-1)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Use blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(player, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No fuel: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(player, cost)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Use blocked -- not enough money!"));
            return;
        }

        if (!entry.getMessage().isBlank() && !entry.getMessage().isEmpty()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', entry.getMessage())));
        }
        List<String> effects = entry.getEffects();
        List<String> commands = entry.getCommands();
        boolean removeItem = entry.getRemoveItem();
        if (commands!=null) { 
            for (String command : commands) {
                String cmd = command.replace("<player>", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
        if (effects!=null) {
            boolean appliedEffects = applyEffects(player, effects, item, removeItem);
            if (appliedEffects && removeItem) removeItem(player, item);
        } else if (removeItem) removeItem(player, item);

        // Set cooldowns and remove ammo
        if (hasAmmoItem == 2) removeItem(player, entry.getAmmoItem());
        adjustCooldown(player, entry);
        adjustHunger(player, entry);
        if (ecoHook != null && cost > 0 && !removeMoney(player, cost)) {
            javaPlugin.getLogger().warning("Error: failed to remove money for " + player.getName() + "'s " + entry.getKeyString() + " usage!");
        }
    }

    private boolean applyEffects(Player player, List<String> effects, ItemStack item, boolean removeItem) {
        for (String line : effects) {
            try {
                String[] parts = line.split(":");
                PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                int amplifier = Integer.parseInt(parts[1]);
                int duration = Integer.parseInt(parts[2])*20;
                if (effectType != null) {
                    player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, true));
                } else {
                    javaPlugin.getLogger().warning("Unknown potion effect type: " + parts[0]);
                    return false;
                }
            } catch (Exception e) {
                javaPlugin.getLogger().warning("Error parsing effect: " + line + " - " + e.getMessage());
                return false;
            }
        }
        return true;
    }
}
