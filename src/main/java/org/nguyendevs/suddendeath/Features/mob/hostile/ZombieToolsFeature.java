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
        ToolTier(String prefix, String configKey) { this.prefix = prefix; this.configKey = configKey; }
    }

    private enum ToolType {
        AXE("_AXE"), PICKAXE("_PICKAXE"), SHOVEL("_SHOVEL");
        final String suffix;
        ToolType(String suffix) { this.suffix = suffix; }
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

        double spawnChance = Feature.ZOMBIE_TOOLS.getDouble("chance-percent") / 100.0;
        if (RANDOM.nextDouble() > spawnChance) return;

        ToolTier tier = selectTier();
        if (tier == null) return;

        ToolType type = ToolType.values()[RANDOM.nextInt(ToolType.values().length)];
        Material mat = Material.getMaterial(tier.prefix + type.suffix);
        if (mat == null) return;

        ItemStack tool = new ItemStack(mat);
        double enchantChance = Feature.ZOMBIE_TOOLS.getDouble("enchantment-chance") / 100.0;
        if (RANDOM.nextDouble() <= enchantChance) {
            applyRandomEnchants(tool);
        }

        if (zombie.getEquipment() != null) {
            zombie.getEquipment().setItemInMainHand(tool);
            zombie.getEquipment().setItemInMainHandDropChance(0.0f);
            zombie.setCanPickupItems(false);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onZombieDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!Feature.ZOMBIE_TOOLS.isEnabled(zombie)) return;

        double dropChance = Feature.ZOMBIE_TOOLS.getDouble("drop-chance-percent") / 100.0;
        if (RANDOM.nextDouble() > dropChance) return;

        ItemStack mainHand = zombie.getEquipment() != null ? zombie.getEquipment().getItemInMainHand() : null;
        if (mainHand != null && isTool(mainHand.getType())) {
            ItemStack dropItem = mainHand.clone();
            ItemMeta meta = dropItem.getItemMeta();
            if (meta instanceof Damageable damageable) {
                int maxDurability = dropItem.getType().getMaxDurability();
                int damage = ThreadLocalRandom.current().nextInt(maxDurability);
                damageable.setDamage(damage);
                dropItem.setItemMeta(meta);
            }
            zombie.getWorld().dropItemNaturally(zombie.getLocation(), dropItem);
        }
    }

    private ToolTier selectTier() {
        double netherite = Feature.ZOMBIE_TOOLS.getDouble("netherite-chance");
        double diamond = Feature.ZOMBIE_TOOLS.getDouble("diamond-chance");
        double gold = Feature.ZOMBIE_TOOLS.getDouble("gold-chance");
        double iron = Feature.ZOMBIE_TOOLS.getDouble("iron-chance");
        double wood = Feature.ZOMBIE_TOOLS.getDouble("wood-chance");
        double total = netherite + diamond + gold + iron + wood;
        double randomVal = RANDOM.nextDouble() * total;

        if (randomVal <= netherite) return ToolTier.NETHERITE;
        if (randomVal <= netherite + diamond) return ToolTier.DIAMOND;
        if (randomVal <= netherite + diamond + gold) return ToolTier.GOLDEN;
        if (randomVal <= netherite + diamond + gold + iron) return ToolTier.IRON;
        return ToolTier.WOODEN;
    }

    private void applyRandomEnchants(ItemStack item) {
        int maxEnchants = (int) Feature.ZOMBIE_TOOLS.getDouble("max-enchantments");
        int count = RANDOM.nextInt(maxEnchants) + 1;
        List<Enchantment> possibleEnchants = new ArrayList<>();
        possibleEnchants.add(Enchantment.DIG_SPEED);
        possibleEnchants.add(Enchantment.DURABILITY);
        possibleEnchants.add(Enchantment.LOOT_BONUS_BLOCKS);
        if (item.getType().name().contains("_AXE")) {
            possibleEnchants.add(Enchantment.DAMAGE_ALL);
            possibleEnchants.add(Enchantment.DAMAGE_UNDEAD);
        }
        for (int i = 0; i < count; i++) {
            if (possibleEnchants.isEmpty()) break;
            Enchantment enchant = possibleEnchants.get(RANDOM.nextInt(possibleEnchants.size()));
            int level = RANDOM.nextInt(enchant.getMaxLevel()) + enchant.getStartLevel();
            try {
                item.addUnsafeEnchantment(enchant, level);
            } catch (Exception ignored) {}
            possibleEnchants.remove(enchant);
        }
    }

    private boolean isTool(Material mat) {
        String name = mat.name();
        return name.endsWith("_AXE") || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL");
    }
}