package org.nguyendevs.suddendeath.features.base;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.nguyendevs.suddendeath.SuddenDeath;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class AbstractFeature implements IFeature {
    protected SuddenDeath plugin;
    protected static final Random RANDOM = new Random();
    protected List<BukkitTask> tasks = new ArrayList<>();

    @Override
    public void initialize(SuddenDeath plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        onEnable();
    }

    @Override
    public void shutdown() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        onDisable();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    protected void onEnable() {}
    protected void onDisable() {}

    protected void registerTask(BukkitTask task) {
        tasks.add(task);
    }
}