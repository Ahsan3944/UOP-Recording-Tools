package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class CheckpointService {
    private final JavaPlugin plugin;
    private final DataStore store;
    public CheckpointService(JavaPlugin p, DataStore s) { plugin = p; store = s; }

    public void save(String name, List<Player> ps) {
        validateName(name);
        if (ps == null || ps.isEmpty()) throw new IllegalArgumentException("No players selected.");
        Player p = ps.get(0);
        Location l = p.getLocation();
        if (l.getWorld() == null) throw new IllegalStateException("Player world unavailable.");
        String b = "checkpoints." + name;
        store.set(b + ".world", l.getWorld().getName()); store.set(b + ".x", l.getX()); store.set(b + ".y", l.getY());
        store.set(b + ".z", l.getZ()); store.set(b + ".yaw", (double) l.getYaw()); store.set(b + ".pitch", (double) l.getPitch());
        store.set(b + ".players", ps.stream().map(x -> x.getUniqueId().toString()).toList()); store.saveNow();
    }

    public boolean tp(String name, List<Player> ps) {
        validateName(name);
        String b = "checkpoints." + name;
        if (!store.data().contains(b) || ps == null || ps.isEmpty()) return false;
        World w = plugin.getServer().getWorld(store.data().getString(b + ".world"));
        if (w == null) return false;
        Location l = new Location(w, store.data().getDouble(b + ".x"), store.data().getDouble(b + ".y"), store.data().getDouble(b + ".z"),
                (float) store.data().getDouble(b + ".yaw"), (float) store.data().getDouble(b + ".pitch"));
        for (Player p : ps) p.teleport(l);
        return true;
    }

    public Set<String> list() { var s = store.data().getConfigurationSection("checkpoints"); return s == null ? Set.of() : s.getKeys(false); }
    public void delete(String n) { validateName(n); store.remove("checkpoints." + n); store.saveNow(); }
    public void clear() { store.remove("checkpoints"); store.saveNow(); }
    private void validateName(String n) { if (n == null || n.isBlank() || n.length() > 64 || !n.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Invalid checkpoint name."); }
}
