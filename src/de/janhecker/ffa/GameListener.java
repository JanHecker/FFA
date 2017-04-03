package de.janhecker.ffa;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {

    private Main main;

    public GameListener(Main main) {
        this.main = main;
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        event.setJoinMessage("");
        Player p = event.getPlayer();
        new User(main, p);
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        event.setQuitMessage("");
        Player p = event.getPlayer();
        if (main.isOnlineMode()) {
            main.getUser(p.getUniqueId()).quit();
        } else {
            main.getOfflineUser(p.getName()).quit();
        }
    }

    @EventHandler
    public void entityDamage(EntityDamageByEntityEvent event) {
        if (!main.getSpawn().getWorld().getName().equals(event.getEntity().getWorld().getName())) return;
        if (!(event.getEntity() instanceof Player)) return;
        Entity damager = event.getDamager();
        if (!(damager instanceof Player || damager.getType() == EntityType.ARROW)) return;
        Player p;
        User user;
        if (damager.getType() == EntityType.ARROW) {
            Arrow a = (Arrow) damager;
            if (a.getShooter().toString().startsWith("CraftPlayer")) {
                p = (Player) event.getEntity();
                if (p.getHealth() - event.getFinalDamage() <= 0) {
                    if (main.isOnlineMode()) {
                        user = main.getUser(p.getUniqueId());
                    } else {
                        user = main.getOfflineUser(p.getName());
                    }
                    Bukkit.broadcastMessage("Yes");
                    event.setCancelled(true);
                    Player k = (Player) a.getShooter();
                    user.kill(k);
                    a.remove();
                }
            }
            return;
        }
        p = (Player) event.getEntity();
        Player killer = (Player) damager;
        User killUser;
        if (main.isOnlineMode()) {
            killUser = main.getUser(killer.getUniqueId());
            user = main.getUser(p.getUniqueId());
        } else {
            killUser = main.getOfflineUser(killer.getName());
            user = main.getOfflineUser(p.getName());
        }
        if (killUser.isInSpawn()) {
            event.setCancelled(true);
            String damagerInSpawn = main.getCfg().getString("Messages.DamagerInSpawn")
                    .replace('&', '§').replace("%PLAYER%", p.getName());
            killer.sendMessage(main.getPrefix() + damagerInSpawn);
            return;
        }
        if (user.isInSpawn()) {
            event.setCancelled(true);
            String damagedInSpawn = main.getCfg().getString("Messages.DamagedInSpawn")
                    .replace('&', '§').replace("%PLAYER%", p.getName());
            killer.sendMessage(main.getPrefix() + damagedInSpawn);
            return;
        }
        if (p.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            user.kill(killer);
        }
    }

    @EventHandler
    public void food(FoodLevelChangeEvent event) {
        if (main.getSpawn().getWorld().getName().equals(event.getEntity().getWorld().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void damage(EntityDamageEvent event) {
        if (!main.getSpawn().getWorld().getName().equals(event.getEntity().getWorld().getName())) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
                event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void drop(PlayerDropItemEvent event) {
        if (main.getSpawn().getWorld().getName().equals(event.getPlayer().getWorld().getName())
                && !event.getPlayer().hasPermission("ffa.admin")) {
            event.setCancelled(true);
        }
    }

}
