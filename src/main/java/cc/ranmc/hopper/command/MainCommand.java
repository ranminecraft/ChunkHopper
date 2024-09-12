package cc.ranmc.hopper.command;

import cc.ranmc.hopper.utils.ConfigUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static cc.ranmc.hopper.Main.PREFIX;
import static cc.ranmc.hopper.utils.BaseUtil.color;
import static cc.ranmc.hopper.utils.BaseUtil.print;

public class MainCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command cmd,
                             @NotNull String label,
                             String[] args) {

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("ch.admin")) {
                    ConfigUtil.reload();
                    sender.sendMessage(PREFIX + color("&a重载成功"));
                    return true;
                } else {
                    sender.sendMessage(PREFIX + color("&c没有权限"));
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
}
