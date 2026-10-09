package com.mixologybatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pairs each current order with a distinct processed inventory potion of the
 * same type that was processed at the order's station.
 */
final class OrderMatch
{
	private static final OrderMatch NONE = new OrderMatch(Collections.emptyList(), new int[0]);

	private final List<PotionOrder> orders;
	private final int[] matchedSlots;

	private OrderMatch(List<PotionOrder> orders, int[] matchedSlots)
	{
		this.orders = orders;
		this.matchedSlots = matchedSlots;
	}

	static OrderMatch none()
	{
		return NONE;
	}

	/**
	 * @param orders current orders in conveyor order; null entries are unknown orders
	 * @param inventory current inventory
	 * @param processedStations station that processed the finished potion in each
	 *     inventory slot, or null where it is unknown
	 */
	static OrderMatch match(
		List<PotionOrder> orders,
		List<InventorySlot> inventory,
		Station[] processedStations)
	{
		boolean[] used = new boolean[inventory.size()];
		int[] matchedSlots = new int[orders.size()];
		for (int index = 0; index < orders.size(); index++)
		{
			matchedSlots[index] = -1;
			PotionOrder order = orders.get(index);
			if (order == null)
			{
				continue;
			}
			// Each slot satisfies exactly one (potion, station) key, so taking the
			// first unused match is optimal.
			for (int slot = 0; slot < inventory.size(); slot++)
			{
				InventorySlot actual = inventory.get(slot);
				if (!used[slot]
					&& actual.isFinished()
					&& actual.getPotion() == order.getPotion()
					&& slot < processedStations.length
					&& processedStations[slot] == order.getStation())
				{
					used[slot] = true;
					matchedSlots[index] = slot;
					break;
				}
			}
		}
		return new OrderMatch(Collections.unmodifiableList(new ArrayList<>(orders)), matchedSlots);
	}

	List<PotionOrder> getOrders()
	{
		return orders;
	}

	boolean isFillable(int orderIndex)
	{
		return matchedSlots[orderIndex] >= 0;
	}

	int getMatchedSlot(int orderIndex)
	{
		return matchedSlots[orderIndex];
	}

	int getFillableCount()
	{
		int count = 0;
		for (int slot : matchedSlots)
		{
			if (slot >= 0)
			{
				count++;
			}
		}
		return count;
	}
}
