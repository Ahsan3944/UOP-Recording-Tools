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
            if ("all".equals(n)) n = "*";
            if (!l.contains(n)) l.add(n);
        }
        store.set(base(p), l);
        store.saveNow();
    }

    public void revoke(Player p, String... nodes) {
        if (p == null) throw new IllegalArgumentException("Player not found.");
        var l = new ArrayList<>(store.data().getStringList(base(p)));
        boolean changed = false;
        for (String raw : nodes) {
            String n = normalize(raw);
            if ("all".equals(n)) {
                changed |= !l.isEmpty();
                l.clear();
            } else if ("*".equals(n)) {
                changed |= l.removeIf(x -> normalizeStored(x).equals("*"));
            } else {
                changed |= l.removeIf(x -> normalizeStored(x).equals(n));
            }
        }
        if (l.isEmpty()) store.remove(base(p));
        else store.set(base(p), l);
        if (changed) store.saveNow();
    }

    public boolean has(Player p, String node) {
        if (p == null) return false;
        if (p.isOp()) return true;
        node = normalize(node);
        for (String raw : store.data().getStringList(base(p))) {
            String x = normalizeStored(raw);
            if (x.equals("*") || x.equals(node)) return true;
            if (x.endsWith(".*") && node.startsWith(x.substring(0, x.length() - 1))) return true;
        }
        return false;
    }

    public List<String> list(Player p) {
        if (p == null) throw new IllegalArgumentException("Player not found.");
        return List.copyOf(store.data().getStringList(base(p)));
    }

    public void reset(Player p, String node) {
        if (p == null) throw new IllegalArgumentException("Player not found.");
        if (node == null || node.isBlank() || "all".equalsIgnoreCase(node)) store.remove(base(p));
        else {
            var l = new ArrayList<>(list(p));
            String n = normalize(node);
            l.removeIf(x -> normalizeStored(x).equals(n));
            if (l.isEmpty()) store.remove(base(p));
            else store.set(base(p), l);
        }
        store.saveNow();
    }

    private String normalize(String node) {
        if (node == null || node.isBlank()) throw new IllegalArgumentException("Permission node required.");
        return node.trim().toLowerCase(Locale.ROOT).replace(' ', '.');
    }

    private String normalizeStored(String node) {
        if (node == null || node.isBlank()) return "";
        return node.trim().toLowerCase(Locale.ROOT).replace(' ', '.');
    }
}
