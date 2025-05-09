package dev.tbm00.spigot.item64.listener;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import dev.tbm00.spigot.item64.Item64;
import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class RewardBreak implements Listener {
    private final Item64 item64;
    private final ConsoleCommandSender console;
    private final ConfigHandler configHandler;
    private final boolean enabled;
    private final Set<String> inactiveWorlds;
    private final List<ItemEntry> rewards = new ArrayList<>();
    private final Random rand = new Random();

    public RewardBreak(Item64 item64, ConfigHandler configHandler) {
        this.item64 = item64;
        this.configHandler = configHandler;
        enabled = configHandler.isRewardedBreakingEnabled();
        console = Bukkit.getServer().getConsoleSender();
        inactiveWorlds = configHandler.getInactiveWorlds();
        if (configHandler.isRewardedBreakingEnabled()) {
            for (ItemEntry entry : configHandler.getItemEntries()) {
                if (entry.getRewardChance()>0) rewards.add(entry);
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled) return;

        // check if block should get rewarded, if so, reward the player
        Block block = event.getBlock();

        if (!inactiveWorlds.contains(block.getWorld().getName()) 
        && configHandler.getRewardedBreaking().contains(block.getType().name())) {
            double chance = rand.nextDouble(100.0);

            if (chance < configHandler.getRewardedBreakingChance()) {
                // algorithm to *fairly* pick the reward based on chance.
                int entryCount = rewards.size();
                int randomIndex = 0;
                ItemEntry selectedEntry = null;
                /*int attemptsLeft = 100;
                while (attemptsLeft>0) {*/
                while (true) {
                    //attemptsLeft--;
                    randomIndex = rand.nextInt(entryCount);
                    selectedEntry = rewards.get(randomIndex);

                    if (rand.nextDouble() * 100 < selectedEntry.getRewardChance()) {
                        giveRewardToPlayer(selectedEntry, event.getPlayer(), block);
                        block.setType(Material.AIR);
                        return;
                    }
                }
            }
        }
    }

    private void giveRewardToPlayer(ItemEntry entry, Player player, Block block) {
        boolean rewarded = false;
        try {
            if (!entry.getRewardCommands().isEmpty()) {
                String name = player.getName();
                for (String cmd : entry.getRewardCommands()) {
                    cmd = cmd.replace("<player>", name);
                    Bukkit.dispatchCommand(console, cmd);
                    rewarded = true;
                }
            } if (entry.getGiveItem() && entry.getMaterial()!=null) {
                ItemStack item = configHandler.getConfiguredItemStack(entry);
                item.setAmount(1);
                block.getWorld().dropItem(block.getLocation(), item);
                rewarded = true;
            }
        } catch (Exception e) {
            item64.logRed("Error when giving reward " + entry.getKeyString() + " to " + player.getDisplayName() + " - ");
            item64.getLogger().warning(e.getMessage());
            return;
        }
        if (rewarded) {
            if (!entry.getRewardMessage().isBlank() && !entry.getRewardMessage().isEmpty()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', entry.getRewardMessage())));
            }
            item64.logYellow(player.getDisplayName() + " found reward: " + entry.getKeyString());
        }
    }
}
