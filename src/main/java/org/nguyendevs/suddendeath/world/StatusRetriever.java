package org.nguyendevs.suddendeath.world;

import org.nguyendevs.suddendeath.manager.EventManager.WorldStatus;

public interface StatusRetriever {
	WorldStatus getStatus();
}