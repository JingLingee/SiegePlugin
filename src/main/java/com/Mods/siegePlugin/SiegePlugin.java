package com.Mods.siegePlugin;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class SiegePlugin extends JavaPlugin {

    private Scoreboard scoreboard;
    private Team redTeam;
    private Team blueTeam;
    private CoreManager coreManager;
    private MineManager mineManager;
    private DataManager dataManager;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // 기본 config 생성
        setupTeams();

        coreManager = new CoreManager(this);
        mineManager = new MineManager(this);
        dataManager = new DataManager(this);

        dataManager.loadData(); // 파일에서 좌표 및 체력 불러오기

        getCommand("공성전").setExecutor(new SiegeCommand(this));
        getCommand("공성전").setTabCompleter(new SiegeCommand(this));

        getServer().getPluginManager().registerEvents(new SiegeListener(this), this);
        getServer().getPluginManager().registerEvents(mineManager, this);
    }

    @Override
    public void onDisable() {
        if (coreManager != null) {
            coreManager.removeAllCoresTemporary(); // 서버 꺼질 땐 UI만 가림 (블록은 유지)
        }
        if (mineManager != null) {
            mineManager.resetAllRegeneratingBlocks(); // 재생 중인 기반암 복구
        }
        if (dataManager != null) {
            dataManager.saveData(); // 파일에 저장
        }
    }

    private void setupTeams() {
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        redTeam = scoreboard.getTeam("Red");
        if (redTeam == null) redTeam = scoreboard.registerNewTeam("Red");
        redTeam.color(NamedTextColor.RED);

        blueTeam = scoreboard.getTeam("Blue");
        if (blueTeam == null) blueTeam = scoreboard.registerNewTeam("Blue");
        blueTeam.color(NamedTextColor.BLUE);
    }

    public Team getRedTeam() { return redTeam; }
    public Team getBlueTeam() { return blueTeam; }
    public Scoreboard getScoreboard() { return scoreboard; }
    public CoreManager getCoreManager() { return coreManager; }
    public MineManager getMineManager() { return mineManager; }
}