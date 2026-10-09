package com.mixologybatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BatchPlan
{
	static final int INVENTORY_SIZE = 28;

	private final List<BatchEntry> entries;
	private final String error;
	private final Station[] stations;
	private final BatchPlan base;
	private final int droppedOrderCount;

	private BatchPlan(List<BatchEntry> entries, String error)
	{
		this(entries, error, new Station[0], null, 0);
	}

	private BatchPlan(
		List<BatchEntry> entries,
		String error,
		Station[] stations,
		BatchPlan base,
		int droppedOrderCount)
	{
		this.entries = entries;
		this.error = error;
		this.stations = stations;
		this.base = base == null ? this : base;
		this.droppedOrderCount = droppedOrderCount;
	}

	static BatchPlan create(Map<Potion, Integer> counts, StationOrder stationOrder)
	{
		Station[] stations = stationOrder.getStations();
		List<List<Potion>> batches = new ArrayList<>(stations.length);
		for (int i = 0; i < stations.length; i++)
		{
			batches.add(new ArrayList<>());
		}

		int total = 0;
		for (Potion potion : Potion.values())
		{
			int count = counts.getOrDefault(potion, 0);
			if (count < 0)
			{
				return invalid("Potion counts cannot be negative.");
			}
			total += count;
			for (int copy = 0; copy < count; copy++)
			{
				batches.get(copy % stations.length).add(potion);
			}
		}

		if (total > INVENTORY_SIZE)
		{
			return invalid("Configured batch has " + total + " potions; reduce it to 28 or fewer.");
		}

		List<BatchEntry> result = new ArrayList<>(total);
		int slot = 0;
		for (int stationOrdinal = 0; stationOrdinal < stations.length; stationOrdinal++)
		{
			List<Potion> batch = batches.get(stationOrdinal);
			for (int position = 0; position < batch.size(); position++)
			{
				result.add(new BatchEntry(
					batch.get(position),
					stations[stationOrdinal],
					slot++,
					stationOrdinal,
					position,
					batch.size()));
			}
		}

		return new BatchPlan(Collections.unmodifiableList(result), null, stations, null, 0);
	}

	/**
	 * Current orders that the configured stock cannot cover, counting duplicate
	 * orders separately. Skipped potions are never brewed for orders, and
	 * incomplete order data brews nothing.
	 */
	List<PotionOrder> ordersToBrew(List<PotionOrder> orders, Set<Potion> skippedPotions)
	{
		List<PotionOrder> result = new ArrayList<>();
		if (orders.contains(null))
		{
			return result;
		}
		List<PotionOrder> covering = new ArrayList<>();
		for (PotionOrder order : orders)
		{
			if (skippedPotions.contains(order.getPotion()))
			{
				continue;
			}
			covering.add(order);
			if (Collections.frequency(covering, order) > getConfiguredCount(order.getStation(), order.getPotion()))
			{
				result.add(order);
			}
		}
		return result;
	}

	/**
	 * Appends order potions after all stock, grouped in station order, so the
	 * stock layout is unchanged. Orders that don't fit in the inventory are
	 * dropped and counted rather than displacing stock.
	 */
	BatchPlan withOrderPotions(List<PotionOrder> orders)
	{
		if (!isValid() || orders.isEmpty())
		{
			return this;
		}

		int room = INVENTORY_SIZE - entries.size();
		List<List<Potion>> extras = new ArrayList<>(stations.length);
		for (int index = 0; index < stations.length; index++)
		{
			extras.add(new ArrayList<>());
		}
		int accepted = 0;
		for (int stationOrdinal = 0; stationOrdinal < stations.length; stationOrdinal++)
		{
			for (PotionOrder order : orders)
			{
				if (order.getStation() == stations[stationOrdinal] && accepted < room)
				{
					extras.get(stationOrdinal).add(order.getPotion());
					accepted++;
				}
			}
		}

		int[] stockCounts = new int[stations.length];
		for (BatchEntry entry : entries)
		{
			stockCounts[entry.getStationOrdinal()]++;
		}

		List<BatchEntry> result = new ArrayList<>(entries.size() + accepted);
		for (BatchEntry entry : entries)
		{
			int stationOrdinal = entry.getStationOrdinal();
			result.add(new BatchEntry(
				entry.getPotion(),
				entry.getStation(),
				entry.getInventorySlot(),
				stationOrdinal,
				entry.getStationPosition(),
				stockCounts[stationOrdinal] + extras.get(stationOrdinal).size()));
		}
		int slot = entries.size();
		for (int stationOrdinal = 0; stationOrdinal < stations.length; stationOrdinal++)
		{
			List<Potion> stationExtras = extras.get(stationOrdinal);
			int stationTotal = stockCounts[stationOrdinal] + stationExtras.size();
			for (int index = 0; index < stationExtras.size(); index++)
			{
				result.add(new BatchEntry(
					stationExtras.get(index),
					stations[stationOrdinal],
					slot++,
					stationOrdinal,
					stockCounts[stationOrdinal] + index,
					stationTotal,
					true));
			}
		}
		return new BatchPlan(
			Collections.unmodifiableList(result),
			null,
			stations,
			base,
			orders.size() - accepted);
	}

	/**
	 * The configured stock plan this plan was derived from; itself for a stock plan.
	 */
	BatchPlan getBase()
	{
		return base;
	}

	/**
	 * Orders that needed brewing but did not fit in the inventory.
	 */
	int getDroppedOrderCount()
	{
		return droppedOrderCount;
	}

	static BatchPlan defaultPlan()
	{
		EnumMap<Potion, Integer> counts = new EnumMap<>(Potion.class);
		counts.put(Potion.MMA, 3);
		counts.put(Potion.MML, 4);
		counts.put(Potion.AAM, 3);
		counts.put(Potion.ALA, 3);
		counts.put(Potion.LLL, 3);
		counts.put(Potion.MLL, 3);
		counts.put(Potion.ALL, 3);
		counts.put(Potion.MAL, 6);
		return create(counts, StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE);
	}

	private static BatchPlan invalid(String error)
	{
		return new BatchPlan(Collections.emptyList(), error);
	}

	boolean isValid()
	{
		return error == null;
	}

	String getError()
	{
		return error;
	}

	int size()
	{
		return entries.size();
	}

	BatchEntry get(int slot)
	{
		return entries.get(slot);
	}

	List<BatchEntry> getEntries()
	{
		return entries;
	}

	int getConfiguredCount(Potion potion)
	{
		int count = 0;
		for (BatchEntry entry : entries)
		{
			if (entry.getPotion() == potion)
			{
				count++;
			}
		}
		return count;
	}

	int getConfiguredCount(Station station, Potion potion)
	{
		int count = 0;
		for (BatchEntry entry : entries)
		{
			if (entry.getStation() == station && entry.getPotion() == potion)
			{
				count++;
			}
		}
		return count;
	}
}
