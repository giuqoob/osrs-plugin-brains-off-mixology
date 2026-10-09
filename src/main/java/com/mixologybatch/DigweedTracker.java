package com.mixologybatch;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks which Digweed corners are ready. The first observation after entering
 * the lab only seeds the state, so a Digweed that was already there is
 * highlighted but not announced as a new spawn.
 */
final class DigweedTracker
{
	private final Set<DigweedSpot> ready = EnumSet.noneOf(DigweedSpot.class);
	private boolean seeded;

	/**
	 * @return corners whose Digweed appeared since the previous observation
	 */
	Set<DigweedSpot> observe(Map<DigweedSpot, Boolean> readiness)
	{
		Set<DigweedSpot> spawned = EnumSet.noneOf(DigweedSpot.class);
		for (DigweedSpot spot : DigweedSpot.values())
		{
			boolean isReady = readiness.getOrDefault(spot, false);
			if (isReady && ready.add(spot) && seeded)
			{
				spawned.add(spot);
			}
			else if (!isReady)
			{
				ready.remove(spot);
			}
		}
		seeded = true;
		return spawned;
	}

	Set<DigweedSpot> getReady()
	{
		return Collections.unmodifiableSet(ready);
	}

	void reset()
	{
		ready.clear();
		seeded = false;
	}
}
