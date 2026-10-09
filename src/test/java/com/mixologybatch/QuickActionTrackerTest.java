package com.mixologybatch;

import net.runelite.api.gameval.SpotanimID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QuickActionTrackerTest
{
	@Test
	public void spotanimsMapToTheirStations()
	{
		assertEquals(QuickAction.AGITATOR, QuickAction.fromSpotanim(SpotanimID.VFX_MACHINERY_ALCHEMY01_AGITATOR01));
		assertEquals(QuickAction.ALEMBIC, QuickAction.fromSpotanim(SpotanimID.VFX_MACHINERY_ALCHEMY01_ALEMBIC01));
		assertNull(QuickAction.fromSpotanim(0));
		assertNull(QuickAction.forStation(Station.CONCENTRATE));
	}

	@Test
	public void emptyStationNeverOpensAWindow()
	{
		QuickActionTracker tracker = new QuickActionTracker();
		tracker.open(QuickAction.AGITATOR, false, 3, 0);

		assertFalse(tracker.isOpen(Station.HOMOGENISE));
	}

	@Test
	public void agitatorWindowLastsTwoProgressSteps()
	{
		QuickActionTracker tracker = new QuickActionTracker();
		tracker.open(QuickAction.AGITATOR, true, 3, 0);
		assertTrue(tracker.isOpen(Station.HOMOGENISE));
		assertFalse(tracker.isOpen(Station.CRYSTALLISE));

		tracker.update(QuickAction.AGITATOR, true, 4, 0);
		tracker.update(QuickAction.AGITATOR, true, 5, 0);
		assertTrue(tracker.isOpen(Station.HOMOGENISE));

		tracker.update(QuickAction.AGITATOR, true, 6, 0);
		assertFalse(tracker.isOpen(Station.HOMOGENISE));
	}

	@Test
	public void alembicWindowLastsOneProgressStep()
	{
		QuickActionTracker tracker = new QuickActionTracker();
		tracker.open(QuickAction.ALEMBIC, true, 7, 1);
		tracker.update(QuickAction.ALEMBIC, true, 8, 1);
		assertTrue(tracker.isOpen(Station.CRYSTALLISE));

		tracker.update(QuickAction.ALEMBIC, true, 9, 1);
		assertFalse(tracker.isOpen(Station.CRYSTALLISE));
	}

	@Test
	public void hitMissOrEmptyStationClosesTheWindow()
	{
		QuickActionTracker tracker = new QuickActionTracker();

		// A stale non-zero skillshot value is the baseline; only a change is a hit.
		tracker.open(QuickAction.AGITATOR, true, 3, 1);
		tracker.update(QuickAction.AGITATOR, true, 3, 1);
		assertTrue(tracker.isOpen(Station.HOMOGENISE));
		tracker.update(QuickAction.AGITATOR, true, 3, 2);
		assertFalse(tracker.isOpen(Station.HOMOGENISE));

		tracker.open(QuickAction.AGITATOR, true, 3, 0);
		tracker.update(QuickAction.AGITATOR, true, 1, 0);
		assertFalse(tracker.isOpen(Station.HOMOGENISE));

		tracker.open(QuickAction.ALEMBIC, true, 3, 0);
		tracker.update(QuickAction.ALEMBIC, false, 3, 0);
		assertFalse(tracker.isOpen(Station.CRYSTALLISE));
		assertFalse(tracker.hasOpenWindow());
	}
}
