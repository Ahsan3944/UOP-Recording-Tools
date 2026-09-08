package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/** Persistent recording-position snapshots. A checkpoint stores one position per eligible player. */
public final class CheckpointService {
    private final JavaPlugin plugin;
    private final DataStore store;

    public CheckpointService(JavaPlugin p, DataStore s) { plugin = p; store = s; }

    public SaveResult save(String name, List<Player> requestedPlayers) {
        String normalized = validateName(name);
        Collection<Player> candidates = requestedPlayers == null || requestedPlayers.isEmpty()
                ? plugin.getServer().getOnlinePlayers() : requestedPlayers;

        Map<String, Map<String, Object>> snapshots = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();
        for (Player player : candidates) {
            if (!isEligible(player)) {
                skipped.add(player.getName());
                continue;
            }
            Location l = player.getLocation();
            World world = l.getWorld();
            if (world == null) {
                skipped.add(player.getName());
                continue;
            }
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("uuid", player.getUniqueId().toString());
            snapshot.put("name", player.getName());
            snapshot.put("world", world.getName());
            snapshot.put("x", l.getX());
            snapshot.put("y", l.getY());
            snapshot.put("z", l.getZ());
            snapshot.put("yaw", (double) l.getYaw());
            snapshot.put("pitch", (double) l.getPitch());
            snapshots.put(player.getUniqueId().toString(), snapshot);
        }

        if (snapshots.isEmpty()) return SaveResult.failure("No eligible Survival/Creative players were found to save.");

        String base = "checkpoints." + normalized;
        store.set(base + ".createdAt", System.currentTimeMillis());
        store.set(base + ".players", new ArrayList<>(snapshots.values()));
        store.saveNow();
        return SaveResult.success(snapshots.size(), skipped);
    }

    public RestoreResult tp(String name) {
        String normalized = validateName(name);
        String base = "checkpoints." + normalized;
        if (!store.data().contains(base)) return RestoreResult.missing();

        List<Map<?, ?>> snapshots = store.data().getMapList(base + ".players");
        int teleported = 0, offline = 0, missingWorlds = 0;
        for (Map<?, ?> snapshot : snapshots) {
            UUID uuid;
            try { uuid = UUID.fromString(String.valueOf(snapshot.get("uuid"))); }
            catch (Exception e) { offline++; continue; }

            Player player = plugin.getServer().getPlayer(uuid);
            if (player == null) { offline++; continue; }

            World world = plugin.getServer().getWorld(String.valueOf(snapshot.get("world")));
            if (world == null) { missingWorlds++; continue; }

            try {
                double x = number(snapshot.get("x")), y = number(snapshot.get("y")), z = number(snapshot.get("z"));
                float yaw = (float) number(snapshot.get("yaw")), pitch = (float) number(snapshot.get("pitch"));
                if (player.teleport(new Location(world, x, y, z, yaw, pitch))) teleported++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to restore checkpoint '" + normalized + "' for " + player.getName() + ": " + e.getMessage());
            }
        }
        return new RestoreResult(true, snapshots.size(), teleported, offline, missingWorlds);
    }

    public Set<String> list() {
        var section = store.data().getConfigurationSection("checkpoints");
        return section == null ? Set.of() : new TreeSet<>(section.getKeys(false));
    }

    public boolean delete(String name) {
        String normalized = validateName(name);
        String base = "checkpoints." + normalized;
        if (!store.data().contains(base)) return false;
        store.remove(base);
        store.saveNow();
        return true;
    }

    public int clear() {
        int count = list().size();
        if (count > 0) {
            store.remove("checkpoints");
            store.saveNow();
        }
        return count;
    }

    private boolean isEligible(Player player) {
        if (player == null) return false;
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.CREATIVE;
    }

    private double number(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(value));
    }

    private String validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 64 || !name.matches("[A-Za-z0-9_-]+"))
            throw new IllegalArgumentException("Invalid checkpoint name.");
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public record SaveResult(boolean success, int playerCount, List<String> skippedPlayers, String error) {
        public static SaveResult success(int count, List<String> skipped) { return new SaveResult(true, count, skipped, null); }
        public static SaveResult failure(String error) { return new SaveResult(false, 0, List.of(), error); }
    }

    public record RestoreResult(boolean found, int savedPlayers, int teleportedPlayers, int offlinePlayers, int missingWorlds) {
        public static RestoreResult missing() { return new RestoreResult(false, 0, 0, 0, 0); }
    }
}
