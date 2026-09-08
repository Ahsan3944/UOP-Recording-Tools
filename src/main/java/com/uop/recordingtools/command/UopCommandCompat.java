package com.uop.recordingtools.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Normalizes multi-token command forms before delegating to the main command.
 * Also supplies contextual completions for command forms whose syntax depends
 * on the selected scope/mode.
 */
public final class UopCommandCompat implements CommandExecutor, TabCompleter {
    private static final List<String> ARMOR_MODES = List.of("plain", "enchanted", "max");
    private static final List<String> WEAPONS = List.of("sword", "mace", "axe", "bow", "crossbow", "trident", "pickaxe", "shovel", "hoe");
    private static final List<String> ARMOR_SLOTS = List.of("helmet", "chestplate", "leggings", "boots", "head", "chest", "legs", "feet");
    private static final List<String> HAND_SLOTS = List.of("mainhand", "offhand");
    private static final List<String> TAG_COLORS = List.of("black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white", "#RRGGBB");

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
        List<String> corrected = contextual(args);
        if (corrected != null) return filter(corrected, args[args.length - 1]);
        return delegate.onTabComplete(sender, command, alias, args);
    }

    private List<String> contextual(String[] a) {
        if (a.length < 2) return null;
        String root = a[0].toLowerCase(Locale.ROOT);

        if (root.equals("armor")) return armorTab(a);
        if (root.equals("enchant") || root.equals("disenchant")) return enchantTab(a, root.equals("disenchant"));
        if (root.equals("name") && a.length >= 4 && a[1].equalsIgnoreCase("tag")) return tagTab(a);
        return null;
    }

    private List<String> armorTab(String[] a) {
        if (a.length < 3) return null;
        String action = a[1].toLowerCase(Locale.ROOT);

        // /uop armor weapon set <targets> <mode> [level] <weapons...>
        if (action.equals("weapon")) {
            if (a.length == 3) return List.of("set");
            if (a.length == 4 && a[2].equalsIgnoreCase("set")) return players();
            if (a.length == 5 && a[2].equalsIgnoreCase("set")) return ARMOR_MODES;
            if (a.length == 6 && a[2].equalsIgnoreCase("set") && a[4].equalsIgnoreCase("enchanted")) return levels10();
            if ((a.length == 6 && a[2].equalsIgnoreCase("set") && !a[4].equalsIgnoreCase("enchanted"))
                    || (a.length >= 7 && a[2].equalsIgnoreCase("set"))) return WEAPONS;
            return null;
        }

        return null;
    }

    private List<String> enchantTab(String[] a, boolean remove) {
        if (a.length < 2) return null;
        String scope = a[1].toLowerCase(Locale.ROOT);
        boolean slotScope = scope.equals("armor") || scope.equals("hand");

        if (a.length == 2) return List.of("all", "armor", "equipment", "mainhand", "offhand", "inventory", "hand");
        if (a.length == 3) return players();

        if (slotScope) {
            if (a.length == 4) return scope.equals("armor") ? ARMOR_SLOTS : HAND_SLOTS;
            if (a.length == 5) return enchantments();
            if (!remove && a.length == 6) return levels255();
            return List.of();
        }

        if (a.length == 4) return enchantments();
        if (!remove && a.length == 5) return levels255();
        return List.of();
    }

    private List<String> tagTab(String[] a) {
        if (a.length == 5 && (a[2].equalsIgnoreCase("create") || a[2].equalsIgnoreCase("edit"))) return TAG_COLORS;
        if (a.length == 6 && (a[2].equalsIgnoreCase("create") || a[2].equalsIgnoreCase("edit"))) return List.of("true", "false");
        if (a.length == 7 && (a[2].equalsIgnoreCase("create") || a[2].equalsIgnoreCase("edit"))) return List.of("true", "false");
        if (a.length == 4 && (a[2].equalsIgnoreCase("give") || a[2].equalsIgnoreCase("change") || a[2].equalsIgnoreCase("remove"))) return players();
        if (a.length == 4 && a[2].equalsIgnoreCase("delete")) return List.of();
        return null;
    }

    private List<String> players() {
        List<String> out = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        out.addAll(List.of("@a", "@p", "@r", "@s"));
        return out;
    }

    private List<String> enchantments() {
        List<String> out = new ArrayList<>();
        for (Enchantment enchantment : Enchantment.values()) out.add(enchantment.getKey().getKey());
        return out;
    }

    private List<String> levels10() {
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= 10; i++) out.add(String.valueOf(i));
        return out;
    }

    private List<String> levels255() {
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= 255; i++) out.add(String.valueOf(i));
        return out;
    }

    private List<String> filter(List<String> values, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(x -> x.toLowerCase(Locale.ROOT).startsWith(p)).distinct().sorted().toList();
    }

    private String[] normalize(String[] args) {
        if (args.length < 2 || !"armor".equalsIgnoreCase(args[0])) return args;

        if ("set".equalsIgnoreCase(args[1]) && args.length >= 7 && "enchanted".equalsIgnoreCase(args[4])) {
            return merge(args, 4, 5);
        }
        if ("give".equalsIgnoreCase(args[1]) && args.length >= 8
                && "direct".equalsIgnoreCase(args[3]) && "enchanted".equalsIgnoreCase(args[5])) {
            return merge(args, 5, 6);
        }
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
