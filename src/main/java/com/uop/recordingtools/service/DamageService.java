package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class DamageService {
    private final DataStore store;

    public DamageService(JavaPlugin p, DataStore s) { store = s; }

    public void global(double v) { store.set("damage.global", clamp(v)); store.saveNow(); }
    public void outgoing(Player p, double v) { store.set("damage.outgoing." + p.getUniqueId(), clamp(v)); store.saveNow(); }
    public void incoming(Player p, double v) { store.set("damage.incoming." + p.getUniqueId(), clamp(v)); store.saveNow(); }
    public void pair(Player a, Player b, double v) {
        store.set("damage.pairs." + a.getUniqueId() + "." + b.getUniqueId(), clamp(v));
        store.saveNow();
    }

    /**
     * Priority is exact pair > outgoing > incoming > global.
     * Rules are selected, not multiplied together.
     */
    public double multiplier(Entity damager, Entity victim) {
        if (damager instanceof Player a && victim instanceof Player v) {
            String pair = "damage.pairs." + a.getUniqueId() + "." + v.getUniqueId();
            if (store.data().contains(pair)) return clamp(store.data().getDouble(pair));

            String out = "damage.outgoing." + a.getUniqueId();
            if (store.data().contains(out)) return clamp(store.data().getDouble(out));

            String in = "damage.incoming." + v.getUniqueId();
            if (store.data().contains(in)) return clamp(store.data().getDouble(in));
        } else if (damager instanceof Player a) {
            String out = "damage.outgoing." + a.getUniqueId();
            if (store.data().contains(out)) return clamp(store.data().getDouble(out));
        } else if (victim instanceof Player v) {
            String in = "damage.incoming." + v.getUniqueId();
            if (store.data().contains(in)) return clamp(store.data().getDouble(in));
        }

        return clamp(store.data().getDouble("damage.global", 1.0));
    }

    public void reset(String type, Player a, Player b) {
        switch (type.toLowerCase()) {
            case "global" -> store.remove("damage.global");
            case "outgoing" -> store.remove("damage.outgoing." + a.getUniqueId());
            case "incoming" -> store.remove("damage.incoming." + a.getUniqueId());
            case "pair" -> store.remove("damage.pairs." + a.getUniqueId() + "." + b.getUniqueId());
            case "all" -> store.remove("damage");
            default -> throw new IllegalArgumentException("Unknown damage reset type: " + type);
        }
        store.saveNow();
    }

    public void resetAll() { store.remove("damage"); store.saveNow(); }

    private double clamp(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return 1.0;
        return Math.max(0, Math.min(10, x));
    }
}
