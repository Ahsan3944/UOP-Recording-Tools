package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

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

    /** Priority: exact pair > outgoing > incoming > global. Rules are selected, not multiplied. */
    public double multiplier(Entity damager, Entity victim) {
        Player attacker = resolvePlayerAttacker(damager);
        if (attacker != null && victim instanceof Player target) {
            String pair = "damage.pairs." + attacker.getUniqueId() + "." + target.getUniqueId();
            if (store.data().contains(pair)) return storedMultiplier(pair);

            String out = "damage.outgoing." + attacker.getUniqueId();
            if (store.data().contains(out)) return storedMultiplier(out);

            String in = "damage.incoming." + target.getUniqueId();
            if (store.data().contains(in)) return storedMultiplier(in);
        } else if (attacker != null) {
            String out = "damage.outgoing." + attacker.getUniqueId();
            if (store.data().contains(out)) return storedMultiplier(out);
        } else if (victim instanceof Player target) {
            String in = "damage.incoming." + target.getUniqueId();
            if (store.data().contains(in)) return storedMultiplier(in);
        }

        return storedMultiplier("damage.global", 1.0);
    }

    private Player resolvePlayerAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    public void reset(String type, Player a, Player b) {
        String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "global" -> store.remove("damage.global");
            case "outgoing" -> {
                if (a == null) throw new IllegalArgumentException("Player is required for outgoing reset.");
                store.remove("damage.outgoing." + a.getUniqueId());
            }
            case "incoming" -> {
                if (a == null) throw new IllegalArgumentException("Player is required for incoming reset.");
                store.remove("damage.incoming." + a.getUniqueId());
            }
            case "pair" -> {
                if (a == null || b == null) throw new IllegalArgumentException("Two players are required for pair reset.");
                store.remove("damage.pairs." + a.getUniqueId() + "." + b.getUniqueId());
            }
            case "all" -> store.remove("damage");
            default -> throw new IllegalArgumentException("Unknown damage reset type: " + type);
        }
        store.saveNow();
    }

    public void resetAll() { store.remove("damage"); store.saveNow(); }

    private double storedMultiplier(String path) { return storedMultiplier(path, 1.0); }

    private double storedMultiplier(String path, double fallback) {
        return clamp(store.data().getDouble(path, fallback));
    }

    private double clamp(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return 1.0;
        return Math.max(0.0, Math.min(10.0, x));
    }
}
