package com.mixologybatch;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InspectModeTest
{
	private static final FinishedPotion UNKNOWN = FinishedPotion.unknown(Potion.MAL);
	private static final FinishedPotion KNOWN = FinishedPotion.known(Potion.MAL, Station.CRYSTALLISE);

	@Test
	public void modesAllowTheirConfiguredFinishedPotions()
	{
		assertFalse(InspectMode.OFF.allows(UNKNOWN));
		assertFalse(InspectMode.OFF.allows(KNOWN));
		assertTrue(InspectMode.UNKNOWN_FINISHED.allows(UNKNOWN));
		assertFalse(InspectMode.UNKNOWN_FINISHED.allows(KNOWN));
		assertTrue(InspectMode.ALL_FINISHED.allows(UNKNOWN));
		assertTrue(InspectMode.ALL_FINISHED.allows(KNOWN));
	}
}
