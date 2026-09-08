package com.uop.recordingtools.listener;

import com.uop.recordingtools.UopRecordingToolsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class RecordingListener implements Listener {
    private final UopRecordingToolsPlugin plugin;

    public RecordingListener(UopRecordingToolsPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> plugin.inventory().tickHistory(),
                20L,
                plugin.getConfig().getLong("history-sample-ticks", 10L)
        );
    }

    @EventHandler
    public void pre(AsyncPlayerPreLoginEvent event) {
        if (!plugin.protection().allowed(event.getName(), event.getUniqueId(), event.getAddress())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    "This player identity is protected by UOP Recording Tools.");
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        plugin.freeze().apply(event.getPlayer());
        plugin.names().refresh(event.getPlayer());
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        // Freeze state is persistent; do not clear it on disconnect.
    }

    @EventHandler
    public void move(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.freeze().frozen(player) || event.getTo() == null) return;

        boolean positionChanged = event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ();
        boolean rotationChanged = event.getFrom().getYaw() != event.getTo().getYaw()
                || event.getFrom().getPitch() != event.getTo().getPitch();

        if (!positionChanged && (!plugin.freeze().full(player) || !rotationChanged)) return;

        var to = event.getFrom().clone();
        if (!plugin.freeze().full(player)) {
            to.setYaw(event.getTo().getYaw());
            to.setPitch(event.getTo().getPitch());
        }
        event.setTo(to);
    }

    @EventHandler
    public void damage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && plugin.freeze().full(player)) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            event.setDamage(event.getDamage() * plugin.damage().multiplier(byEntity.getDamager(), byEntity.getEntity()));
        }
    }

    @EventHandler
    public void drop(PlayerDropItemEvent event) {
        if (plugin.freeze().frozen(event.getPlayer()) || plugin.inventory().locked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void pickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && plugin.freeze().frozen(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void swap(PlayerSwapHandItemsEvent event) {
        if (plugin.freeze().full(event.getPlayer()) || plugin.inventory().locked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.freeze().full(player) || plugin.inventory().locked(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void drag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && (plugin.freeze().full(player) || plugin.inventory().locked(player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void interact(PlayerInteractEvent event) {
        if (plugin.freeze().frozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
