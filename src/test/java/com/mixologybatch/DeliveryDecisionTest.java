package com.mixologybatch;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static com.mixologybatch.OrderMatchTest.emptyInventory;
import static com.mixologybatch.OrderMatchTest.finished;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeliveryDecisionTest
{
	private static final Set<Potion> SKIP_TRIPLES = EnumSet.of(Potion.MMM, Potion.AAA);

	private final List<InventorySlot> inventory = emptyInventory();
	private final Station[] stations = new Station[inventory.size()];

	@Test
	public void thresholdOfTwoWaitsForASecondFillableOrder()
	{
		put(0, Potion.MMA, Station.HOMOGENISE);
		List<PotionOrder> orders = Arrays.asList(
			order(Potion.MMA, Station.HOMOGENISE),
			order(Potion.MML, Station.CONCENTRATE),
			order(Potion.ALA, Station.CRYSTALLISE));

		DeliveryDecision decision = decide(orders, 2, true);
		assertEquals(DeliveryDecision.Kind.REFILL, decision.getKind());
		assertEquals(1, decision.getFillable());
		assertEquals(2, decision.getRequired());

		put(1, Potion.ALA, Station.CRYSTALLISE);
		assertEquals(DeliveryDecision.Kind.DELIVER, decide(orders, 2, true).getKind());
	}

	@Test
	public void skippedOrdersLowerTheTargetToWhatIsPossible()
	{
		List<PotionOrder> orders = Arrays.asList(
			order(Potion.MMM, Station.HOMOGENISE),
			order(Potion.MMM, Station.CONCENTRATE),
			order(Potion.MML, Station.CRYSTALLISE));

		DeliveryDecision missing = decide(orders, 2, true);
		assertEquals(DeliveryDecision.Kind.REFILL, missing.getKind());
		assertEquals(1, missing.getRequired());

		put(3, Potion.MML, Station.CRYSTALLISE);
		DeliveryDecision ready = decide(orders, 2, true);
		assertEquals(DeliveryDecision.Kind.DELIVER, ready.getKind());
		assertEquals(1, ready.getRequired());
	}

	@Test
	public void fillableSkippedOrdersStillCount()
	{
		put(0, Potion.MMM, Station.HOMOGENISE);
		put(1, Potion.MML, Station.CRYSTALLISE);
		List<PotionOrder> orders = Arrays.asList(
			order(Potion.MMM, Station.HOMOGENISE),
			order(Potion.AAA, Station.CONCENTRATE),
			order(Potion.MML, Station.CRYSTALLISE));

		DeliveryDecision decision = decide(orders, 3, true);
		assertEquals(DeliveryDecision.Kind.DELIVER, decision.getKind());
		assertEquals(2, decision.getRequired());
		assertEquals(2, decision.getFillable());
	}

	@Test
	public void allSkippedOrdersAllowAWasteDepositOfTheFirstPotion()
	{
		put(4, Potion.MAL, Station.CRYSTALLISE);
		put(9, Potion.MML, Station.HOMOGENISE);
		List<PotionOrder> orders = Arrays.asList(
			order(Potion.MMM, Station.HOMOGENISE),
			order(Potion.AAA, Station.CONCENTRATE),
			order(Potion.MMM, Station.CRYSTALLISE));

		DeliveryDecision decision = decide(orders, 2, true);
		assertEquals(DeliveryDecision.Kind.WASTE, decision.getKind());
		assertEquals(4, decision.getWastedSlot());
		assertEquals(Potion.MAL, decision.getWastedPotion());
	}

	@Test
	public void fullInventoryWithoutMatchesMustWaste()
	{
		put(0, Potion.ALL, Station.CONCENTRATE);
		List<PotionOrder> orders = Arrays.asList(
			order(Potion.MMA, Station.HOMOGENISE),
			order(Potion.MML, Station.CONCENTRATE),
			order(Potion.ALA, Station.CRYSTALLISE));

		assertEquals(DeliveryDecision.Kind.WASTE, decide(orders, 1, false).getKind());
	}

	@Test
	public void missingOrderDataFallsBackToTheOriginalBehaviour()
	{
		put(0, Potion.MMA, Station.HOMOGENISE);
		assertEquals(
			DeliveryDecision.Kind.UNKNOWN,
			decide(Arrays.asList(order(Potion.MMA, Station.HOMOGENISE), null, null), 1, true).getKind());
		assertEquals(
			DeliveryDecision.Kind.UNKNOWN,
			DeliveryDecision.decide(OrderMatch.none(), SKIP_TRIPLES, 1, true, inventory).getKind());
	}

	@Test
	public void canRefillNeedsRoomAndAMissingConfiguredPotion()
	{
		EnumMap<Potion, Integer> counts = new EnumMap<>(Potion.class);
		counts.put(Potion.MMA, 3);
		BatchPlan plan = BatchPlan.create(counts, StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE);
		List<InventorySlot> refill = emptyInventory();
		assertTrue(BatchStateResolver.canRefill(plan, refill));

		refill.set(0, finished(Potion.MMA));
		refill.set(1, finished(Potion.MMA));
		refill.set(2, finished(Potion.MMA));
		assertFalse(BatchStateResolver.canRefill(plan, refill));
		assertFalse(BatchStateResolver.canRefill(BatchPlan.create(new EnumMap<>(Potion.class),
			StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE), refill));
	}

	private DeliveryDecision decide(List<PotionOrder> orders, int threshold, boolean canRefill)
	{
		return DeliveryDecision.decide(
			OrderMatch.match(orders, inventory, stations),
			SKIP_TRIPLES,
			threshold,
			canRefill,
			inventory);
	}

	private void put(int slot, Potion potion, Station station)
	{
		inventory.set(slot, finished(potion));
		stations[slot] = station;
	}

	private static PotionOrder order(Potion potion, Station station)
	{
		return new PotionOrder(potion, station);
	}
}
