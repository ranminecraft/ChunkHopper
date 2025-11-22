package cc.ranmc.hopper;

import cc.ranmc.hopper.command.MainCommand;
import cc.ranmc.hopper.listener.MainListener;
import cc.ranmc.hopper.utils.BasicUtil;
import cc.ranmc.hopper.utils.ConfigUtil;
import cc.ranmc.hopper.utils.TickUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import lombok.Setter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static cc.ranmc.hopper.utils.BasicUtil.print;
import static cc.ranmc.hopper.utils.TickUtil.tickFolia;

public class Main extends JavaPlugin implements Listener {

    @Getter
    private static Main instance;
    @Setter
    public static String PREFIX;
    @Getter
    @Setter
    private YamlConfiguration dataYml, chunkYml;
    @Getter
    @Setter
    private File dataFile, chunkFile;
    @Getter
    @Setter
    private List<String> lockList = new ArrayList<>();
    @Getter
    @Setter
    private Map<String, Integer> hopperCountMap;
    @Getter
    @Setter
    private Map<String, Integer> redStoneCountMap;
    @Getter
    @Setter
    private boolean enable;
    @Getter
    @Setter
    private int delay;
    private BukkitTask task = null;
    private ScheduledTask foliaTask = null;
    @Getter
    private final Map<Inventory,List<ItemStack>> itemListMap = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        print("&e-----------------------");
        print("&b漏斗箱子 &dBy阿然");
        print("&b插件版本:" + getDescription().getVersion());
        print("&b服务器版本:" + getServer().getVersion());
        print("&cQQ 2263055528");
        print("&e-----------------------");

        ConfigUtil.reload();

        if (BasicUtil.isFolia()) {
            foliaTask = Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> tickFolia(), 20, 20);
        } else {
            task = Bukkit.getScheduler().runTaskTimer(this, TickUtil::tick, 20, 20);
        }

        // 注册监听器
        Bukkit.getPluginManager().registerEvents(new MainListener(), this);

        // 注册指令
        MainCommand command = new MainCommand();
        Objects.requireNonNull(Bukkit.getPluginCommand("ch")).setExecutor(command);
        Objects.requireNonNull(Bukkit.getPluginCommand("chunkhopper")).setExecutor(command);

        // BStats
        new Metrics(this, 28105);

        super.onEnable();
    }


    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        if (foliaTask != null) foliaTask.cancel();
        super.onDisable();
    }
}
