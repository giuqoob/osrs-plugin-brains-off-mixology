package com.mixologybatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OrderMatchTest
{
	@Test
	public void decodesOrderVarbitsWithTheGameModifierOrder()
	{
		assertEquals(new PotionOrder(Potion.MMA, Station.HOMOGENISE), PotionOrder.fromVarbits(2, 1));
		assertEquals(new PotionOrder(Potion.ALA, Station.CONCENTRATE), PotionOrder.fromVarbits(6, 2));
		assertEquals(new PotionOrder(Potion.MAL, Station.CRYSTALLISE), PotionOrder.fromVarbits(10, 3));
	}

	@Test
	public void zeroedOrderVarbitsDecodeToNoOrder()
	{
		assertNull(PotionOrder.fromVarbits(0, 0));
		assertNull(PotionOrder.fromVarbits(2, 0));
		assertNull(PotionOrder.fromVarbits(0, 1));
		assertNull(PotionOrder.fromVarbits(11, 1));
		assertNull(PotionOrder.fromVarbits(2, 4));
	}

	@Test
	public void matchesOnlyFinishedPotionsProcessedAtTheOrderStation()
	{
		List<InventorySlot> inventory = emptyInventory();
		Station[] stations = new Station[inventory.size()];
		inventory.set(0, InventorySlot.fromItemId(Potion.MMA.getUnfinishedItemId()));
		inventory.set(1, finished(Potion.MMA));
		stations[1] = Station.CRYSTALLISE;
		inventory.set(2, finished(Potion.MML));
		stations[2] = Station.HOMOGENISE;

		OrderMatch match = OrderMatch.match(
			Arrays.asList(
				new PotionOrder(Potion.MMA, Station.HOMOGENISE),
				new PotionOrder(Potion.MML, Station.HOMOGENISE),
				new PotionOrder(Potion.MMA, Station.CRYSTALLISE)),
			inventory,
			stations);

		assertFalse(match.isFillable(0));
		assertTrue(match.isFillable(1));
		assertEquals(2, match.getMatchedSlot(1));
		assertTrue(match.isFillable(2));
		assertEquals(1, match.getMatchedSlot(2));
		assertEquals(2, match.getFillableCount());
	}

	@Test
	public void duplicateOrdersNeedDistinctPotions()
	{
		List<InventorySlot> inventory = emptyInventory();
		Station[] stations = new Station[inventory.size()];
		inventory.set(4, finished(Potion.MAL));
		stations[4] = Station.CONCENTRATE;
		PotionOrder order = new PotionOrder(Potion.MAL, Station.CONCENTRATE);

		OrderMatch match = OrderMatch.match(Arrays.asList(order, order, null), inventory, stations);

		assertTrue(match.isFillable(0));
		assertFalse(match.isFillable(1));
		assertFalse(match.isFillable(2));
		assertEquals(1, match.getFillableCount());

		inventory.set(9, finished(Potion.MAL));
		stations[9] = Station.CONCENTRATE;
		assertEquals(2, OrderMatch.match(Arrays.asList(order, order, null), inventory, stations).getFillableCount());
	}

	@Test
	public void unknownProcessingStationNeverMatches()
	{
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(0, finished(Potion.LLL));

		OrderMatch match = OrderMatch.match(
			Arrays.asList(new PotionOrder(Potion.LLL, Station.CRYSTALLISE)),
			inventory,
			new Station[inventory.size()]);

		assertEquals(0, match.getFillableCount());
	}

	@Test
	public void noOrdersMatchNothing()
	{
		assertEquals(0, OrderMatch.none().getFillableCount());
		assertTrue(OrderMatch.none().getOrders().isEmpty());
	}

	static InventorySlot finished(Potion potion)
	{
		return InventorySlot.fromItemId(potion.getFinishedItemId());
	}

	static List<InventorySlot> emptyInventory()
	{
		List<InventorySlot> inventory = new ArrayList<>(BatchPlan.INVENTORY_SIZE);
		for (int slot = 0; slot < BatchPlan.INVENTORY_SIZE; slot++)
		{
			inventory.add(InventorySlot.empty());
		}
		return inventory;
	}
}
