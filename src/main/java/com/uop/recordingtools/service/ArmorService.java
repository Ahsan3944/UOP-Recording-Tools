package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import com.uop.recordingtools.util.Items;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class ArmorService {
    private final DataStore store;
    public ArmorService(JavaPlugin p, DataStore s) { store = s; }

    public boolean set(Player p, String matName, String mode, boolean weapons, List<String> weaponNames) {
        Material chest = Items.material(matName);
        if (chest == null) throw new IllegalArgumentException("Invalid armor material: " + matName);
        int lvl = levelFor(mode);
        ItemStack[] armor = {Items.armor(chest, "head"), Items.armor(chest, "chest"), Items.armor(chest, "legs"), Items.armor(chest, "feet")};
        if (lvl > 0) for (ItemStack i : armor) enchantAll(i, lvl);
        replaceArmorSafely(p, armor);
        if (weapons) giveWeapons(p, weaponNames, mode, lvl);
        return true;
    }

    private int levelFor(String mode) {
        if (mode == null) throw new IllegalArgumentException("Armor mode required.");
        String x = mode.trim().toLowerCase(Locale.ROOT);
        if (x.equals("plain")) return 0;
        if (x.equals("max")) return 10;
        if (x.startsWith("enchanted ")) {
            try { int n = Integer.parseInt(x.substring(10).trim()); if (n >= 1 && n <= 10) return n; }
            catch (NumberFormatException ignored) {}
        }
        throw new IllegalArgumentException("Armor mode must be plain, enchanted 1-10, or max.");
    }

    private void replaceArmorSafely(Player p, ItemStack[] armor) {
        ItemStack[] old = p.getInventory().getArmorContents();
        p.getInventory().setArmorContents(armor);
        for (ItemStack item : old) {
            if (item == null || item.getType().isAir()) continue;
            Map<Integer, ItemStack> leftovers = p.getInventory().addItem(item.clone());
            for (ItemStack leftover : leftovers.values()) p.getWorld().dropItemNaturally(p.getLocation(), leftover);
        }
    }

    /** Mirrors the Fabric loadout tiers: add every applicable non-cursed enchantment. */
    private void enchantAll(ItemStack item, int tier) {
        if (item == null || item.getType().isAir()) return;
        for (Enchantment enchantment : Enchantment.values()) {
            if (isCurse(enchantment) || !enchantment.canEnchantItem(item)) continue;
            String id = enchantment.getKey().getKey().toLowerCase(Locale.ROOT);
            if (!allowedAtTier(id, tier)) continue;
            int level = tier == 10 ? enchantment.getMaxLevel() : Math.min(tier, enchantment.getMaxLevel());
            if (level >= 1) Items.enchant(item, enchantment, level);
        }
    }

    private boolean isCurse(Enchantment enchantment) {
        String id = enchantment.getKey().getKey().toLowerCase(Locale.ROOT);
        return id.contains("curse") || id.equals("binding") || id.equals("vanishing");
    }

    private boolean allowedAtTier(String id, int tier) {
        if (tier == 1) return id.equals("mending");
        if (tier == 2) return id.equals("mending") || id.equals("unbreaking");
        if (tier <= 4) return id.equals("mending") || id.equals("unbreaking") || id.contains("protection") || id.equals("feather_falling") || id.equals("respiration");
        if (tier <= 6) return allowedAtTier(id, 4) || id.equals("aqua_affinity") || id.equals("depth_strider") || id.equals("soul_speed") || id.equals("swift_sneak");
        if (tier <= 8) return allowedAtTier(id, 6) || id.equals("thorns") || id.equals("frost_walker") || id.equals("silk_touch") || id.equals("fortune");
        return true;
    }

    public void giveWeapons(Player p, List<String> names, String mode, int lvl) {
        if (names == null || names.isEmpty()) throw new IllegalArgumentException("Select at least one weapon/tool, or use false.");
        for (String n : names) {
            Material m = switch (n.toLowerCase(Locale.ROOT)) {
                case "sword" -> Material.NETHERITE_SWORD; case "mace" -> Material.MACE; case "axe" -> Material.NETHERITE_AXE;
                case "bow" -> Material.BOW; case "crossbow" -> Material.CROSSBOW; case "trident" -> Material.TRIDENT;
                case "pickaxe" -> Material.NETHERITE_PICKAXE; case "shovel" -> Material.NETHERITE_SHOVEL; case "hoe" -> Material.NETHERITE_HOE;
                default -> null;
            };
            if (m == null) throw new IllegalArgumentException("Unknown weapon/tool: " + n);
            ItemStack i = new ItemStack(m);
            if (!"plain".equalsIgnoreCase(mode)) enchantAll(i, lvl);
            Map<Integer, ItemStack> leftovers = p.getInventory().addItem(i);
            for (ItemStack leftover : leftovers.values()) p.getWorld().dropItemNaturally(p.getLocation(), leftover);
        }
    }

    public void saveLoadout(String name, Player p) {
        String key = loadoutKey(name);
        String base = "loadouts." + key;
        store.set(base + ".owner", p.getUniqueId().toString());
        store.set(base + ".ownerName", p.getName());
        store.set(base + ".hotbar", cloneList(Arrays.copyOfRange(p.getInventory().getContents(), 0, 9)));
        store.set(base + ".armor", cloneList(p.getInventory().getArmorContents()));
        store.set(base + ".offhand", cloneList(new ItemStack[]{p.getInventory().getItemInOffHand()}));
        store.remove(base + ".inventory");
        store.saveNow();
    }

    public boolean giveSaved(String name, Player p) {
        String key = loadoutKey(name);
        String base = "loadouts." + key;
        if (!store.data().contains(base)) return false;
        List<?> hotbar = store.data().getList(base + ".hotbar");
        if (hotbar == null) {
            List<?> legacyInventory = store.data().getList(base + ".inventory");
            hotbar = legacyInventory == null ? null : legacyInventory.subList(0, Math.min(9, legacyInventory.size()));
        }
        List<?> armor = store.data().getList(base + ".armor");
        List<?> off = store.data().getList(base + ".offhand");
        restoreHotbarSafely(p, deserialize(hotbar, 9));
        restoreArmorSafely(p, deserialize(armor, 4));
        ItemStack[] o = deserialize(off, 1);
        ItemStack currentOffhand = p.getInventory().getItemInOffHand();
        p.getInventory().setItemInOffHand(o[0]);
        returnItemSafely(p, currentOffhand);
        return true;
    }

    private void restoreHotbarSafely(Player p, ItemStack[] hotbar) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack current = p.getInventory().getItem(slot);
            p.getInventory().setItem(slot, hotbar[slot]);
            returnItemSafely(p, current);
        }
    }

    private void restoreArmorSafely(Player p, ItemStack[] armor) {
        ItemStack[] current = p.getInventory().getArmorContents();
        p.getInventory().setArmorContents(armor);
        for (ItemStack item : current) returnItemSafely(p, item);
    }

    private void returnItemSafely(Player p, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        Map<Integer, ItemStack> leftovers = p.getInventory().addItem(item.clone());
        for (ItemStack leftover : leftovers.values()) p.getWorld().dropItemNaturally(p.getLocation(), leftover);
    }

    public Set<String> loadouts() { var sec = store.data().getConfigurationSection("loadouts"); return sec == null ? Set.of() : sec.getKeys(false); }
    public void delete(String name) { store.remove("loadouts." + loadoutKey(name)); store.saveNow(); }

    private String loadoutKey(String name) {
        validateName(name);
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private List<ItemStack> cloneList(ItemStack[] items) { List<ItemStack> out = new ArrayList<>(items.length); for (ItemStack i : items) out.add(i == null ? new ItemStack(Material.AIR) : i.clone()); return out; }
    private ItemStack[] deserialize(List<?> l, int size) { ItemStack[] a = new ItemStack[size]; Arrays.fill(a, new ItemStack(Material.AIR)); if (l != null) for (int i = 0; i < Math.min(size, l.size()); i++) if (l.get(i) instanceof ItemStack x) a[i] = x.clone(); return a; }
    private void validateName(String name) { if (name == null || name.isBlank() || name.length() > 32 || !name.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Invalid loadout name."); }
}
