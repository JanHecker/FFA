package de.janhecker.ffa;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;

public class FFACommand implements CommandExecutor {

    private Main main;

    public FFACommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(main.getPrefix() + "§cThis command can only be executed by a player.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("ffa.admin")) {
            p.sendMessage(main.getPrefix() + main.getCfg().getString("Messages.No#Permission").replace('&', '§'));
            return true;
        }
        if (args.length != 1) {
            p.sendMessage(main.getPrefix() + "§cSyntax Error. Use: /" + label + " <setspawn | setinv>\n" +
                    "§c<setspawn> sets the spawn for all players.\n" +
                    "§c<setinv> sets (respawn) inventory for all players.");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "setspawn":
                main.getInventoryCfg().set("Locations.Spawn", p.getLocation());
                p.sendMessage(main.getPrefix() + "§aYou updated the spawn location!");
                break;
            case "setinv":
                ArrayList<ItemStack> list = new ArrayList<>();
                ItemStack[] contents = p.getInventory().getContents();
                for (ItemStack item : contents) {
                    if (item != null) {
                        list.add(item);
                    }
                }
                main.getInventoryCfg().set("Contents", list);
                ArrayList<ItemStack> armorList = new ArrayList<>();
                ItemStack[] armorContents = p.getInventory().getArmorContents();
                for (ItemStack item : armorContents) {
                    if (item != null) {
                        armorList.add(item);
                    }
                }
                main.getInventoryCfg().set("Armor", armorList);
                //TODO
                try {
                    main.getInventoryCfg().save(main.getInventoryFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                p.sendMessage(main.getPrefix() + "§aYou updated the inventory!");
                break;
            default:
                p.sendMessage(main.getPrefix() + "§cSyntax Error. Use: /" + label + " <setspawn | setinv>\n" +
                        "§c<setspawn> sets the spawn for all players.\n" +
                        "§c<setinv> sets (respawn) inventory for all players.");
                break;
        }
        return true;
    }
}
