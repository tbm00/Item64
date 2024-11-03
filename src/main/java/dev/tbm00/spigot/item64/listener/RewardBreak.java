package dev.tbm00.spigot.item64.listener;

import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.model.ItemEntry;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class RewardBreak implements Listener {
    private final JavaPlugin javaPlugin;
    private final ConsoleCommandSender console;
    private final ConfigHandler configHandler;
    private final boolean enabled;
    private static Set<String> inactiveWorlds;
    private static List<ItemEntry> rewards = new ArrayList<>();
    private Random rand;

    public RewardBreak(JavaPlugin javaPlugin, ConfigHandler configHandler) {
        this.javaPlugin = javaPlugin;
        this.configHandler = configHandler;
        enabled = configHandler.isRewardedBreakingEnabled();
        console = Bukkit.getServer().getConsoleSender();
        inactiveWorlds = configHandler.getInactiveWorlds();
        if (configHandler.isRewardedBreakingEnabled()) {
            rand = new Random();
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
            int chance = rand.nextInt(100);

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
            } if (entry.getGiveItem() && !(entry.getMaterial()==null || entry.getMaterial().isBlank())) {
                ItemStack item = new ItemStack(Material.valueOf(entry.getMaterial()));
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return;
                if (!entry.getLore().isEmpty())
                    meta.setLore(entry.getLore().stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                if (entry.getHideEnchants())
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                addEnchantments(meta, entry);
                if (entry.getName()!=null && !entry.getName().isBlank())
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', entry.getName()));
                if (entry.getKeyString()!=null && !entry.getKeyString().isBlank())
                    meta.getPersistentDataContainer().set(new NamespacedKey(javaPlugin, entry.getKeyString()), PersistentDataType.STRING, "true");
                item.setItemMeta(meta);
                item.setAmount(1);
                block.getWorld().dropItem(block.getLocation(), item);
                rewarded = true;
            }
        } catch (Exception e) {
            javaPlugin.getLogger().warning("Error when giving reward " + entry.getKeyString() + " to " + player.getDisplayName() + " - " + e.getMessage());
            return;
        }
        if (rewarded) {
            if (!entry.getRewardMessage().isBlank() && !entry.getRewardMessage().isEmpty()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', entry.getRewardMessage())));
            }
            javaPlugin.getLogger().info(player.getDisplayName() + " found reward: " + entry.getKeyString());
        }
    }

    private void addEnchantments(ItemMeta meta, ItemEntry entry) {
        if (entry.getEnchants().isEmpty()) return;

        for (String line : entry.getEnchants()) {
            String[] parts = line.split(":");
            String name = parts[0];
            int level = Integer.parseInt(parts[1]);

            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
            if (enchantment != null) {
                meta.addEnchant(enchantment, level, true);
            } else {
                javaPlugin.getLogger().warning("Unknown enchantment '" + name + "' in " + entry.getKeyString());
            }
        }
    }
}
