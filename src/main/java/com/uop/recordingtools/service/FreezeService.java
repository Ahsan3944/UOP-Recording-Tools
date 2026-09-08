package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class FreezeService {
    private final DataStore store;

    public FreezeService(JavaPlugin plugin, DataStore store) {
        this.store = store;
    }

    public boolean exempt(Player player) {
        return player.isOp() || player.hasPermission("uop.freeze.exempt");
    }

    public void freeze(Player player, String mode) {
        if (exempt(player)) return;
        String normalized = "full".equalsIgnoreCase(mode) ? "full" : "normal";
        store.set("freeze." + player.getUniqueId(), normalized);
        apply(player);
        store.saveNow();
    }

    public void unfreeze(Player player) {
        store.remove("freeze." + player.getUniqueId());
        player.setWalkSpeed(.2f);
        player.setFlySpeed(.1f);
        store.saveNow();
    }

    public void apply(Player player) {
        if (!frozen(player) || exempt(player)) return;
        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);
    }

    public String mode(Player player) {
        return store.data().getString("freeze." + player.getUniqueId());
    }

    public boolean frozen(Player player) {
        return mode(player) != null && !exempt(player);
    }

    public boolean full(Player player) {
        return "full".equalsIgnoreCase(mode(player)) && !exempt(player);
    }
}
