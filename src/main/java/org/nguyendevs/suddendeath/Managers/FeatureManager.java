package org.nguyendevs.suddendeath.Managers;

import org.bukkit.entity.EntityType;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Features.base.IFeature;
import org.nguyendevs.suddendeath.Features.combat.*;
import org.nguyendevs.suddendeath.Features.items.*;
import org.nguyendevs.suddendeath.Features.mob.attributes.*;
import org.nguyendevs.suddendeath.Features.mob.hostile.*;
import org.nguyendevs.suddendeath.Features.nether.NetherShieldFeature;
import org.nguyendevs.suddendeath.Features.player.*;
import org.nguyendevs.suddendeath.Features.world.*;
import org.nguyendevs.suddendeath.Player.Modifier;
import org.nguyendevs.suddendeath.Utils.ConfigFile;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class FeatureManager {
    private final SuddenDeath plugin;
    private final List<IFeature> loadedFeatures = new ArrayList<>();

    public FeatureManager(SuddenDeath plugin) {
        this.plugin = plugin;
    }

    public void registerAllFeatures() {
        register(new PlayerCoreFeature());

        register(new EverburningBlazesFeature());
        register(new HomingFlameBarrageFeature());
        register(new BreezeDashFeature());
        register(new CreeperRevengeFeature());
        register(new TridentWrathFeature());
        register(new EnderPowerFeature());
        register(new ImmortalEvokerFeature());
        register(new AbyssalVortexFeature());
        register(new PhantomBladeFeature());
        register(new SilverfishSummonFeature());
        register(new SkeletonFeatures());
        register(new ThiefSlimesFeature());
        register(new PoisonedSlimesFeature());
        register(new SpiderFeatures());
        register(new SpiderNestFeature());
        register(new StrayFrostFeature());
        register(new WitchScrollsFeature());
        register(new WitherRushFeature());
        register(new WitherMachineGunFeature());
        register(new UndeadRageFeature());
        register(new UndeadGunnersFeature());
        register(new ZombieBreakBlockFeature());
        register(new ZombieToolsFeature());
        register(new PillagerFireworkFeature());

        register(new ForceOfUndeadFeature());
        register(new QuickMobsFeature());
        register(new TankyMonstersFeature());
        register(new ArmorPiercingFeature());

        register(new ArrowSlowFeature());
        register(new MobCriticalStrikesFeature());
        register(new SharpKnifeFeature());

        register(new AdvancedPlayerDropsFeature());
        register(new BleedingFeature());
        register(new BloodScreenFeature());
        register(new ElectricityShockFeature());
        register(new FallStunFeature());
        register(new HungerNauseaFeature());
        register(new InfectionFeature());
        register(new NetherShieldFeature());
        register(new PhysicEnderPearlFeature());
        register(new RealisticPickupFeature());
        register(new StoneStiffnessFeature());

        register(new DangerousCoalFeature());
        register(new FreddyFeature());
        register(new SnowSlowFeature());
        register(new WhispersOfTheDesertFeature());

        initializeFeatureConfigs();
    }

    private void register(IFeature feature) {
        feature.initialize(plugin);
        loadedFeatures.add(feature);
    }

    private void initializeFeatureConfigs() {
        for (Feature feature : Feature.values()) {
            feature.updateConfig();
            ConfigFile modifiers = feature.getConfigFile();
            boolean saveNeeded = false;
            for (Modifier mod : feature.getModifiers()) {
                if (modifiers.getConfig().contains(mod.name())) {
                    continue;
                }
                if (mod.type() == Modifier.Type.NONE) {
                    modifiers.getConfig().set(mod.name(), mod.value());
                    saveNeeded = true;
                } else if (mod.type() == Modifier.Type.EACH_MOB) {
                    for (EntityType type : Utils.getLivingEntityTypes()) {
                        String path = mod.name() + "." + type.name();
                        if (!modifiers.getConfig().contains(path)) {
                            modifiers.getConfig().set(path, mod.value());
                            saveNeeded = true;
                        }
                    }
                }
            }
            if (saveNeeded) {
                modifiers.save();
            }
        }
    }

    public void shutdownAll() {
        for (IFeature feature : loadedFeatures) {
            feature.shutdown();
        }
        loadedFeatures.clear();
    }

    public void reloadFeatures() {
        for (Feature feature : Feature.values()) {
            feature.updateConfig();
        }
        initializeFeatureConfigs();
    }
}