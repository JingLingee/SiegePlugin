package com.Mods.siegePlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SiegeCommand implements CommandExecutor, TabCompleter {

    private final SiegePlugin plugin;

    public SiegeCommand(SiegePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.isOp()) return true;

        if (args.length == 3 && args[0].equals("팀")) {
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) return true;

            String teamType = args[1];
            if (teamType.equals("빨강")) {
                plugin.getRedTeam().addEntry(target.getName());
                sender.sendMessage(Component.text(target.getName() + "님을 빨강팀으로 설정했습니다.").color(NamedTextColor.GREEN));
                target.sendMessage(Component.text("당신의 팀이 [빨강팀]으로 변경되었습니다.").color(NamedTextColor.RED));
            } else if (teamType.equals("파랑")) {
                plugin.getBlueTeam().addEntry(target.getName());
                sender.sendMessage(Component.text(target.getName() + "님을 파랑팀으로 설정했습니다.").color(NamedTextColor.GREEN));
                target.sendMessage(Component.text("당신의 팀이 [파랑팀]으로 변경되었습니다.").color(NamedTextColor.BLUE));
            }
            return true;
        }

        if (args.length >= 3 && args[0].equals("게임설정")) {
            if (args[1].equals("광산지정")) {
                return plugin.getMineManager().onCommand(sender, command, label, args);
            }

            String team = args[1];
            if (!team.equals("빨강") && !team.equals("파랑")) {
                sender.sendMessage(Component.text("팀은 '빨강' 또는 '파랑'만 가능합니다. (광산지정 제외)").color(NamedTextColor.RED));
                return true;
            }

            String action = args[2];

            if (action.equals("코어지정") && sender instanceof Player player) {
                if (plugin.getCoreManager().getCore(team).hasCore()) {
                    sender.sendMessage(Component.text("오류: 이미 해당 팀의 코어가 존재합니다! 코어삭제를 먼저 진행해주세요.").color(NamedTextColor.RED));
                    return true;
                }
                plugin.getCoreManager().setCoreLocation(team, player.getLocation());
                sender.sendMessage(Component.text(team + "팀 코어가 생성되었습니다.").color(NamedTextColor.GREEN));
                return true;
            }

            if (action.equals("코어삭제")) {
                if (!plugin.getCoreManager().getCore(team).hasCore()) {
                    sender.sendMessage(Component.text("오류: 삭제할 코어가 존재하지 않습니다.").color(NamedTextColor.RED));
                    return true;
                }
                plugin.getCoreManager().removeCore(team);
                sender.sendMessage(Component.text(team + "팀 코어가 삭제되었습니다.").color(NamedTextColor.GREEN));
                return true;
            }

            if (action.equals("코어체력") && args.length == 4) {
                try {
                    double hp = Double.parseDouble(args[3]);
                    plugin.getCoreManager().setCoreHealth(team, hp);
                } catch (NumberFormatException ignored) {}
                return true;
            }

            if (action.equals("코어블록") && args.length == 4) {
                Material mat = Material.matchMaterial(args[3].toUpperCase());
                if (mat != null && mat.isBlock()) {
                    plugin.getCoreManager().setCoreMaterial(team, mat);
                }
                return true;
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("팀");
            completions.add("게임설정");
        }
        else if (args.length == 2 && args[0].equals("팀")) {
            completions.add("빨강");
            completions.add("파랑");
        }
        else if (args.length == 2 && args[0].equals("게임설정")) {
            completions.add("빨강");
            completions.add("파랑");
            completions.add("광산지정");
        }
        else if (args.length == 3 && args[0].equals("팀")) {
            for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
        }
        else if (args.length == 3 && args[0].equals("게임설정") && (args[1].equals("빨강") || args[1].equals("파랑"))) {
            completions.add("코어지정");
            completions.add("코어삭제");
            completions.add("코어체력");
            completions.add("코어블록");
        }
        else if (args.length == 3 && args[0].equals("게임설정") && args[1].equals("광산지정")) {
            completions.add("추가");
            completions.add("삭제");
            completions.add("종료"); // 자동완성에 추가됨
        }
        else if (args.length == 4 && args[0].equals("게임설정") && args[2].equals("코어블록")) {
            String input = args[3].toLowerCase();
            for (Material mat : Material.values()) {
                if (mat.isBlock() && mat.name().toLowerCase().contains(input)) {
                    completions.add(mat.name().toLowerCase());
                }
            }
        }

        return completions;
    }
}