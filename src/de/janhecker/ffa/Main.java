package de.janhecker.ffa;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class Main extends JavaPlugin {

    private static Main main;
    private FileConfiguration cfg;
    private YamlConfiguration inventoryCfg, statsCfg;
    private File inventoryFile, statsFile;
    private HashMap<UUID, User> users;
    private HashMap<String, User> offlineUsers;
    private Location spawn;
    private double radius;
    private String prefix;
    private boolean unbreakable, onlineMode;

    @Override
    public void onEnable() {
        main = this;
        users = new HashMap<>();
        offlineUsers = new HashMap<>();
        loadConfig();
        loadStatsCfg();
        PluginManager manager = Bukkit.getPluginManager();
        manager.registerEvents(new GameListener(this), this);
        getCommand("ffa").setExecutor(new FFACommand(this));
        for (Player all : Bukkit.getOnlinePlayers()) {
            User user = new User(this, all);
            if (onlineMode) {
                users.put(all.getUniqueId(), user);
            } else {
                offlineUsers.put(all.getName(), user);
            }
        }
    }

    @Override
    public void onDisable() {
        if (onlineMode) {
            users.forEach((uuid, user) -> user.quit());
        } else {
            offlineUsers.forEach((uuid, user) -> user.quit());
        }
        users.clear();
        offlineUsers.clear();
    }

    public static Main get() {
        return main;
    }

    public User getUser(UUID uuid) {
        return users.get(uuid);
    }

    public HashMap<UUID, User> getUsers() {
        return users;
    }

    public YamlConfiguration getStatsCfg() {
        return statsCfg;
    }

    private void loadStatsCfg() {
        statsFile = new File("plugins/FFA/stats.yml");
        if (statsFile.isFile()) {
            statsCfg = YamlConfiguration.loadConfiguration(statsFile);
        } else {
            statsCfg = new YamlConfiguration();
            statsCfg.options().header("Here are all stats of the players.");
            try {
                statsCfg.save(statsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveStatsCfg() {
        try {
            statsCfg.save(statsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        File file = new File("plugins/FFA/config.yml");
        inventoryFile = new File("plugins/FFA/inventory.yml");
        saveDefaultConfig();
        cfg = this.getConfig();
        if (inventoryFile.isFile()) {
            inventoryCfg = YamlConfiguration.loadConfiguration(inventoryFile);
        } else {
            inventoryCfg = new YamlConfiguration();
            inventoryCfg.options().header("Here is the inventory (when a player respawns). Modify it with '/ffa setinv'.");
            try {
                inventoryCfg.save(inventoryFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        onlineMode = cfg.getBoolean("Online#Mode");
        unbreakable = cfg.getBoolean("Unbreakable");
//        spawn = (Location) cfg.get("Locations.Spawn", new Location(Bukkit.getWorld("world"), -769.5, 110.0, 354.5, 0F, 0F));
        if (cfg.get("Locations.Spawn") == null) {
            Location loc = Bukkit.getWorlds().get(0).getSpawnLocation();
            spawn = loc;
            cfg.set("Locations.Spawn", loc);
        } else {
            spawn = (Location) cfg.get("Locations.Spawn");
        }
        prefix = cfg.getString("Messages.Prefix").replace('&', '§') + " ";
        radius = cfg.getDouble("Spawn#Radius");
    }

    public FileConfiguration getCfg() {
        return cfg;
    }

    public YamlConfiguration getInventoryCfg() {
        return inventoryCfg;
    }

    public Location getSpawn() {
        return this.spawn;
    }

    public void setSpawn(Location location) {
        this.spawn = location;
    }

    public String getPrefix() {
        return prefix;
    }

    public File getInventoryFile() {
        return inventoryFile;
    }

    public double getRadius() {
        return radius;
    }

    public boolean getUnbreakable() {
        return unbreakable;
    }

    public boolean isOnlineMode() {
        return onlineMode;
    }

    public User getOfflineUser(String name) {
        return offlineUsers.get(name);
    }

    public HashMap<String, User> getOfflineUsers() {
        return offlineUsers;
    }

    //    public void sendHealth(Player p) {
//        ScoreboardManager manager = Bukkit.getScoreboardManager();
//        Scoreboard board = manager.getNewScoreboard();
//        Objective objective = board.registerNewObjective("showhealth", "health");
//        objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
//        objective.setDisplayName("/ 20");
//        p.setScoreboard(board);
//        p.setHealth(p.getHealth()); //Update their health
//    }

    public void broadcast(String message) {
        Bukkit.getOnlinePlayers().forEach(all -> all.sendMessage(message));
    }

}
