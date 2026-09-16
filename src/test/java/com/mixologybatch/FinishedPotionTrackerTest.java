package com.mixologybatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FinishedPotionTrackerTest
{
	@Test
	public void existingFinishedPotionsStartUnknown()
	{
		FinishedPotionTracker tracker = new FinishedPotionTracker();
		tracker.initialize(
			inventoryWith(Potion.MAL.getFinishedItemId()),
			Collections.emptyMap());

		assertEquals(1, tracker.snapshot().size());
		assertEquals(Optional.empty(), tracker.snapshot().get(0).getStation());
	}

	@Test
	public void completedPotionMatchesOneClearReturn()
	{
		FinishedPotionTracker tracker = trackerWithOccupiedStation();
		List<InventorySlot> inventory = inventoryWith(Potion.MAL.getFinishedItemId());

		tracker.observeInventory(inventory);
		tracker.observeStation(Station.HOMOGENISE, null);
		tracker.finishTick();

		assertEquals(
			Optional.of(Station.HOMOGENISE),
			tracker.snapshot().get(0).getStation());
	}

	@Test
	public void unfinishedEarlyRemovalDoesNotCreateFinishedPotion()
	{
		FinishedPotionTracker tracker = trackerWithOccupiedStation();

		tracker.observeStation(Station.HOMOGENISE, null);
		tracker.observeInventory(inventoryWith(Potion.MAL.getUnfinishedItemId()));
		tracker.finishTick();

		assertEquals(Collections.emptyMap(), tracker.snapshot());
	}

	@Test
	public void explicitSwapExchangesKnowledgeForIdenticalRecipes()
	{
		FinishedPotionTracker tracker = trackerWithInspectedMixalots();

		tracker.observeSwap(0, 1);
		tracker.observeInventory(finishedMixalots(2));

		assertEquals(Optional.of(Station.CONCENTRATE), tracker.snapshot().get(0).getStation());
		assertEquals(Optional.of(Station.HOMOGENISE), tracker.snapshot().get(1).getStation());
	}

	@Test
	public void deliveryRemovesOnePotionAndPreservesSurvivor()
	{
		FinishedPotionTracker tracker = trackerWithInspectedMixalots();
		List<InventorySlot> inventory = finishedMixalots(2);
		inventory.set(0, InventorySlot.empty());

		tracker.observeInventory(inventory);

		assertFalse(tracker.snapshot().containsKey(0));
		assertEquals(Optional.of(Station.CONCENTRATE), tracker.snapshot().get(1).getStation());
	}

	@Test
	public void inspectionSetsProcessingForExpectedPotion()
	{
		FinishedPotionTracker tracker = new FinishedPotionTracker();
		tracker.initialize(
			inventoryWith(Potion.MAL.getFinishedItemId()),
			Collections.emptyMap());

		tracker.beginInspection(0);
		tracker.resolveInspection("It's a vial of Crystallised Mixalot.");

		assertEquals(Optional.of(Station.CRYSTALLISE), tracker.snapshot().get(0).getStation());
	}

	private static FinishedPotionTracker trackerWithOccupiedStation()
	{
		FinishedPotionTracker tracker = new FinishedPotionTracker();
		Map<Station, Potion> stations = new EnumMap<>(Station.class);
		stations.put(Station.HOMOGENISE, Potion.MAL);
		tracker.initialize(inventoryWith(0), stations);
		return tracker;
	}

	private static FinishedPotionTracker trackerWithInspectedMixalots()
	{
		FinishedPotionTracker tracker = new FinishedPotionTracker();
		tracker.initialize(finishedMixalots(2), Collections.emptyMap());
		tracker.beginInspection(0);
		tracker.resolveInspection("It's a vial of Homogenised Mixalot.");
		tracker.beginInspection(1);
		tracker.resolveInspection("It's a vial of Concentrated Mixalot.");
		return tracker;
	}

	private static List<InventorySlot> inventoryWith(int itemId)
	{
		List<InventorySlot> inventory = new ArrayList<>();
		inventory.add(InventorySlot.fromItemId(itemId));
		return inventory;
	}

	private static List<InventorySlot> finishedMixalots(int size)
	{
		List<InventorySlot> inventory = new ArrayList<>(size);
		for (int slot = 0; slot < size; slot++)
		{
			inventory.add(InventorySlot.fromItemId(Potion.MAL.getFinishedItemId()));
		}
		return inventory;
	}
}
