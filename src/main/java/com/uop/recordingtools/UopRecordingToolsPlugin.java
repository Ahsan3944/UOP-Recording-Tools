package com.uop.recordingtools;

import com.uop.recordingtools.command.UopCommand;
import com.uop.recordingtools.listener.RecordingListener;
import com.uop.recordingtools.service.*;
import com.uop.recordingtools.storage.DataStore;
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

    @Override public void onEnable() {
        saveDefaultConfig();
        store = new DataStore(this); store.load();
        permissions = new PermissionService(this, store);
        armor = new ArmorService(this, store);
        checkpoints = new CheckpointService(this, store);
        damage = new DamageService(this, store);
        enchant = new EnchantService();
        freeze = new FreezeService(this, store);
        inventory = new InventoryService(this, store);
        names = new NameService(this, store);
        protection = new ProtectionService(this, store);
        UopCommand command = new UopCommand(this);
        PluginCommand root = getCommand("uop");
        if (root != null) { root.setExecutor(command); root.setTabCompleter(command); }
        getServer().getPluginManager().registerEvents(new RecordingListener(this), this);
        names.refreshAll();
        getLogger().info("UOP Recording Tools enabled.");
    }

    @Override public void onDisable() { if (store != null) store.save(); }
    public void reloadPlugin() { reloadConfig(); store.load(); names.refreshAll(); }
    public DataStore store(){return store;} public PermissionService permissions(){return permissions;}
    public ArmorService armor(){return armor;} public CheckpointService checkpoints(){return checkpoints;}
    public DamageService damage(){return damage;} public EnchantService enchant(){return enchant;}
    public FreezeService freeze(){return freeze;} public InventoryService inventory(){return inventory;}
    public NameService names(){return names;} public ProtectionService protection(){return protection;}
}
