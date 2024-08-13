package dev.tbm00.spigot.item64.command;

import java.util.ArrayList;
import java.util.List;

//import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.item64.ItemManager;
import dev.tbm00.spigot.item64.model.ItemEntry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

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
        if (sender.hasPermission("item64.help") && args.length == 0) {
            showHelp(sender);
            return true;
        }

        if (args.length != 1 && args.length != 2) return false;

        String subCommand = args[0].toLowerCase();
        String argument = null;
        if (args.length == 2) argument = args[1];

        if (sender.hasPermission("item64.help") && subCommand.equals("help")) {
            showHelp(sender);
            return true;
        }

        // Run a give Item Command
        if (itemManager.isEnabled() && subCommand.equals("give") && argument != null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be run by a player!");
                return false;
            }
            for (ItemEntry entry : itemEntries) {
                if (argument.equals(entry.getKeyString())) {
                    Player player = (Player) sender;
                    if (player.hasPermission(entry.getGivePerm()) == true) {
                        // Create the item
                        ItemStack item = new ItemStack(Material.valueOf(entry.getItem()));
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', entry.getName()));
                            meta.setLore(entry.getLore().stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
                            if (entry.getGlowing()) {
                                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                                meta.addEnchant(org.bukkit.enchantments.Enchantment.ARROW_DAMAGE, 1, true);
                            }
                            meta.getPersistentDataContainer().set(new NamespacedKey(javaPlugin, entry.getKeyString()), PersistentDataType.STRING, "true");
                            item.setItemMeta(meta);
                        }
                        player.getInventory().addItem(item);
                        player.sendMessage(ChatColor.GREEN + "You have been given the " + entry.getKeyString());
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_RED + "--- " + ChatColor.RED + "Item64 Admin Commands" + ChatColor.DARK_RED + " ---\n"
            + ChatColor.WHITE + "/itm help" + ChatColor.GRAY + " Display this command list\n"
            + ChatColor.WHITE + "/itm give <itemKey>" + ChatColor.GRAY + " Spawn in a custom <item>\n"
            );
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command consoleCommand, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.clear();
            int i = 0;
            for (ItemEntry n : itemEntries) {
                if (n!=null && sender.hasPermission(n.getGivePerm()) && n.getKeyString().startsWith(args[0])) {
                    i = i+1;
                }
            }
            if (i>=1) list.add("give");
            if (sender.hasPermission("item64.help")) list.add("help");
        }
        if (args.length == 2) {
            list.clear();
            if (args[0].toString().equals("give")) {
                for (ItemEntry n : itemEntries) {
                    if (n!=null && sender.hasPermission(n.getGivePerm()) && n.getKeyString().startsWith(args[1])) {
                        list.add(n.getKeyString());
                    }
                }
            }
        }
        return list;
    }
}
