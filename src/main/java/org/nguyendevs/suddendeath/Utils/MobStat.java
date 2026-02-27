package org.nguyendevs.suddendeath.Utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;


public enum MobStat {
    HELMET(
            new ItemStack(Material.LEATHER_HELMET),
            "equipment.helmet",
            ItemUtils.serialize(new ItemStack(Material.AIR)),
            Type.ITEMSTACK,
            "Helmet",
            new String[]{"Give your monster a helmet!"}
    ),
    CHESTPLATE(
            new ItemStack(Material.LEATHER_CHESTPLATE),
            "equipment.chestplate",
            ItemUtils.serialize(new ItemStack(Material.AIR)),
            Type.ITEMSTACK,
            "Chestplate",
            new String[]{"Give your monster a nice chestplate!"}
    ),
    LEGGINGS(
            new ItemStack(Material.LEATHER_LEGGINGS),
            "equipment.leggings",
            ItemUtils.serialize(new ItemStack(Material.AIR)),
            Type.ITEMSTACK,
            "Leggings",
            new String[]{"Give your monster some pants!"}
    ),
    BOOTS(
            new ItemStack(Material.LEATHER_BOOTS),
            "equipment.boots",
            ItemUtils.serialize(new ItemStack(Material.AIR)),
            Type.ITEMSTACK,
            "Boots",
            new String[]{"Give your monster a pair of shoes!"}
    ),
    MAIN_HAND(
            new ItemStack(Material.IRON_SWORD),
            "equipment.mainHand",
            ItemUtils.serialize(new ItemStack(Material.AIR)),
            Type.ITEMSTACK,
            "Main Hand Item",
            new String[]{"The item your monster holds."}
    ),
    OFF_HAND(
            new ItemStack(Material.BOW),
            "equipment.offHand",
            ItemUtils.serialize(new ItemStack(Material.AIR)),
            Type.ITEMSTACK,
            "Off Hand Item",
            new String[]{"The second item your monster holds."}
    ),
    POTION_EFFECTS(
            new ItemStack(Material.POTION),
            "eff",
            null,
            Type.POTION_EFFECTS,
            "Potion Effects",
            new String[]{"Give your monster cool potion effects!"}
    ),
    DISPLAY_NAME(
            new ItemStack(Material.NAME_TAG),
            "name",
            "None",
            Type.STRING,
            "Display Name",
            new String[]{"Set the custom name of your monster.",
                         "Leave 'None' to hide the mob name."
            }
    ),
    SPAWN_COEFFICIENT(
            new ItemStack(Material.EMERALD),
            "spawn-coef",
            0.0,
            Type.DOUBLE,
            "Spawn Coefficient",
            new String[]{
                    "Spawn Coefficient determines the frequency",
                    "at which your monster spawns.",
                    "",
                    "Formula:",
                    "Chance = (This Mob's Weight) / (Total Weight)",
                    "Total Weight = Vanilla Weight + All Custom Mobs' Weights",
                    "",
                    "Example:",
                    "- Vanilla Zombie Weight: 20",
                    "- Custom Zombie Weight: 50",
                    "",
                    "> Custom Zombie Spawn Chance = 50 / (20 + 50) = 50/70",
                    "> Approximately 71% chance to spawn instead of Vanilla."
            }
    ),
    MAXIMUM_HEALTH(
            new ItemStack(Material.IRON_CHESTPLATE),
            "hp",
            20.0,
            Type.DOUBLE,
            "Maximum Health",
            new String[]{"The amount of HP your monster has."}
    ),
    ATTACK_DAMAGE(
            new ItemStack(Material.RED_DYE),
            "atk",
            4.0,
            Type.DOUBLE,
            "Attack Damage",
            new String[]{"Increase the damage your monster deals."}
    ),
    MOVEMENT_SPEED(
            new ItemStack(Material.NETHERITE_BOOTS),
            "ms",
            0.23,
            Type.DOUBLE,
            "Movement Speed",
            new String[]{"Make your monster faster!"}
    );

    private final ItemStack item;
    private final String path;
    private final Object defaultValue;
    private final Type type;
    private final String name;
    private final List<String> lore;

    MobStat(ItemStack item, String path, Object defaultValue, Type type, String name, String[] lore) {
        if (item == null || path == null || type == null || name == null || lore == null) {
            SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
                    "Invalid MobStat initialization for: " + name);
            throw new IllegalArgumentException("MobStat parameters cannot be null");
        }
        this.item = item.clone(); // Ensure immutability
        this.path = path;
        this.defaultValue = defaultValue;
        this.type = type;
        this.name = name;
        this.lore = Collections.unmodifiableList(Arrays.asList(lore));
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
        try {
            return item.clone();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error cloning item for MobStat: " + name, e);
            return new ItemStack(Material.AIR);
        }
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
        POTION_EFFECTS
    }
}