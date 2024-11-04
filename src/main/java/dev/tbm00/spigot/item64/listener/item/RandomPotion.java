package dev.tbm00.spigot.item64.listener.item;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
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

public class RandomPotion extends ItemLeader implements InteractHandler {

    public RandomPotion(Item64 item64, ConfigHandler configHandler, Economy ecoHook, GDHook gdHook, DCHook dcHook) {
        super(item64, configHandler, ecoHook, gdHook, dcHook);
        super.registerHandler(this);
    }

    @Override
    public boolean canHandle(ItemEntry entry) {
        return "RANDOM_POTION".equals(entry.getType());
    }

    @Override
    public void handle(PlayerInteractEvent event, Player player, ItemEntry entry) {
        triggerRandomPotion(event, player, entry);
    }

    private void triggerRandomPotion(PlayerInteractEvent event, Player player, ItemEntry entry) {
        event.setCancelled(true);
        if (!passGDPvpCheck(player.getLocation())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- claim pvp protection!"));
            return;
        }

        if (player.getFoodLevel() < entry.getHunger()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> cooldowns);
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(entry.getID()-1)) < entry.getCooldown()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(player, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(player, cost)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- not enough money!"));
            return;
        }

        Action action = event.getAction();
        boolean rightClick = !(action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        shootPotion(player, entry, rightClick);

        // Set cooldowns and remove resources
        adjustCooldown(player, entry);
        adjustHunger(player, entry);
        if (ecoHook != null && cost > 0 && !removeMoney(player, cost))
            item64.logRed("Error: failed to remove money for " + player.getName() + "'s " + entry.getKeyString() + " usage!");
        if (hasAmmoItem == 2)
            removeItem(player, entry.getAmmoItem());
    }

    private void shootPotion(Player player, ItemEntry entry, boolean rightClick) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        String effectLine = rightClick ? entry.getREffects().get(ThreadLocalRandom.current().nextInt(entry.getREffects().size()))
                                        : entry.getLEffects().get(ThreadLocalRandom.current().nextInt(entry.getLEffects().size()));
        try {
            String[] parts = effectLine.split(":");
            PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
            int amplifier = Integer.parseInt(parts[1]);
            int duration = Integer.parseInt(parts[2])*20;
            if (effectType != null) {
                potionMeta.addCustomEffect(new PotionEffect(effectType, duration, amplifier), true);
                potion.setItemMeta(potionMeta);
        
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting " + effectType.getName().toLowerCase() + "..."));
                player.getWorld().spawn(player.getLocation().add(0, 1.5, 0), ThrownPotion.class, thrownPotion -> {
                    thrownPotion.setItem(potion);
                    thrownPotion.setBounce(false);
                    thrownPotion.setVelocity(player.getLocation().getDirection().multiply(1.4));
        
                    double random = entry.getRandom();
                    if (random > 0) randomizeProjectile(thrownPotion, random);
                    
                    if (!rightClick) {
                        thrownPotion.setVisualFire(true);
                        if (entry.getDamage() >= 0)
                            thrownPotion.setMetadata("Item64-randomPotion-left", new FixedMetadataValue(item64, "true"));
                    }
                    thrownPotion.setMetadata("Item64-keyString", new FixedMetadataValue(item64, entry.getKeyString()));
                    thrownPotion.setShooter(player);
                    magicPotions.add(thrownPotion);
                });
            } else {
                item64.logRed("Unknown potion effect type: " + parts[0]);
            }
        } catch (Exception e) {
            item64.logRed("Exception while throwing potion: " + " - ");
            item64.getLogger().warning(e.getMessage());
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion thrownPotion = (ThrownPotion) event.getEntity();
        if (!magicPotions.contains(thrownPotion)) return;
        magicPotions.remove(thrownPotion);

        handlePotionHit(event, thrownPotion);
    }

    private void handlePotionHit(PotionSplashEvent event, ThrownPotion thrownPotion) {
        Location location = thrownPotion.getLocation();
        Player player = (Player) thrownPotion.getShooter();
        boolean passDCPvpLocCheck = true, passGDPvpCheck = true;
        ItemEntry entry = configHandler.getItemEntryByKeyString(thrownPotion.getMetadata("Item64-keyString").get(0).asString());
        
        if (dcHook != null && !passDCPvpLocCheck(location, 4.0)) passDCPvpLocCheck = false;
        if (gdHook != null) {
            if (!passGDPvpCheck(location)) passGDPvpCheck = false;
            else if (!passGDPvpCheck(player.getLocation())) passGDPvpCheck = false;
        }

        if (!passDCPvpLocCheck) {
            event.setCancelled(true);
            thrownPotion.remove();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- pvp protection!"));
            refundPlayer(player, entry);
        } else if (!passGDPvpCheck) {
            event.setCancelled(true);
            thrownPotion.remove();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- claim pvp protection!"));
            refundPlayer(player, entry);
        } else if (thrownPotion.hasMetadata("Item64-randomPotion-left"))
            damageEntities(player, location, 0.9, 1.3, entry.getDamage(), 20);
    }
}
