package com.uop.recordingtools.service;

import com.uop.recordingtools.util.Items;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public final class EnchantService {
    public void apply(Player p, String scope, Enchantment e, int level, String slot) {
        if (level < 1 || level > 255) throw new IllegalArgumentException("Enchantment level must be 1-255.");
        for (ItemStack i : items(p, scope, slot)) {
            if (i != null && !i.getType().isAir()) Items.enchant(i, e, level);
        }
    }

    public void remove(Player p, String scope, Enchantment e, String slot) {
        for (ItemStack i : items(p, scope, slot)) {
            if (i != null && !i.getType().isAir()) Items.clearEnchantment(i, e);
        }
    }

    private List<ItemStack> items(Player p, String scope, String slot) {
        var inv = p.getInventory();
        List<ItemStack> out = new ArrayList<>();
        switch (scope.toLowerCase(Locale.ROOT)) {
            case "all" -> {
                // Storage, armor and offhand are distinct logical scopes. Do not use
                // getContents() here because it can already contain equipment slots.
                out.addAll(Arrays.asList(inv.getStorageContents()));
                out.addAll(Arrays.asList(inv.getArmorContents()));
                out.add(inv.getItemInOffHand());
            }
            case "armor" -> {
                if (slot == null || slot.equalsIgnoreCase("all")) out.addAll(Arrays.asList(inv.getArmorContents()));
                else switch (slot.toLowerCase(Locale.ROOT)) {
                    case "head", "helmet" -> out.add(inv.getHelmet());
                    case "chest", "chestplate" -> out.add(inv.getChestplate());
                    case "legs", "leggings" -> out.add(inv.getLeggings());
                    case "feet", "boots" -> out.add(inv.getBoots());
                    default -> throw new IllegalArgumentException("Invalid armor slot: " + slot);
                };
            }
            case "equipment" -> {
                out.addAll(Arrays.asList(inv.getArmorContents()));
                out.add(inv.getItemInMainHand());
                out.add(inv.getItemInOffHand());
            }
            case "mainhand" -> out.add(inv.getItemInMainHand());
            case "offhand" -> out.add(inv.getItemInOffHand());
            case "hand" -> {
                if (slot == null || slot.equalsIgnoreCase("mainhand")) out.add(inv.getItemInMainHand());
                else if (slot.equalsIgnoreCase("offhand")) out.add(inv.getItemInOffHand());
                else throw new IllegalArgumentException("Invalid hand slot: " + slot);
            }
            case "inventory" -> out.addAll(Arrays.asList(inv.getStorageContents()));
            default -> throw new IllegalArgumentException("Invalid enchantment scope: " + scope);
        }
        return out;
    }
}
