package org.nguyendevs.suddendeath.features.base;

import org.bukkit.event.Listener;
import org.nguyendevs.suddendeath.SuddenDeath;

public interface IFeature extends Listener {
    void initialize(SuddenDeath plugin);
    void shutdown();
    String getName();
    boolean isEnabled();
}