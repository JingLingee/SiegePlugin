package com.Mods.siegePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class DataManager {

    private final SiegePlugin plugin;

    public DataManager(SiegePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadData() {
        FileConfiguration config = plugin.getConfig();

        // 1. 코어 데이터 불러오기
        String[] teams = {"빨강", "파랑"};
        for (String team : teams) {
            String path = "cores." + team + ".";
            if (config.contains(path + "world")) {
                World world = Bukkit.getWorld(config.getString(path + "world"));
                if (world != null) {
                    double x = config.getDouble(path + "x");
                    double y = config.getDouble(path + "y");
                    double z = config.getDouble(path + "z");
                    Location loc = new Location(world, x, y, z);

                    double maxHealth = config.getDouble(path + "maxHealth", 1000.0);
                    double health = config.getDouble(path + "health", maxHealth);
                    Material material = Material.valueOf(config.getString(path + "material", "BEACON"));

                    plugin.getCoreManager().loadCoreData(team, loc, maxHealth, health, material);
                }
            }
        }

        // 2. 광산 블록 데이터 불러오기
        if (config.contains("mines")) {
            List<String> mineList = config.getStringList("mines");
            for (String str : mineList) {
                String[] parts = str.split(",");
                if (parts.length == 4) {
                    World world = Bukkit.getWorld(parts[0]);
                    if (world != null) {
                        try {
                            double x = Double.parseDouble(parts[1]);
                            double y = Double.parseDouble(parts[2]);
                            double z = Double.parseDouble(parts[3]);
                            Location loc = new Location(world, x, y, z);
                            plugin.getMineManager().addMineBlockRaw(loc);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
    }

    public void saveData() {
        FileConfiguration config = plugin.getConfig();

        // 이전 찌꺼기 삭제
        config.set("cores", null);
        config.set("mines", null);

        // 1. 코어 데이터 쓰기
        String[] teams = {"빨강", "파랑"};
        for (String team : teams) {
            CoreManager.TeamCore core = plugin.getCoreManager().getCore(team);
            if (core.hasCore()) {
                String path = "cores." + team + ".";
                config.set(path + "world", core.getLocation().getWorld().getName());
                config.set(path + "x", core.getLocation().getX());
                config.set(path + "y", core.getLocation().getY());
                config.set(path + "z", core.getLocation().getZ());
                config.set(path + "health", core.getHealth());
                config.set(path + "maxHealth", core.getMaxHealth());
                config.set(path + "material", core.getMaterial().name());
            }
        }

        // 2. 광산 블록 데이터 쓰기
        List<String> mineList = new ArrayList<>();
        for (Location loc : plugin.getMineManager().getMineBlocks()) {
            if (loc.getWorld() != null) {
                String str = loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
                mineList.add(str);
            }
        }
        config.set("mines", mineList);

        plugin.saveConfig();
    }
}