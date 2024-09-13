package org.nguyendevs.suddendeath.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public enum MobStat {
    HELMET(new ItemStack(Material.LEATHER_HELMET), "equipment.helmet", serialize(), Type.ITEMSTACK, "Helmet", new String[]{"Give your monster a helmet!"}),
    CHESTPLATE(new ItemStack(Material.LEATHER_CHESTPLATE), "equipment.chestplate", serialize(), Type.ITEMSTACK, "Chestplate", new String[]{"Give your monster a nice chestplate!"}),
    LEGGINGS(new ItemStack(Material.LEATHER_LEGGINGS), "equipment.leggings", serialize(), Type.ITEMSTACK, "Leggings", new String[]{"Give your monster some pants!"}),
    BOOTS(new ItemStack(Material.LEATHER_BOOTS), "equipment.boots", serialize(), Type.ITEMSTACK, "Boots", new String[]{"Give your monster a pair of shoes!"}),

    MAIN_HAND(new ItemStack(Material.IRON_SWORD), "equipment.mainHand", serialize(), Type.ITEMSTACK, "Main Hand Item", new String[]{"The item your monster holds."}),
    OFF_HAND(new ItemStack(Material.BOW), "equipment.offHand", serialize(), Type.ITEMSTACK, "Off Hand Item", new String[]{"The second item your monster holds."}),

    POTION_EFFECTS(new ItemStack(Material.POTION), "eff", null, Type.POTION_EFFECTS, "Potion Effects", new String[]{"Give your monster cool potion effects!"}),
    DISPLAY_NAME(new ItemStack(Material.NAME_TAG), "name", "My Custom Mob", Type.STRING, "Display Name", new String[]{"Set the custom name of your monster."}),
    SPAWN_COEFFICIENT(new ItemStack(Material.EMERALD), "spawn-coef", 0, Type.DOUBLE, "Spawn Coefficient", new String[]{"Spawn Coefficient determins the frequency", "at which your monster spawns.", "&9See Wiki for more detailed information."}),

    MAXIMUM_HEALTH(new ItemStack(Material.IRON_CHESTPLATE), "hp", 20, Type.DOUBLE, "Maximum Health", new String[]{"The amount of HP your monster has."}),
    ATTACK_DAMAGE(new ItemStack(Material.RED_DYE), "atk", 4, Type.DOUBLE, "Attack Damage", new String[]{"Increase the damage your monster deals."}),
    MOVEMENT_SPEED(new ItemStack(Material.LEATHER_BOOTS), "ms", .02, Type.DOUBLE, "Movement Speed", new String[]{"Make your monster faster!"}),
    ;

    private final String path;
    private final Object defaultValue;
    private final Type type;

    private final ItemStack item;
    private final String name;
    private final List<String> lore;

    MobStat(ItemStack item, String path, Object defaultValue, Type type, String name, String[] lore) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.type = type;

        this.item = item;
        this.name = name;
        this.lore = Arrays.asList(lore);
    }

    public String getPath() {
        return path;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Type getType() {
        return type;
    }

    public ItemStack getNewItem() {
        return item.clone();
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public enum Type {
        STRING,
        DOUBLE,
        ITEMSTACK,
        POTION_EFFECTS;
    }

    static String serialize() {
        return ItemUtils.serialize(new ItemStack(Material.AIR));
    }
}
