package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ZombieToolsFeature extends AbstractFeature {

    private enum ToolTier {
        NETHERITE("NETHERITE", "netherite-chance"),
        DIAMOND("DIAMOND", "diamond-chance"),
        GOLDEN("GOLDEN", "gold-chance"),
        IRON("IRON", "iron-chance"),
        WOODEN("WOODEN", "wood-chance");

        final String prefix;
        final String configKey;

        ToolTier(String prefix, String configKey) {
            this.prefix = prefix;
            this.configKey = configKey;
        }
    }

    private enum ToolType {
        AXE("_AXE"), PICKAXE("_PICKAXE"), SHOVEL("_SHOVEL");

        final String suffix;

        ToolType(String suffix) {
            this.suffix = suffix;
        }
    }

    @Override
    public String getName() {
        return "Zombie Tools";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZombieSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.hasMetadata("SDCustomMob")) return;
        if (!Feature.ZOMBIE_TOOLS.isEnabled(zombie)) return;
        if (RANDOM.nextDouble() > Feature.ZOMBIE_TOOLS.getDouble("chance-percent") / 100.0) return;

        ToolTier tier = selectTier();
        if (tier == null) return;

        ToolType type = ToolType.values()[RANDOM.nextInt(ToolType.values().length)];
        Material mat = Material.getMaterial(tier.prefix + type.suffix);
        if (mat == null) return;

        ItemStack tool = new ItemStack(mat);
        if (RANDOM.nextDouble() <= Feature.ZOMBIE_TOOLS.getDouble("enchantment-chance") / 100.0) {
            applyRandomEnchants(tool);
        }

        zombie.getEquipment();
        zombie.getEquipment().setItemInMainHand(tool);
        zombie.getEquipment().setItemInMainHandDropChance(0.0f);
        zombie.setCanPickupItems(false);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onZombieDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!Feature.ZOMBIE_TOOLS.isEnabled(zombie)) return;
        if (RANDOM.nextDouble() > Feature.ZOMBIE_TOOLS.getDouble("drop-chance-percent") / 100.0) return;

        zombie.getEquipment();
        ItemStack mainHand = zombie.getEquipment().getItemInMainHand();
        if (!isTool(mainHand.getType())) return;

        ItemStack dropItem = mainHand.clone();
        ItemMeta meta = dropItem.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(ThreadLocalRandom.current().nextInt(dropItem.getType().getMaxDurability()));
            dropItem.setItemMeta(meta);
        }
        zombie.getWorld().dropItemNaturally(zombie.getLocation(), dropItem);
    }

    private ToolTier selectTier() {
        double netherite = Feature.ZOMBIE_TOOLS.getDouble("netherite-chance");
        double diamond = Feature.ZOMBIE_TOOLS.getDouble("diamond-chance");
        double gold = Feature.ZOMBIE_TOOLS.getDouble("gold-chance");
        double iron = Feature.ZOMBIE_TOOLS.getDouble("iron-chance");
        double wood = Feature.ZOMBIE_TOOLS.getDouble("wood-chance");
        double total = netherite + diamond + gold + iron + wood;
        double roll = RANDOM.nextDouble() * total;

        if (roll <= netherite) return ToolTier.NETHERITE;
        if (roll <= netherite + diamond) return ToolTier.DIAMOND;
        if (roll <= netherite + diamond + gold) return ToolTier.GOLDEN;
        if (roll <= netherite + diamond + gold + iron) return ToolTier.IRON;
        return ToolTier.WOODEN;
    }

    private void applyRandomEnchants(ItemStack item) {
        int count = RANDOM.nextInt((int) Feature.ZOMBIE_TOOLS.getDouble("max-enchantments")) + 1;
        List<Enchantment> possible = new ArrayList<>(List.of(
                Enchantment.EFFICIENCY,
                Enchantment.UNBREAKING,
                Enchantment.FORTUNE
        ));
        if (item.getType().name().contains("_AXE")) {
            possible.add(Enchantment.SHARPNESS);
            possible.add(Enchantment.SMITE);
        }
        for (int i = 0; i < count && !possible.isEmpty(); i++) {
            Enchantment enchant = possible.remove(RANDOM.nextInt(possible.size()));
            int level = RANDOM.nextInt(enchant.getMaxLevel()) + enchant.getStartLevel();
            try {
                item.addUnsafeEnchantment(enchant, level);
            } catch (Exception ignored) {}
        }
    }

    private boolean isTool(Material mat) {
        String name = mat.name();
        return name.endsWith("_AXE") || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL");
    }
}