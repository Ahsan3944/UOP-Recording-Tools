package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class InventoryService {
    private final JavaPlugin plugin;
    private final DataStore store;
    private final Set<UUID> recording = new HashSet<>();

    public InventoryService(JavaPlugin p, DataStore s) { plugin = p; store = s; }

    public void set(Player p, int slot, ItemStack item) {
        validateSlot(slot);
        record(p);
        p.getInventory().setItem(slot, item);
        record(p);
    }

    public void remove(Player p, int slot) { set(p, slot, new ItemStack(Material.AIR)); }

    public void clear(Player p) {
        record(p);
        p.getInventory().clear();
        p.getInventory().setArmorContents(new ItemStack[4]);
        p.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        record(p);
    }

    public void give(Player p, Material m, int amount) {
        if (m == null || m.isAir()) throw new IllegalArgumentException("Invalid item.");
        validateAmount(amount);
        record(p);
        p.getInventory().addItem(new ItemStack(m, amount));
        record(p);
    }

    public void take(Player p, Material m, int amount) {
        if (m == null || m.isAir()) throw new IllegalArgumentException("Invalid item.");
        validateAmount(amount);
        record(p);
        int left = amount;
        for (int i = 0; i < 41 && left > 0; i++) {
            ItemStack x = p.getInventory().getItem(i);
            if (x != null && x.getType() == m) {
                int n = Math.min(left, x.getAmount());
                x.setAmount(x.getAmount() - n);
                left -= n;
                if (x.getAmount() <= 0) p.getInventory().setItem(i, new ItemStack(Material.AIR));
            }
        }
        record(p);
    }

    public void swap(Player a, int s1, Player b, int s2) {
        validateSlot(s1); validateSlot(s2);
        record(a); record(b);
        ItemStack x = a.getInventory().getItem(s1), y = b.getInventory().getItem(s2);
        a.getInventory().setItem(s1, y); b.getInventory().setItem(s2, x);
        record(a); record(b);
    }

    public void backup(Player p, String name) {
        requireName(name);
        store.set("inventories." + p.getUniqueId() + ".backups." + name, snapshot(p));
        store.saveNow();
    }

    public boolean restore(Player p, String name) {
        requireName(name);
        Object o = store.get("inventories." + p.getUniqueId() + ".backups." + name);
        if (!(o instanceof List<?> l)) return false;
        restoreSnapshot(p, l);
        return true;
    }

    public Set<String> backups(Player p) {
        var s = store.data().getConfigurationSection("inventories." + p.getUniqueId() + ".backups");
        return s == null ? Set.of() : s.getKeys(false);
    }

    public void backupDelete(Player p, String n) {
        requireName(n);
        store.remove("inventories." + p.getUniqueId() + ".backups." + n);
        store.saveNow();
    }

    public void lock(Player p, boolean yes) {
        store.set("inventories." + p.getUniqueId() + ".locked", yes);
        store.saveNow();
    }

    public boolean locked(Player p) { return store.data().getBoolean("inventories." + p.getUniqueId() + ".locked", false); }

    public void startRecord(Collection<Player> ps) {
        recording.addAll(ps.stream().map(Player::getUniqueId).toList());
        for (Player p : ps) record(p);
    }

    public void stopRecord(Collection<Player> ps) { recording.removeAll(ps.stream().map(Player::getUniqueId).toList()); }
    public boolean recording(Player p) { return recording.contains(p.getUniqueId()); }
    public Set<UUID> recording() { return Collections.unmodifiableSet(recording); }

    public void tickHistory() {
        for (UUID id : new ArrayList<>(recording)) {
            Player p = plugin.getServer().getPlayer(id);
            if (p != null) record(p);
        }
    }

    public void record(Player p) {
        if (!recording(p)) return;
        String b = "inventories." + p.getUniqueId() + ".history";
        List<Map<?, ?>> h = store.data().getMapList(b);
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("time", System.currentTimeMillis());
        snap.put("items", snapshot(p));
        h.add(snap);
        int max = Math.max(1, plugin.getConfig().getInt("max-history-snapshots", 600));
        while (h.size() > max) h.remove(0);
        store.set(b, h);
        if (h.size() == 1 || h.size() % 10 == 0) store.save();
    }

    private List<ItemStack> snapshot(Player p) { return Arrays.asList(p.getInventory().getContents()); }

    private void restoreSnapshot(Player p, List<?> l) {
        ItemStack[] a = new ItemStack[41];
        Arrays.fill(a, new ItemStack(Material.AIR));
        for (int i = 0; i < Math.min(41, l.size()); i++) if (l.get(i) instanceof ItemStack x) a[i] = x.clone();
        p.getInventory().setContents(a);
    }

    public boolean back(Player p, String time) {
        long seconds = parse(time);
        if (seconds < 0) throw new IllegalArgumentException("Invalid history time: " + time);
        List<Map<?, ?>> h = store.data().getMapList("inventories." + p.getUniqueId() + ".history");
        if (h.isEmpty()) return false;
        long target = System.currentTimeMillis() - seconds * 1000L;
        Map<?, ?> best = null;
        for (Map<?, ?> m : h) {
            Object t = m.get("time");
            if (t instanceof Number n && n.longValue() <= target) best = m;
        }
        if (best == null) best = h.get(0);
        Object items = best.get("items");
        if (items instanceof List<?> l) { restoreSnapshot(p, l); return true; }
        return false;
    }

    private long parse(String s) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException("History time required.");
        try {
            String x = s.toLowerCase(Locale.ROOT);
            long multiplier = x.endsWith("m") ? 60 : x.endsWith("h") ? 3600 : 1;
            String number = x.endsWith("m") || x.endsWith("h") || x.endsWith("s") ? x.substring(0, x.length() - 1) : x;
            long value = Long.parseLong(number);
            if (value < 0) throw new NumberFormatException();
            return Math.multiplyExact(value, multiplier);
        } catch (Exception e) { throw new IllegalArgumentException("Invalid history time: " + s); }
    }

    private void validateSlot(int slot) { if (slot < 0 || slot > 40) throw new IllegalArgumentException("Inventory slot must be 0-40."); }
    private void validateAmount(int amount) { if (amount < 1 || amount > 99) throw new IllegalArgumentException("Amount must be 1-99."); }
    private void requireName(String name) { if (name == null || name.isBlank() || name.length() > 64) throw new IllegalArgumentException("Invalid backup name."); }
}
