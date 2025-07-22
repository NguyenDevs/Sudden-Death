package org.nguyendevs.suddendeath.world;

import org.nguyendevs.suddendeath.manager.EventManager.WorldStatus;

/**
 * Interface for retrieving the status of a world event in the SuddenDeath plugin.
 */
public interface StatusRetriever {
	/**
	 * Retrieves the current world status associated with this event handler.
	 *
	 * @return The WorldStatus enum value representing the event's status.
	 */
	WorldStatus getStatus();
}