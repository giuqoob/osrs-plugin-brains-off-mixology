package com.mixologybatch;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.mixologybatch.OrderMatchTest.emptyInventory;
import static com.mixologybatch.OrderMatchTest.finished;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProcessedStationTrackerTest
{
	@Test
	public void recordsTheStationThatReleasedThePotionOnTheSameTick()
	{
		ProcessedStationTracker tracker = new ProcessedStationTracker();
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(0, unfinished(Potion.MMA));
		tracker.observe(inventory, stations(), 1);

		inventory.set(0, InventorySlot.empty());
		tracker.observe(inventory, stations(Station.HOMOGENISE, Potion.MMA), 2);

		inventory.set(0, finished(Potion.MMA));
		tracker.observe(inventory, stations(), 3);

		assertEquals(Station.HOMOGENISE, tracker.stationFor(0));
	}

	@Test
	public void pairsAStationReleaseThatArrivesAfterTheInventoryChange()
	{
		ProcessedStationTracker tracker = new ProcessedStationTracker();
		List<InventorySlot> inventory = emptyInventory();
		tracker.observe(inventory, stations(Station.CRYSTALLISE, Potion.MAL), 1);

		inventory.set(5, finished(Potion.MAL));
		tracker.observe(inventory, stations(Station.CRYSTALLISE, Potion.MAL), 2);
		assertNull(tracker.stationFor(5));

		tracker.observe(inventory, stations(), 3);
		assertEquals(Station.CRYSTALLISE, tracker.stationFor(5));
	}

	@Test
	public void unpairedChangesExpire()
	{
		ProcessedStationTracker tracker = new ProcessedStationTracker();
		List<InventorySlot> inventory = emptyInventory();
		tracker.observe(inventory, stations(Station.CONCENTRATE, Potion.LLL), 1);
		tracker.observe(inventory, stations(), 2);

		inventory.set(3, finished(Potion.LLL));
		tracker.observe(inventory, stations(), 2 + ProcessedStationTracker.PAIRING_TICKS + 1);

		assertNull(tracker.stationFor(3));
	}

	@Test
	public void forgetsDepositedPotionsAndFollowsDraggedOnes()
	{
		ProcessedStationTracker tracker = new ProcessedStationTracker();
		List<InventorySlot> inventory = emptyInventory();
		tracker.observe(inventory, stations(Station.CONCENTRATE, Potion.ALL), 1);
		inventory.set(2, finished(Potion.ALL));
		tracker.observe(inventory, stations(), 2);
		assertEquals(Station.CONCENTRATE, tracker.stationFor(2));

		inventory.set(2, InventorySlot.empty());
		inventory.set(7, finished(Potion.ALL));
		tracker.observe(inventory, stations(), 3);
		assertNull(tracker.stationFor(2));
		assertEquals(Station.CONCENTRATE, tracker.stationFor(7));

		inventory.set(7, InventorySlot.empty());
		tracker.observe(inventory, stations(), 4);
		assertNull(tracker.stationFor(7));
	}

	@Test
	public void potionsPresentBeforeTrackingStartedAreUnknown()
	{
		ProcessedStationTracker tracker = new ProcessedStationTracker();
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(0, finished(Potion.MMA));
		tracker.observe(inventory, stations(), 1);
		tracker.observe(inventory, stations(), 2);

		assertNull(tracker.stationFor(0));
	}

	private static InventorySlot unfinished(Potion potion)
	{
		return InventorySlot.fromItemId(potion.getUnfinishedItemId());
	}

	private static Map<Station, Potion> stations()
	{
		return new EnumMap<>(Station.class);
	}

	private static Map<Station, Potion> stations(Station station, Potion potion)
	{
		Map<Station, Potion> active = stations();
		active.put(station, potion);
		return active;
	}
}
