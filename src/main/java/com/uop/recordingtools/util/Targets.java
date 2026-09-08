package com.uop.recordingtools.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.util.*;

public final class Targets {
    private Targets() {}

    public static List<Player> players(CommandSender sender, String... args) {
        LinkedHashMap<UUID, Player> out = new LinkedHashMap<>();
        for (String token : args) {
            if (token == null || token.isBlank()) continue;
            try {
                if (token.equalsIgnoreCase("all")) {
                    for (Player p : Bukkit.getOnlinePlayers()) out.put(p.getUniqueId(), p);
                    continue;
                }
                if (token.startsWith("@")) {
                    for (Entity e : Bukkit.selectEntities(sender, token)) {
                        if (e instanceof Player p && p.isOnline()) out.put(p.getUniqueId(), p);
                    }
                } else {
                    for (String n : token.split(",")) {
                        n = n.trim();
                        if (n.isEmpty()) continue;
                        Player p = Bukkit.getPlayerExact(n);
                        if (p != null && p.isOnline()) out.put(p.getUniqueId(), p);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid selectors simply produce no matches; callers provide the user-facing error.
            }
        }
        return new ArrayList<>(out.values());
    }

    /**
     * Resolves a target for command forms that require exactly one player.
     * Multi-target selectors must not silently resolve to the first match.
     */
    public static Player one(CommandSender sender, String token) {
        List<Player> players = players(sender, token);
        return players.size() == 1 ? players.get(0) : null;
    }
}
