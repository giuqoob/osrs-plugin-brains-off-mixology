package com.mixologybatch;

import java.util.Optional;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PotionInspectionTest
{
	@Test
	public void parsesInspectedCrystalisedMixalot()
	{
		String text = "It's a vial of <col=6800bf>Crystalised Mixalot</col>. "
			+ "Its quality has been<br>improved!";

		FinishedPotion potion = PotionInspection.parse(text).get();

		assertEquals(Potion.MAL, potion.getPotion());
		assertEquals(Optional.of(Station.CRYSTALLISE), potion.getStation());
	}

	@Test
	public void parsesAlternateCrystallisedSpelling()
	{
		assertPotion(
			"It's a vial of Crystallised Mystic mana amalgam.",
			Potion.MMA,
			Station.CRYSTALLISE);
	}

	@Test
	public void parsesHomogenousPotion()
	{
		assertPotion(
			"It's a vial of <col=00ff00>Homogenous Marley's moonlight</col>.",
			Potion.MML,
			Station.HOMOGENISE);
	}

	@Test
	public void parsesAlternateHomogenisedSpelling()
	{
		assertPotion(
			"It's a vial of Homogenised Aqualux amalgam.",
			Potion.ALA,
			Station.HOMOGENISE);
	}

	@Test
	public void parsesConcentratedPotion()
	{
		assertPotion(
			"It's a vial of Concentrated Anti-leech lotion.",
			Potion.ALL,
			Station.CONCENTRATE);
	}

	@Test
	public void rejectsUnrelatedDialog()
	{
		assertFalse(PotionInspection.parse("You inspect a Concentrated Mixalot.").isPresent());
		assertFalse(PotionInspection.parse(null).isPresent());
	}

	private static void assertPotion(String text, Potion expectedPotion, Station expectedStation)
	{
		FinishedPotion potion = PotionInspection.parse(text).get();

		assertEquals(expectedPotion, potion.getPotion());
		assertEquals(Optional.of(expectedStation), potion.getStation());
	}
}
