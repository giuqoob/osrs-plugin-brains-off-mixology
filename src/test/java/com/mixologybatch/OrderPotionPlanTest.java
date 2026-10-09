package com.mixologybatch;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static com.mixologybatch.OrderMatchTest.emptyInventory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class OrderPotionPlanTest
{
	private static final Set<Potion> SKIP_TRIPLES = EnumSet.of(Potion.MMM, Potion.AAA);

	/** The wiki XP strategy: three of every non-triple, one per station. */
	private final BatchPlan stock = wikiStock();

	@Test
	public void onlyOrdersTheStockCannotCoverAreBrewed()
	{
		List<PotionOrder> orders = Arrays.asList(
			order(Potion.MMA, Station.HOMOGENISE),
			order(Potion.LLL, Station.CONCENTRATE),
			order(Potion.MMM, Station.CRYSTALLISE));

		assertEquals(
			Collections.singletonList(order(Potion.LLL, Station.CONCENTRATE)),
			stock.ordersToBrew(orders, SKIP_TRIPLES));
	}

	@Test
	public void duplicateOrdersBeyondStockAreBrewedAgain()
	{
		List<PotionOrder> orders = Arrays.asList(
			order(Potion.MMA, Station.HOMOGENISE),
			order(Potion.MMA, Station.HOMOGENISE),
			order(Potion.ALA, Station.CRYSTALLISE));

		assertEquals(
			Collections.singletonList(order(Potion.MMA, Station.HOMOGENISE)),
			stock.ordersToBrew(orders, SKIP_TRIPLES));
	}

	@Test
	public void unknownOrdersBrewNothing()
	{
		assertTrue(stock.ordersToBrew(Arrays.asList(order(Potion.LLL, Station.CONCENTRATE), null, null), SKIP_TRIPLES).isEmpty());
		assertSame(stock, stock.withOrderPotions(Collections.emptyList()));
	}

	@Test
	public void orderPotionsFollowAllStockInStationOrder()
	{
		BatchPlan withOrders = stock.withOrderPotions(Arrays.asList(
			order(Potion.LLL, Station.CONCENTRATE),
			order(Potion.MMM, Station.CRYSTALLISE)));

		assertEquals(23, withOrders.size());
		assertSame(stock, withOrders.getBase());
		for (int rank = 0; rank < stock.size(); rank++)
		{
			assertEquals(stock.get(rank).getPotion(), withOrders.get(rank).getPotion());
			assertEquals(stock.get(rank).getStation(), withOrders.get(rank).getStation());
			assertFalse(withOrders.get(rank).isOrderPotion());
		}

		BatchEntry crystallise = withOrders.get(21);
		assertEquals(Potion.MMM, crystallise.getPotion());
		assertEquals(Station.CRYSTALLISE, crystallise.getStation());
		assertTrue(crystallise.isOrderPotion());
		assertEquals(7, crystallise.getStationPosition());
		assertEquals(8, crystallise.getStationTotal());
		assertEquals(8, withOrders.get(0).getStationTotal());

		BatchEntry concentrate = withOrders.get(22);
		assertEquals(Potion.LLL, concentrate.getPotion());
		assertEquals(Station.CONCENTRATE, concentrate.getStation());
		assertTrue(concentrate.isOrderPotion());
	}

	@Test
	public void orderPotionsThatDoNotFitAreCountedNotForcedIn()
	{
		Map<Potion, Integer> counts = new EnumMap<>(Potion.class);
		counts.put(Potion.MAL, 27);
		BatchPlan full = BatchPlan.create(counts, StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE);

		BatchPlan withOrders = full.withOrderPotions(Arrays.asList(
			order(Potion.LLL, Station.CONCENTRATE),
			order(Potion.MMA, Station.HOMOGENISE)));

		assertEquals(28, withOrders.size());
		assertEquals(Potion.MMA, withOrders.get(27).getPotion());
		assertEquals(1, withOrders.getDroppedOrderCount());
	}

	@Test
	public void refillMixesStockFirstThenOrderPotionsAtTheEnd()
	{
		BatchPlan withOrders = stock.withOrderPotions(Collections.singletonList(order(Potion.LLL, Station.CONCENTRATE)));
		List<InventorySlot> inventory = emptyInventory();
		CyclePlan cycle = CyclePlan.create(withOrders, inventory);
		assertTrue(cycle.belongsTo(stock));

		for (int rank = 0; rank < withOrders.size(); rank++)
		{
			int nextSlot = cycle.firstEmptySlot(inventory);
			Potion potion = BatchStateResolver.nextNeededPotion(withOrders, cycle, inventory, nextSlot);
			assertEquals(withOrders.get(rank).getPotion(), potion);
			inventory.set(nextSlot, InventorySlot.fromItemId(potion.getUnfinishedItemId()));
		}
		assertEquals(Potion.LLL, inventory.get(21).getPotion());
		assertEquals(-1, cycle.firstEmptySlot(inventory));
	}

	@Test
	public void orderPotionIsUsedOnItsStationAfterThatStationsStock()
	{
		BatchPlan withOrders = stock.withOrderPotions(Arrays.asList(
			order(Potion.MMM, Station.CRYSTALLISE),
			order(Potion.LLL, Station.CONCENTRATE)));
		List<InventorySlot> inventory = emptyInventory();
		for (BatchEntry entry : withOrders.getEntries())
		{
			inventory.set(entry.getInventorySlot(), InventorySlot.fromItemId(entry.getPotion().getUnfinishedItemId()));
		}
		CyclePlan cycle = CyclePlan.create(withOrders, inventory);
		BatchStateResolver resolver = new BatchStateResolver();

		finishStock(inventory, withOrders, Station.CRYSTALLISE);
		Guidance guidance = process(resolver, withOrders, cycle, inventory);
		assertEquals(Guidance.Action.USE_ITEM_ON_STATION, guidance.getAction());
		assertEquals(21, guidance.getEntry().getInventorySlot());
		assertEquals(Station.CRYSTALLISE, guidance.getEntry().getStation());

		finish(inventory, 21);
		guidance = process(resolver, withOrders, cycle, inventory);
		assertEquals(Guidance.Action.USE_STATION, guidance.getAction());
		assertEquals(Station.HOMOGENISE, guidance.getEntry().getStation());
		assertFalse(guidance.getEntry().isOrderPotion());

		finishStock(inventory, withOrders, Station.HOMOGENISE);
		finishStock(inventory, withOrders, Station.CONCENTRATE);
		guidance = process(resolver, withOrders, cycle, inventory);
		// Nothing unfinished precedes the last order potion, so a left-click takes it.
		assertEquals(Guidance.Action.USE_STATION, guidance.getAction());
		assertEquals(22, guidance.getEntry().getInventorySlot());
		assertTrue(guidance.getEntry().isOrderPotion());
	}

	@Test
	public void useItemStepKeepsTheStationGuarded()
	{
		BatchEntry orderEntry = stock.withOrderPotions(Collections.singletonList(order(Potion.MMM, Station.CRYSTALLISE))).get(21);
		Guidance guidance = Guidance.useItemOnStation(orderEntry);

		assertFalse(StationMenuGuard.canUseStation(guidance, Station.CRYSTALLISE, false, null));
		assertTrue(StationMenuGuard.canUseStation(guidance, Station.CRYSTALLISE, true, null));
	}

	@Test
	public void activeStationAfterUsingTheOrderPotionWaitsOnIt()
	{
		BatchPlan withOrders = stock.withOrderPotions(Collections.singletonList(order(Potion.MMM, Station.CRYSTALLISE)));
		List<InventorySlot> inventory = emptyInventory();
		for (BatchEntry entry : withOrders.getEntries())
		{
			inventory.set(entry.getInventorySlot(), InventorySlot.fromItemId(entry.getPotion().getFinishedItemId()));
		}
		inventory.set(21, InventorySlot.empty());
		inventory.set(7, InventorySlot.fromItemId(withOrders.get(7).getPotion().getUnfinishedItemId()));
		CyclePlan cycle = CyclePlan.create(withOrders, inventory);
		Map<Station, Potion> active = new EnumMap<>(Station.class);
		active.put(Station.CRYSTALLISE, Potion.MMM);

		Guidance guidance = new BatchStateResolver().resolve(
			withOrders, cycle, false, inventory, null, new int[]{0, 0, 0}, active,
			Guidance.useItemOnStation(withOrders.get(21)));

		assertEquals(Guidance.Action.WAIT_STATION, guidance.getAction());
		assertTrue(guidance.getEntry().isOrderPotion());
	}

	private static Guidance process(BatchStateResolver resolver, BatchPlan plan, CyclePlan cycle, List<InventorySlot> inventory)
	{
		return resolver.resolve(
			plan, cycle, false, inventory, null, new int[]{0, 0, 0}, new EnumMap<>(Station.class), Guidance.outside());
	}

	private static void finishStock(List<InventorySlot> inventory, BatchPlan plan, Station station)
	{
		for (BatchEntry entry : plan.getEntries())
		{
			if (entry.getStation() == station && !entry.isOrderPotion())
			{
				finish(inventory, entry.getInventorySlot());
			}
		}
	}

	private static void finish(List<InventorySlot> inventory, int slot)
	{
		inventory.set(slot, InventorySlot.fromItemId(inventory.get(slot).getPotion().getFinishedItemId()));
	}

	private static BatchPlan wikiStock()
	{
		Map<Potion, Integer> counts = new EnumMap<>(Potion.class);
		for (Potion potion : Arrays.asList(Potion.MMA, Potion.MML, Potion.AAM, Potion.ALA, Potion.MLL, Potion.ALL, Potion.MAL))
		{
			counts.put(potion, 3);
		}
		return BatchPlan.create(counts, StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE);
	}

	private static PotionOrder order(Potion potion, Station station)
	{
		return new PotionOrder(potion, station);
	}

	@Test
	public void stockPlanIsItsOwnBase()
	{
		assertSame(stock, stock.getBase());
		assertEquals(0, stock.getDroppedOrderCount());
	}
}
