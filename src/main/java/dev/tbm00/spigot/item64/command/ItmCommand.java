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

import dev.tbm00.spigot.item64.ItemManager;
import dev.tbm00.spigot.item64.model.ItemEntry;

public class ItmCommand implements TabExecutor {
    private final JavaPlugin javaPlugin;
    private final ItemManager itemManager;
    private final List<ItemEntry> itemEntries;

    public ItmCommand(JavaPlugin javaPlugin, ItemManager itemManager) {
        this.javaPlugin = javaPlugin;
        this.itemManager = itemManager;
        this.itemEntries = itemManager.getItemEntries();
    }

    public boolean onCommand(CommandSender sender, Command consoleCommand, String label, String[] args) {
        if (!itemManager.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "Item64 is disabled!");
            return false;
        }
        
        if (args.length < 1 || args.length > 3) {
            // Invalid argument length - run HELP cmd
            if (sender.hasPermission("item64.help")) runHelpCmd(sender);
            return false;
        }

        String subCommand = args[0].toLowerCase();
        String argument = args.length >= 2 ? args[1] : null;
        String argument2 = args.length >= 3 ? args[2] : null;

        // Run HELP cmd
        if (sender.hasPermission("item64.help") && subCommand.equals("help")) {
            runHelpCmd(sender);
            return true;
        }

        // Run GIVE cmd
        if (subCommand.equals("give") && argument != null)
            return runGiveCmd(sender, argument, argument2);
        
        // Unknown subcommand - run HELP cmd
        if (sender.hasPermission("item64.help")) runHelpCmd(sender);
        
        return false;
    }

    private void runHelpCmd(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_RED + "--- " + ChatColor.RED + "Item64 Admin Commands" + ChatColor.DARK_RED + " ---\n"
            + ChatColor.WHITE + "/itm help" + ChatColor.GRAY + " Display this command list\n"
            + ChatColor.WHITE + "/itm give <itemKey> [player]" + ChatColor.GRAY + " Spawn a custom item\n"
            );
    }

    private boolean runGiveCmd(CommandSender sender, String argument, String argument2) {
        Player player = getPlayer(sender, argument2);
        if (player == null) return false;

        ItemEntry entry = getItemEntryByKey(argument);
        if (entry == null || !hasPermission(sender, entry)) return false;

        giveItemToPlayer(player, entry);
        return true;
    }

    private Player getPlayer(CommandSender sender, String argument2) {
        if (argument2 == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be run by a player!");
                return null;
            }
            return (Player) sender;
        } else {
            Player player = javaPlugin.getServer().getPlayer(argument2);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Could not find target player!");
            }
            return player;
        }
    }

    private ItemEntry getItemEntryByKey(String key) {
        return itemEntries.stream()
            .filter(entry -> entry.getKeyString().equals(key))
            .findFirst()
            .orElse(null);
    }

    private boolean hasPermission(CommandSender sender, ItemEntry entry) {
        return sender.hasPermission(entry.getGivePerm()) || sender instanceof ConsoleCommandSender;
    }

    private void giveItemToPlayer(Player player, ItemEntry entry) {
        ItemStack item = new ItemStack(Material.valueOf(entry.getItem()));
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
        }
        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "You have been given the " + entry.getKeyString());
        javaPlugin.getLogger().info(player.getDisplayName() + " has been given the " + entry.getKeyString());
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
            if (sender.hasPermission("item64.help")) list.add("help");
        } else if (args.length == 2 && "give".equals(args[0])) {
            list.clear();
            itemEntries.stream()
                .filter(entry -> sender.hasPermission(entry.getGivePerm()) && entry.getKeyString().startsWith(args[1]))
                .forEach(entry -> list.add(entry.getKeyString()));
        } else if (args.length == 3 && "give".equals(args[0])) {
            Bukkit.getOnlinePlayers().forEach(player -> list.add(player.getName()));
        }
        return list;
    }
}
