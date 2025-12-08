package org.nguyendevs.suddendeath.listener;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.NoInteractItemEntity;
import org.nguyendevs.suddendeath.player.ExperienceCalculator;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.*;
import java.util.logging.Level;

public class GeneralEntityListener implements Listener {
    private static final Random RANDOM = new Random();
    private final Set<UUID> allowed = new HashSet<>();
    private final NamespacedKey bounces;

    public GeneralEntityListener(SuddenDeath plugin) {
        this.bounces = new NamespacedKey(plugin, "bounces");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().hasMetadata("NPC")) return;
        Entity entity = event.getEntity();
        if (Feature.CREEPER_REVENGE.isEnabled(entity) && entity instanceof Creeper creeper && RANDOM.nextDouble() <= Feature.CREEPER_REVENGE.getDouble("chance-percent") / 100.0) {
            float power = creeper.isPowered() ? 6.0f : 3.0f;
            new BukkitRunnable() { @Override public void run() { entity.getWorld().createExplosion(entity.getLocation(), power); } }.runTaskLater(SuddenDeath.getInstance(), 15);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC") || event.getDamager().hasMetadata("NPC")) return;
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof LivingEntity entity && Feature.MOB_CRITICAL_STRIKES.isEnabled(player) && RANDOM.nextDouble() <= Feature.MOB_CRITICAL_STRIKES.getDouble("crit-chance." + entity.getType().name()) / 100.0) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 2.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 32, 0, 0, 0, 0.5);
            event.setDamage(event.getDamage() * (1 + Feature.MOB_CRITICAL_STRIKES.getDouble("damage-percent") / 100.0));
        }
        if (event.getEntity() instanceof Player player && (event.getDamager() instanceof Slime || event.getDamager() instanceof MagmaCube) && Feature.THIEF_SLIMES.isEnabled(player) && RANDOM.nextDouble() <= Feature.THIEF_SLIMES.getDouble("chance-percent") / 100.0) {
            int exp = (int) Feature.THIEF_SLIMES.getDouble("exp");
            ExperienceCalculator calculator = new ExperienceCalculator(player);
            calculator.setTotalExperience(Math.max(calculator.getTotalExperience() - exp, 0));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0f, 1.0f);
        }
        if (event.getEntity() instanceof Slime slime && event.getDamager() instanceof Player player && Feature.POISONED_SLIMES.isEnabled(slime) && RANDOM.nextDouble() <= Feature.POISONED_SLIMES.getDouble("chance-percent") / 100.0) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.POISON, (int) (Feature.POISONED_SLIMES.getDouble("duration") * 20), (int) Feature.POISONED_SLIMES.getDouble("amplifier")));
        }
        if (isEnderEntity(event.getEntity()) && event.getDamager() instanceof Player player && Feature.ENDER_POWER.isEnabled(event.getEntity()) && RANDOM.nextDouble() <= Feature.ENDER_POWER.getDouble("chance-percent") / 100.0) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, (int) (Feature.ENDER_POWER.getDouble("duration") * 20), 0));
            player.teleport(player.getLocation().setDirection(player.getLocation().getDirection().multiply(-1)));
        }
        if (isNetherEntity(event.getEntity()) && event.getDamager() instanceof Player player && Feature.NETHER_SHIELD.isEnabled(event.getEntity()) && RANDOM.nextDouble() <= Feature.NETHER_SHIELD.getDouble("chance-percent") / 100.0) {
            event.setCancelled(true);
            applyNetherShield((LivingEntity) event.getEntity(), player, event.getDamage());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && !(entity instanceof Player) && Feature.TANKY_MONSTERS.isEnabled(entity)) {
            double reduction = Feature.TANKY_MONSTERS.getDouble("dmg-reduction-percent." + entity.getType().name()) / 100.0;
            event.setDamage(event.getDamage() * (1 - reduction));
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity().hasMetadata("NPC") || !(event.getEntity() instanceof Monster monster)) return;
        if (Feature.QUICK_MOBS.isEnabled(monster)) {
            AttributeInstance speed = monster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(speed.getBaseValue() * (1 + Feature.QUICK_MOBS.getDouble("additional-ms-percent." + monster.getType().name()) / 100.0));
        }
        if (Feature.FORCE_OF_THE_UNDEAD.isEnabled(monster)) {
            AttributeInstance dmg = monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (dmg != null) dmg.setBaseValue(dmg.getBaseValue() * (1 + Feature.FORCE_OF_THE_UNDEAD.getDouble("additional-ad-percent." + monster.getType().name()) / 100.0));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        Player player = event.getPlayer();
        if (!allowed.contains(player.getUniqueId())) { event.setCancelled(true); return; }
        allowed.remove(player.getUniqueId());
        if (Feature.PHYSIC_ENDER_PEARL.isEnabled(player.getWorld())) {
            player.getWorld().playSound(event.getFrom(), Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 1.0f);
            player.getWorld().playSound(event.getTo(), Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntityType() != EntityType.ENDER_PEARL || !(event.getEntity().getShooter() instanceof Player player)) return;
        if (!Feature.PHYSIC_ENDER_PEARL.isEnabled(player.getWorld())) {
            allowed.add(player.getUniqueId());
            event.getEntity().getPersistentDataContainer().set(bounces, PersistentDataType.INTEGER, (int) Feature.PHYSIC_ENDER_PEARL.getDouble("max-bounces") + 1);
        } else {
            event.getEntity().getPersistentDataContainer().set(bounces, PersistentDataType.INTEGER, 0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntityType() != EntityType.ENDER_PEARL) return;
        EnderPearl oldPearl = (EnderPearl) event.getEntity();
        PersistentDataContainer data = oldPearl.getPersistentDataContainer();
        int bounceCount = data.has(bounces, PersistentDataType.INTEGER) ? data.get(bounces, PersistentDataType.INTEGER) : 0;
        double maxBounces = Feature.PHYSIC_ENDER_PEARL.getDouble("max-bounces");
        if (bounceCount >= maxBounces) { if (oldPearl.getShooter() instanceof Player p) allowed.add(p.getUniqueId()); return; }

        Vector velocity = oldPearl.getVelocity();
        Vector normal = event.getHitEntity() != null ? oldPearl.getLocation().toVector().subtract(event.getHitEntity().getBoundingBox().getCenter()).normalize() : (event.getHitBlockFace() != null ? event.getHitBlockFace().getDirection() : new Vector(0,1,0));
        Vector reflection = velocity.subtract(normal.multiply(2 * velocity.dot(normal))).multiply(Feature.PHYSIC_ENDER_PEARL.getDouble("bounciness"));

        if (reflection.lengthSquared() < 0.01) { if (oldPearl.getShooter() instanceof Player p) allowed.add(p.getUniqueId()); return; }
        EnderPearl newPearl = (EnderPearl) oldPearl.getWorld().spawn(oldPearl.getLocation(), EnderPearl.class);
        newPearl.setShooter(oldPearl.getShooter());
        newPearl.setVelocity(reflection);
        newPearl.getPersistentDataContainer().set(bounces, PersistentDataType.INTEGER, bounceCount + 1);
        oldPearl.remove();
    }

    private void applyNetherShield(LivingEntity entity, Player player, double damage) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 0.5f);
        player.setVelocity(player.getEyeLocation().getDirection().multiply(-0.6).setY(0.3));
        player.setFireTicks((int) Feature.NETHER_SHIELD.getDouble("burn-duration"));
        Utils.damage(player, damage * Feature.NETHER_SHIELD.getDouble("dmg-reflection-percent") / 100.0, true);
    }

    private boolean isNetherEntity(Entity entity) { return entity instanceof PigZombie || entity instanceof MagmaCube || entity instanceof Blaze || entity instanceof Piglin || entity instanceof Strider || entity instanceof Hoglin || entity instanceof Zoglin || entity instanceof PiglinBrute; }
    private boolean isEnderEntity(Entity entity) { return entity instanceof Enderman || entity.getType().name().equalsIgnoreCase("SHULKER") || entity instanceof Endermite || entity instanceof EnderDragon; }
}