/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 */
package me.Mixer.bluemapresidence;

import java.util.List;
import me.Mixer.bluemapresidence.Main;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand
implements CommandExecutor,
TabCompleter {
    static Main cfg;

    public ReloadCommand(Main instance) {
        cfg = instance;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender == null) {
            ReloadCommand.$$$reportNull$$$0(0);
        }
        if (command == null) {
            ReloadCommand.$$$reportNull$$$0(1);
        }
        if (label == null) {
            ReloadCommand.$$$reportNull$$$0(2);
        }
        if (args == null) {
            ReloadCommand.$$$reportNull$$$0(3);
        }
        if (sender instanceof Player && !sender.hasPermission("blueres.reload")) {
            sender.sendMessage(cfg.Placeholder((Player)sender, ChatColor.translateAlternateColorCodes((char)'&', (String)cfg.getConfig().getString("messages.no_permissions", "No permissions!"))));
            return true;
        }
        cfg.reloadConfig();
        cfg.refreshPl();
        sender.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)cfg.getConfig().getString("messages.reload_successfully", "Reloaded successfully.")));
        return false;
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender == null) {
            ReloadCommand.$$$reportNull$$$0(4);
        }
        if (command == null) {
            ReloadCommand.$$$reportNull$$$0(5);
        }
        if (label == null) {
            ReloadCommand.$$$reportNull$$$0(6);
        }
        if (args == null) {
            ReloadCommand.$$$reportNull$$$0(7);
        }
        return null;
    }

    private static /* synthetic */ void $$$reportNull$$$0(int n) {
        Object[] objectArray;
        Object[] objectArray2;
        Object[] objectArray3 = new Object[3];
        switch (n) {
            default: {
                objectArray2 = objectArray3;
                objectArray3[0] = "sender";
                break;
            }
            case 1: 
            case 5: {
                objectArray2 = objectArray3;
                objectArray3[0] = "command";
                break;
            }
            case 2: 
            case 6: {
                objectArray2 = objectArray3;
                objectArray3[0] = "label";
                break;
            }
            case 3: 
            case 7: {
                objectArray2 = objectArray3;
                objectArray3[0] = "args";
                break;
            }
        }
        objectArray2[1] = "me/Mixer/bluemapresidence/ReloadCommand";
        switch (n) {
            default: {
                objectArray = objectArray2;
                objectArray2[2] = "onCommand";
                break;
            }
            case 4: 
            case 5: 
            case 6: 
            case 7: {
                objectArray = objectArray2;
                objectArray2[2] = "onTabComplete";
                break;
            }
        }
        throw new IllegalArgumentException(String.format("Argument for @NotNull parameter '%s' of %s.%s must not be null", objectArray));
    }
}

