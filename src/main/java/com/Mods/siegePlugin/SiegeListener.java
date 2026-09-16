package com.Mods.siegePlugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Team;

public class SiegeListener implements Listener {

    private final SiegePlugin plugin;

    public SiegeListener(SiegePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();

        if (plugin.getCoreManager().getCore("빨강").hasCore() &&
                loc.equals(plugin.getCoreManager().getCore("빨강").getLocation())) {
            event.setCancelled(true);
            plugin.getCoreManager().damageCore("빨강", 1.0);
            return;
        }

        if (plugin.getCoreManager().getCore("파랑").hasCore() &&
                loc.equals(plugin.getCoreManager().getCore("파랑").getLocation())) {
            event.setCancelled(true);
            plugin.getCoreManager().damageCore("파랑", 1.0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player damager = null;
        if (event.getDamager() instanceof Player p) {
            damager = p;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            damager = p;
        }

        if (damager == null) return;

        Team victimTeam = plugin.getScoreboard().getEntryTeam(victim.getName());
        Team damagerTeam = plugin.getScoreboard().getEntryTeam(damager.getName());

        if (victimTeam != null && damagerTeam != null && victimTeam.getName().equals(damagerTeam.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().setScoreboard(plugin.getScoreboard());

        // 수정된 부분: getBossBar()를 사용해 접근합니다.
        if (plugin.getCoreManager().getCore("빨강").hasCore()) {
            event.getPlayer().showBossBar(plugin.getCoreManager().getCore("빨강").getBossBar());
        }
        if (plugin.getCoreManager().getCore("파랑").hasCore()) {
            event.getPlayer().showBossBar(plugin.getCoreManager().getCore("파랑").getBossBar());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Team team = plugin.getScoreboard().getEntryTeam(player.getName());

        if (team != null) {
            event.renderer((source, sourceDisplayName, message, viewer) ->
                    Component.text()
                            .append(sourceDisplayName.color(team.color()))
                            .append(Component.text(": ").color(NamedTextColor.WHITE))
                            .append(message)
                            .build()
            );
        }
    }
}