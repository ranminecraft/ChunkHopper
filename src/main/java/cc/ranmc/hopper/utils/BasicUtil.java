package cc.ranmc.hopper.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

public class BasicUtil {

    /**
     * 判断背包是否已满
     */
    public static boolean isInventoryFull(Inventory inventory) {
        for (int i = 0; i < 5; i++) {
            if (inventory.getItem(i) == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 文本颜色
     */
    public static String color(String text) {
        return text.replace("&","§");
    }

    /**
     * 后台信息
     */
    public static void print(String msg) {
        Bukkit.getConsoleSender().sendMessage(color(msg));
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
}
