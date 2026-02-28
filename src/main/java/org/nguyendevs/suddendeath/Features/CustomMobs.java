package org.nguyendevs.suddendeath.Features;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.ConfigFile;
import org.nguyendevs.suddendeath.Utils.ItemUtils;

import java.util.*;
import java.util.logging.Level;

public class CustomMobs implements Listener {

    private static final Random RANDOM = new Random();
    private static final String METADATA_KEY = "SDCustomMob";
    private static final int EFFECT_DURATION = 9999999;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getType().isAlive() || entity.hasMetadata("SDCommandSpawn")) return;

        try {
            List<String> blacklist = SuddenDeath.getInstance().getConfigManager().getMainConfig().getConfig()
                    .getStringList("custom-mobs-world-blacklist");
            if (blacklist.contains(entity.getWorld().getName())) return;

            ConfigFile mobConfigFile = SuddenDeath.getInstance().getConfigManager().getMobConfig(entity.getType());
            if (mobConfigFile == null) return;

            FileConfiguration config = mobConfigFile.getConfig();

            double defaultSpawnCoef = SuddenDeath.getInstance().getConfigManager().getMainConfig().getConfig()
                    .getDouble("default-spawn-coef." + entity.getType().name(), 0.0);

            Map<String, Double> spawnCoefficients = new LinkedHashMap<>();
            spawnCoefficients.put("DEFAULT_KEY", defaultSpawnCoef);

            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section == null || !section.contains("spawn-coef")) continue;
                spawnCoefficients.merge(key, section.getDouble("spawn-coef", 0.0), Double::sum);
            }

            String selectedId = selectCustomMobType(spawnCoefficients);
            if (selectedId.isEmpty() || "DEFAULT_KEY".equalsIgnoreCase(selectedId)) return;

            ConfigurationSection selectedSection = config.getConfigurationSection(selectedId);
            if (selectedSection != null) {
                String spawnType = selectedSection.getString("spawn-type", "All");
                if (!isSpawnTypeAllowed(spawnType, event.getSpawnReason())) return;
            }

            applyCustomMobProperties(entity, config, selectedId);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling CreatureSpawnEvent for entity: " + entity.getType(), e);
        }
    }

    private boolean isSpawnTypeAllowed(String spawnType, CreatureSpawnEvent.SpawnReason reason) {
        return switch (spawnType) {
            case "Spawners" -> reason == CreatureSpawnEvent.SpawnReason.SPAWNER
                    || reason == CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER;
            case "Natural Only" -> reason != CreatureSpawnEvent.SpawnReason.SPAWNER
                    && reason != CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER
                    && reason != CreatureSpawnEvent.SpawnReason.CUSTOM
                    && reason != CreatureSpawnEvent.SpawnReason.COMMAND;
            case "Spawner Only", "Regular Spawner Only" -> reason == CreatureSpawnEvent.SpawnReason.SPAWNER;
            case "Trial Spawner Only" -> reason == CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER;
            default -> true;
        };
    }

    private String selectCustomMobType(Map<String, Double> spawnCoefficients) {
        double total = spawnCoefficients.values().stream().mapToDouble(Double::doubleValue).sum();
        double index = RANDOM.nextDouble() * total;
        double cumulative = 0.0;
        for (Map.Entry<String, Double> entry : spawnCoefficients.entrySet()) {
            cumulative += entry.getValue();
            if (index <= cumulative) return entry.getKey();
        }
        return "";
    }

    public static void applyCustomMobProperties(LivingEntity entity, FileConfiguration config, String id) {
        try {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) return;

            String name = section.getString("name", "");
            if (!name.isEmpty() && !name.equalsIgnoreCase("none")) {
                entity.customName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacyAmpersand().deserialize(name));
                entity.setCustomNameVisible(true);
            } else {
                entity.setCustomNameVisible(false);
            }

            entity.setMetadata(METADATA_KEY, new FixedMetadataValue(SuddenDeath.getInstance(), true));

            EntityEquipment equipment = entity.getEquipment();
            if (equipment != null) {
                equipment.setHelmet(ItemUtils.deserialize(section.getString("equipment.helmet")));
                equipment.setChestplate(ItemUtils.deserialize(section.getString("equipment.chestplate")));
                equipment.setLeggings(ItemUtils.deserialize(section.getString("equipment.leggings")));
                equipment.setBoots(ItemUtils.deserialize(section.getString("equipment.boots")));
                equipment.setItemInMainHand(ItemUtils.deserialize(section.getString("equipment.mainHand")));
                equipment.setItemInOffHand(ItemUtils.deserialize(section.getString("equipment.offHand")));
            }

            setAttribute(entity, Attribute.GENERIC_MAX_HEALTH, section.getDouble("hp", 20.0));
            setAttribute(entity, Attribute.GENERIC_MOVEMENT_SPEED, section.getDouble("ms", 0.2));
            setAttribute(entity, Attribute.GENERIC_ATTACK_DAMAGE, section.getDouble("atk", 2.0));

            ConfigurationSection effectsSection = section.getConfigurationSection("eff");
            if (effectsSection != null) {
                for (String effectKey : effectsSection.getKeys(false)) {
                    applyPotionEffect(entity, effectKey, effectsSection.getInt(effectKey, 1));
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying custom mob properties for ID: " + id + " for entity: " + entity.getType(), e);
        }
    }

    private static void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        try {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) instance.setBaseValue(value);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error setting attribute " + attribute + " for entity: " + entity.getType(), e);
        }
    }

    private static void applyPotionEffect(LivingEntity entity, String effectName, int amplifier) {
        try {
            PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase().replace("-", "_"));
            if (type == null) return;
            entity.addPotionEffect(new PotionEffect(type, EFFECT_DURATION, Math.max(amplifier - 1, 0)));
        } catch (Exception ignored) {}
    }
}