package org.nguyendevs.suddendeath.features.world;

import org.nguyendevs.suddendeath.manager.EventManager.WorldStatus;

public interface StatusRetriever {
	WorldStatus getStatus();
}