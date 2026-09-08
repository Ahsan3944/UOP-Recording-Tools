package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class PermissionService {
    private final DataStore store;
    public PermissionService(JavaPlugin p, DataStore s) { store = s; }
    private String base(Player p) { return "permissions." + p.getUniqueId(); }

    public void grant(Player p, String... nodes) {
        if (p == null) throw new IllegalArgumentException("Player not found.");
        var l = new ArrayList<>(store.data().getStringList(base(p)));
        for (String n : nodes) {
            n = normalize(n);
            if ("all".equalsIgnoreCase(n)) n = "*";
            if (!l.contains(n)) l.add(n);
        }
        store.set(base(p), l); store.saveNow();
    }

    public void revoke(Player p, String... nodes) {
        if (p == null) throw new IllegalArgumentException("Player not found.");
        var l = new ArrayList<>(store.data().getStringList(base(p)));
        for (String n : nodes) l.removeIf(x -> x.equalsIgnoreCase(normalize(n)) || ("all".equalsIgnoreCase(n) && x.equals("*")));
        store.set(base(p), l); store.saveNow();
    }

    public boolean has(Player p, String node) {
        if (p == null || p.isOp()) return p != null && p.isOp();
        node = normalize(node);
        for (String x : store.data().getStringList(base(p))) {
            if (x.equals("*") || x.equalsIgnoreCase(node)) return true;
            if (x.endsWith(".*") && node.startsWith(x.substring(0, x.length() - 1))) return true;
        }
        return false;
    }

    public List<String> list(Player p) { return List.copyOf(store.data().getStringList(base(p))); }

    public void reset(Player p, String node) {
        if (p == null) throw new IllegalArgumentException("Player not found.");
        if (node == null || node.isBlank()) store.remove(base(p));
        else {
            var l = new ArrayList<>(list(p));
            String n = normalize(node);
            l.removeIf(x -> x.equalsIgnoreCase(n));
            store.set(base(p), l);
        }
        store.saveNow();
    }

    private String normalize(String node) {
        if (node == null || node.isBlank()) throw new IllegalArgumentException("Permission node required.");
        return node.toLowerCase(Locale.ROOT).trim();
    }
}
