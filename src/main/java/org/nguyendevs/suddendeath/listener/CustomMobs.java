package org.nguyendevs.suddendeath.listener;

import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.ConfigFile;
import org.nguyendevs.suddendeath.util.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CustomMobs implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void a(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        EntityType mb = entity.getType();
        if (!mb.isAlive())
            return;

        /*
         * list of worlds in which custom mobs do not spawn
         */
        if (SuddenDeath.plugin.getConfig().getStringList("custom-mobs-world-blacklist").contains(entity.getWorld().getName()))
            return;

        FileConfiguration config = new ConfigFile(mb).getConfig();
        double total = SuddenDeath.plugin.getConfig().getDouble("default-spawn-coef." + entity.getType().name());
        LinkedHashMap<String, Double> map = new LinkedHashMap<>();
        map.put("DEFAULT_KEY", total);
        for (String s : config.getKeys(false)) {
            if (!Objects.requireNonNull(config.getConfigurationSection(s)).contains("spawn-coef"))
                continue;
            total += config.getDouble(s + ".spawn-coef");
            map.put(s, total);
        }
        double index = new Random().nextDouble() * total;
        String id = "";
        for (int j = 0; j < map.size(); j++)
            if (index > ecc(map, j) && index < ecc(map, j + 1))
                id = new ArrayList<>(map.keySet()).get(j);
        if (id.isEmpty() || id.equalsIgnoreCase("DEFAULT_KEY"))
            return;

        ItemStack helmet = ItemUtils.deserialize(config.getString(id + ".equipment.helmet"));
        ItemStack chestplate = ItemUtils.deserialize(config.getString(id + ".equipment.chestplate"));
        ItemStack leggings = ItemUtils.deserialize(config.getString(id + ".equipment.leggings"));
        ItemStack boots = ItemUtils.deserialize(config.getString(id + ".equipment.boots"));
        ItemStack mainHand = ItemUtils.deserialize(config.getString(id + ".equipment.mainHand"));
        ItemStack offHand = ItemUtils.deserialize(config.getString(id + ".equipment.offHand"));
        List<String> effects = Objects.requireNonNull(config.getConfigurationSection(id)).contains("eff")
                ? new ArrayList<>(Objects.requireNonNull(config.getConfigurationSection(id + ".eff")).getKeys(false))
                : new ArrayList<>();

        String name = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(config.getString(id + ".name")));
        double hp = config.getDouble(id + ".hp");
        double atk = config.getDouble(id + ".atk");
        double ms = config.getDouble(id + ".ms");

        entity.setCustomNameVisible(true);
        entity.setCustomName(name);
        entity.setMetadata("SDCustomMob", new FixedMetadataValue(SuddenDeath.plugin, true));
        if (entity instanceof LivingEntity) {
            Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(hp);

            Objects.requireNonNull(entity.getEquipment()).setHelmet(helmet);
            entity.getEquipment().setChestplate(chestplate);
            entity.getEquipment().setLeggings(leggings);
            entity.getEquipment().setBoots(boots);
            entity.getEquipment().setItemInMainHand(mainHand);

            for (String effect : effects)
                try {
                    PotionEffectType type = PotionEffectType.getByName(effect.toUpperCase().replace("-", "_"));
                    int amplifier = config.getInt(id + ".eff." + effect);
                    assert type != null;
                    entity.addPotionEffect(new PotionEffect(type, 9999999, (amplifier > 0 ? amplifier - 1 : 0)));
                } catch (Exception ignored) {
                }

            entity.getEquipment().setItemInOffHand(offHand);
            Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(ms);
            Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(atk);
        }
    }

    private double ecc(Map<String, Double> map, double index) {
        double ecc = 0;
        if (index < 0)
            return ecc;
        for (int j = 0; j < index; j++) {
            if (index >= map.size())
                return Double.MAX_VALUE;
            ecc += new ArrayList<>(map.values()).get(j);
        }
        return ecc;
    }
}