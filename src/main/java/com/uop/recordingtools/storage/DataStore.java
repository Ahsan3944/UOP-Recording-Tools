package com.uop.recordingtools.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class DataStore {
    private final JavaPlugin plugin;
    private final File dir;
    private YamlConfiguration data;

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "data");
    }

    public synchronized void load() {
        dir.mkdirs();
        File f = new File(dir, "data.yml");
        data = YamlConfiguration.loadConfiguration(f);
    }

    public synchronized void save() {
        if (data == null) return;
        File target = new File(dir, "data.yml");
        File temp = new File(dir, "data.yml.tmp");
        try {
            data.save(temp);
            try {
                Files.move(temp.toPath(), target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data: " + e.getMessage());
            if (temp.exists() && !temp.delete()) {
                plugin.getLogger().warning("Could not remove temporary data file: " + temp.getName());
            }
        }
    }

    public synchronized YamlConfiguration data() { return data; }
    public synchronized void set(String path, Object value) { data.set(path, value); }
    public synchronized Object get(String path) { return data.get(path); }
    public synchronized void remove(String path) { data.set(path, null); }
    public synchronized void saveNow() { save(); }
}
