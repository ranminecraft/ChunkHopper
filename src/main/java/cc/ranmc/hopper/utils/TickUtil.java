package cc.ranmc.hopper.utils;

import cc.ranmc.hopper.Main;
import org.bukkit.Bukkit;

import static cc.ranmc.hopper.utils.BasicUtil.isInventoryFull;

public class TickUtil {

    private static final Main plugin = Main.getInstance();

    public static void tickFolia() {
        plugin.getItemListMap().forEach((inventory, list) -> {
            if (inventory.getLocation() != null) {
                Bukkit.getRegionScheduler().runDelayed(plugin, inventory.getLocation(), task -> {
                    if (inventory.getHolder() != null && list != null && !list.isEmpty()) {
                        if (isInventoryFull(inventory)) {
                            inventory.addItem(list.get(0));
                            list.remove(0);
                        }
                    } else {
                        plugin.getItemListMap().remove(inventory);
                    }
                }, 1);
            }
        });
    }

    public static void tick() {
        plugin.getItemListMap().forEach((inventory, list) -> {
            if (inventory != null &&
                    inventory.getHolder() != null &&
                    list != null &&
                    !list.isEmpty()) {
                if (isInventoryFull(inventory)) {
                    inventory.addItem(list.get(0));
                    list.remove(0);
                }
            } else {
                plugin.getItemListMap().remove(inventory);
            }
        });
    }
}
