package org.nguyendevs.suddendeath.util;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class SpawnUtils {
    private static final List<LivingEntity> spawningEntities = new ArrayList<>();
    private static boolean allMobs;
    private static boolean playSound;
    private static Sound spawnSoundName;
    private static float spawnSoundLoudness;
    private static float spawnSoundPitch;
    private static float spawnSoundDelay;
    private static boolean useJockeyRider;
    private static boolean spawnParticles;
    private static boolean useBlockUnderEntityForParticles;
    private static BlockData spawnParticleBlockData;
    private static boolean canBeDamagedWhileSpawning;
    private static float spawnDuration;
    private static final List<EntityType> whiteList = new ArrayList<>();
    private static final List<EntityType> blackList = new ArrayList<>();
    private static final List<SpawnReason> blockedReasons = new ArrayList<>();
    private static final List<String> bannedWorlds = new ArrayList<>();

    public static List<LivingEntity> getSpawningEntities() {
        return spawningEntities;
    }

    public static boolean getAllMobs() {
        return allMobs;
    }

    public static void setAllMobs(boolean allMobs) {
        SpawnUtils.allMobs = allMobs;
    }

    public static boolean getPlaySound() {
        return playSound;
    }

    public static void setPlaySound(boolean playSound) {
        SpawnUtils.playSound = playSound;
    }

    public static Sound getSpawnSoundName() {
        return spawnSoundName;
    }

    public static void setSpawnSoundName(Sound spawnSoundName) {
        SpawnUtils.spawnSoundName = spawnSoundName;
    }

    public static float getSpawnSoundLoudness() {
        return spawnSoundLoudness;
    }

    public static void setSpawnSoundLoudness(float spawnSoundLoudness) {
        SpawnUtils.spawnSoundLoudness = spawnSoundLoudness;
    }

    public static float getSpawnSoundPitch() {
        return spawnSoundPitch;
    }

    public static void setSpawnSoundPitch(float spawnSoundPitch) {
        SpawnUtils.spawnSoundPitch = spawnSoundPitch;
    }

    public static float getSpawnSoundDelay() {
        return spawnSoundDelay;
    }

    public static void setSpawnSoundDelay(float spawnSoundDelay) {
        SpawnUtils.spawnSoundDelay = spawnSoundDelay;
    }

    public static boolean getSpawnParticles() {
        return spawnParticles;
    }

    public static void setUseJockeyRider(boolean useRider) {
        useJockeyRider = useRider;
    }

    public static boolean getUseJockeyRider() {
        return useJockeyRider;
    }

    public static void setSpawnParticles(boolean spawnParticles) {
        SpawnUtils.spawnParticles = spawnParticles;
    }

    public static boolean getUseBlockUnderEntityForParticles() {
        return useBlockUnderEntityForParticles;
    }

    public static void setUseBlockUnderEntityForParticles(boolean useBlockUnderEntityForParticles) {
        SpawnUtils.useBlockUnderEntityForParticles = useBlockUnderEntityForParticles;
    }

    public static boolean getCanBeDamagedWhileSpawning() {
        return canBeDamagedWhileSpawning;
    }

    public static void setCanBeDamagedWhileSpawning(boolean canBeDamagedWhileSpawning) {
        SpawnUtils.canBeDamagedWhileSpawning = canBeDamagedWhileSpawning;
    }

    public static BlockData getSpawnParticleBlockData() {
        return spawnParticleBlockData;
    }

    public static void setSpawnParticleBlockData(BlockData spawnParticleBlockData) {
        SpawnUtils.spawnParticleBlockData = spawnParticleBlockData;
    }

    public static float getSpawnDuration() {
        return spawnDuration;
    }

    public static void setSpawnDuration(float spawnDuration) {
        SpawnUtils.spawnDuration = spawnDuration;
    }

    public static List<EntityType> getWhiteList() {
        return whiteList;
    }

    public static List<EntityType> getBlackList() {
        return blackList;
    }

    public static List<SpawnReason> getBlockedReasons() {
        return blockedReasons;
    }

    public static List<String> getBannedWorlds() {
        return bannedWorlds;
    }
}
