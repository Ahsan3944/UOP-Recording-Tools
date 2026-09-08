package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import java.util.*;

public final class NameService {
    private final DataStore store;
    private final Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
    private static final String TEAM = "uop_hidden";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public NameService(org.bukkit.plugin.java.JavaPlugin p, DataStore s) { store = s; }

    public void hide(Player p) { store.set("names.hidden." + p.getUniqueId(), true); apply(p); store.saveNow(); }
    public void show(Player p) { store.remove("names.hidden." + p.getUniqueId()); remove(p); refresh(p); store.saveNow(); }

    private void apply(Player p) {
        if (hidden(p)) {
            Team t = board.getTeam(TEAM);
            if (t == null) t = board.registerNewTeam(TEAM);
            t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            t.addEntry(p.getName());
        }
        applyTag(p);
    }

    private void applyTag(Player p) {
        String tag = tagOf(p);
        String n = tag == null ? p.getName() : "[" + tag + "§r] " + p.getName();
        p.setCustomName(tag == null ? null : n);
        p.setCustomNameVisible(tag != null && !hidden(p));
        p.setDisplayName(n);
    }

    private void remove(Player p) {
        Team t = board.getTeam(TEAM);
        if (t != null) t.removeEntry(p.getName());
    }

    public boolean hidden(Player p) { return store.data().getBoolean("names.hidden." + p.getUniqueId(), false); }

    public void refresh(Player p) {
        remove(p);
        if (hidden(p)) {
            Team t = board.getTeam(TEAM);
            if (t == null) t = board.registerNewTeam(TEAM);
            t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            t.addEntry(p.getName());
        }
        applyTag(p);
    }

    public void refreshAll() { for (Player p : Bukkit.getOnlinePlayers()) refresh(p); }

    public Component chatName(Player p) {
        String tag = tagOf(p);
        if (tag == null) return Component.text(p.getName());
        return LEGACY.deserialize("[" + tag + "§r] " + p.getName());
    }

    public void create(String id, String color, boolean bold, boolean italic, String text) {
        validateId(id);
        text = normalizeText(text);
        String val = legacy(color) + (bold ? "§l" : "") + (italic ? "§o" : "") + text;
        store.set("tags." + id + ".text", val);
        store.set("tags." + id + ".bold", bold);
        store.set("tags." + id + ".italic", italic);
        store.set("tags." + id + ".color", normalizeColor(color));
        store.saveNow();
    }

    public void edit(String id, String color, boolean bold, boolean italic, String text) {
        validateId(id);
        if (!store.data().contains("tags." + id)) throw new IllegalArgumentException("Tag not found: " + id);
        create(id, color, bold, italic, text);
        for (Player p : Bukkit.getOnlinePlayers()) if (id.equals(tagId(p))) refresh(p);
    }

    public Set<String> tags() {
        var s = store.data().getConfigurationSection("tags");
        return s == null ? Set.of() : s.getKeys(false);
    }

    public void deleteTag(String id) {
        validateId(id);
        if (!store.data().contains("tags." + id)) throw new IllegalArgumentException("Tag not found: " + id);
        store.remove("tags." + id);
        for (Player p : Bukkit.getOnlinePlayers()) if (id.equals(tagId(p))) removeTag(p);
        store.saveNow();
    }

    public void give(Player p, String id) {
        validateId(id);
        if (!store.data().contains("tags." + id)) throw new IllegalArgumentException("Tag not found: " + id);
        store.set("names.tag." + p.getUniqueId(), id);
        refresh(p);
        store.saveNow();
    }

    public void removeTag(Player p) { store.remove("names.tag." + p.getUniqueId()); refresh(p); store.saveNow(); }
    public String tagId(Player p) { return store.data().getString("names.tag." + p.getUniqueId()); }
    public boolean hasTag(Player p) { return tagOf(p) != null; }

    private String tagOf(Player p) {
        String id = tagId(p);
        if (id == null || !store.data().contains("tags." + id)) return null;
        String text = store.data().getString("tags." + id + ".text");
        return text == null || text.isEmpty() ? null : text;
    }

    private void validateId(String id) {
        if (id == null || id.isBlank() || id.length() > 32 || !id.matches("[A-Za-z0-9_-]+"))
            throw new IllegalArgumentException("Invalid tag id.");
    }

    private String normalizeText(String s) {
        if (s == null) return "";
        s = ChatColor.stripColor(s);
        return s.length() > 32 ? s.substring(0, 32) : s;
    }

    private String normalizeColor(String c) {
        return c == null ? "white" : c.trim().toLowerCase(Locale.ROOT);
    }

    private String legacy(String c) {
        if (c == null) return "";
        c = c.trim();
        if (c.startsWith("#") && c.matches("#[0-9a-fA-F]{6}")) {
            StringBuilder b = new StringBuilder("§x");
            for (char ch : c.substring(1).toCharArray()) b.append('§').append(ch);
            return b.toString();
        }
        return switch (c.toLowerCase(Locale.ROOT)) {
            case "black" -> "§0"; case "dark_blue" -> "§1"; case "dark_green" -> "§2"; case "dark_aqua" -> "§3";
            case "dark_red" -> "§4"; case "dark_purple" -> "§5"; case "gold" -> "§6"; case "gray" -> "§7";
            case "dark_gray" -> "§8"; case "blue" -> "§9"; case "green" -> "§a"; case "aqua" -> "§b";
            case "red" -> "§c"; case "light_purple" -> "§d"; case "yellow" -> "§e"; case "white" -> "§f";
            default -> ChatColor.WHITE.toString();
        };
    }
}
