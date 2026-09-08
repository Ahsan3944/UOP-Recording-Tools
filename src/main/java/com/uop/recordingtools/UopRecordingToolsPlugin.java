package com.uop.recordingtools;

import com.uop.recordingtools.command.UopCommandCompat;
import com.uop.recordingtools.listener.RecordingListener;
import com.uop.recordingtools.service.*;
import com.uop.recordingtools.storage.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class UopRecordingToolsPlugin extends JavaPlugin {
    private DataStore store;
    private PermissionService permissions;
    private ArmorService armor;
    private CheckpointService checkpoints;
    private DamageService damage;
    private EnchantService enchant;
    private FreezeService freeze;
    private InventoryService inventory;
    private NameService names;
    private ProtectionService protection;
    private int inventoryHistoryTask = -1;

    @Override public void onEnable() {
        saveDefaultConfig();
        store = new DataStore(this); store.load();
        permissions = new PermissionService(this, store);
        armor = new ArmorService(this, store);
        checkpoints = new CheckpointService(this, store);
        damage = new DamageService(this, store);
        enchant = new EnchantService();
        freeze = new FreezeService(this, store, permissions);
        inventory = new InventoryService(this, store);
        names = new NameService(this, store);
        protection = new ProtectionService(this, store);
        UopCommandCompat compat = new UopCommandCompat(this);
        PluginCommand root = getCommand("uop");
        if (root != null) { root.setExecutor(compat); root.setTabCompleter(compat); }
        getServer().getPluginManager().registerEvents(new RecordingListener(this), this);
        scheduleInventoryHistory();
        names.refreshAll();
        getLogger().info("UOP Recording Tools enabled.");
    }

    private void scheduleInventoryHistory() {
        if (inventoryHistoryTask != -1) {
            Bukkit.getScheduler().cancelTask(inventoryHistoryTask);
        }
        long sampleTicks = Math.max(1L, getConfig().getLong("history-sample-ticks", 100L));
        inventoryHistoryTask = getServer().getScheduler().runTaskTimer(this, inventory::tickHistory, sampleTicks, sampleTicks).getTaskId();
    }

    @Override public void onDisable() {
        if (inventoryHistoryTask != -1) {
            Bukkit.getScheduler().cancelTask(inventoryHistoryTask);
            inventoryHistoryTask = -1;
        }
        if (store != null) store.save();
    }
    public void reloadPlugin() { inventory.resetRuntime(); reloadConfig(); store.load(); scheduleInventoryHistory(); names.refreshAll(); }
    public DataStore store(){return store;} public PermissionService permissions(){return permissions;}
    public ArmorService armor(){return armor;} public CheckpointService checkpoints(){return checkpoints;}
    public DamageService damage(){return damage;} public EnchantService enchant(){return enchant;}
    public FreezeService freeze(){return freeze;} public InventoryService inventory(){return inventory;}
    public NameService names(){return names;} public ProtectionService protection(){return protection;}
}
