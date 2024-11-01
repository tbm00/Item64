package dev.tbm00.spigot.item64.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.item64.ConfigHandler;
import dev.tbm00.spigot.item64.model.ItemEntry;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ItmCommand implements TabExecutor {
    private final JavaPlugin javaPlugin;
    private final ConfigHandler configHandler;
    private final List<ItemEntry> itemEntries;

    public ItmCommand(JavaPlugin javaPlugin, ConfigHandler configHandler) {
        this.javaPlugin = javaPlugin;
        this.configHandler = configHandler;
        this.itemEntries = configHandler.getItemEntries();
    }

    public boolean onCommand(CommandSender sender, Command consoleCommand, String label, String[] args) {
        if (!configHandler.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "Item64 is disabled!");
            return false;
        }
        
        if (args.length < 1 || args.length > 4) {
            // Invalid argument length - run HELP cmd
            if (sender.hasPermission("item64.help")) runHelpCmd(sender);
            return false;
        }

        String subCommand = args[0].toLowerCase();
        String argument = args.length >= 2 ? args[1] : null;
        String argument2 = args.length >= 3 ? args[2] : null;
        String argument3 = args.length >= 4 ? args[3] : null;

        // Run HELP cmd
        if (sender.hasPermission("item64.help") && subCommand.equals("help")) {
            runHelpCmd(sender);
            return true;
        }

        // Run HEAL cmd
        if (subCommand.equals("heal"))
            return runHealCmd(sender, argument, argument2);

        // Run GIVE cmd
        if (subCommand.equals("give") && argument != null)
            return runGiveCmd(sender, argument, argument2, argument3);
        
        // Unknown subcommand - run HELP cmd
        if (sender.hasPermission("item64.help")) runHelpCmd(sender);
        
        return false;
    }

    private void runHelpCmd(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_RED + "--- " + ChatColor.RED + "Item64 Admin Commands" + ChatColor.DARK_RED + " ---\n"
            + ChatColor.WHITE + "/itm help" + ChatColor.GRAY + " Display this command list\n"
            + ChatColor.WHITE + "/itm give <itemKey> [player] (#)" + ChatColor.GRAY + " Spawn custom item(s)\n"
            + ChatColor.WHITE + "/itm heal [player]" + ChatColor.GRAY + " Heal yourself or [player]\n"
            + ChatColor.WHITE + "/itm heal [player] -fx" + ChatColor.GRAY + " Heal & remove effects on yourself or [player]\n"
        );
    }

    private boolean runHealCmd(CommandSender sender, String argument, String argument2) {
        if (!sender.hasPermission("item64.heal") && !(sender instanceof ConsoleCommandSender))
            return false;
        Player player = getPlayer(sender, argument);
        if (player == null) return false;

        player.setHealth(20);
        player.setFoodLevel(20);
        if (argument2 != null && argument2.equalsIgnoreCase("-fx")) {
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "You have been healed!"));

        if (!player.equals(sender))
            sender.sendMessage(ChatColor.GREEN + player.getName() + " has been healed.");
        return true;
    }

    private boolean runGiveCmd(CommandSender sender, String argument, String argument2, String argument3) {
        Player player = getPlayer(sender, argument2);
        if (player == null) return false;

        ItemEntry entry = getItemEntryByKey(argument);
        if (entry == null || !hasPermission(sender, entry)) return false;
        
        int quantity = 1;
        if (argument3 != null) quantity = Integer.parseInt(argument3);

        giveItemToPlayer(player, entry, quantity);
        return true;
    }
    
    private ItemEntry getItemEntryByKey(String key) {
        return itemEntries.stream()
            .filter(entry -> entry.getKeyString().equals(key))
            .findFirst()
            .orElse(null);
    }

    private Player getPlayer(CommandSender sender, String arg) {
        if (arg == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be run by a player!");
                return null;
            }
            return (Player) sender;
        } else {
            Player player = javaPlugin.getServer().getPlayer(arg);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Could not find target player!");
            }
            return player;
        }
    }

    private boolean hasPermission(CommandSender sender, ItemEntry entry) {
        return sender.hasPermission(entry.getGivePerm()) || sender instanceof ConsoleCommandSender;
    }

    private void giveItemToPlayer(Player player, ItemEntry entry, int quantity) {
        try {
            ItemStack item = new ItemStack(Material.valueOf(entry.getMaterial()));
            ItemMeta meta = item.getItemMeta();
    
            if (meta != null) {
                if (!entry.getLore().isEmpty())
                    meta.setLore(entry.getLore().stream()
                        .map(l -> ChatColor.translateAlternateColorCodes('&', l))
                        .toList());
    
                if (entry.getHideEnchants()) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                addEnchantments(meta, entry);
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', entry.getName()));
                meta.getPersistentDataContainer().set(new NamespacedKey(javaPlugin, entry.getKeyString()), PersistentDataType.STRING, "true");
                item.setItemMeta(meta);
                item.setAmount(quantity);
            }
            player.getInventory().addItem(item);
            player.sendMessage(ChatColor.GREEN + "You have been given the " + entry.getKeyString());
            javaPlugin.getLogger().info(player.getDisplayName() + " has been given the " + entry.getKeyString());
        } catch (Exception e) {
            javaPlugin.getLogger().warning("Error when giving item: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "There was an error giving the item.");
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
                javaPlugin.getLogger().warning("Unknown enchantment: " + name);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command consoleCommand, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.clear();
            if ("give".startsWith(args[0]) && itemEntries.stream().anyMatch(entry -> sender.hasPermission(entry.getGivePerm())))
                list.add("give");
            if ("heal".startsWith(args[0]) && sender.hasPermission("item64.heal")) 
                list.add("heal");
            if (sender.hasPermission("item64.help")) list.add("help");
        } else if (args.length == 2 && ("give".equals(args[0]) || "heal".equals(args[0]))) {
            list.clear();
            if ("give".equals(args[0])) {
                itemEntries.stream()
                    .filter(entry -> sender.hasPermission(entry.getGivePerm()) && entry.getKeyString().startsWith(args[1]))
                    .forEach(entry -> list.add(entry.getKeyString()));
            } else if ("heal".equals(args[0])) {
                Bukkit.getOnlinePlayers().forEach(player -> list.add(player.getName()));
            }
        } else if (args.length == 3 && "give".equals(args[0])) {
            Bukkit.getOnlinePlayers().forEach(player -> list.add(player.getName()));
        }
        return list;
    }
}