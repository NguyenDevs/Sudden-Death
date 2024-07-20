package org.nguyendevs.suddendeath.listener;

import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.CustomItem;
import org.nguyendevs.suddendeath.util.Utils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class Listener1 implements Listener {
    private static final Random random = new Random();

    public Listener1() {
        new BukkitRunnable() {
            public void run() {
                for (World w : Bukkit.getWorlds())
                    if (Feature.WITCH_SCROLLS.isEnabled(w))
                        w.getEntitiesByClass(Witch.class).forEach(Loops::loop4s_witch);
            }
        }.runTaskTimer(SuddenDeath.plugin, 20, 80);

        new BukkitRunnable() {
            public void run() {
                for (World w : Bukkit.getWorlds())
                    if (Feature.EVERBURNING_BLAZES.isEnabled(w))
                        w.getEntitiesByClass(Blaze.class).forEach(Loops::loop3s_blaze);

                Bukkit.getOnlinePlayers().forEach(Loops::loop3s_player);
            }
        }.runTaskTimer(SuddenDeath.plugin, 20, 60);

        new BukkitRunnable() {
            public void run() {
                for (World w : Bukkit.getWorlds())
                    if (Feature.ANGRY_SPIDERS.isEnabled(w) || Feature.LEAPING_SPIDERS.isEnabled(w))
                        for (Spider t : w.getEntitiesByClass(Spider.class))
                            if (t.getTarget() instanceof Player)
                                Loops.loop3s_spider(t);
            }
        }.runTaskTimer(SuddenDeath.plugin, 20, 40);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void a(EntityDamageEvent event) {
        if (event.isCancelled() || event.getEntity().hasMetadata("NPC") || event.getDamage() <= 0)
            return;

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // fall stun
            if (Feature.FALL_STUN.isEnabled(player) && event.getCause() == DamageCause.FALL) {
                player.removePotionEffect(PotionEffectType.SLOW);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
                        (int) (event.getDamage() * 10 * Feature.FALL_STUN.getDouble("duration-amplifier")), 2));
                new BukkitRunnable() {
                    double ti = 0;
                    Location loc = player.getLocation().clone();

                    public void run() {
                        ti += .25;
                        for (double j = 0; j < Math.PI * 2; j += Math.PI / 16) {
                            Location loc1 = loc.clone().add(Math.cos(j) * ti, .1, Math.sin(j) * ti);
                            Objects.requireNonNull(loc1.getWorld()).spawnParticle(Particle.BLOCK_CRACK, loc1, 0, Material.DIRT.createBlockData());
                        }
                        Objects.requireNonNull(loc.getWorld()).playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 2, 2);
                        if (ti >= 2)
                            cancel();
                    }
                }.runTaskTimer(SuddenDeath.plugin, 0, 1);
            }

            // start bleeding
            if (Feature.BLEEDING.isEnabled(player) && event.getCause() != DamageCause.STARVATION && event.getCause() != DamageCause.DROWNING
                    && event.getCause() != DamageCause.SUICIDE && event.getCause() != DamageCause.MELTING && event.getCause() != DamageCause.FIRE_TICK
                    && event.getCause() != DamageCause.VOID && event.getCause() != DamageCause.SUFFOCATION
                    && event.getCause() != DamageCause.POISON) {
                double chance = Feature.BLEEDING.getDouble("chance-percent") / 100;
                PlayerData data = PlayerData.get(player);
                if (random.nextDouble() <= chance && !data.isBleeding()) {
                    data.setBleeding(true);
                    player.sendMessage(ChatColor.DARK_RED + Utils.msg("now-bleeding"));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1, 2);
                }
            }
        }

        // tanky monsters
        if (event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof Player))
            if (Feature.TANKY_MONSTERS.isEnabled(event.getEntity()))
                event.setDamage(event.getDamage()
                        * (1 - Feature.TANKY_MONSTERS.getDouble("dmg-reduction-percent." + event.getEntity().getType().name()) / 100));

        // undead rage
        if (event.getEntity() instanceof Zombie)
            if (Feature.UNDEAD_GUNNERS.isEnabled(event.getEntity())) {
                Zombie z = (Zombie) event.getEntity();
                int duration = (int) (Feature.UNDEAD_RAGE.getDouble("rage-duration") * 20);
                z.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, z.getLocation().add(0, 1.7, 0), 6, .35, .35, .35, 0);
                z.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 1));
                z.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1));
            }

        // witch scrolls
        if (event.getEntity() instanceof Witch) {
            Witch w = (Witch) event.getEntity();
            double chance = Feature.WITCH_SCROLLS.getDouble("chance-percent") / 100;
            if (Feature.WITCH_SCROLLS.isEnabled(w) && random.nextDouble() <= chance) {
                event.setCancelled(true);
                w.getWorld().playSound(w.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 2);
                new BukkitRunnable() {
                    final double r = 1.5;
                    double step = 0;
                    final Location loc = w.getLocation();

                    public void run() {
                        for (double j = 0; j < 3; j++) {
                            step += Math.PI / 20;
                            for (double i = 0; i < Math.PI * 2; i += Math.PI / 16) {
                                Location loc1 = loc.clone().add(r * Math.cos(i) * Math.sin(step), r * (1 + Math.cos(step)),
                                        r * Math.sin(i) * Math.sin(step));
                                Objects.requireNonNull(loc1.getWorld()).spawnParticle(Particle.REDSTONE, loc1, 0, new Particle.DustOptions(Color.WHITE, 1));
                            }
                        }
                        if (step >= Math.PI * 2)
                            cancel();
                    }

                }.runTaskTimer(SuddenDeath.plugin, 0, 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void b(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || event.getEntity().hasMetadata("NPC") || event.getDamager().hasMetadata("NPC") || event.getDamage() <= 0)
            return;

        // infection from zombie to player
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if ((event.getDamager() instanceof PigZombie || event.getDamager() instanceof Zombie) && Feature.INFECTION.isEnabled(player)) {
                PlayerData data = PlayerData.get(player);
                double chance = Feature.INFECTION.getDouble("chance-percent") / 100;
                if (random.nextDouble() <= chance && !data.isInfected()) {
                    data.setInfected(true);
                    player.sendMessage(ChatColor.DARK_RED + Utils.msg("now-infected"));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
                    player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1, 2);
                }
            }

            // arrow slow
            if (event.getDamager() instanceof Arrow && Feature.ARROW_SLOW.isEnabled(player))
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (Feature.ARROW_SLOW.getDouble("slow-duration") * 20), 2));

            // skeleton shocking arrows
            if (event.getDamager() instanceof Arrow && Feature.SHOCKING_SKELETON_ARROWS.isEnabled(player)) {
                Arrow a = (Arrow) event.getDamager();
                if (a.getShooter() != null && a.getShooter() instanceof Skeleton) {
                    Skeleton shooter = (Skeleton) a.getShooter();

                    // Spawn smoke particles around the player
                    new BukkitRunnable() {
                        final Location loc = player.getLocation();
                        double ti = 0;

                        public void run() {
                            for (int j1 = 0; j1 < 3; j1++) {
                                ti += Math.PI / 15;
                                Location loc1 = loc.clone().add(Math.cos(ti), 1, Math.sin(ti));
                                Objects.requireNonNull(loc1.getWorld()).spawnParticle(Particle.SMOKE_NORMAL, loc1, 0);
                            }
                            if (ti >= Math.PI * 2)
                                cancel();
                        }
                    }.runTaskTimer(SuddenDeath.plugin, 0, 1);

                    // Apply shaking effect to the player
                    double duration = Feature.SHOCKING_SKELETON_ARROWS.getDouble("shock-duration");
                    new BukkitRunnable() {
                        int ticksPassed = 0;

                        public void run() {
                            ticksPassed++;
                            if (ticksPassed > duration * 20) // Convert seconds to ticks
                                cancel();

                            // Apply shaking effect (damage with 0.0)
                            player.playHurtAnimation(0.005f);
                        }
                    }.runTaskTimer(SuddenDeath.plugin, 0, 2);
                    // Play hurt sound
                    new BukkitRunnable() {
                        int playCount = 0;

                        public void run() {
                            if (playCount >= duration * 20) {
                                cancel();
                                return;
                            }
                            player.playSound(player.getLocation(), "minecraft:entity.player.hurt", 1.0f, 1.2f);
                            playCount++;
                        }
                    }.runTaskTimer(SuddenDeath.plugin, 0, 2);
                }
            }

        }

        // nether shield on nether mobs
        if ((event.getEntity() instanceof PigZombie || event.getEntity() instanceof MagmaCube || event.getEntity() instanceof Blaze)
                && event.getDamager() instanceof Player) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            Player damager = (Player) event.getDamager();
            double chance = Feature.NETHER_SHIELD.getDouble("chance-percent") / 100;
            if (Feature.NETHER_SHIELD.isEnabled(entity) && random.nextDouble() <= chance) {
                event.setCancelled(true);
                entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 2);
                int radius = 1;

                if (entity instanceof MagmaCube)
                    if (((MagmaCube) entity).getSize() == 4)
                        radius = 2;

                for (double j = 0; j < Math.PI * 2; j += .3) {
                    double x = Math.cos(j) * radius;
                    double z = Math.sin(j) * radius;
                    for (double y = 0; y < 2; y += .2) {
                        if (random.nextDouble() < .3)
                            continue;

                        Location loc = entity.getLocation().clone();
                        loc.add(x, y, z);
                        if (loc.getBlock().getType().isSolid())
                            continue;

                        Objects.requireNonNull(loc.getWorld()).spawnParticle(Particle.FLAME, loc, 0);
                        if (random.nextDouble() < .45)
                            loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 0);
                    }
                }
                damager.setVelocity(damager.getEyeLocation().getDirection().multiply(-.6).setY(.3));
                damager.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, damager.getLocation().add(0, 1, 0), 0);
                damager.setFireTicks((int) Feature.NETHER_SHIELD.getDouble("burn-duration"));
                Utils.damage(damager, event.getDamage() * Feature.NETHER_SHIELD.getDouble("dmg-reflection-percent") / 100, true);
            }
        }

        if (Feature.INFECTION.isEnabled(event.getEntity())) {

            // sharp knife bleeding effect
            if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
                Player damager = (Player) event.getDamager();
                Player player = (Player) event.getEntity();
                ItemStack i = damager.getInventory().getItemInMainHand();
                if (Utils.isPluginItem(i, false)) {
                    if (Objects.requireNonNull(i.getItemMeta()).getDisplayName().equals(CustomItem.SHARP_KNIFE.getName())) {
                        PlayerData data = PlayerData.get(player);
                        if (!data.isBleeding()) {
                            data.setBleeding(true);
                            player.sendMessage(ChatColor.DARK_RED + Utils.msg("now-bleeding"));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
                            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1, 2);
                        }
                    }
                }
            }

            // infection from ZOMBIE to PLAYER
            if ((event.getEntity() instanceof PigZombie || event.getEntity() instanceof Zombie) && event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                ItemStack i = player.getInventory().getItemInMainHand();
                if (i.getType() == Material.AIR && !Utils.hasCreativeGameMode(player)) {
                    PlayerData data = PlayerData.get(player);
                    double chance = Feature.INFECTION.getDouble("chance-percent") / 100;
                    if (random.nextDouble() <= chance && !data.isInfected()) {
                        data.setInfected(true);
                        player.sendMessage(ChatColor.DARK_RED + Utils.msg("now-infected"));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
                        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1, 2);
                    }
                }
            }
        }
    }

    @EventHandler
    public void c(EntityRegainHealthEvent event) {
        if (event.getEntity().hasMetadata("NPC") || !(event.getEntity() instanceof Player)
                || (event.getRegainReason() != RegainReason.SATIATED && event.getRegainReason() != RegainReason.REGEN))
            return;

        Player player = (Player) event.getEntity();
        PlayerData data = PlayerData.get(player);
        if ((Feature.INFECTION.isEnabled(player) && data.isInfected()) || (Feature.BLEEDING.isEnabled(player) && data.isBleeding()))
            event.setCancelled(true);
    }

    @EventHandler
    public void d(PlayerDeathEvent event) {
        if (event.getEntity().hasMetadata("NPC"))
            return;

        Player player = event.getEntity();
        PlayerData data = PlayerData.get(player);
        data.setBleeding(false);
        data.setInfected(false);
        if (Feature.ADVANCED_PLAYER_DROPS.isEnabled(player)) {
            FileConfiguration advanced = Feature.ADVANCED_PLAYER_DROPS.getConfigFile().getConfig();
            if (advanced.getBoolean("drop-skull")) {
                ItemStack skull = new ItemStack(Material.SKELETON_SKULL);
                if (advanced.getBoolean("player-skull")) {
                    skull.setType(Material.PLAYER_HEAD);
                    SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                    assert skullMeta != null;
                    skullMeta.setOwningPlayer(player);
                    skull.setItemMeta(skullMeta);
                }
                player.getWorld().dropItemNaturally(player.getLocation(), skull);
            }

            if (advanced.getInt("dropped-bones") > 0) {
                ItemStack bone = CustomItem.HUMAN_BONE.a().clone();
                bone.setAmount(advanced.getInt("dropped-bones"));
                player.getWorld().dropItemNaturally(player.getLocation(), bone);
            }
            if (advanced.getInt("dropped-flesh") > 0) {
                ItemStack flesh = CustomItem.RAW_HUMAN_FLESH.a().clone();
                flesh.setAmount(advanced.getInt("dropped-flesh"));
                player.getWorld().dropItemNaturally(player.getLocation(), flesh);
            }
        }
    }

    @EventHandler
    public void e(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == Objects.requireNonNull(event.getTo()).getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ())
            return;

        Player player = event.getPlayer();
        PlayerData data = PlayerData.get(player);
        if (player.hasMetadata("NPC"))
            return;

        // electricity shock
        if (Feature.ELECTRICITY_SHOCK.isEnabled(player)) {
            Block b = player.getLocation().getBlock();
            if (isPoweredRedstoneBlock(b) && !Utils.hasCreativeGameMode(player) && !data.isOnCooldown(Feature.ELECTRICITY_SHOCK)) {
                data.applyCooldown(Feature.ELECTRICITY_SHOCK, 3);

                // Spawn particle effects
                player.getWorld().spawnParticle(Particle.SNOW_SHOVEL, player.getLocation(), 16, 0, 0, 0, .15);
                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 24, 0, 0, 0, .15);

                // Apply damage
                Utils.damage(player, Feature.ELECTRICITY_SHOCK.getDouble("damage"), true);

                // Apply shaking effect
                new BukkitRunnable() {
                    int ticksPassed = 0;

                    public void run() {
                        ticksPassed++;
                        if (ticksPassed > 15) // 15 intervals of 2 ticks each
                            cancel();

                        // Apply shaking effect
                        player.playHurtAnimation(0.009f);
                    }
                }.runTaskTimer(SuddenDeath.plugin, 0, 2);

                // Play hurt sound
                new BukkitRunnable() {
                    int playCount = 0;

                    public void run() {
                        if (playCount >= 15) {
                            cancel();
                            return;
                        }
                        player.playSound(player.getLocation(), "minecraft:entity.player.hurt", 1.0f, 1.2f);
                        playCount++;
                    }
                }.runTaskTimer(SuddenDeath.plugin, 0, 2);
            }
        }


        // bleeding block effect
        if (Feature.BLEEDING.isEnabled(player) && SuddenDeath.plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SD_EFFECT))
            if (player.isOnGround() && !Utils.hasCreativeGameMode(player) && data.isBleeding())
                player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 5, Material.REDSTONE_WIRE.createBlockData());

        // infection effect
        if (Feature.INFECTION.isEnabled(player) && SuddenDeath.plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SD_EFFECT))
            if (player.isOnGround() && !Utils.hasCreativeGameMode(player) && data.isInfected())
                player.getWorld().spawnParticle(Particle.SPELL_MOB, player.getLocation().add(0, 1, 0), 5, .3, 0, .3, 0);

        // armor weight
        if (Feature.ARMOR_WEIGHT.isEnabled(player))
            PlayerData.get(player).updateMovementSpeed();
    }

    @EventHandler
    public void f(EntityDeathEvent event) {
        if (event.getEntity().hasMetadata("NPC"))
            return;

        Entity entity = event.getEntity();

        // creeper revenge
        if (Feature.CREEPER_REVENGE.isEnabled(entity)) {
            double chance = Feature.CREEPER_REVENGE.getDouble("chance-percent") / 100;
            if (random.nextDouble() <= chance && entity instanceof Creeper) {
                new BukkitRunnable() {
                    public void run() {
                        if (((Creeper) entity).isPowered()) {
                            entity.getWorld().createExplosion(entity.getLocation(), 6);
                            return;
                        }
                        entity.getWorld().createExplosion(entity.getLocation(), 3);
                    }
                }.runTaskLater(SuddenDeath.plugin, 15);
            }
        }

        // silverfishes summon
        if (Feature.SILVERFISHES_SUMMON.isEnabled(entity) && entity instanceof Zombie) {
            double chance = Feature.SILVERFISHES_SUMMON.getDouble("chance-percent") / 100;
            int min = (int) Feature.SILVERFISHES_SUMMON.getDouble("min");
            int max = (int) Feature.SILVERFISHES_SUMMON.getDouble("max");
            if (random.nextDouble() <= chance)
                for (int j = 0; j < min + random.nextInt(max); j++) {
                    Random r = random;
                    Vector v = new Vector(r.nextDouble() - .5, r.nextDouble() - .5, r.nextDouble() - .5);
                    entity.getWorld().spawnParticle(Particle.SMOKE_LARGE, entity.getLocation(), 0);
                    entity.getWorld().spawnEntity(entity.getLocation(), EntityType.SILVERFISH).setVelocity(v);
                }
        }
    }

    @EventHandler
    public void g(EntitySpawnEvent event) {
        if (event.getEntity().hasMetadata("NPC"))
            return;
        if (!(event.getEntity() instanceof Monster))
            return;

        Monster m = (Monster) event.getEntity();

        // quick mobs
        if (Feature.QUICK_MOBS.isEnabled(m)) {
            double ms = Objects.requireNonNull(m.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue();
            ms *= 1 + Feature.QUICK_MOBS.getDouble("additional-ms-percent." + event.getEntity().getType().name()) / 100;
            Objects.requireNonNull(m.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(ms);
        }

        // force of the undead
        if (Feature.FORCE_OF_THE_UNDEAD.isEnabled(m)) {
            double ad = Objects.requireNonNull(m.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).getBaseValue();
            ad *= 1 + Feature.FORCE_OF_THE_UNDEAD.getDouble("additional-ad-percent." + event.getEntity().getType().name()) / 100;
            Objects.requireNonNull(m.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(ad);
        }
    }

    @EventHandler
    public void h(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // stone stiffness
        if (Feature.STONE_STIFFNESS.isEnabled(player)) {
            Block b = event.getClickedBlock();
            if (event.hasBlock() && event.getAction() == Action.LEFT_CLICK_BLOCK && !Utils.hasCreativeGameMode(player) && !event.hasItem()) {
                assert b != null;
                if (Arrays
                        .asList(new Material[]{Material.STONE, Material.COAL_ORE, Material.IRON_ORE, Material.NETHER_QUARTZ_ORE, Material.GOLD_ORE,
                                Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.REDSTONE_ORE, Material.EMERALD_ORE, Material.COBBLESTONE,
                                Material.STONE_SLAB, Material.COBBLESTONE_SLAB, Material.BRICK_STAIRS, Material.BRICK, Material.MOSSY_COBBLESTONE})
                        .contains(b.getType()))
                    Utils.damage(player, Feature.STONE_STIFFNESS.getDouble("damage"), true);
            }
        }

        if (!event.hasItem())
            return;

        ItemStack i = player.getInventory().getItemInMainHand();
        if (!Utils.isPluginItem(i, false))
            return;

        // bleeding cure
        if (Feature.BLEEDING.isEnabled(player))
            if (Objects.requireNonNull(i.getItemMeta()).getDisplayName().equals(CustomItem.BANDAGE.getName())) {
                event.setCancelled(true);
                PlayerData data = PlayerData.get(player);
                if (!data.isBleeding())
                    return;

                consume(player);
                data.setBleeding(false);
                player.sendMessage(ChatColor.YELLOW + Utils.msg("use-bandage"));
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 0);
            }

        // infection cure
        if (Feature.INFECTION.isEnabled(player))
            if (Objects.requireNonNull(i.getItemMeta()).getDisplayName().equals(CustomItem.STRANGE_BREW.getName())) {
                event.setCancelled(true);
                PlayerData data = PlayerData.get(player);
                if (!data.isInfected())
                    return;

                consume(player);
                data.setInfected(false);
                player.sendMessage(ChatColor.YELLOW + Utils.msg("use-strange-brew"));
                player.removePotionEffect(PotionEffectType.CONFUSION);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1, 0);
            }
    }

    private void consume(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItemInMainHand(item.getAmount() < 1 ? null : item);
    }

    @EventHandler
    public void i(FurnaceSmeltEvent event) {
        ItemStack item = event.getSource();
        if (!Utils.isPluginItem(item, false))
            return;

        if (Objects.requireNonNull(item.getItemMeta()).getDisplayName().equals(CustomItem.RAW_HUMAN_FLESH.getName()))
            event.setResult(CustomItem.COOKED_HUMAN_FLESH.a());
    }

    public boolean isPoweredRedstoneBlock(Block b) {
        Material m = b.getType();
        return b.isBlockPowered()
                && (m == Material.REDSTONE_WIRE || m == Material.COMPARATOR || m == Material.REPEATER || m == Material.REDSTONE_TORCH);
    }
}