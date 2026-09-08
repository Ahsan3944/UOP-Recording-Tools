package com.uop.recordingtools.listener;

import com.uop.recordingtools.UopRecordingToolsPlugin;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

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

    @EventHandler public void chat(AsyncChatEvent e) {
        Player player = e.getPlayer();
        if (!plugin.names().hasTag(player)) return;
        e.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) ->
                plugin.names().chatName(source).append(net.kyori.adventure.text.Component.text(" » ")).append(message)));
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
        if (e instanceof EntityDamageByEntityEvent x) {
            if (x.getDamager() instanceof Player p && plugin.freeze().frozen(p)) {
                e.setCancelled(true);
                return;
            }
            if (x.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p
                    && plugin.freeze().frozen(p)) {
                e.setCancelled(true);
                return;
            }
        }
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

    @EventHandler public void interact(PlayerInteractEvent e) {
        if (!plugin.freeze().frozen(e.getPlayer())) return;
        if (!plugin.freeze().full(e.getPlayer()) && isArmor(e.getItem())
                && (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) return;
        e.setCancelled(true);
    }

    @EventHandler public void interactEntity(PlayerInteractEntityEvent e) {
        if (plugin.freeze().frozen(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void armorStand(PlayerArmorStandManipulateEvent e) {
        if (plugin.freeze().frozen(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void bucketEmpty(PlayerBucketEmptyEvent e) {
        if (plugin.freeze().frozen(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void bucketFill(PlayerBucketFillEvent e) {
        if (plugin.freeze().frozen(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void held(PlayerItemHeldEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void consume(PlayerItemConsumeEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void fish(PlayerFishEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void shear(PlayerShearEntityEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void sneak(PlayerToggleSneakEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void sprint(PlayerToggleSprintEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void flight(PlayerToggleFlightEvent e) {
        if (plugin.freeze().full(e.getPlayer())) e.setCancelled(true);
    }

    private boolean isArmor(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        Material type = item.getType();
        String name = type.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || type == Material.TURTLE_HELMET;
    }
}
