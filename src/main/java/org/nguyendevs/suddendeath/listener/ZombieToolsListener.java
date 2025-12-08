package org.nguyendevs.suddendeath.listener;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.nguyendevs.suddendeath.util.Feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ZombieToolsListener implements Listener {
    private static final Random random = new Random();

    // Enum nội bộ để quản lý Tier và Material
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
        SWORD("_SWORD"), // Thêm kiếm nếu muốn, nhưng prompt chỉ yêu cầu rìu, xẻng, cúp
        AXE("_AXE"),
        PICKAXE("_PICKAXE"),
        SHOVEL("_SHOVEL");

        final String suffix;

        ToolType(String suffix) {
            this.suffix = suffix;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onZombieSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!Feature.ZOMBIE_TOOLS.isEnabled(zombie)) return;

        // Kiểm tra tỉ lệ spawn chung
        double spawnChance = Feature.ZOMBIE_TOOLS.getDouble("chance-percent") / 100.0;
        if (random.nextDouble() > spawnChance) return;

        // Chọn Tier công cụ
        ToolTier tier = selectTier();
        if (tier == null) return; // Fallback an toàn

        // Chọn loại công cụ (Rìu, Xẻng, Cúp)
        ToolType type = ToolType.values()[random.nextInt(ToolType.values().length)];

        // Bỏ qua Kiếm nếu chỉ muốn công cụ lao động
        if (type == ToolType.SWORD) type = ToolType.AXE;

        Material mat = Material.getMaterial(tier.prefix + type.suffix);
        if (mat == null) return;

        ItemStack tool = new ItemStack(mat);

        // Xử lý phù phép
        double enchantChance = Feature.ZOMBIE_TOOLS.getDouble("enchantment-chance") / 100.0;
        if (random.nextDouble() <= enchantChance) {
            applyRandomEnchants(tool);
        }

        // Trang bị cho Zombie
        if (zombie.getEquipment() != null) {
            zombie.getEquipment().setItemInMainHand(tool);
            // Đặt tỉ lệ rơi mặc định là 0 để chúng ta tự xử lý rơi đồ với độ bền ngẫu nhiên ở sự kiện Death
            zombie.getEquipment().setItemInMainHandDropChance(0.0f);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onZombieDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!Feature.ZOMBIE_TOOLS.isEnabled(zombie)) return;

        // Kiểm tra tỉ lệ rơi đồ
        double dropChance = Feature.ZOMBIE_TOOLS.getDouble("drop-chance-percent") / 100.0;
        if (random.nextDouble() > dropChance) return;

        ItemStack mainHand = zombie.getEquipment() != null ? zombie.getEquipment().getItemInMainHand() : null;

        // Kiểm tra xem item trên tay có phải là công cụ (Tool) hay không
        if (mainHand != null && isTool(mainHand.getType())) {
            ItemStack dropItem = mainHand.clone();

            // Random độ bền
            ItemMeta meta = dropItem.getItemMeta();
            if (meta instanceof Damageable damageable) {
                int maxDurability = dropItem.getType().getMaxDurability();
                // Random lượng damage đã nhận (từ 0 đến maxDurability - 1)
                // Damage càng cao = độ bền còn lại càng thấp
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
        double randomVal = random.nextDouble() * total;

        if (randomVal <= netherite) return ToolTier.NETHERITE;
        if (randomVal <= netherite + diamond) return ToolTier.DIAMOND;
        if (randomVal <= netherite + diamond + gold) return ToolTier.GOLDEN;
        if (randomVal <= netherite + diamond + gold + iron) return ToolTier.IRON;
        return ToolTier.WOODEN;
    }

    private void applyRandomEnchants(ItemStack item) {
        int maxEnchants = (int) Feature.ZOMBIE_TOOLS.getDouble("max-enchantments");
        int count = random.nextInt(maxEnchants) + 1;

        // Danh sách các enchant phù hợp cho tools (cơ bản)
        List<Enchantment> possibleEnchants = new ArrayList<>();
        possibleEnchants.add(Enchantment.DIG_SPEED);
        possibleEnchants.add(Enchantment.DURABILITY);
        possibleEnchants.add(Enchantment.DAMAGE_ALL); // Cho Rìu
        possibleEnchants.add(Enchantment.LOOT_BONUS_BLOCKS);

        // Nếu là rìu thì thêm enchant xác thương
        if (item.getType().name().contains("_AXE")) {
            possibleEnchants.add(Enchantment.DAMAGE_ALL);
            possibleEnchants.add(Enchantment.DAMAGE_UNDEAD);
        }

        for (int i = 0; i < count; i++) {
            if (possibleEnchants.isEmpty()) break;
            Enchantment enchant = possibleEnchants.get(random.nextInt(possibleEnchants.size()));

            // Random level từ StartLevel đến MaxLevel của enchant đó
            int level = random.nextInt(enchant.getMaxLevel()) + enchant.getStartLevel();

            try {
                item.addUnsafeEnchantment(enchant, level);
            } catch (Exception ignored) {}

            // Xóa enchant đã dùng để không trùng lặp (đơn giản hóa logic conflict)
            possibleEnchants.remove(enchant);
        }
    }

    private boolean isTool(Material mat) {
        String name = mat.name();
        return name.endsWith("_AXE") || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL");
    }
}