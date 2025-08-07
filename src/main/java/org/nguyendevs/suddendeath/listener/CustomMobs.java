package org.nguyendevs.suddendeath.listener;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.ConfigFile;
import org.nguyendevs.suddendeath.util.ItemUtils;

import java.util.*;
import java.util.logging.Level;

public class CustomMobs implements Listener {
    private static final Random RANDOM = new Random();
    private static final String METADATA_KEY = "SDCustomMob";
    private static final int EFFECT_DURATION = 9999999;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getType().isAlive()) {
            return;
        }

        try {
            // Check world blacklist
            List<String> blacklist = SuddenDeath.getInstance().getConfig().getStringList("custom-mobs-world-blacklist");
            if (blacklist.contains(entity.getWorld().getName())) {
                return;
            }

            // Load configuration
            FileConfiguration config = new ConfigFile(entity.getType()).getConfig();
            double defaultSpawnCoef = SuddenDeath.getInstance().getConfig().getDouble("default-spawn-coef." + entity.getType().name(), 0.0);
            Map<String, Double> spawnCoefficients = new LinkedHashMap<>();
            spawnCoefficients.put("DEFAULT_KEY", defaultSpawnCoef);

            // Collect spawn coefficients
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section == null || !section.contains("spawn-coef")) {
                    continue;
                }
                double spawnCoef = section.getDouble("spawn-coef", 0.0);
                spawnCoefficients.put(key, spawnCoefficients.getOrDefault(key, 0.0) + spawnCoef);
            }

            // Select custom mob type
            String selectedId = selectCustomMobType(spawnCoefficients);
            if (selectedId.isEmpty() || "DEFAULT_KEY".equalsIgnoreCase(selectedId)) {
                return;
            }

            // Apply custom mob properties
            applyCustomMobProperties(entity, config, selectedId);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling CreatureSpawnEvent for entity: " + entity.getType(), e);
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

    private void applyCustomMobProperties(LivingEntity entity, FileConfiguration config, String id) {
        try {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "Configuration section not found for ID: " + id + " for entity: " + entity.getType());
                return;
            }

            // Set custom name
            String name = ChatColor.translateAlternateColorCodes('&', section.getString("name", ""));
            if (!name.isEmpty() && !name.equals("None") && !name.equals("none")) {
                entity.setCustomName(name);
                entity.setCustomNameVisible(true);
            } else {
                entity.setCustomNameVisible(false);
            }

            // Set metadata
            entity.setMetadata(METADATA_KEY, new FixedMetadataValue(SuddenDeath.getInstance(), true));

            // Set equipment
            EntityEquipment equipment = entity.getEquipment();
            if (equipment == null) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "EntityEquipment is null for entity: " + entity.getType());
                return;
            }
            equipment.setHelmet(ItemUtils.deserialize(section.getString("equipment.helmet")));
            equipment.setChestplate(ItemUtils.deserialize(section.getString("equipment.chestplate")));
            equipment.setLeggings(ItemUtils.deserialize(section.getString("equipment.leggings")));
            equipment.setBoots(ItemUtils.deserialize(section.getString("equipment.boots")));
            equipment.setItemInMainHand(ItemUtils.deserialize(section.getString("equipment.mainHand")));
            equipment.setItemInOffHand(ItemUtils.deserialize(section.getString("equipment.offHand")));

            // Set attributes
            setAttribute(entity, Attribute.GENERIC_MAX_HEALTH, section.getDouble("hp", 20.0));
            setAttribute(entity, Attribute.GENERIC_MOVEMENT_SPEED, section.getDouble("ms", 0.2));
            setAttribute(entity, Attribute.GENERIC_ATTACK_DAMAGE, section.getDouble("atk", 2.0));

            // Apply potion effects
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

    private void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        try {
            AttributeInstance attributeInstance = entity.getAttribute(attribute);
            if (attributeInstance != null) {
                attributeInstance.setBaseValue(value);
            } else {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "Attribute " + attribute + " not found for entity: " + entity.getType());
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error setting attribute " + attribute + " for entity: " + entity.getType(), e);
        }
    }

    private void applyPotionEffect(LivingEntity entity, String effectName, int amplifier) {
        try {
            PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase().replace("-", "_"));
            if (type == null) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "Invalid potion effect type: " + effectName + " for entity: " + entity.getType());
                return;
            }
            entity.addPotionEffect(new PotionEffect(type, EFFECT_DURATION, Math.max(amplifier - 1, 0)));
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying potion effect " + effectName + " for entity: " + entity.getType(), e);
        }
    }
}