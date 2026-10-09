package com.mixologybatch;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DigweedTrackerTest
{
	@Test
	public void digweedPresentOnEntryIsShownButNotAnnounced()
	{
		DigweedTracker tracker = new DigweedTracker();

		assertTrue(tracker.observe(readiness(DigweedSpot.SOUTH_WEST)).isEmpty());
		assertEquals(EnumSet.of(DigweedSpot.SOUTH_WEST), tracker.getReady());
	}

	@Test
	public void newSpawnIsAnnouncedOnce()
	{
		DigweedTracker tracker = new DigweedTracker();
		tracker.observe(readiness());

		assertEquals(EnumSet.of(DigweedSpot.NORTH_EAST), tracker.observe(readiness(DigweedSpot.NORTH_EAST)));
		assertTrue(tracker.observe(readiness(DigweedSpot.NORTH_EAST)).isEmpty());
		assertEquals(EnumSet.of(DigweedSpot.NORTH_EAST), tracker.getReady());
	}

	@Test
	public void pickedDigweedClearsAndCanRespawn()
	{
		DigweedTracker tracker = new DigweedTracker();
		tracker.observe(readiness());
		tracker.observe(readiness(DigweedSpot.NORTH_WEST));

		assertTrue(tracker.observe(readiness()).isEmpty());
		assertTrue(tracker.getReady().isEmpty());
		assertEquals(EnumSet.of(DigweedSpot.NORTH_WEST), tracker.observe(readiness(DigweedSpot.NORTH_WEST)));
	}

	@Test
	public void resetSeedsAgainWithoutAnnouncing()
	{
		DigweedTracker tracker = new DigweedTracker();
		tracker.observe(readiness());
		tracker.reset();

		assertTrue(tracker.observe(readiness(DigweedSpot.SOUTH_EAST)).isEmpty());
		assertEquals(EnumSet.of(DigweedSpot.SOUTH_EAST), tracker.getReady());
	}

	private static Map<DigweedSpot, Boolean> readiness(DigweedSpot... readySpots)
	{
		Map<DigweedSpot, Boolean> readiness = new EnumMap<>(DigweedSpot.class);
		for (DigweedSpot spot : DigweedSpot.values())
		{
			readiness.put(spot, false);
		}
		for (DigweedSpot spot : readySpots)
		{
			readiness.put(spot, true);
		}
		return readiness;
	}
}
