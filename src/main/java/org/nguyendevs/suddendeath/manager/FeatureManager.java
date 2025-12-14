package org.nguyendevs.suddendeath.manager;

import org.bukkit.entity.EntityType;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.features.base.IFeature;
import org.nguyendevs.suddendeath.features.combat.*;
import org.nguyendevs.suddendeath.features.items.*;
import org.nguyendevs.suddendeath.features.mob.attributes.*;
import org.nguyendevs.suddendeath.features.mob.hostile.*;
import org.nguyendevs.suddendeath.features.nether.NetherShieldFeature;
import org.nguyendevs.suddendeath.features.player.*;
import org.nguyendevs.suddendeath.features.world.*;
import org.nguyendevs.suddendeath.player.Modifier;
import org.nguyendevs.suddendeath.util.ConfigFile;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;

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

        register(new BlazeFeatures());
        register(new BreezeFeature());
        register(new CreeperFeature());
        register(new DrownedFeature());
        register(new EnderFeatures());
        register(new EvokerFeature());
        register(new GuardianFeature());
        register(new PhantomFeature());
        register(new SilverfishFeature());
        register(new SkeletonFeatures());
        register(new SlimeFeatures());
        register(new SpiderFeatures());
        register(new StrayFeature());
        register(new WitchFeature());
        register(new WitherSkeletonFeature());
        register(new UndeadRageFeature());
        register(new UndeadGunnersFeature());
        register(new ZombieBreakBlockFeature());
        register(new ZombieToolsFeature());
        register(new PillagerFireWorkFeature());

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
                if (modifiers.getConfig().contains(mod.getName())) {
                    continue;
                }
                if (mod.getType() == Modifier.Type.NONE) {
                    modifiers.getConfig().set(mod.getName(), mod.getValue());
                    saveNeeded = true;
                } else if (mod.getType() == Modifier.Type.EACH_MOB) {
                    for (EntityType type : Utils.getLivingEntityTypes()) {
                        if (!modifiers.getConfig().contains(mod.getName() + "." + type.name())) {
                            modifiers.getConfig().set(mod.getName() + "." + type.name(), mod.getValue());
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