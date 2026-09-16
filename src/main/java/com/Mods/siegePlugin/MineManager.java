package com.Mods.siegePlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MineManager implements CommandExecutor, Listener {

    private final SiegePlugin plugin;

    private final Set<Location> mineBlocks = new HashSet<>();
    private final Set<Location> regeneratingBlocks = new HashSet<>();
    private final Map<UUID, String> editMode = new HashMap<>();
    private final Map<Material, Double> oreChances = new LinkedHashMap<>();

    public MineManager(SiegePlugin plugin) {
        this.plugin = plugin;
        oreChances.put(Material.STONE, 20.0);
        oreChances.put(Material.IRON_ORE, 30.0);
        oreChances.put(Material.GOLD_ORE, 20.0);
        oreChances.put(Material.REDSTONE_ORE, 15.0);
        oreChances.put(Material.LAPIS_ORE, 10.0);
        oreChances.put(Material.DIAMOND_ORE, 5.0);
    }

    // 파일 로드 및 세이브용 메서드
    public Set<Location> getMineBlocks() { return mineBlocks; }
    public void addMineBlockRaw(Location loc) { mineBlocks.add(loc); }

    public void resetAllRegeneratingBlocks() {
        for (Location loc : regeneratingBlocks) {
            loc.getBlock().setType(getRandomOre());
        }
        regeneratingBlocks.clear();
    }

    private Material getRandomOre() {
        double random = Math.random() * 100.0;
        double current = 0.0;

        for (Map.Entry<Material, Double> entry : oreChances.entrySet()) {
            current += entry.getValue();
            if (random <= current) return entry.getKey();
        }
        return Material.STONE;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player) || !sender.isOp()) return true;

        if (args.length == 3 && args[1].equals("광산지정")) {
            String action = args[2];

            if (action.equals("추가")) {
                editMode.put(player.getUniqueId(), "ADD");
                player.sendMessage(Component.text("⚒ [광산 추가 모드]가 켜졌습니다!").color(NamedTextColor.GREEN));
                return true;
            }
            else if (action.equals("삭제")) {
                editMode.put(player.getUniqueId(), "REMOVE");
                player.sendMessage(Component.text("🗑 [광산 삭제 모드]가 켜졌습니다!").color(NamedTextColor.RED));
                return true;
            }
            else if (action.equals("종료")) {
                editMode.remove(player.getUniqueId());
                player.sendMessage(Component.text("✅ 광산 설정 모드를 종료했습니다.").color(NamedTextColor.GREEN));
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMineBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        if (editMode.containsKey(player.getUniqueId())) {
            String mode = editMode.get(player.getUniqueId());
            event.setCancelled(true);

            if (mode.equals("ADD")) {
                if (!mineBlocks.contains(loc)) {
                    mineBlocks.add(loc);
                    event.getBlock().setType(getRandomOre());
                    player.sendActionBar(Component.text("광산 블록이 추가되었습니다.").color(NamedTextColor.GREEN));
                }
            } else if (mode.equals("REMOVE")) {
                if (mineBlocks.contains(loc)) {
                    mineBlocks.remove(loc);
                    regeneratingBlocks.remove(loc);
                    player.sendActionBar(Component.text("광산 블록이 해제되었습니다.").color(NamedTextColor.RED));
                }
            }
            return;
        }

        if (mineBlocks.contains(loc)) {
            if (regeneratingBlocks.contains(loc)) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);

            Collection<ItemStack> drops = event.getBlock().getDrops(player.getInventory().getItemInMainHand());
            for (ItemStack item : drops) {
                loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), item);
            }

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            event.getBlock().setType(Material.BEDROCK);
            regeneratingBlocks.add(loc);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (mineBlocks.contains(loc)) {
                        loc.getBlock().setType(getRandomOre());
                        regeneratingBlocks.remove(loc);
                        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_PLACE, 0.8f, 1.0f);
                    }
                }
            }.runTaskLater(plugin, 100L);
        }
    }
}