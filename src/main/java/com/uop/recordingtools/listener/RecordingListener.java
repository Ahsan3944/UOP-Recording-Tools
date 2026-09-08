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
import org.bukkit.event.player.*;

public final class RecordingListener implements Listener {
    private final UopRecordingToolsPlugin plugin;

    public RecordingListener(UopRecordingToolsPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> plugin.inventory().tickHistory(), 20L,
                Math.max(1L, plugin.getConfig().getLong("history-sample-ticks", 10L)));
    }

    @EventHandler public void pre(AsyncPlayerPreLoginEvent e) {
        if (!plugin.protection().allowed(e.getName(), e.getUniqueId(), e.getAddress()))
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "This player identity is protected by UOP Recording Tools.");
    }

    @EventHandler public void join(PlayerJoinEvent e) {
        plugin.freeze().apply(e.getPlayer());
        plugin.names().refresh(e.getPlayer());
    }

    @EventHandler public void move(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!plugin.freeze().frozen(p) || e.getTo() == null) return;
        boolean position = e.getFrom().getX() != e.getTo().getX() || e.getFrom().getY() != e.getTo().getY() || e.getFrom().getZ() != e.getTo().getZ();
        boolean rotation = e.getFrom().getYaw() != e.getTo().getYaw() || e.getFrom().getPitch() != e.getTo().getPitch();
        if (!position && (!plugin.freeze().full(p) || !rotation)) return;
        var to = e.getFrom().clone();
        if (!plugin.freeze().full(p)) { to.setYaw(e.getTo().getYaw()); to.setPitch(e.getTo().getPitch()); }
        e.setTo(to);
    }

    @EventHandler public void damage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && plugin.freeze().full(p)) { e.setCancelled(true); return; }
        var damager = e instanceof EntityDamageByEntityEvent x ? x.getDamager() : null;
        e.setDamage(e.getDamage() * plugin.damage().multiplier(damager, e.getEntity()));
    }

    @EventHandler public void drop(PlayerDropItemEvent e) {
        if (plugin.freeze().frozen(e.getPlayer()) || plugin.inventory().locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void pickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && plugin.freeze().frozen(p)) e.setCancelled(true);
    }

    @EventHandler public void swap(PlayerSwapHandItemsEvent e) {
        if (plugin.freeze().full(e.getPlayer()) || plugin.inventory().locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (plugin.freeze().full(p) || plugin.inventory().locked(p)) { e.setCancelled(true); return; }
    }

    @EventHandler public void drag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p && (plugin.freeze().full(p) || plugin.inventory().locked(p))) e.setCancelled(true);
    }

    /** Normal freeze blocks world interaction; full freeze uses the same block plus all hand actions. */
    @EventHandler public void interact(PlayerInteractEvent e) {
        if (plugin.freeze().frozen(e.getPlayer())) e.setCancelled(true);
    }

    /** Full freeze prevents interacting with entities (NPCs, armor stands, vehicles, etc.). */
    @EventHandler public void interactEntity(PlayerInteractEntityEvent e) {
        if (plugin.freeze().frozen(e.getPlayer())) e.setCancelled(true);
    }

    /** Full freeze prevents changing the selected hotbar slot. */
    @EventHandler public void held(PlayerItemHeldEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    /** Full freeze prevents consuming food/potions and similar held items. */
    @EventHandler public void consume(PlayerItemConsumeEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    /** Full freeze prevents fishing state changes and hook actions. */
    @EventHandler public void fish(PlayerFishEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    /** Full freeze prevents shearing entities. */
    @EventHandler public void shear(PlayerShearEntityEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    /** Full freeze prevents changing sneak/sprint state used by gameplay actions. */
    @EventHandler public void sneak(PlayerToggleSneakEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void sprint(PlayerToggleSprintEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }
}
