package com.uop.recordingtools.service;

import com.uop.recordingtools.storage.DataStore;
import com.uop.recordingtools.util.Items;
import org.bukkit.Material;
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
        if (lvl > 0) for (ItemStack i : armor) enchantArmor(i, lvl);
        p.getInventory().setArmorContents(armor);
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

    private void enchantArmor(ItemStack i, int lvl) {
        var prot = Items.enchantment("protection");
        if (prot != null) Items.enchant(i, prot, lvl);
    }

    public void giveWeapons(Player p, List<String> names, String mode, int lvl) {
        if (names == null || names.isEmpty()) return;
        boolean enchanted = !"plain".equalsIgnoreCase(mode);
        for (String n : names) {
            Material m = switch (n.toLowerCase(Locale.ROOT)) {
                case "sword" -> Material.NETHERITE_SWORD; case "mace" -> Material.MACE; case "axe" -> Material.NETHERITE_AXE;
                case "bow" -> Material.BOW; case "crossbow" -> Material.CROSSBOW; case "trident" -> Material.TRIDENT;
                case "pickaxe" -> Material.NETHERITE_PICKAXE; case "shovel" -> Material.NETHERITE_SHOVEL; case "hoe" -> Material.NETHERITE_HOE;
                default -> null;
            };
            if (m == null) throw new IllegalArgumentException("Unknown weapon/tool: " + n);
            ItemStack i = new ItemStack(m);
            if (enchanted) {
                if (m.name().contains("SWORD") || m.name().contains("AXE")) Items.enchant(i, Items.enchantment("sharpness"), lvl);
                if (m == Material.BOW) Items.enchant(i, Items.enchantment("power"), lvl);
                if (m == Material.CROSSBOW) Items.enchant(i, Items.enchantment("quick_charge"), lvl);
                if (m == Material.TRIDENT) Items.enchant(i, Items.enchantment("impaling"), lvl);
            }
            p.getInventory().addItem(i);
        }
    }

    public void saveLoadout(String name, Player p) {
        validateName(name);
        String base = "loadouts." + name;
        store.set(base + ".owner", p.getUniqueId().toString()); store.set(base + ".ownerName", p.getName());
        store.set(base + ".inventory", cloneList(p.getInventory().getContents()));
        store.set(base + ".armor", cloneList(p.getInventory().getArmorContents()));
        store.set(base + ".offhand", cloneList(new ItemStack[]{p.getInventory().getItemInOffHand()}));
        store.saveNow();
    }

    public boolean giveSaved(String name, Player p) {
        validateName(name);
        if (!store.data().contains("loadouts." + name)) return false;
        List<?> inv = store.data().getList("loadouts." + name + ".inventory");
        List<?> armor = store.data().getList("loadouts." + name + ".armor");
        List<?> off = store.data().getList("loadouts." + name + ".offhand");
        p.getInventory().setContents(deserialize(inv, 41)); p.getInventory().setArmorContents(deserialize(armor, 4));
        ItemStack[] o = deserialize(off, 1); p.getInventory().setItemInOffHand(o[0]);
        return true;
    }

    public Set<String> loadouts() { var sec = store.data().getConfigurationSection("loadouts"); return sec == null ? Set.of() : sec.getKeys(false); }
    public void delete(String name) { validateName(name); store.remove("loadouts." + name); store.saveNow(); }

    private List<ItemStack> cloneList(ItemStack[] items) { List<ItemStack> out = new ArrayList<>(items.length); for (ItemStack i : items) out.add(i == null ? new ItemStack(Material.AIR) : i.clone()); return out; }
    private ItemStack[] deserialize(List<?> l, int size) { ItemStack[] a = new ItemStack[size]; Arrays.fill(a, new ItemStack(Material.AIR)); if (l != null) for (int i = 0; i < Math.min(size, l.size()); i++) if (l.get(i) instanceof ItemStack x) a[i] = x.clone(); return a; }
    private void validateName(String name) { if (name == null || name.isBlank() || name.length() > 64 || !name.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Invalid loadout name."); }
}
