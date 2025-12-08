package org.nguyendevs.suddendeath.listener;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.NoInteractItemEntity;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.Random;
import java.util.logging.Level;

public class MobAbilityListener implements Listener {
    private static final Random RANDOM = new Random();
    private final NamespacedKey totemUsed;

    public MobAbilityListener(SuddenDeath plugin) {
        this.totemUsed = new NamespacedKey(plugin, "totem_used");
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.EVERBURNING_BLAZES.isEnabled(world)) {
                            for (Blaze b : world.getEntitiesByClass(Blaze.class)) if (b.getTarget() instanceof Player) Loops.loop3s_blaze(b);
                        }
                        if (Feature.HOMING_FLAME_BARRAGE.isEnabled(world)) {
                            for (Blaze b : world.getEntitiesByClass(Blaze.class)) if (b.getTarget() instanceof Player) applyHomingFlameBarrage(b);
                        }
                        if (Feature.ANGRY_SPIDERS.isEnabled(world) || Feature.LEAPING_SPIDERS.isEnabled(world)) {
                            for (Spider s : world.getEntitiesByClass(Spider.class)) if (s.getTarget() instanceof Player) Loops.loop3s_spider(s);
                        }
                        if (Feature.SPIDER_WEB.isEnabled(world)) {
                            for (CaveSpider cs : world.getEntitiesByClass(CaveSpider.class)) if (cs.getTarget() instanceof Player) applyCaveSpiderWeb(cs);
                        }
                        if (Feature.WITCH_SCROLLS.isEnabled(world)) {
                            world.getEntitiesByClass(Witch.class).forEach(Loops::loop4s_witch);
                        }
                        if (Feature.ABYSSAL_VORTEX.isEnabled(world)) {
                            for (Guardian g : world.getEntitiesByClass(Guardian.class)) if (g.getTarget() instanceof Player) applyAbyssalVortex(g);
                        }
                        if (Feature.BREEZE_DASH.isEnabled(world)) {
                            for (Breeze b : world.getEntitiesByClass(Breeze.class)) if (b.getTarget() instanceof Player) applyBreezeDash(b);
                        }
                        if (Feature.IMMORTAL_EVOKER.isEnabled(world)) {
                            for (Evoker e : world.getEntitiesByClass(Evoker.class)) {
                                if (e.getTarget() instanceof Player && e.getPersistentDataContainer().getOrDefault(totemUsed, PersistentDataType.BYTE, (byte)0) == 1) applyImmortalEvokerFangs(e);
                            }
                        }
                    }
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Mob Ability loops", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) return;
        if (event.getEntity() instanceof Witch witch && Feature.WITCH_SCROLLS.isEnabled(witch) && RANDOM.nextDouble() <= Feature.WITCH_SCROLLS.getDouble("chance-percent") / 100.0) {
            event.setCancelled(true);
            applyWitchScrollsEffect(witch);
        }
        if (event.getEntity() instanceof Evoker evoker && Feature.IMMORTAL_EVOKER.isEnabled(evoker) && evoker.getHealth() - event.getFinalDamage() <= 0 && RANDOM.nextDouble() <= Feature.IMMORTAL_EVOKER.getDouble("chance-percent") / 100.0) {
            event.setCancelled(true);
            evoker.setHealth(evoker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
            evoker.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, (int) Feature.IMMORTAL_EVOKER.getDouble("resistance-amplifier") - 1));
            evoker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 2));
            evoker.getPersistentDataContainer().set(totemUsed, PersistentDataType.BYTE, (byte) 1);
            evoker.getWorld().playSound(evoker.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            evoker.getWorld().spawnParticle(Particle.TOTEM, evoker.getLocation().add(0, 1, 0), 100, 0.7, 0.7, 0.7, 0.3);
        }
    }

    private void applyHomingFlameBarrage(Blaze blaze) {
        if (RANDOM.nextDouble() > Feature.HOMING_FLAME_BARRAGE.getDouble("chance-percent") / 100.0) return;
        Player target = (Player) blaze.getTarget();
        int amount = (int) Feature.HOMING_FLAME_BARRAGE.getDouble("shoot-amount");
        double damage = Feature.HOMING_FLAME_BARRAGE.getDouble("damage");
        blaze.getWorld().playSound(blaze.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        Location blazeEye = blaze.getEyeLocation();
        Vector initDir = target.getEyeLocation().subtract(blazeEye).toVector().normalize();
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= amount || blaze.isDead() || !target.isOnline()) { cancel(); return; }
                new BukkitRunnable() {
                    int ticks = 0;
                    Location cur = blazeEye.clone();
                    @Override
                    public void run() {
                        if (ticks >= 80 || blaze.isDead() || !target.isOnline()) { cancel(); return; }
                        cur.add(target.getEyeLocation().subtract(cur).toVector().normalize().multiply(0.8));
                        blaze.getWorld().spawnParticle(Particle.FLAME, cur, 1, 0, 0, 0, 0);
                        if (cur.distanceSquared(target.getEyeLocation()) < 1.0) {
                            if (!Utils.hasCreativeGameMode(target)) {
                                Utils.damage(target, damage, true);
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 0.5f);
                            }
                            cancel();
                        }
                        ticks++;
                    }
                }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
                count++;
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 5);
    }

    private void applyCaveSpiderWeb(CaveSpider spider) {
        if (RANDOM.nextDouble() > Feature.SPIDER_WEB.getDouble("chance-percent") / 100.0) return;
        Player target = (Player) spider.getTarget();
        int amount = (int) Feature.SPIDER_WEB.getDouble("amount-per-shoot");
        if (amount <= 2) shootSingleWeb(spider, target, amount);
        else shootContinuousWebs(spider, target, amount);
    }

    private void shootSingleWeb(CaveSpider spider, Player target, int amount) {
        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
        NoInteractItemEntity item = new NoInteractItemEntity(spider.getLocation().add(0, 1, 0), new ItemStack(Material.COBWEB));
        item.getEntity().setVelocity(target.getEyeLocation().subtract(spider.getLocation().add(0, 1, 0)).toVector().normalize());
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                t++;
                if (t > 20 || item.getEntity().isDead()) { item.close(); cancel(); return; }
                item.getEntity().getWorld().spawnParticle(Particle.CRIT, item.getEntity().getLocation(), 0);
                for (Entity e : item.getEntity().getNearbyEntities(1, 1, 1)) {
                    if (e instanceof Player p) { item.close(); placeCobwebsAroundPlayer(p, amount); cancel(); return; }
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
    }

    private void shootContinuousWebs(CaveSpider spider, Player target, int total) {
        new BukkitRunnable() {
            int shots = 0;
            @Override
            public void run() {
                if (shots >= total || spider.isDead() || spider.getTarget() != target) { cancel(); return; }
                shootSingleWeb(spider, target, 1);
                shots++;
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 3);
    }

    private void placeCobwebsAroundPlayer(Player player, int amount) {
        int placed = 0;
        for (int i = 0; i < 10 && placed < amount; i++) {
            Location loc = player.getLocation().add(RANDOM.nextInt(3)-1, RANDOM.nextInt(3)-1, RANDOM.nextInt(3)-1);
            if (loc.getBlock().getType() == Material.AIR) { loc.getBlock().setType(Material.COBWEB); placed++; }
        }
    }

    private void applyAbyssalVortex(Guardian guardian) {
        if (RANDOM.nextDouble() > Feature.ABYSSAL_VORTEX.getDouble("chance-percent") / 100.0) return;
        Player target = (Player) guardian.getTarget();
        guardian.setAI(false);
        guardian.setInvulnerable(true);
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= Feature.ABYSSAL_VORTEX.getDouble("duration") * 20 || !guardian.isValid()) { guardian.setAI(true); guardian.setInvulnerable(false); cancel(); return; }
                Vector dir = guardian.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
                target.setVelocity(dir.multiply(Feature.ABYSSAL_VORTEX.getDouble("strength")));
                ticks++;
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
    }

    private void applyBreezeDash(Breeze breeze) {
        if (RANDOM.nextDouble() > Feature.BREEZE_DASH.getDouble("chance-percent") / 100.0) return;
        Player target = (Player) breeze.getTarget();
        breeze.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int)(Feature.BREEZE_DASH.getDouble("duration")*20), (int)Feature.BREEZE_DASH.getDouble("amplifier")));
        new BukkitRunnable() {
            int shots = 0;
            @Override
            public void run() {
                if (shots >= Feature.BREEZE_DASH.getDouble("shoot-amount")) { cancel(); return; }
                WindCharge wc = (WindCharge) breeze.getWorld().spawnEntity(breeze.getLocation().add(0,1,0), EntityType.WIND_CHARGE);
                wc.setVelocity(target.getLocation().subtract(breeze.getLocation()).toVector().normalize().multiply(1.5));
                shots++;
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 10);
    }

    private void applyImmortalEvokerFangs(Evoker evoker) {
        Player target = (Player) evoker.getTarget();
        Location initLoc = target.getLocation();
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 20) { cancel(); return; }
                evoker.getWorld().spawnEntity(initLoc.clone().add(RANDOM.nextDouble()-0.5, 0, RANDOM.nextDouble()-0.5), EntityType.EVOKER_FANGS);
                t++;
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 2);
    }

    private void applyWitchScrollsEffect(Witch witch) {
        witch.getWorld().playSound(witch.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f);
        Location loc = witch.getLocation();
        new BukkitRunnable() {
            double step = 0;
            @Override
            public void run() {
                step += Math.PI / 20;
                for (double i = 0; i < Math.PI * 2; i += Math.PI / 16) {
                    Location pl = loc.clone().add(1.5 * Math.cos(i) * Math.sin(step), 1.5 * (1 + Math.cos(step)), 1.5 * Math.sin(i) * Math.sin(step));
                    pl.getWorld().spawnParticle(Particle.REDSTONE, pl, 0, new Particle.DustOptions(Color.WHITE, 1));
                }
                if (step >= Math.PI * 2) cancel();
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
    }
}