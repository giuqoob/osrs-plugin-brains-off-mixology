package com.mixologybatch;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OrderFulfillmentTest
{
	@Test
	public void unknownRequestedRecipeNeedsInspection()
	{
		assertEquals(OrderFulfillment.Status.UNKNOWN,
			OrderFulfillment.evaluate(
				Collections.singletonList(FinishedPotion.unknown(Potion.MAL)),
				Collections.singletonList(new OrderFulfillment.Order(Potion.MAL, Station.CRYSTALLISE))));
	}

	@Test
	public void confirmedMatchTakesPrecedence()
	{
		assertEquals(OrderFulfillment.Status.READY,
			OrderFulfillment.evaluate(
				Arrays.asList(
					FinishedPotion.unknown(Potion.MAL),
					FinishedPotion.known(Potion.MAL, Station.CRYSTALLISE)),
				Collections.singletonList(new OrderFulfillment.Order(Potion.MAL, Station.CRYSTALLISE))));
	}

	@Test
	public void unrelatedUnknownRecipeDoesNotMatch()
	{
		assertEquals(OrderFulfillment.Status.NO_MATCH,
			OrderFulfillment.evaluate(
				Collections.singletonList(FinishedPotion.unknown(Potion.MMM)),
				Collections.singletonList(new OrderFulfillment.Order(Potion.MAL, Station.CRYSTALLISE))));
	}

	@Test
	public void knownWrongModifierDoesNotMatch()
	{
		assertEquals(OrderFulfillment.Status.NO_MATCH,
			OrderFulfillment.evaluate(
				Collections.singletonList(FinishedPotion.known(Potion.MAL, Station.HOMOGENISE)),
				Collections.singletonList(new OrderFulfillment.Order(Potion.MAL, Station.CRYSTALLISE))));
	}

	@Test
	public void matchesLaterCurrentOrder()
	{
		assertEquals(OrderFulfillment.Status.READY,
			OrderFulfillment.evaluate(
				Collections.singletonList(FinishedPotion.known(Potion.MAL, Station.CRYSTALLISE)),
				Arrays.asList(
					new OrderFulfillment.Order(Potion.MMM, Station.HOMOGENISE),
					new OrderFulfillment.Order(Potion.MAL, Station.CRYSTALLISE),
					new OrderFulfillment.Order(Potion.AAA, Station.CONCENTRATE))));
	}
}
