package de.janhecker.ffa;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class User {

    private Main main;
    private Player p;

    private Scoreboard board;
    private Objective objective;
    private int killStreak, kills, deaths;
    private Score killScore, deathScore, killstreakScore;
    @SuppressWarnings("FieldCanBeLocal")
    private long firstJoin;

    public User(Main main, Player p) {
        this.main = main;
        this.p = p;
        this.join();
        if (main.isOnlineMode()) {
            main.getUsers().put(p.getUniqueId(), this);
        } else {
            main.getOfflineUsers().put(p.getName(), this);
        }
    }

    private void join() {
        p.setGameMode(GameMode.valueOf(main.getCfg().getString("Default#GameMode", GameMode.ADVENTURE.toString())));
        String name = p.getName();
        if (main.isOnlineMode()) {
            String uuid = p.getUniqueId().toString();
            if (!main.getStatsCfg().isSet(uuid + ".First#Join")) {
                main.getStatsCfg().set(uuid + ".First#Join", System.currentTimeMillis());
                main.getStatsCfg().set(uuid + ".Kills", 0);
                main.getStatsCfg().set(uuid + ".Deaths", 0);
            }
            firstJoin = main.getStatsCfg().getLong(uuid + ".First#Join");
            kills = main.getStatsCfg().getInt(uuid + ".Kills");
            deaths = main.getStatsCfg().getInt(uuid + ".Deaths");
            killStreak = 0;
        } else {
            if (!main.getStatsCfg().isSet(name + ".First#Join")) {
                main.getStatsCfg().set(name + ".First#Join", System.currentTimeMillis());
                main.getStatsCfg().set(name + ".Kills", 0);
                main.getStatsCfg().set(name + ".Deaths", 0);
            }
            firstJoin = main.getStatsCfg().getLong(name + ".First#Join");
            kills = main.getStatsCfg().getInt(name + ".Kills");
            deaths = main.getStatsCfg().getInt(name + ".Deaths");
            killStreak = 0;
        }
        restore();
        String join = main.getCfg().getString("Messages.Join").replace('&', '§').replace("%PLAYER%", name);
        String joinBC = main.getCfg().getString("Messages.Broadcast.Join").replace('&', '§').replace("%PLAYER%", name);
        if (!Objects.equals(join, "")) p.sendMessage(main.getPrefix() + join);
        if (!Objects.equals(joinBC, "")) main.broadcast(main.getPrefix() + joinBC);
        Bukkit.getScheduler().runTaskLater(main, this::sendSidebar, 10);
    }

    public void quit() {
        if (main.isOnlineMode()) {
            String uuid = p.getUniqueId().toString();
            main.getStatsCfg().set(uuid + ".Kills", kills);
            main.getStatsCfg().set(uuid + ".Deaths", deaths);
        } else {
            String name = p.getName();
            main.getStatsCfg().set(name + ".Kills", kills);
            main.getStatsCfg().set(name + ".Deaths", deaths);
        }
        main.saveStatsCfg();
        String quitBC = main.getCfg().getString("Messages.Broadcast.Quit").replace('&', '§').replace("%PLAYER%", p.getName());
        if (!Objects.equals(quitBC, "")) main.broadcast(main.getPrefix() + quitBC);
    }

    public void kill(Player killer) {
        restore();
        killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 2, true, false));
        User killUser;
        if (main.isOnlineMode()) {
            killUser = main.getUser(killer.getUniqueId());
        } else {
            killUser = main.getOfflineUser(killer.getName());
        }
        killUser.addKill();
        killUser.addKillStreak();
        this.addDeath();
        this.resetKillStreak();

        String killBC = main.getCfg().getString("Messages.Broadcast.Kill").replace('&', '§')
                .replace("%KILLER%", killer.getName()).replace("%PLAYER%", p.getName());
        if (!Objects.equals(killBC, "")) killer.sendMessage(main.getPrefix() + killBC);
        String kill = main.getCfg().getString("Messages.Kill").replace('&', '§').replace("%PLAYER%", p.getName());
        if (!Objects.equals(kill, "")) killer.sendMessage(main.getPrefix() + kill);
        String death = main.getCfg().getString("Messages.Death").replace('&', '§').replace("%KILLER%", killer.getName());
        if (!Objects.equals(death, "")) p.sendMessage(main.getPrefix() + death);
    }

    private void restore() {
        PlayerInventory inv = p.getInventory();
        inv.setArmorContents(null);
        inv.clear();
        p.setHealthScale(20);
        p.setHealth(20);
        p.setFireTicks(0);
        p.setLastDamage(0);
        p.setExhaustion(0);
        p.setSneaking(false);
        p.setSprinting(false);
        p.setExp(0);
        p.setLevel(0);
        p.setSaturation(20);
        p.setFoodLevel(20);
        if (main.getSpawn() == null) {
            main.broadcast(main.getPrefix() + "§4Set the default spawn first (with \"/ffa setspawn\")");
        } else {
            p.teleport(main.getSpawn());
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(main, () -> {
            if (main.getInventoryCfg().getList("Contents") == null ||
                    main.getInventoryCfg().getList("Armor") == null) {
                ArrayList<ItemStack> list = new ArrayList<>();
                ArrayList<ItemStack> armorList = new ArrayList<>();
                list.add(new ItemStack(Material.DIAMOND_SWORD));
                list.add(new ItemStack(Material.GOLDEN_APPLE, 10));
                armorList.add(new ItemStack(Material.IRON_BOOTS));
                main.getInventoryCfg().set("Contents", list);
                main.getInventoryCfg().set("Armor", armorList);
                main.broadcast(main.getPrefix() + "§4Set the default inventory first (with \"/ffa setinv\")");
            }
            ItemStack[] contents = inv.getContents();
            List<?> list = main.getInventoryCfg().getList("Contents");
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = (ItemStack) list.get(i);
                if (main.getUnbreakable()) {
                    ItemMeta meta = stack.getItemMeta();
                    meta.spigot().setUnbreakable(true);
                    meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                    stack.setItemMeta(meta);
                }
                contents[i] = stack;
            }
            inv.setContents(contents);
            ItemStack[] armorContents = inv.getArmorContents();
            List<?> armorList = main.getInventoryCfg().getList("Armor");
            for (int i = 0; i < armorList.size(); i++) {
                ItemStack stack = (ItemStack) armorList.get(i);
                if (main.getUnbreakable()) {
                    ItemMeta meta = stack.getItemMeta();
                    meta.spigot().setUnbreakable(true);
                    meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                    stack.setItemMeta(meta);
                }
                armorContents[i] = stack;
            }
            inv.setArmorContents(armorContents);
            p.updateInventory();
        }, 5);
    }

    public int getKillStreak() {
        return killStreak;
    }

    public void addKillStreak() {
        killStreak = killStreak + 1;
        updateSidebarKillstreak();
    }

    public void addKill() {
        kills = kills + 1;
        updateSidebarKills();
    }

    public void addDeath() {
        deaths = deaths + 1;
        updateSidebarDeaths();
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    private void resetKillStreak() {
        killStreak = 0;
    }

    private void sendSidebar() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        board = manager.getNewScoreboard();
        objective = board.registerNewObjective(p.getName(), "foobar");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.setDisplayName(main.getCfg().getString("Sidebar.Title").replace('&', '§'));
        objective.getScore(" ").setScore(11);

        objective.getScore(main.getCfg().getString("Sidebar.Kills").replace('&', '§')).setScore(10);
        killScore = objective.getScore(main.getCfg().getString("Sidebar.KillValue").replace('&', '§')
                .replace("%VALUE%", String.valueOf(kills)));
        killScore.setScore(9);
        objective.getScore("  ").setScore(8);

        objective.getScore(main.getCfg().getString("Sidebar.Map").replace('&', '§')).setScore(7);
        objective.getScore(main.getCfg().getString("Sidebar.MapValue").replace('&', '§')
                .replace("%VALUE%", main.getSpawn().getWorld().getName())).setScore(6);
        objective.getScore("   ").setScore(5);

        objective.getScore(main.getCfg().getString("Sidebar.Deaths").replace('&', '§')).setScore(4);
        deathScore = objective.getScore(main.getCfg().getString("Sidebar.DeathValue").replace('&', '§')
                .replace("%VALUE%", String.valueOf(deaths)));
        deathScore.setScore(3);
        objective.getScore("    ").setScore(2);

        objective.getScore(main.getCfg().getString("Sidebar.Killstreak").replace('&', '§')).setScore(1);
        killstreakScore = objective.getScore(main.getCfg().getString("Sidebar.KillstreakValue").replace('&', '§')
                .replace("%VALUE%", String.valueOf(killStreak)));
        killstreakScore.setScore(0);

        p.setScoreboard(board);
    }

    public void updateSidebarKills() {
        String killEntry = killScore.getEntry();
        if (killEntry.contains("§")) {
            killEntry = killEntry.substring(0, 2);
            killEntry += kills;
        } else {
            killEntry = String.valueOf(kills);
        }
        board.resetScores(killScore.getEntry());
        killScore = objective.getScore(killEntry);
        killScore.setScore(9);
    }

    public void updateSidebarDeaths() {
        String deathEntry = deathScore.getEntry();
        if (deathEntry.contains("§")) {
            deathEntry = deathEntry.substring(0, 2);
            deathEntry += deaths;
        } else {
            deathEntry = String.valueOf(deaths);
        }
        board.resetScores(deathScore.getEntry());
        deathScore = objective.getScore(deathEntry);
        deathScore.setScore(3);
    }

    public void updateSidebarKillstreak() {
        String killstreakEntry = killstreakScore.getEntry();
        if (killstreakEntry.contains("§")) {
            killstreakEntry = killstreakEntry.substring(0, 2);
            killstreakEntry += killStreak;
        } else {
            killstreakEntry = String.valueOf(killStreak);
        }
        board.resetScores(killstreakScore.getEntry());
        killstreakScore = objective.getScore(killstreakEntry);
        killstreakScore.setScore(0);
    }

    public boolean isInSpawn() {
//        Validate.notNull(p.getLocation(), "ploc");
//        Validate.notNull(main.getSpawn(), "spawn");
//        Validate.notNull(main.getRadius(), "radius");
        return p.getLocation().distance(main.getSpawn()) <= main.getRadius();
    }

}