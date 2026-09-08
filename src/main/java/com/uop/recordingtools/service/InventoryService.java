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
    private final Map<UUID, List<ItemStack>> lastSnapshots = new HashMap<>();
    private final Map<UUID, Integer> lastSelectedSlots = new HashMap<>();

    public InventoryService(JavaPlugin p, DataStore s) { plugin = p; store = s; }

    public void resetRuntime() {
        recording.clear();
        lastSnapshots.clear();
        lastSelectedSlots.clear();
    }

    public void set(Player p, int slot, ItemStack item) {
        validateSlot(slot);
        if (item == null) throw new IllegalArgumentException("Item cannot be null.");
        if (!item.getType().isAir()) validateAmount(item.getAmount());
        record(p);
        p.getInventory().setItem(slot, item.clone());
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
        a.getInventory().setItem(s1, y == null ? null : y.clone());
        b.getInventory().setItem(s2, x == null ? null : x.clone());
        record(a); record(b);
    }

    public void backup(Player p, String name) {
        requireName(name);
        Map<String, Object> backup = new LinkedHashMap<>();
        backup.put("selected-slot", p.getInventory().getHeldItemSlot());
        backup.put("items", snapshot(p));
        store.set("inventories." + p.getUniqueId() + ".backups." + name, backup);
        store.saveNow();
    }

    public boolean restore(Player p, String name) {
        requireName(name);
        Object o = store.get("inventories." + p.getUniqueId() + ".backups." + name);
        if (o instanceof Map<?, ?> backup) {
            Object items = backup.get("items");
            if (!(items instanceof List<?> l)) return false;
            int selected = p.getInventory().getHeldItemSlot();
            Object selectedValue = backup.get("selected-slot");
            if (selectedValue instanceof Number n) selected = n.intValue();
            if (selected < 0 || selected > 8) return false;
            record(p);
            restoreSnapshot(p, l);
            p.getInventory().setHeldItemSlot(selected);
            record(p);
            store.saveNow();
            return true;
        }
        if (o instanceof List<?> l) {
            record(p);
            restoreSnapshot(p, l);
            record(p);
            store.saveNow();
            return true;
        }
        return false;
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
        for (Player p : ps) {
            lastSnapshots.remove(p.getUniqueId());
            lastSelectedSlots.remove(p.getUniqueId());
            String historyPath = "inventories." + p.getUniqueId() + ".history";
            if (!store.data().contains(historyPath)) store.set(historyPath, new ArrayList<>());
            record(p);
        }
        store.saveNow();
    }

    public void stopRecord(Collection<Player> ps) {
        recording.removeAll(ps.stream().map(Player::getUniqueId).toList());
        for (Player p : ps) {
            lastSnapshots.remove(p.getUniqueId());
            lastSelectedSlots.remove(p.getUniqueId());
        }
        store.saveNow();
    }

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
        UUID id = p.getUniqueId();
        String b = "inventories." + id + ".history";
        if (!store.data().contains(b)) {
            recording.remove(id);
            lastSnapshots.remove(id);
            lastSelectedSlots.remove(id);
            return;
        }
        List<ItemStack> current = snapshot(p);
        int selected = p.getInventory().getHeldItemSlot();
        List<ItemStack> previous = lastSnapshots.get(id);
        Integer previousSelected = lastSelectedSlots.get(id);
        if (previous != null && previousSelected != null && inventoryEquals(previous, current)
                && previousSelected == selected) return;

        List<Map<?, ?>> h = store.data().getMapList(b);
        Map<String, Object> snap = new LinkedHashMap<>();
        long now = System.currentTimeMillis();
        snap.put("time", now);
        snap.put("selected-slot", selected);
        snap.put("items", current);
        h.add(snap);
        lastSnapshots.put(id, cloneSnapshot(current));
        lastSelectedSlots.put(id, selected);

        long windowSeconds = plugin.getConfig().getLong("history-window-seconds", 300L);
        if (windowSeconds > 0) {
            long cutoff;
            try {
                cutoff = Math.subtractExact(now, Math.multiplyExact(windowSeconds, 1000L));
            } catch (ArithmeticException e) {
                cutoff = Long.MIN_VALUE;
            }
            final long historyCutoff = cutoff;
            h.removeIf(entry -> {
                Object value = entry.get("time");
                return value instanceof Number n && n.longValue() < historyCutoff;
            });
        }

        int max = Math.max(1, plugin.getConfig().getInt("max-history-snapshots", 600));
        while (h.size() > max) h.remove(0);
        store.set(b, h);
        if (h.size() == 1 || h.size() % 10 == 0) store.save();
    }

    private List<ItemStack> snapshot(Player p) {
        ItemStack[] contents = p.getInventory().getContents();
        List<ItemStack> copy = new ArrayList<>(contents.length);
        for (ItemStack item : contents) copy.add(item == null ? null : item.clone());
        return copy;
    }

    private List<ItemStack> cloneSnapshot(List<ItemStack> source) {
        List<ItemStack> copy = new ArrayList<>(source.size());
        for (ItemStack item : source) copy.add(item == null ? null : item.clone());
        return copy;
    }

    private boolean inventoryEquals(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            ItemStack x = a.get(i), y = b.get(i);
            if (x == null || y == null) {
                if (x != y) return false;
            } else if (!x.isSimilar(y) || x.getAmount() != y.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private void restoreSnapshot(Player p, List<?> l) {
        ItemStack[] a = new ItemStack[41];
        Arrays.fill(a, new ItemStack(Material.AIR));
        for (int i = 0; i < Math.min(41, l.size()); i++) {
            if (l.get(i) instanceof ItemStack x) a[i] = x.clone();
        }
        p.getInventory().setContents(a);
    }

    public boolean back(Player p, String time) {
        long seconds = parse(time);
        List<Map<?, ?>> h = store.data().getMapList("inventories." + p.getUniqueId() + ".history");
        if (h.isEmpty()) return false;
        long target;
        try {
            target = Math.subtractExact(System.currentTimeMillis(), Math.multiplyExact(seconds, 1000L));
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("History time is too large: " + time);
        }

        Map<?, ?> best = null;
        Map<?, ?> oldest = null;
        long bestTime = Long.MIN_VALUE;
        long oldestTime = Long.MAX_VALUE;
        for (Map<?, ?> m : h) {
            Object t = m.get("time");
            if (!(t instanceof Number n)) continue;
            long timestamp = n.longValue();
            if (timestamp < oldestTime) {
                oldestTime = timestamp;
                oldest = m;
            }
            if (timestamp <= target && timestamp > bestTime) {
                bestTime = timestamp;
                best = m;
            }
        }
        if (best == null) best = oldest;
        if (best == null) return false;
        Object items = best.get("items");
        if (items instanceof List<?> l) {
            record(p);
            restoreSnapshot(p, l);
            Object selected = best.get("selected-slot");
            if (selected instanceof Number n && n.intValue() >= 0 && n.intValue() <= 8) {
                p.getInventory().setHeldItemSlot(n.intValue());
            }
            record(p);
            store.saveNow();
            return true;
        }
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
    private void requireName(String name) {
        if (name == null || name.isBlank() || name.length() > 64 || !name.matches("[A-Za-z0-9._ -]{1,64}"))
            throw new IllegalArgumentException("Invalid backup name. Use letters, numbers, spaces, '.', '_' or '-'.");
    }
}
