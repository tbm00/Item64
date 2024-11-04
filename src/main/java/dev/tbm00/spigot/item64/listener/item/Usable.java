package dev.tbm00.spigot.item64.listener.item;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.Economy;

import dev.tbm00.spigot.item64.Item64;
import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.hook.*;
import dev.tbm00.spigot.item64.model.ItemEntry;
import dev.tbm00.spigot.item64.listener.ItemLeader;
import dev.tbm00.spigot.item64.listener.InteractHandler;

public class Usable extends ItemLeader implements InteractHandler {

    public Usable(Item64 item64, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        super(item64, configHandler, ecoHook, gdHook, dcHook);
        super.registerHandler(this);
    }

    @Override
    public boolean canHandle(ItemEntry entry) {
        return "USABLE".equals(entry.getType());
    }

    @Override
    public void handle(PlayerInteractEvent event, Player player, ItemEntry entry) {
        triggerUsable(event, player, entry);
    }

    private void triggerUsable(PlayerInteractEvent event, Player player, ItemEntry entry) {
        event.setCancelled(true);
        ItemStack item = event.getItem();

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
            item64.logRed("Error: failed to remove money for " + player.getName() + "'s " + entry.getKeyString() + " usage!");
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
                    item64.logRed("Unknown potion effect type: " + parts[0]);
                    return false;
                }
            } catch (Exception e) {
                item64.logRed("Error parsing effect: " + line + " - ");
                item64.getLogger().warning(e.getMessage());
                return false;
            }
        }
        return true;
    }
}
