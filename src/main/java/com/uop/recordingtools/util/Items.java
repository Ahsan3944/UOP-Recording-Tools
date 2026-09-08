package com.uop.recordingtools.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class Items {
    private Items() {}

    public static Material material(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "leather" -> Material.LEATHER_CHESTPLATE;
            case "chainmail" -> Material.CHAINMAIL_CHESTPLATE;
            case "iron" -> Material.IRON_CHESTPLATE;
            case "gold" -> Material.GOLDEN_CHESTPLATE;
            case "diamond" -> Material.DIAMOND_CHESTPLATE;
            case "netherite" -> Material.NETHERITE_CHESTPLATE;
            default -> null;
        };
    }

    public static ItemStack armor(Material mat, String piece) {
        if (mat == null || piece == null) return new ItemStack(Material.AIR);
        String suffix = switch (piece.toLowerCase(Locale.ROOT)) {
            case "head", "helmet" -> "HELMET";
            case "chest", "chestplate" -> "CHESTPLATE";
            case "legs", "leggings" -> "LEGGINGS";
            case "feet", "boots" -> "BOOTS";
            default -> null;
        };
        if (suffix == null) return new ItemStack(Material.AIR);
        String n = mat.name().replace("_CHESTPLATE", "") + "_" + suffix;
        try {
            return new ItemStack(Material.valueOf(n));
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.AIR);
        }
    }

    public static void enchant(ItemStack item, Enchantment ench, int level) {
        if (item == null || item.getType().isAir() || ench == null) return;
        if (level < 1 || level > 255) throw new IllegalArgumentException("Enchantment level must be 1-255.");
        int maxLevel = ench.getMaxLevel();
        if (level > maxLevel) throw new IllegalArgumentException("Enchantment level must not exceed " + maxLevel + " for " + ench.getKey().getKey() + ".");
        item.addUnsafeEnchantment(ench, level);
    }

    public static void clearEnchantment(ItemStack item, Enchantment ench) {
        if (item != null && !item.getType().isAir() && ench != null) item.removeEnchantment(ench);
    }

    public static Enchantment enchantment(String name) {
        if (name == null || name.isBlank()) return null;
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        if (normalized.startsWith("minecraft:")) normalized = normalized.substring("minecraft:".length());
        for (Enchantment e : Enchantment.values()) {
            if (e.getKey().getKey().equalsIgnoreCase(normalized)
                    || e.getKey().toString().equalsIgnoreCase("minecraft:" + normalized)) return e;
        }
        return null;
    }

    public static int level(String mode, String level) {
        if (mode == null) return -1;
        if (mode.equalsIgnoreCase("plain")) return 0;
        if (mode.equalsIgnoreCase("max")) return 10;
        try {
            int n = Integer.parseInt(level);
            return n >= 1 && n <= 10 ? n : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static ItemStack named(ItemStack item, String name) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
