package org.nguyendevs.suddendeath.features.base;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.ArrayList;
import java.util.List;

public interface IFeature extends Listener {
    void initialize(SuddenDeath plugin);
    void shutdown();
    String getName();
    boolean isEnabled();
}