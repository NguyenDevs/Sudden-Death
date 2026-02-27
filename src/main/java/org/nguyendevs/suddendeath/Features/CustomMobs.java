package org.nguyendevs.suddendeath.Features;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@SuppressWarnings("deprecation")

public class CustomMobs implements Listener {
    private static final Random RANDOM = new Random();
    private static final String METADATA_KEY = "SDCustomMob";
    private static final int EFFECT_DURATION = 9999999;

    // Config caching
    private static final Map<EntityType, Map<String, Double>> SPAWN_COEFS_CACHE = new ConcurrentHashMap<>();
    private static final Map<EntityType, Map<String, String>> SPAWN_TYPES_CACHE = new ConcurrentHashMap<>();

    public static void clearCache() {
        SPAWN_COEFS_CACHE.clear();
        SPAWN_TYPES_CACHE.clear();
    }

    /** Null on servers older than 1.21 where TRIAL_SPAWNER doesn't exist. */
    private static final CreatureSpawnEvent.SpawnReason TRIAL_SPAWNER_REASON;
    static {
        CreatureSpawnEvent.SpawnReason found = null;
        try {
            found = CreatureSpawnEvent.SpawnReason.valueOf("TRIAL_SPAWNER");
        } catch (IllegalArgumentException ignored) {
        }
        TRIAL_SPAWNER_REASON = found;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getType().isAlive()) {
            return;
        }

        if (entity.hasMetadata("SDCommandSpawn")) {
            return;
        }

        try {
            List<String> blacklist = SuddenDeath.getInstance().getConfigManager().getMainConfig().getConfig()
                    .getStringList("custom-mobs-world-blacklist");
            if (blacklist.contains(entity.getWorld().getName())) {
                return;
            }

            ConfigFile mobConfigFile = SuddenDeath.getInstance().getConfigManager().getMobConfig(entity.getType());
            if (mobConfigFile == null)
                return;

            Map<String, Double> spawnCoefficients = SPAWN_COEFS_CACHE.computeIfAbsent(entity.getType(), type -> {
                Map<String, Double> map = new LinkedHashMap<>();
                double defaultSpawnCoef = SuddenDeath.getInstance().getConfigManager().getMainConfig().getConfig()
                        .getDouble("default-spawn-coef." + type.name(), 0.0);
                map.put("DEFAULT_KEY", defaultSpawnCoef);

                FileConfiguration c = mobConfigFile.getConfig();
                for (String key : c.getKeys(false)) {
                    ConfigurationSection section = c.getConfigurationSection(key);
                    if (section == null || !section.contains("spawn-coef")) {
                        continue;
                    }
                    double spawnCoef = section.getDouble("spawn-coef", 0.0);
                    map.put(key, map.getOrDefault(key, 0.0) + spawnCoef);
                }
                return map;
            });

            String selectedId = selectCustomMobType(spawnCoefficients);
            if (selectedId.isEmpty() || "DEFAULT_KEY".equalsIgnoreCase(selectedId)) {
                return;
            }

            Map<String, String> spawnTypes = SPAWN_TYPES_CACHE.computeIfAbsent(entity.getType(), type -> {
                Map<String, String> map = new HashMap<>();
                FileConfiguration c = mobConfigFile.getConfig();
                for (String key : c.getKeys(false)) {
                    ConfigurationSection section = c.getConfigurationSection(key);
                    if (section != null) {
                        map.put(key, section.getString("spawn-type", "All"));
                    }
                }
                return map;
            });

            String spawnType = spawnTypes.getOrDefault(selectedId, "All");
            CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
            if (!isSpawnTypeAllowed(spawnType, reason)) {
                return;
            }

            FileConfiguration config = mobConfigFile.getConfig();
            applyCustomMobProperties(entity, config, selectedId);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling CreatureSpawnEvent for entity: " + entity.getType(), e);
        }
    }

    private boolean isSpawnTypeAllowed(String spawnType, CreatureSpawnEvent.SpawnReason reason) {
        switch (spawnType) {
            case "Spawners":
                return reason == CreatureSpawnEvent.SpawnReason.SPAWNER
                        || (TRIAL_SPAWNER_REASON != null && reason == TRIAL_SPAWNER_REASON);
            case "Natural Only":
                return reason != CreatureSpawnEvent.SpawnReason.SPAWNER
                        && (TRIAL_SPAWNER_REASON == null || reason != TRIAL_SPAWNER_REASON)
                        && reason != CreatureSpawnEvent.SpawnReason.CUSTOM
                        && reason != CreatureSpawnEvent.SpawnReason.COMMAND;
            case "Spawner Only": // 1.20.4: no trial spawner
            case "Regular Spawner Only":
                return reason == CreatureSpawnEvent.SpawnReason.SPAWNER;
            case "Trial Spawner Only":
                return TRIAL_SPAWNER_REASON != null && reason == TRIAL_SPAWNER_REASON;
            default: // "All"
                return true;
        }
    }

    private String selectCustomMobType(Map<String, Double> spawnCoefficients) {
        double total = spawnCoefficients.values().stream().mapToDouble(Double::doubleValue).sum();
        double index = RANDOM.nextDouble() * total;
        double cumulative = 0.0;
        List<String> keys = new ArrayList<>(spawnCoefficients.keySet());

        for (int i = 0; i < keys.size(); i++) {
            cumulative += spawnCoefficients.get(keys.get(i));
            if (index <= cumulative) {
                return keys.get(i);
            }
        }
        return "";
    }

    public static void applyCustomMobProperties(LivingEntity entity, FileConfiguration config, String id) {
        try {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                return;
            }

            String name = ChatColor.translateAlternateColorCodes('&', section.getString("name", ""));
            if (!name.isEmpty() && !name.equals("None") && !name.equals("none")) {
                entity.setCustomName(name);
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
            AttributeInstance attributeInstance = entity.getAttribute(attribute);
            if (attributeInstance != null) {
                attributeInstance.setBaseValue(value);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error setting attribute " + attribute + " for entity: " + entity.getType(), e);
        }
    }

    private static void applyPotionEffect(LivingEntity entity, String effectName, int amplifier) {
        try {
            PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase().replace("-", "_"));
            if (type == null)
                return;
            entity.addPotionEffect(new PotionEffect(type, EFFECT_DURATION, Math.max(amplifier - 1, 0)));
        } catch (Exception ignored) {
        }
    }
}