package com.Mods.siegePlugin;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;

public class CoreManager {
    private final SiegePlugin plugin;
    private final Objective sidebarObjective;
    private final String RED_SCORE = "§c❤ 빨강팀 코어";
    private final String BLUE_SCORE = "§9❤ 파랑팀 코어";

    public class TeamCore {
        private Location location;
        private double health = 1000;
        private double maxHealth = 1000;
        private Material material = Material.BEACON;
        private final BossBar bossBar;
        private final String teamName;
        private final NamedTextColor color;

        public TeamCore(String teamName, NamedTextColor color) {
            this.teamName = teamName;
            this.color = color;
            this.bossBar = BossBar.bossBar(
                    Component.text(teamName + "팀 코어 체력: " + (int)health + " / " + (int)maxHealth).color(color),
                    1.0f,
                    color == NamedTextColor.RED ? BossBar.Color.RED : BossBar.Color.BLUE,
                    BossBar.Overlay.PROGRESS
            );
        }
        public boolean hasCore() { return location != null; }
        public Location getLocation() { return location; }
        public BossBar getBossBar() { return bossBar; }
        public double getHealth() { return health; }
        public double getMaxHealth() { return maxHealth; }
        public Material getMaterial() { return material; }
    }

    private final Map<String, TeamCore> cores = new HashMap<>();

    public CoreManager(SiegePlugin plugin) {
        this.plugin = plugin;
        cores.put("빨강", new TeamCore("빨강", NamedTextColor.RED));
        cores.put("파랑", new TeamCore("파랑", NamedTextColor.BLUE));

        Scoreboard board = plugin.getScoreboard();
        Objective obj = board.getObjective("siege_core");
        if (obj != null) obj.unregister();

        sidebarObjective = board.registerNewObjective("siege_core", Criteria.DUMMY, Component.text("⚔ 공성전 ⚔").color(NamedTextColor.YELLOW));
        sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        sidebarObjective.getScore("  ").setScore(3);
        sidebarObjective.getScore(RED_SCORE).setScore(0);
        sidebarObjective.getScore(" ").setScore(1);
        sidebarObjective.getScore(BLUE_SCORE).setScore(0);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(board);
        }
    }

    public TeamCore getCore(String team) {
        return cores.get(team);
    }

    // 서버 시작(또는 리로드) 시 파일에서 데이터를 덮어씌움
    public void loadCoreData(String team, Location loc, double maxHealth, double health, Material material) {
        TeamCore core = cores.get(team);
        if (core == null) return;

        core.location = loc;
        core.maxHealth = maxHealth;
        core.health = health;
        core.material = material;

        core.location.getBlock().setType(core.material); // 확실하게 코어 블록 재배치

        // ★ 이 부분이 추가되었습니다! 리로드 시 기존 접속자에게 보스바 다시 띄우기
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(core.bossBar);
        }

        updateUI(team);
    }

    // 서버 종료(또는 리로드 전) 시 보스바 가리기
    public void removeAllCoresTemporary() {
        for (TeamCore core : cores.values()) {
            if (core.hasCore()) {
                for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(core.bossBar);
            }
        }
    }

    public void setCoreLocation(String team, Location loc) {
        TeamCore core = cores.get(team);
        if (core == null) return;

        core.location = loc.getBlock().getLocation();
        core.location.getBlock().setType(core.material);
        core.health = core.maxHealth;

        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(core.bossBar);
        updateUI(team);
    }

    public void removeCore(String team) {
        TeamCore core = cores.get(team);
        if (core == null || !core.hasCore()) return;

        core.location.getBlock().setType(Material.AIR);
        core.location = null;

        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(core.bossBar);

        String scoreName = team.equals("빨강") ? RED_SCORE : BLUE_SCORE;
        sidebarObjective.getScore(scoreName).setScore(0);
    }

    public void setCoreHealth(String team, double health) {
        TeamCore core = cores.get(team);
        if (core == null) return;
        core.maxHealth = health;
        core.health = health;
        if (core.hasCore()) updateUI(team);
    }

    public void setCoreMaterial(String team, Material material) {
        TeamCore core = cores.get(team);
        if (core == null || !material.isBlock()) return;
        core.material = material;
        if (core.hasCore()) core.location.getBlock().setType(material);
    }

    public void damageCore(String team, double amount) {
        TeamCore core = cores.get(team);
        if (core == null || !core.hasCore() || core.health <= 0) return;

        core.health -= amount;
        if (core.health < 0) core.health = 0;
        updateUI(team);

        core.location.getWorld().playSound(core.location, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);

        if (core.health == 0) {
            removeCore(team);
            Bukkit.broadcast(Component.text(team + "팀의 코어가 파괴되었습니다!").color(core.color));
        } else {
            core.location.getBlock().setType(core.material);
        }
    }

    public void updateUI(String team) {
        TeamCore core = cores.get(team);
        if (core == null) return;

        core.bossBar.name(Component.text(core.teamName + "팀 코어 체력: " + (int)core.health + " / " + (int)core.maxHealth).color(core.color));
        float progress = (float) (core.health / core.maxHealth);
        core.bossBar.progress(Math.max(0.0f, Math.min(1.0f, progress)));

        String scoreName = team.equals("빨강") ? RED_SCORE : BLUE_SCORE;
        sidebarObjective.getScore(scoreName).setScore((int) core.health);
    }
}