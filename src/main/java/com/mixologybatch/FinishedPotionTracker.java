package com.mixologybatch;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class FinishedPotionTracker
{
	private final Map<Integer, FinishedPotion> finishedPotions = new HashMap<>();
	private final EnumMap<Station, Potion> stationContents = new EnumMap<>(Station.class);
	private final List<Arrival> pendingArrivals = new ArrayList<>();
	private final List<Completion> pendingCompletions = new ArrayList<>();
	private int inventorySize;
	private boolean removedFinishedPotion;
	private PendingInspection pendingInspection;

	void initialize(List<InventorySlot> inventory, Map<Station, Potion> stations)
	{
		clear();
		Objects.requireNonNull(inventory);
		inventorySize = inventory.size();
		for (int slot = 0; slot < inventory.size(); slot++)
		{
			InventorySlot item = inventory.get(slot);
			if (item.isFinished())
			{
				finishedPotions.put(slot, FinishedPotion.unknown(item.getPotion()));
			}
		}

		for (Map.Entry<Station, Potion> entry : Objects.requireNonNull(stations).entrySet())
		{
			if (entry.getKey() != null && entry.getValue() != null)
			{
				stationContents.put(entry.getKey(), entry.getValue());
			}
		}
	}

	void observeInventory(List<InventorySlot> inventory)
	{
		Objects.requireNonNull(inventory);
		inventorySize = inventory.size();

		finishedPotions.entrySet().removeIf(entry ->
		{
			int slot = entry.getKey();
			if (slot < inventory.size() && isSamePotion(inventory.get(slot), entry.getValue()))
			{
				return false;
			}
			cancelInspectionAt(slot);
			removedFinishedPotion = true;
			return true;
		});

		for (int slot = 0; slot < inventory.size(); slot++)
		{
			InventorySlot item = inventory.get(slot);
			FinishedPotion tracked = finishedPotions.get(slot);
			if (item.isFinished() && tracked == null)
			{
				FinishedPotion arrival = FinishedPotion.unknown(item.getPotion());
				finishedPotions.put(slot, arrival);
				pendingArrivals.add(new Arrival(slot, arrival));
			}
		}
	}

	void observeStation(Station station, Potion potion)
	{
		Objects.requireNonNull(station);
		Potion previous = stationContents.get(station);
		if (previous == potion)
		{
			return;
		}

		if (previous != null && potion == null)
		{
			pendingCompletions.add(new Completion(previous, station));
		}

		if (potion == null)
		{
			stationContents.remove(station);
		}
		else
		{
			stationContents.put(station, potion);
		}
	}

	void observeSwap(int sourceSlot, int targetSlot)
	{
		if (!isValidSlot(sourceSlot) || !isValidSlot(targetSlot) || sourceSlot == targetSlot)
		{
			return;
		}

		FinishedPotion source = finishedPotions.remove(sourceSlot);
		FinishedPotion target = finishedPotions.remove(targetSlot);
		if (target != null)
		{
			finishedPotions.put(sourceSlot, target);
		}
		if (source != null)
		{
			finishedPotions.put(targetSlot, source);
		}

		cancelInspectionAt(sourceSlot);
		cancelInspectionAt(targetSlot);
		pendingArrivals.clear();
		pendingCompletions.clear();
	}

	void finishTick()
	{
		if (!removedFinishedPotion
			&& pendingArrivals.size() == 1
			&& pendingCompletions.size() == 1)
		{
			Arrival arrival = pendingArrivals.get(0);
			Completion completion = pendingCompletions.get(0);
			if (arrival.finishedPotion.getPotion() == completion.potion
				&& finishedPotions.get(arrival.slot) == arrival.finishedPotion)
			{
				finishedPotions.put(
					arrival.slot,
					FinishedPotion.known(completion.potion, completion.station));
			}
		}

		pendingArrivals.clear();
		pendingCompletions.clear();
		removedFinishedPotion = false;
	}

	void beginInspection(int slot)
	{
		FinishedPotion potion = finishedPotions.get(slot);
		pendingInspection = potion == null
			? null
			: new PendingInspection(slot, potion.getPotion());
	}

	void resolveInspection(String text)
	{
		PendingInspection inspection = pendingInspection;
		pendingInspection = null;
		if (inspection == null)
		{
			return;
		}

		FinishedPotion current = finishedPotions.get(inspection.slot);
		Optional<FinishedPotion> parsed = PotionInspection.parse(text);
		if (current != null
			&& current.getPotion() == inspection.expectedPotion
			&& parsed.isPresent()
			&& parsed.get().getPotion() == inspection.expectedPotion)
		{
			finishedPotions.put(inspection.slot, parsed.get());
		}
	}

	void cancelInspection()
	{
		pendingInspection = null;
	}

	void clear()
	{
		finishedPotions.clear();
		stationContents.clear();
		pendingArrivals.clear();
		pendingCompletions.clear();
		inventorySize = 0;
		removedFinishedPotion = false;
		pendingInspection = null;
	}

	Map<Integer, FinishedPotion> snapshot()
	{
		return Map.copyOf(finishedPotions);
	}

	private void cancelInspectionAt(int slot)
	{
		if (pendingInspection != null && pendingInspection.slot == slot)
		{
			pendingInspection = null;
		}
	}

	private static boolean isSamePotion(InventorySlot item, FinishedPotion tracked)
	{
		return item.isFinished() && item.getPotion() == tracked.getPotion();
	}

	private boolean isValidSlot(int slot)
	{
		return slot >= 0 && slot < inventorySize;
	}

	private static final class Arrival
	{
		private final int slot;
		private final FinishedPotion finishedPotion;

		private Arrival(int slot, FinishedPotion finishedPotion)
		{
			this.slot = slot;
			this.finishedPotion = finishedPotion;
		}
	}

	private static final class Completion
	{
		private final Potion potion;
		private final Station station;

		private Completion(Potion potion, Station station)
		{
			this.potion = potion;
			this.station = station;
		}
	}

	private static final class PendingInspection
	{
		private final int slot;
		private final Potion expectedPotion;

		private PendingInspection(int slot, Potion expectedPotion)
		{
			this.slot = slot;
			this.expectedPotion = expectedPotion;
		}
	}
}
