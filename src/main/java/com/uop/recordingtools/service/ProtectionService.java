package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import org.bukkit.entity.Player;
import java.net.InetAddress;
import java.util.*;

public final class ProtectionService {
    private final DataStore store;
    public ProtectionService(org.bukkit.plugin.java.JavaPlugin p, DataStore s) { store = s; }

    private String b(String name) { return "protection." + name.toLowerCase(Locale.ROOT); }

    public void add(Player p, String mode) {
        mode = normalizeMode(mode);
        String base = b(p.getName());
        store.set(base + ".name", p.getName());
        store.set(base + ".uuid", p.getUniqueId().toString());
        store.set(base + ".ip", ip(p));
        store.set(base + ".mode", mode);
        store.saveNow();
    }

    public void remove(Player p) { store.remove(b(p.getName())); store.saveNow(); }

    public Set<String> list() {
        var s = store.data().getConfigurationSection("protection");
        return s == null ? Set.of() : s.getKeys(false);
    }

    public void reset() { store.remove("protection"); store.saveNow(); }

    public boolean allowed(String name, UUID uuid, InetAddress addr) {
        String base = b(name);
        if (!store.data().contains(base)) return true;

        String expectedUuid = store.data().getString(base + ".uuid", "");
        String expectedIp = store.data().getString(base + ".ip", "");
        String mode = normalizeMode(store.data().getString(base + ".mode", "both"));
        boolean uuidMatches = uuid != null && uuid.toString().equalsIgnoreCase(expectedUuid);
        boolean ipMatches = !expectedIp.isBlank() && Objects.equals(ip(addr), expectedIp);

        return switch (mode) {
            case "uuid" -> uuidMatches;
            case "ip" -> ipMatches;
            default -> uuidMatches && ipMatches;
        };
    }

    private String normalizeMode(String mode) {
        if (mode == null) return "both";
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "uuid", "ip", "both" -> mode.toLowerCase(Locale.ROOT);
            default -> throw new IllegalArgumentException("Protection mode must be uuid, ip, or both.");
        };
    }

    private String ip(Player p) { return p.getAddress() == null ? "" : ip(p.getAddress().getAddress()); }
    private String ip(InetAddress a) { return a == null ? "" : a.getHostAddress(); }
}
