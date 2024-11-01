package dev.tbm00.spigot.item64.listener;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
import nl.marido.deluxecombat.api.DeluxeCombatAPI;

import dev.tbm00.spigot.item64.ItemManager;
import dev.tbm00.spigot.item64.ListenerLeader;
import dev.tbm00.spigot.item64.hook.GDHook;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class RandomPotion extends ListenerLeader implements Listener {

    public RandomPotion(JavaPlugin javaPlugin, ItemManager itemManager, Economy ecoHook, GDHook gdHook, DeluxeCombatAPI dcHook) {
        super(javaPlugin, itemManager, ecoHook, gdHook, dcHook);
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player shooter = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || shooter == null) return;

        ItemEntry entry = getItemEntryByItem(item);
        if (entry == null || !entry.getType().equals("RANDOM_POTION") || !shooter.hasPermission(entry.getUsePerm()))
            return;

        triggerRandomPotion(event, shooter, entry);
    }

    private void triggerRandomPotion(PlayerInteractEvent event, Player shooter, ItemEntry entry) {
        event.setCancelled(true);
        if (!passGDPvpCheck(shooter.getLocation())) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- claim pvp protection!"));
            return;
        }

        if (shooter.getFoodLevel() < entry.getHunger()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- you're too hungry!"));
            return;
        }

        List<Long> playerCooldowns = activeCooldowns.computeIfAbsent(shooter.getUniqueId(), k -> itemManager.getCooldowns());
        if (((System.currentTimeMillis() / 1000) - playerCooldowns.get(2)) < entry.getCooldown()) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- active cooldown!"));
            return;
        }

        int hasAmmoItem = hasItem(shooter, entry.getAmmoItem());
        if (hasAmmoItem == 0) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No ammo: " + entry.getAmmoItem().toLowerCase()));
            return;
        }

        double cost = entry.getMoney();
        if (ecoHook != null && cost > 0 && !hasMoney(shooter, cost)) {
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- not enough money!"));
            return;
        }

        Action action = event.getAction();
        boolean rightClick = !(action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        shootPotion(shooter, rightClick);

        adjustCooldowns(shooter, itemManager.getHungers(), 2);
        if (ecoHook != null && cost > 0 && !removeMoney(shooter, cost))
            javaPlugin.getLogger().warning("Error: failed to remove money for " + shooter.getName() + "'s " + entry.getKeyString() + " usage!");
        if (hasAmmoItem == 2)
            removeItem(shooter, entry.getAmmoItem());
    }

    private void shootPotion(Player shooter, boolean rightClick) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        PotionEffectType effectType = rightClick ? positiveEFFECTS[ThreadLocalRandom.current().nextInt(positiveEFFECTS.length)]
                                                 : negativeEFFECTS[ThreadLocalRandom.current().nextInt(negativeEFFECTS.length)];
        potionMeta.addCustomEffect(new PotionEffect(effectType, 600, 1), true);
        potion.setItemMeta(potionMeta);

        shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Shooting " + effectType.getName().toLowerCase() + "..."));
        shooter.getWorld().spawn(shooter.getLocation().add(0, 1.5, 0), ThrownPotion.class, thrownPotion -> {
            thrownPotion.setItem(potion);
            thrownPotion.setBounce(false);
            thrownPotion.setVelocity(shooter.getLocation().getDirection().multiply(1.4));
            ItemEntry entry = itemManager.getItemEntryByType("RANDOM_POTION");

            double random = entry.getRandom();
            if (random > 0) randomizeVelocity(thrownPotion, random);
            
            if (!rightClick) {
                thrownPotion.setVisualFire(true);
                if (entry.getDamage() >= 0)
                    thrownPotion.setMetadata("randomPotionLeft", new FixedMetadataValue(javaPlugin, "true"));
            }
            thrownPotion.setShooter(shooter);
            magicPotions.add(thrownPotion);
        });
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
        Player shooter = (Player) thrownPotion.getShooter();
        boolean passDCPvpLocCheck = true, passGDPvpCheck = true;
        
        if (dcHook != null && !passDCPvpLocCheck(location, 4.0)) passDCPvpLocCheck = false;
        if (gdHook != null) {
            if (!passGDPvpCheck(location)) passGDPvpCheck = false;
            else if (!passGDPvpCheck(shooter.getLocation())) passGDPvpCheck = false;
        }

        if (!passDCPvpLocCheck) {
            event.setCancelled(true);
            thrownPotion.remove();
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- pvp protection!"));
            refundPlayer(shooter, itemManager.getItemEntryByType("RANDOM_POTION"));
        } else if (!passGDPvpCheck) {
            event.setCancelled(true);
            thrownPotion.remove();
            shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Magic blocked -- claim pvp protection!"));
            refundPlayer(shooter, itemManager.getItemEntryByType("RANDOM_POTION"));
        } else if (thrownPotion.hasMetadata("randomPotionLeft"))
            damagePlayers(shooter, location, 0.9, 1.3, itemManager.getItemEntryByType("RANDOM_POTION").getDamage(), 20);
    }
}
