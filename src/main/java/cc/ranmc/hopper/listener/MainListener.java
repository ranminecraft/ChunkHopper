package cc.ranmc.hopper.listener;

import cc.ranmc.hopper.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.io.IOException;
import java.util.Objects;

import static cc.ranmc.hopper.Main.PREFIX;
import static cc.ranmc.hopper.utils.BaseUtil.color;
import static cc.ranmc.hopper.utils.HopperUtil.countBlock;
import static cc.ranmc.hopper.utils.HopperUtil.getKey;
import static cc.ranmc.hopper.utils.HopperUtil.hopper;
import static io.papermc.lib.PaperLib.getChunkAtAsync;

public class MainListener implements Listener {

    private static final Main plugin = Main.getInstance();

    @EventHandler
    public void onBlockExplodeEvent(BlockExplodeEvent event) {
        if (!plugin.isEnable() &&
                event.isCancelled() &&
                !plugin.getConfig().getBoolean("explode", true)) {
            return;
        }
        hopper(event.getBlock().getLocation());
    }
    
    @EventHandler
    public void onEntityExplodeEvent(EntityExplodeEvent event) {
        if (!plugin.isEnable() &&
                event.isCancelled() &&
                !plugin.getConfig().getBoolean("explode", true)) {
            return;
        }
        hopper(event.getLocation());
    }
    
    @EventHandler
    private void onBlockGrowEvent(BlockGrowEvent event) {
        if (!plugin.isEnable() &&
                event.isCancelled() &&
                !plugin.getConfig().getBoolean("grow", true)) {
            return;
        }
        hopper(event.getBlock().getLocation());
    }

    @EventHandler
    private void onEntityDeathEvent(EntityDeathEvent event) {
        if (!plugin.isEnable() &&
                event.isCancelled() &&
                !plugin.getConfig().getBoolean("entity", true)) {
            return;
        }
        hopper(event.getEntity().getLocation());
    }

    @EventHandler
    private void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
        if (!plugin.isEnable() &&
                event.isCancelled() &&
                !plugin.getConfig().getBoolean("piston", true)) {
            return;
        }
        hopper(event.getBlock().getLocation());
    }

    @EventHandler
    private void onBlockPlaceEvent(BlockPlaceEvent event) {
        if (!plugin.isEnable() || event.isCancelled()) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        String chunkKey = getKey(block.getChunk());
        if (plugin.getConfig().getBoolean("redstone", false) &&
                block.getType() == Material.REDSTONE_WIRE) {
            if (plugin.getRedStoneCountMap().containsKey(chunkKey)) {
                int count = plugin.getRedStoneCountMap().get(chunkKey);
                int max = plugin.getConfig().getInt("redstone-limit",128);
                if (count > max) {
                    event.setCancelled(true);
                    player.sendMessage(color("&c该区块放置红石已达上限\n为了大家游戏流畅度考虑\n请留更多性能给更多玩家"));
                    return;
                } else {
                    player.sendActionBar(color("&e该区块已经放置红石 " + count + " / " + max));
                    plugin.getRedStoneCountMap().put(chunkKey, plugin.getRedStoneCountMap().get(chunkKey) + 1);
                }
            } else {
                player.sendMessage(color("&e该区块计算红石中请稍后\n为了大家游戏流畅度考虑\n请留更多性能给更多玩家"));
                event.setCancelled(true);
                if (plugin.isFolia()) {
                    Bukkit.getRegionScheduler().run(plugin, block.getLocation(), scheduledTask -> countBlock(block));
                } else {
                    getChunkAtAsync(block.getLocation()).thenAccept(chunk -> countBlock(block));
                }
                return;
            }
        }
        if (block.getType() == Material.HOPPER) {
            Hopper hopper = (Hopper) block.getState();
            String key = getKey(hopper);
            if (hopper.getCustomName() != null &&
                    plugin.getChunkYml().contains(hopper.getCustomName())) {
                if (plugin.getConfig().getStringList("disable-world").contains(block.getWorld().getName())) {
                    player.sendMessage(PREFIX + color("&c该世界不允许放置区块漏斗"));
                    event.setCancelled(true);
                    return;
                }
                if (plugin.getDataYml().getString(key) != null) {
                    String[] xyz = Objects.requireNonNull(plugin.getDataYml().getString(key)).split("x");
                    player.sendMessage(PREFIX + color("&c该区块已存在区块漏斗 x" + xyz[0] + " y" + xyz[1] + " z" + xyz[2]));
                    event.setCancelled(true);
                    return;
                }
                player.sendMessage(PREFIX + color("&a你放置了一个区块漏斗"));
                StringBuilder xyz = new StringBuilder();
                Location location = hopper.getLocation();
                xyz.append(location.getBlockX());
                xyz.append("x");
                xyz.append(location.getBlockY());
                xyz.append("x");
                xyz.append(location.getBlockZ());
                plugin.getDataYml().set(key, xyz.toString());
                try {
                    plugin.getDataYml().save(plugin.getDataFile());
                } catch (IOException ignored) {}
            }
            if (plugin.getHopperCountMap().containsKey(chunkKey)) {
                int count = plugin.getHopperCountMap().get(chunkKey);
                int max = plugin.getConfig().getInt("limit",32);
                if (count > max) {
                    event.setCancelled(true);
                    player.sendMessage(color("&c该区块放置漏斗已达上限\n推荐您使用区块漏斗功能\n详情查看菜单中游戏帮助"));
                    return;
                } else {
                    player.sendActionBar(color("&e该区块已经放置漏斗 " + count + " / " + max));
                    plugin.getHopperCountMap().put(chunkKey, plugin.getHopperCountMap().get(chunkKey) + 1);
                }
            } else {
                player.sendMessage(color("&e该区块计算漏斗中请稍后\n推荐您使用区块漏斗功能\n详情查看菜单中游戏帮助"));
                event.setCancelled(true);
                if (plugin.isFolia()) {
                    Bukkit.getRegionScheduler().run(plugin, block.getLocation(), scheduledTask -> countBlock(block));
                } else {
                    getChunkAtAsync(block.getLocation()).thenAccept(chunk -> countBlock(block));
                }
                return;
            }
        }
    }

    @EventHandler
    private void onBlockBreakEvent(BlockBreakEvent event) {
        if (!plugin.isEnable() && event.isCancelled()) return;
        Block block = event.getBlock();
        if (plugin.getConfig().getBoolean("block", true)) {
            hopper(block.getLocation());
        }
        Player player = event.getPlayer();
        Material material = block.getType();
        if (material == Material.HOPPER) {
            Hopper hopper = (Hopper) block.getState();
            String chunkKey = getKey(block.getChunk());
            if (plugin.getHopperCountMap().containsKey(chunkKey)) {
                plugin.getHopperCountMap().put(chunkKey,
                        plugin.getHopperCountMap().get(chunkKey) - 1);
            }
            if (hopper.getCustomName() == null) return;
            if (plugin.getChunkYml().contains(hopper.getCustomName())) {
                player.sendMessage(PREFIX + color("&e你破坏了一个区块漏斗"));
                plugin.getDataYml().set(getKey(hopper), null);
                try {
                    plugin.getDataYml().save(plugin.getDataFile());
                } catch (IOException ignore) {}
            }

        } else if (material == Material.REDSTONE_WIRE ||
                material == Material.REDSTONE_TORCH ||
                material == Material.REPEATER) {
            String chunkKey = getKey(block.getChunk());
            if (plugin.getRedStoneCountMap().containsKey(chunkKey)) {
                plugin.getRedStoneCountMap().put(chunkKey,
                        plugin.getRedStoneCountMap().get(chunkKey) - 1);
            }
        }
    }
}
