package cc.ranmc.hopper.utils;

import cc.ranmc.hopper.Main;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HopperUtil {

    private static final Main plugin = Main.getInstance();

    public static String getKey(Hopper hopper) {
        return hopper.getWorld().getName() + hopper.getChunk().getX() + "x" + hopper.getChunk().getZ();
    }

    public static String getKey(Chunk chunk) {
        return chunk.getWorld().getName() + chunk.getX() + "x" + chunk.getZ();
    }

    /**
     * 计算区块漏斗数量
     */
    public static void countBlock(Block block) {
        int hopper = 0;
        int redStone = 0;
        Entity[] entities = block.getChunk().getEntities();
        for (Entity entity : entities) {
            if (entity.getType() == EntityType.MINECART_HOPPER) {
                hopper++;
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int y = block.getWorld().getMinHeight(); y < block.getWorld().getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (block.getChunk().getBlock(x, y, z).getType() == Material.HOPPER) {
                        hopper++;
                    }
                    if (block.getChunk().getBlock(x, y, z).getType() == Material.REDSTONE_WIRE) {
                        redStone++;
                    }
                }
            }
        }
        Main.getInstance().getHopperCountMap().put(getKey(block.getChunk()), hopper);
        Main.getInstance().getRedStoneCountMap().put(getKey(block.getChunk()), redStone);
    }

    /**
     * 储存掉落物
     */
    public static void hopper(Location location) {


        if (plugin.isFolia()) {
            Bukkit.getServer().getRegionScheduler().runDelayed(plugin, location,scheduledTask -> {
                Chunk chunk = location.getChunk();
                String name = location.getWorld().getName() + chunk.getX() + "x" + chunk.getZ();
                if (plugin.getLockList().contains(name)) {
                    return;
                } else {
                    plugin.getLockList().add(name);
                }
                hopperAddItem(location, chunk, name);
            }, plugin.getDelay());
        } else {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                Chunk chunk = location.getChunk();
                String name = location.getWorld().getName() + chunk.getX() + "x" + chunk.getZ();
                if (plugin.getLockList().contains(name)) {
                    return;
                } else {
                    plugin.getLockList().add(name);
                }
                hopperAddItem(location, chunk, name);
            }, plugin.getDelay());
        }

    }

    /**
     * 添加物品到区块漏斗
     */
    private static void hopperAddItem(Location location, Chunk chunk, String name) {
        plugin.getLockList().remove(name);
        if (!plugin.isEnable() || !chunk.isLoaded() || plugin.getDataYml().getString(name) == null) return;
        Entity[] entities = chunk.getEntities();
        String[] xyz = Objects.requireNonNull(plugin.getDataYml().getString(name)).split("x");
        Block block = location.getWorld().getBlockAt(Integer.parseInt(xyz[0]),
                Integer.parseInt(xyz[1]),
                Integer.parseInt(xyz[2]));
        if (block.getType() == Material.HOPPER) {
            Hopper hopper = (Hopper) block.getState();
            List<String> itemList = plugin.getChunkYml().getStringList(Objects.requireNonNull(hopper.getCustomName()));
            for (Entity entity : entities) {
                if (entity.getType() == EntityType.DROPPED_ITEM) {
                    Item item = (Item) entity;
                    if (itemList.contains(item.getItemStack().getType().toString())) {
                        Inventory inventory = hopper.getInventory();
                        List<ItemStack> list = plugin.getItemListMap().getOrDefault(inventory, new ArrayList<>());
                        list.add(item.getItemStack());
                        plugin.getItemListMap().put(inventory, list);
                        entity.remove();
                    }
                }
            }
        } else {
            plugin.getDataYml().set(name, null);
            try {
                plugin.getDataYml().save(plugin.getDataFile());
            } catch (IOException ignore) {}
        }
    }
}
