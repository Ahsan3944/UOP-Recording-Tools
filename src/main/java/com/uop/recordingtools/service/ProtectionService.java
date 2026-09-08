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
        String address = ip(p);
        if (("ip".equals(mode) || "both".equals(mode)) && address.isBlank())
            throw new IllegalArgumentException("Cannot register IP protection because the player's address is unavailable.");
        String base = b(p.getName());
        store.set(base + ".name", p.getName());
        store.set(base + ".uuid", p.getUniqueId().toString());
        store.set(base + ".ip", address);
        store.set(base + ".mode", mode);
        store.saveNow();
    }

    public void remove(Player p) {
        for (String base : matchingBases(p.getName(), p.getUniqueId())) store.remove(base);
        store.saveNow();
    }

    public Set<String> list() {
        var s = store.data().getConfigurationSection("protection");
        if (s == null) return Set.of();
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String key : s.getKeys(false)) {
            String name = store.data().getString("protection." + key + ".name", key);
            names.add(name);
        }
        return Collections.unmodifiableSet(names);
    }

    public void reset() { store.remove("protection"); store.saveNow(); }

    public boolean allowed(String name, UUID uuid, InetAddress addr) {
        List<String> bases = matchingBases(name, uuid);
        if (bases.isEmpty()) return true;

        for (String base : bases) {
            String expectedUuid = store.data().getString(base + ".uuid", "");
            String expectedIp = store.data().getString(base + ".ip", "");
            String mode = normalizeMode(store.data().getString(base + ".mode", "both"));
            boolean uuidMatches = uuid != null && uuid.toString().equalsIgnoreCase(expectedUuid);
            boolean ipMatches = !expectedIp.isBlank() && Objects.equals(ip(addr), expectedIp);
            if (switch (mode) {
                case "uuid" -> uuidMatches;
                case "ip" -> ipMatches;
                default -> uuidMatches && ipMatches;
            }) return true;
        }
        return false;
    }

    private List<String> matchingBases(String name, UUID uuid) {
        List<String> matches = new ArrayList<>();
        String direct = b(name);
        if (store.data().contains(direct)) matches.add(direct);
        var section = store.data().getConfigurationSection("protection");
        if (section == null) return matches;
        String uuidText = uuid == null ? "" : uuid.toString();
        for (String key : section.getKeys(false)) {
            String base = "protection." + key;
            if (base.equalsIgnoreCase(direct) || matches.contains(base)) continue;
            String storedUuid = store.data().getString(base + ".uuid", "");
            String storedName = store.data().getString(base + ".name", key);
            if ((!uuidText.isBlank() && uuidText.equalsIgnoreCase(storedUuid))
                    || storedName.equalsIgnoreCase(name)) matches.add(base);
        }
        return matches;
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
