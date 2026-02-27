package org.nguyendevs.suddendeath.Features.base;

import org.bukkit.event.Listener;
import org.nguyendevs.suddendeath.SuddenDeath;

@SuppressWarnings("deprecation")

public interface IFeature extends Listener {
    void initialize(SuddenDeath plugin);

    void shutdown();

    String getName();

    boolean isEnabled();
}