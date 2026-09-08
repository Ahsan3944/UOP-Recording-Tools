package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class FreezeService {
    private final DataStore store;
    private final PermissionService permissions;

    public FreezeService(JavaPlugin plugin, DataStore store, PermissionService permissions) {
        this.store = store;
        this.permissions = permissions;
    }

    /** OPs and explicitly exempt staff are never frozen by the service. */
    public boolean exempt(Player player) {
        return player.isOp()
                || player.hasPermission("uop.freeze.exempt")
                || permissions.has(player, "uop.freeze.exempt");
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
        store.saveNow();
    }

    public void apply(Player player) {
        // Movement is enforced by RecordingListener through PlayerMoveEvent.
        // Do not mutate walk/fly speeds here; doing so would alter the player's
        // pre-existing movement configuration when the freeze is removed.
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
