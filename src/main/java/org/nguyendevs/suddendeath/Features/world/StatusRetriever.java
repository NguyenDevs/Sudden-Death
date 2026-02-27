package org.nguyendevs.suddendeath.Features.world;

import org.nguyendevs.suddendeath.Managers.EventManager.WorldStatus;

@SuppressWarnings("deprecation")

public interface StatusRetriever {
	WorldStatus getStatus();
}