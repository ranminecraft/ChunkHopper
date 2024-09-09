package cc.ranmc.hopper;

import io.papermc.lib.PaperLib;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends JavaPlugin implements Listener {

    private String prefix;
    private YamlConfiguration dataYml, chunkYml;
    private File dataFile, chunkFile;
    private List<String> lock = new ArrayList<>();
    private Map<String, Integer> hopperCount;
    private boolean enable;
    private int delay;
    private final boolean folia = isFolia();
    private BukkitTask task = null;
    private ScheduledTask foliaTask = null;
    private final Map<Inventory,List<ItemStack>> map = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        print("&e-----------------------");
        print("&b漏斗箱子 &dBy阿然");
        print("&b插件版本:" + getDescription().getVersion());
        print("&b服务器版本:" + getServer().getVersion());
        print("&cQQ 2263055528");
        print("&e-----------------------");

        loadConfig();

        if (folia) {
            foliaTask = Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> tickFolia(), 20, 20);
        } else {
            task = Bukkit.getScheduler().runTaskTimer(this, this::tick, 20, 20);
        }

        //注册Event
        Bukkit.getPluginManager().registerEvents(this, this);

        super.onEnable();
    }

    public void tickFolia() {
        map.forEach((inventory, list) -> {
            if (inventory.getLocation() != null) {
                Bukkit.getRegionScheduler().runDelayed(this, inventory.getLocation(), task -> {
                    if (inventory.getHolder() != null && list != null && !list.isEmpty()) {
                        if (isInventoryFull(inventory)) {
                            inventory.addItem(list.get(0));
                            list.remove(0);
                        }
                    } else {
                        map.remove(inventory);
                    }
                }, 1);
            }
        });
    }

    public void tick() {
        map.forEach((inventory, list) -> {
            if (inventory != null &&
                    inventory.getHolder() != null &&
                    list != null &&
                    !list.isEmpty()) {
                if (isInventoryFull(inventory)) {
                    inventory.addItem(list.get(0));
                    list.remove(0);
                }
            } else {
                map.remove(inventory);
            }
        });
    }

    public static boolean isInventoryFull(Inventory inventory) {
        for (int i = 0; i < 5; i++) {
            if (inventory.getItem(i) == null) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        if (foliaTask != null) foliaTask.cancel();
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
        if (!enable || !chunk.isLoaded() || dataYml.getString(name) == null) return;
        Entity[] entities = chunk.getEntities();
        String[] xyz = Objects.requireNonNull(dataYml.getString(name)).split("x");
        Block block = location.getWorld().getBlockAt(Integer.parseInt(xyz[0]),
                Integer.parseInt(xyz[1]),
                Integer.parseInt(xyz[2]));
        if (block.getType() == Material.HOPPER) {
            Hopper hopper = (Hopper) block.getState();
            List<String> itemList = chunkYml.getStringList(hopper.getCustomName());
            for (Entity entity : entities) {
                if (entity.getType() == EntityType.DROPPED_ITEM) {
                    Item item = (Item) entity;
                    if (itemList.contains(item.getItemStack().getType().toString())) {
                        Inventory inventory = hopper.getInventory();
                        List<ItemStack> list = map.getOrDefault(inventory, new ArrayList<>());
                        list.add(item.getItemStack());
                        map.put(inventory, list);
                        entity.remove();
                    }
                }
            }
        } else {
            dataYml.set(name, null);
            try {
                dataYml.save(dataFile);
            } catch (IOException ignore) {}
        }
    }

    @EventHandler
    private void onEntityDeathEvent(EntityDeathEvent event) {
        if (!enable && !getConfig().getBoolean("entity", true)) return;
        hopper(event.getEntity().getLocation());
    }

    @EventHandler
    private void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
        if (!enable && !getConfig().getBoolean("piston", true)) return;
        hopper(event.getBlock().getLocation());
    }

    @EventHandler
    private void onBlockPlaceEvent(BlockPlaceEvent event) {
        if (!enable) return;
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (!event.isCancelled() && block.getType() == Material.HOPPER) {
            String chunkName = block.getChunk().toString();
            if (hopperCount.containsKey(chunkName)) {
                int count = hopperCount.get(chunkName);
                if (count >= getConfig().getInt("limit",32)) {
                    event.setCancelled(true);
                    player.sendMessage(color("&c该区块存在漏斗已达上限\n推荐您使用区块漏斗功能\n详情查看菜单中游戏帮助"));
                    return;
                } else {
                    hopperCount.put(chunkName, hopperCount.get(chunkName) + 1);
                }
            } else {
                player.sendMessage(color("&e该区块计算漏斗中请稍后\n推荐您使用区块漏斗功能\n详情查看菜单中游戏帮助"));
                event.setCancelled(true);
                if (folia) {
                    Bukkit.getRegionScheduler().run(this, block.getLocation(), scheduledTask -> countHopper(block));
                } else {
                    PaperLib.getChunkAtAsync(block.getLocation()).thenAccept(chunk -> countHopper(block));
                }
                return;
            }
            Hopper hopper = (Hopper) block.getState();
            String name = hopper.getWorld().getName()+hopper.getChunk().getX() + "x" + hopper.getChunk().getZ();
            if (hopper.getCustomName() == null) return;
            if (chunkYml.contains(hopper.getCustomName())) {
                if (dataYml.getString(name) != null) {
                    String[] xyz = Objects.requireNonNull(dataYml.getString(name)).split("x");
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
                dataYml.set(name,xyz.toString());
                try {
                    dataYml.save(dataFile);
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
        hopperCount.put(chunkName, count);
    }

    @EventHandler
    private void onBlockBreakEvent(BlockBreakEvent event) {
        if (!enable) return;
        Block block = event.getBlock();
        if (getConfig().getBoolean("block", true)) {
            hopper(block.getLocation());
        }
        Player player = event.getPlayer();
        if (block.getType() == Material.HOPPER) {
            Hopper hopper = (Hopper) block.getState();
            String chunkName = block.getChunk().toString();
            if (hopperCount.containsKey(chunkName)) hopperCount.put(chunkName, hopperCount.get(chunkName) - 1);
            if (hopper.getCustomName() == null) return;
            if (chunkYml.contains(hopper.getCustomName())) {
                player.sendMessage(prefix + color("&e你破坏了一个区块漏斗"));
                dataYml.set(hopper.getWorld().getName()+hopper.getChunk().getX()+"x"+hopper.getChunk().getZ(),null);
                try {
                    dataYml.save(dataFile);
                } catch (IOException ignore) {}
            }

        }
    }

    /**
     * 指令输入
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("ch") && args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("ch.admin")) {
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
        // 加载 Config
        if (!new File(getDataFolder() + File.separator + "config.yml").exists()) {
            saveDefaultConfig();
        }
        reloadConfig();

        lock = new ArrayList<>();

        // 加载数据
        chunkFile = new File(this.getDataFolder(), "chunk.yml");
        if (!chunkFile.exists()) {
            this.saveResource("chunk.yml", true);
        }
        chunkYml = YamlConfiguration.loadConfiguration(chunkFile);

        dataFile = new File(this.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            this.saveResource("data.yml", true);
        }
        dataYml = YamlConfiguration.loadConfiguration(dataFile);

        enable = getConfig().getBoolean("enable",true);
        delay = getConfig().getInt("delay",30);

        hopperCount = new HashMap<>();
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
