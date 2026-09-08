package com.uop.recordingtools.util;

import org.bukkit.Material; import org.bukkit.enchantments.Enchantment; import org.bukkit.inventory.ItemStack; import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class Items {
 private Items(){}
 public static Material material(String s){return switch(s.toLowerCase(Locale.ROOT)){case "leather"->Material.LEATHER_CHESTPLATE;case "chainmail"->Material.CHAINMAIL_CHESTPLATE;case "iron"->Material.IRON_CHESTPLATE;case "gold"->Material.GOLDEN_CHESTPLATE;case "diamond"->Material.DIAMOND_CHESTPLATE;case "netherite"->Material.NETHERITE_CHESTPLATE;default->null;};}
 public static ItemStack armor(Material mat,String piece){String suffix=switch(piece){case "head"->"HELMET";case "chest"->"CHESTPLATE";case "legs"->"LEGGINGS";default->"BOOTS";}; String n=mat.name().replace("_CHESTPLATE","")+"_"+suffix; Material m; try{m=Material.valueOf(n);}catch(Exception e){return new ItemStack(Material.AIR);} return new ItemStack(m);}
 public static void enchant(ItemStack item,Enchantment ench,int level){if(item==null||item.getType().isAir()||ench==null)return; try{item.addUnsafeEnchantment(ench,level);}catch(Exception ignored){}}
 public static void clearEnchantment(ItemStack item,Enchantment ench){if(item!=null&&ench!=null)item.removeEnchantment(ench);}
 public static Enchantment enchantment(String name){String n=name.toLowerCase(Locale.ROOT); for(Enchantment e:Enchantment.values()){if(e.getKey().getKey().equalsIgnoreCase(n)||e.getKey().toString().equalsIgnoreCase(n)||e.getName().equalsIgnoreCase(n))return e;} return null;}
 public static int level(String mode,String level){if(mode.equalsIgnoreCase("plain"))return 0;if(mode.equalsIgnoreCase("max"))return 10;try{return Math.max(1,Math.min(10,Integer.parseInt(level)));}catch(Exception e){return 1;}}
 public static ItemStack named(ItemStack item,String name){if(item==null)return null; ItemMeta meta=item.getItemMeta(); if(meta!=null){meta.setDisplayName(name);item.setItemMeta(meta);}return item;}
}
