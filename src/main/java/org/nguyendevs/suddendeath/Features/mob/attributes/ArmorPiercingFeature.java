package org.nguyendevs.suddendeath.Features.mob.attributes;

import org.bukkit.EntityEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@SuppressWarnings("deprecation")

public class ArmorPiercingFeature extends AbstractFeature {

    private final Map<UUID, String> pendingDeathMessages = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "Armor Piercing";
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        if (!(event.getDamager() instanceof LivingEntity damager))
            return;
        if (event.getDamage() <= 0 || player.hasMetadata("NPC"))
            return;
        if (!Feature.ARMOR_PIERCING.isEnabled(player))
            return;

        try {
            double chance = Feature.ARMOR_PIERCING.getDouble(
                    "chance-percent." + damager.getType().name());

            if (RANDOM.nextDouble() * 100 < chance) {
                performTrueDamage(event, player, damager);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error in ArmorPiercingFeature.onEntityDamageByEntity", e);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (pendingDeathMessages.containsKey(player.getUniqueId())) {
            pendingDeathMessages.remove(player.getUniqueId());

            if (!pendingDeathMessages.containsKey(player.getUniqueId())) {
                event.setDeathMessage(null);
            }
        }

    }

    private void performTrueDamage(EntityDamageByEntityEvent event, Player player, LivingEntity damager) {
        double rawDamage = getRawDamage(damager);
        event.setCancelled(true);
        double currentHealth = player.getHealth();
        double newHealth = currentHealth - rawDamage;

        if (Feature.ARMOR_PIERCING.getBoolean("visual-particles")) {
            player.getWorld().spawnParticle(Particle.CRIT,
                    player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                    player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
        }

        if (Feature.ARMOR_PIERCING.getBoolean("visual-sound")) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        }
        Vector knockback = player.getLocation().toVector().subtract(damager.getLocation().toVector()).normalize();
        if (Double.isFinite(knockback.getX()) && Double.isFinite(knockback.getZ())) {
            player.setVelocity(knockback.multiply(0.4).setY(0.3));
        }

        player.playEffect(EntityEffect.HURT);
        if (newHealth <= 0) {
            String mobName = damager.getCustomName() != null
                    ? damager.getCustomName()
                    : formatMobName(damager.getType().name());
            pendingDeathMessages.put(player.getUniqueId(), mobName);

            player.setHealth(0);
        } else {
            player.setHealth(newHealth);
        }
    }

    private double getRawDamage(LivingEntity mob) {
        AttributeInstance damageAttr = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        return damageAttr != null ? damageAttr.getValue() : 2.0;
    }

    private String formatMobName(String mobType) {
        String[] words = mobType.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            formatted.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }

        return formatted.toString();
    }

}