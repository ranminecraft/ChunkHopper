package cn.ranmc;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main extends JavaPlugin implements Listener {

    private String prefix;
    private YamlConfiguration data, countData;
    private File dataYml, countYml;
    private List<String> lock = new ArrayList<>();
    private boolean enable;
    private int delay;
    private String HopperName;
    private final boolean folia = isFolia();

    @Override
    public void onEnable() {
        print("&e-----------------------");
        print("&b漏斗箱子 &dBy阿然");
        print("&b插件版本:"+getDescription().getVersion());
        print("&b服务器版本:"+getServer().getVersion());
        print("&cQQ 2263055528");
        print("&e-----------------------");

        loadConfig();
        //updateCheck();

        //注册Event
        Bukkit.getPluginManager().registerEvents(this, this);

        super.onEnable();
    }

    @Override
    public void onDisable() {
        try {
            countData.save(countYml);
            print("&a漏斗箱子保存数据成功");
        } catch (IOException e) {
            print("&c漏斗箱子保存数据失败");
            throw new RuntimeException(e);
        }
        super.onDisable();
    }

    /**
     * 是 Folia 端
     *
     * @return boolean
     */
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    /**
     * 储存掉落物
     */
    private void hopper(Location location) {
        Chunk chunk = location.getChunk();
        String name = location.getWorld().getName() + chunk.getX() + "x" + chunk.getZ();
        if (lock.contains(name)) {
            return;
        } else {
            lock.add(name);
        }

        if (folia) {
            Bukkit.getServer().getRegionScheduler().runDelayed(
                    this,
                    location,
                    scheduledTask -> hopperAddItem(location, chunk, name),
                    delay);
        } else {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(
                    this,
                    () -> hopperAddItem(location, chunk, name),
                    delay);
        }

    }

    private void hopperAddItem(Location location, Chunk chunk, String name) {
        lock.remove(name);
        List<String> itemList = getConfig().getStringList("itemList");
        if (!enable || !chunk.isLoaded() || data.getString(name)==null) return;
        Entity[] entities = chunk.getEntities();
        String[] xyz = Objects.requireNonNull(data.getString(name)).split("x");
        Block block = location.getWorld().getBlockAt(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
        if (block.getType() == Material.HOPPER) {
            Hopper hopper = (Hopper) block.getState();
            for (Entity entity : entities) {
                if (entity.getType() == EntityType.DROPPED_ITEM) {
                    Item item = (Item) entity;
                    if (itemList.contains(item.getItemStack().getType().toString())) {
                        hopper.getInventory().addItem(item.getItemStack());
                        entity.remove();
                    }
                }
            }
        } else {
            data.set(name, null);
            try { data.save(dataYml); } catch (IOException ignore) {}
        }
    }

    @EventHandler
    private void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
        if (!enable) return;
        hopper(event.getBlock().getLocation());
    }

    @EventHandler
    private void onBlockPlaceEvent(BlockPlaceEvent event) {
        if (!enable) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (!event.isCancelled() && block.getType() == Material.HOPPER) {
            String chunkName = block.getChunk().toString();
            if (countData.contains(chunkName)) {
                int count = countData.getInt(chunkName);
                if (count >= getConfig().getInt("limit",32)) {
                    event.setCancelled(true);
                    player.sendMessage(color("&c该区块存在漏斗已达上限\n推荐您使用区块漏斗功能\n详情查看菜单中游戏帮助"));
                    return;
                } else {
                    countData.set(chunkName, count + 1);

                }
            } else {
                player.sendMessage(color("&e该区块计算漏斗中请稍后\n推荐您使用区块漏斗功能\n详情查看菜单中游戏帮助"));
                event.setCancelled(true);
                if (folia) {
                    Bukkit.getRegionScheduler().run(this, block.getLocation(), scheduledTask -> countHopper(block));
                } else {
                    PaperLib.getChunkAtAsync(block.getLocation()).thenAccept(chunk -> countHopper(block));
                }
            }
            Hopper hopper = (Hopper) block.getState();
            String name = hopper.getWorld().getName()+hopper.getChunk().getX()+"x"+hopper.getChunk().getZ();
            if (HopperName.equals(hopper.getCustomName())) {
                if (data.contains(name)) {
                    String[] xyz = data.getString(name).split("x");
                    player.sendMessage(prefix + color("&c该区块已存在区块漏斗 x"+xyz[0]+" y"+xyz[1]+" z"+xyz[2]));
                    event.setCancelled(true);
                    return;
                }
                player.sendMessage(prefix + color("&a你放置了一个区块漏斗"));
                StringBuilder xyz = new StringBuilder();
                Location location = hopper.getLocation();
                xyz.append(location.getBlockX());
                xyz.append("x");
                xyz.append(location.getBlockY());
                xyz.append("x");
                xyz.append(location.getBlockZ());
                data.set(name,xyz.toString());
                try {
                    data.save(dataYml);
                } catch (IOException ignored) {}
            }

        }
    }

    private void countHopper(Block block) {
        String chunkName = block.getChunk().toString();
        int count = 0;
        Entity[] entities = block.getChunk().getEntities();
        for (Entity entity : entities) {
            if (entity.getType() == EntityType.MINECART_HOPPER) count++;
        }
        for (int x = 0; x < 16; x++) {
            for (int y = -31; y < 320; y++) {
                for (int z = 0; z < 16; z++) {
                    if (block.getChunk().getBlock(x, y, z).getType() == Material.HOPPER) count++;
                }
            }
        }
        countData.set(chunkName, count);
    }

    @EventHandler
    private void onBlockBreakEvent(BlockBreakEvent event) {
        if (!enable) return;
        Block block = event.getBlock();
        hopper(block.getLocation());
        Player player = event.getPlayer();
        String chunkName = block.getChunk().toString();
        if (block.getType() == Material.HOPPER) {
            Hopper hopper = (Hopper) block.getState();
            if (countData.contains(chunkName)) countData.set(chunkName, countData.getInt(chunkName) - 1);
            if (HopperName.equals(hopper.getCustomName())) {
                player.sendMessage(prefix + color("&e你破坏了一个区块漏斗"));
                data.set(hopper.getWorld().getName()+hopper.getChunk().getX()+"x"+hopper.getChunk().getZ(),null);
                try {
                    data.save(dataYml);
                } catch (IOException ignore) {}
            }
        }

    }

    /**
     * 指令输入
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("ch") && args.length==1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("ch.admin")) {
                    try {
                        countData.save(countYml);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    loadConfig();
                    sender.sendMessage(prefix + color("&a重载成功"));
                    return true;
                } else {
                    sender.sendMessage(prefix + color("&c没有权限"));
                    return true;
                }
            }
        }


        if (!(sender instanceof Player)) {
            print("&c该指令不能在控制台输入");
            return true;
        }

        //Player player = (Player) sender;

        sender.sendMessage("未知指令");
        return true;
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        // 加载config
        if (!new File(getDataFolder() + File.separator + "config.yml").exists()) {
            saveDefaultConfig();
        }
        reloadConfig();

        lock = new ArrayList<>();

        // 加载数据
        dataYml = new File(this.getDataFolder(), "data.yml");
        if (!dataYml.exists()) {
            this.saveResource("data.yml", true);
        }
        data = YamlConfiguration.loadConfiguration(dataYml);

        countYml = new File(this.getDataFolder(), "count.yml");
        if (!countYml.exists()) {
            this.saveResource("count.yml", true);
        }
        countData = YamlConfiguration.loadConfiguration(countYml);

        enable = getConfig().getBoolean("enable",true);
        delay = getConfig().getInt("delay",20);
        HopperName = getConfig().getString("name","区块漏斗");
        prefix = color(getConfig().getString("prefix", "&b[区块漏斗]"));
    }

    /**
     * 文本颜色
     */
    private static String color(String text) {
        return text.replace("&","§");
    }

    /**
     * 后台信息
     */
    public void print(String msg) {
        Bukkit.getConsoleSender().sendMessage(color(msg));
    }
}
