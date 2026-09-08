package com.uop.recordingtools.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Normalizes multi-token command forms before delegating to the main command.
 * In particular, "enchanted 5" is treated as one armor mode token.
 */
public final class UopCommandCompat implements CommandExecutor, TabCompleter {
    private final UopCommand delegate;

    public UopCommandCompat(UopCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return delegate.onCommand(sender, command, label, normalize(args));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return delegate.onTabComplete(sender, command, alias, args);
    }

    private String[] normalize(String[] args) {
        if (args.length < 2 || !"armor".equalsIgnoreCase(args[0])) return args;

        // /uop armor set <targets> <material> enchanted <level> [true|false] [weapons...]
        if ("set".equalsIgnoreCase(args[1]) && args.length >= 7 && "enchanted".equalsIgnoreCase(args[4])) {
            return merge(args, 4, 5);
        }

        // /uop armor give <targets> direct <material> enchanted <level> [true|false] [weapons...]
        if ("give".equalsIgnoreCase(args[1]) && args.length >= 8
                && "direct".equalsIgnoreCase(args[3]) && "enchanted".equalsIgnoreCase(args[5])) {
            return merge(args, 5, 6);
        }

        // /uop armor weapon set <targets> enchanted <level> <weapons...>
        if ("weapon".equalsIgnoreCase(args[1]) && args.length >= 7
                && "set".equalsIgnoreCase(args[2]) && "enchanted".equalsIgnoreCase(args[4])) {
            return merge(args, 4, 5);
        }

        return args;
    }

    private String[] merge(String[] args, int first, int second) {
        List<String> out = new ArrayList<>(Arrays.asList(args));
        out.set(first, args[first] + " " + args[second]);
        out.remove(second);
        return out.toArray(String[]::new);
    }
}
