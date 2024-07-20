package org.nguyendevs.suddendeath.events;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.SpawnUtils;

public class CreatureSpawn implements Listener {
    private final Set<Entity> passengers = new HashSet<>();
    private final Set<Entity> noSpawnPassengers = new HashSet<>();

    private void createParticles(Location loc, BlockData data) {
        Objects.requireNonNull(loc.getWorld()).spawnParticle(Particle.BLOCK_CRACK, loc, 10, data);
    }

    private void fixChunkBoundary(LivingEntity ent, Location entLoc) {
        BoundingBox box = ent.getBoundingBox();
        double minX = box.getMinX();
        double minZ = box.getMinZ();
        double maxX = box.getMaxX();
        double maxZ = box.getMaxZ();
        World world = entLoc.getWorld();
        double y = entLoc.getY();
        Location c1 = new Location(world, minX, y, minZ);
        Location c2 = new Location(world, minX, y, maxZ);
        Location c3 = new Location(world, maxX, y, minZ);
        boolean sameX = c1.getChunk() == c3.getChunk();
        boolean sameZ = c1.getChunk() == c2.getChunk();
        if (!sameX || !sameZ) {
            double move;
            if (!sameX) {
                move = box.getWidthX() / 2.0D;
                if (c1.getChunk() == entLoc.getChunk()) {
                    entLoc.add(-move, 0.0D, 0.0D);
                } else {
                    entLoc.add(move, 0.0D, 0.0D);
                }
            }

            if (!sameZ) {
                move = box.getWidthZ() / 2.0D;
                if (c1.getChunk() == entLoc.getChunk()) {
                    entLoc.add(0.0D, 0.0D, -move);
                } else {
                    entLoc.add(0.0D, 0.0D, move);
                }
            }

        }
    }

    private Entity getPrimaryPassenger(LivingEntity ent) {
        List<Entity> entPassengers = ent.getPassengers();
        return entPassengers.isEmpty() ? null : (Entity)entPassengers.get(0);
    }

    private void smoothEntitySpawnFromGrave(final LivingEntity ent) {
        if (!this.passengers.contains(ent)) {
            if (this.noSpawnPassengers.contains(ent)) {
                this.noSpawnPassengers.remove(ent);
            } else {
                final Location particleLocation = ent.getLocation();
                final Location entLoc = particleLocation.clone();
                this.fixChunkBoundary(ent, entLoc);
                final Entity passenger = this.getPrimaryPassenger(ent);
                if (passenger != null) {
                    this.passengers.add(passenger);
                }

                if (entLoc.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType().isSolid() && !entLoc.getBlock().getType().toString().contains("WATER")) {
                    SpawnUtils.getSpawningEntities().add(ent);
                    entLoc.add(0.0D, -2.0D, 0.0D);
                    ent.teleport(entLoc);
                    if (passenger != null) {
                        SuddenDeath.getInstance().getProtocolLibHook().addEntityToChange(ent);
                        if (passenger instanceof LivingEntity) {
                            SuddenDeath.getInstance().getProtocolLibHook().addEntityToChange((LivingEntity)passenger);
                        }
                    }

                    Block blockUnderEntity = particleLocation.clone().add(0.0D, -1.0D, 0.0D).getBlock();
                    final BlockData blockData = SpawnUtils.getUseBlockUnderEntityForParticles() ? blockUnderEntity.getBlockData() : SpawnUtils.getSpawnParticleBlockData();
                    final float step = 1.0F / SpawnUtils.getSpawnDuration() * 2.0F;
                    (new BukkitRunnable() {
                        int soundLoopCounter = 0;

                        public void run() {
                            if (!CreatureSpawn.this.isValid(ent, entLoc)) {
                                SpawnUtils.getSpawningEntities().remove(ent);
                                ent.remove();
                                if (passenger != null) {
                                    CreatureSpawn.this.passengers.remove(passenger);
                                }

                                this.cancel();
                            } else if (!entLoc.getBlock().getType().isSolid() && !entLoc.clone().add(0.0D, 1.0D, 0.0D).getBlock().getType().isSolid()) {
                                if (passenger != null && !passenger.equals(CreatureSpawn.this.getPrimaryPassenger(ent))) {
                                    ent.addPassenger(passenger);
                                }

                                SpawnUtils.getSpawningEntities().remove(ent);
                                if (passenger != null) {
                                    CreatureSpawn.this.passengers.remove(passenger);
                                }

                                this.cancel();
                            } else {
                                if (SpawnUtils.getSpawnParticles()) {
                                    CreatureSpawn.this.createParticles(particleLocation, blockData);
                                }

                                entLoc.add(0.0D, (double)step, 0.0D);
                                ent.teleport(entLoc);
                                if (passenger != null) {
                                    passenger.teleport(entLoc);
                                }

                                if (SpawnUtils.getPlaySound() && (float)this.soundLoopCounter % SpawnUtils.getSpawnSoundDelay() == 0.0F) {
                                    Objects.requireNonNull(entLoc.getWorld()).playSound(particleLocation, SpawnUtils.getSpawnSoundName(), SpawnUtils.getSpawnSoundLoudness(), SpawnUtils.getSpawnSoundPitch());
                                    this.soundLoopCounter = 0;
                                }

                                ++this.soundLoopCounter;
                            }
                        }
                    }).runTaskTimer(SuddenDeath.getInstance(), 1L, 1L);
                }
            }
        }
    }

    private boolean isValid(LivingEntity ent, Location entLoc) {
        if (!ent.isDead() && ent.isValid()) {
            return Objects.requireNonNull(entLoc.getWorld()).isChunkLoaded(entLoc.getBlockX() >> 4, entLoc.getBlockZ() >> 4);
        } else {
            return false;
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onSpawn(CreatureSpawnEvent e) {
        if (!e.isCancelled()) {
            LivingEntity spawnedEntity = e.getEntity();
            EntityType checkType = this.getCheckType(spawnedEntity);
            if (this.isApplicable(e.getSpawnReason(), spawnedEntity.getLocation(), checkType)) {
                this.smoothEntitySpawnFromGrave(spawnedEntity);
            } else {
                Entity passenger = this.getPrimaryPassenger(spawnedEntity);
                if (passenger != null && this.isApplicable(passenger.getType())) {
                    this.noSpawnPassengers.add(passenger);
                }
            }
        }

    }

    private EntityType getCheckType(LivingEntity entity) {
        if (!SpawnUtils.getUseJockeyRider()) {
            return entity.getType();
        } else {
            Entity passenger = this.getPrimaryPassenger(entity);
            return passenger == null ? entity.getType() : passenger.getType();
        }
    }

    private boolean isApplicable(SpawnReason reason, Location loc, EntityType type) {
        if (SpawnUtils.getBlockedReasons().contains(reason)) {
            return false;
        } else if (!loc.getChunk().isLoaded()) {
            return false;
        } else {
            return SpawnUtils.getBannedWorlds().contains(loc.getWorld().getName()) ? false : this.isApplicable(type);
        }
    }

    private boolean isApplicable(EntityType type) {
        if (SpawnUtils.getBlackList().contains(type)) {
            return false;
        } else {
            return SpawnUtils.getAllMobs() || SpawnUtils.getWhiteList().contains(type);
        }
    }

    @EventHandler
    public void noDamage(EntityDamageEvent e) {
        Entity ent = e.getEntity();
        if (SpawnUtils.getSpawningEntities().contains(ent) || this.passengers.contains(ent)) {
            if (!SpawnUtils.getCanBeDamagedWhileSpawning()) {
                e.setCancelled(true);
                return;
            }

            if (e.getCause().equals(DamageCause.SUFFOCATION) || e.getCause().equals(DamageCause.FALL)) {
                e.setCancelled(true);
                return;
            }
        }

    }
}
