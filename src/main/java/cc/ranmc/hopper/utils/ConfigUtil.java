package cc.ranmc.hopper.utils;

import cc.ranmc.hopper.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import static cc.ranmc.hopper.utils.BaseUtil.color;

public class ConfigUtil {

    private static final Main plugin = Main.getInstance();

    /**
     * 加载配置文件
     */
    public static void reload() {
        if (!new File(plugin.getDataFolder() + File.separator + "config.yml").exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();

        plugin.setLockList(new ArrayList<>());

        // 加载数据
        plugin.setChunkFile(new File(plugin.getDataFolder(), "chunk.yml"));
        if (!plugin.getChunkFile().exists()) {
            plugin.saveResource("chunk.yml", true);
        }
        plugin.setChunkYml(YamlConfiguration.loadConfiguration(plugin.getChunkFile()));

        plugin.setDataFile(new File(plugin.getDataFolder(), "data.yml"));
        if (!plugin.getDataFile().exists()) {
            plugin.saveResource("data.yml", true);
        }
        plugin.setDataYml(YamlConfiguration.loadConfiguration(plugin.getDataFile()));

        plugin.setEnable(plugin.getConfig().getBoolean("enable",true));
        plugin.setDelay(plugin.getConfig().getInt("delay",30));

        plugin.setHopperCountMap(new HashMap<>());
        Main.PREFIX = color(plugin.getConfig().getString("prefix", "&b[区块漏斗]"));
    }
}
