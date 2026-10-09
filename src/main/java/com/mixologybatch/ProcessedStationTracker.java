package com.mixologybatch;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Remembers which station processed the finished potion in each inventory slot.
 * The finished item id does not encode the modifier, so it is inferred by pairing
 * a station releasing potion P with a finished P appearing in the inventory.
 * The two changes may arrive on different ticks, so either side waits briefly
 * for the other.
 */
final class ProcessedStationTracker
{
	static final int PAIRING_TICKS = 3;

	private final Station[] stations = new Station[BatchPlan.INVENTORY_SIZE];
	private final Potion[] potions = new Potion[BatchPlan.INVENTORY_SIZE];
	private final List<PendingRelease> pendingReleases = new ArrayList<>();
	private final List<PendingSlot> pendingSlots = new ArrayList<>();
	private List<InventorySlot> previousInventory;
	private Map<Station, Potion> previousActiveStations;

	void observe(List<InventorySlot> inventory, Map<Station, Potion> activeStations, int tick)
	{
		if (previousInventory == null)
		{
			remember(inventory, activeStations);
			return;
		}

		pendingSlots.removeIf(pending -> pending.expiresAt < tick);
		pendingReleases.removeIf(release -> release.expiresAt < tick);
		for (Map.Entry<Station, Potion> previous : previousActiveStations.entrySet())
		{
			if (activeStations.get(previous.getKey()) != previous.getValue())
			{
				pendingReleases.add(new PendingRelease(previous.getKey(), previous.getValue(), tick + PAIRING_TICKS));
			}
		}

		List<PendingSlot> lost = new ArrayList<>();
		List<Integer> newlyFinished = new ArrayList<>();
		for (int slot = 0; slot < stations.length && slot < inventory.size(); slot++)
		{
			InventorySlot current = inventory.get(slot);
			if (stations[slot] != null && !holdsFinished(current, potions[slot]))
			{
				lost.add(new PendingSlot(slot, potions[slot], stations[slot], tick));
				clear(slot);
			}
			if (current.isFinished() && !holdsFinished(previousInventory.get(slot), current.getPotion()))
			{
				newlyFinished.add(slot);
			}
		}

		for (int slot : newlyFinished)
		{
			Potion potion = inventory.get(slot).getPotion();
			PendingSlot moved = takeLost(lost, potion);
			if (moved != null)
			{
				// The player dragged a tracked potion to another slot.
				assign(slot, potion, moved.station);
				continue;
			}
			PendingRelease release = takeRelease(potion);
			if (release != null)
			{
				assign(slot, potion, release.station);
			}
			else
			{
				pendingSlots.add(new PendingSlot(slot, potion, null, tick + PAIRING_TICKS));
			}
		}

		Iterator<PendingSlot> waiting = pendingSlots.iterator();
		while (waiting.hasNext())
		{
			PendingSlot pending = waiting.next();
			if (!holdsFinished(inventory.get(pending.slot), pending.potion) || stations[pending.slot] != null)
			{
				waiting.remove();
				continue;
			}
			PendingRelease release = takeRelease(pending.potion);
			if (release != null)
			{
				assign(pending.slot, pending.potion, release.station);
				waiting.remove();
			}
		}

		remember(inventory, activeStations);
	}

	/**
	 * Station that processed the finished potion in this slot, or null when unknown.
	 */
	Station stationFor(int slot)
	{
		return slot >= 0 && slot < stations.length ? stations[slot] : null;
	}

	void reset()
	{
		for (int slot = 0; slot < stations.length; slot++)
		{
			clear(slot);
		}
		pendingReleases.clear();
		pendingSlots.clear();
		previousInventory = null;
		previousActiveStations = null;
	}

	private void remember(List<InventorySlot> inventory, Map<Station, Potion> activeStations)
	{
		previousInventory = new ArrayList<>(inventory);
		previousActiveStations = new EnumMap<>(Station.class);
		previousActiveStations.putAll(activeStations);
	}

	private void assign(int slot, Potion potion, Station station)
	{
		stations[slot] = station;
		potions[slot] = potion;
	}

	private void clear(int slot)
	{
		stations[slot] = null;
		potions[slot] = null;
	}

	private PendingRelease takeRelease(Potion potion)
	{
		Iterator<PendingRelease> releases = pendingReleases.iterator();
		while (releases.hasNext())
		{
			PendingRelease release = releases.next();
			if (release.potion == potion)
			{
				releases.remove();
				return release;
			}
		}
		return null;
	}

	private static PendingSlot takeLost(List<PendingSlot> lost, Potion potion)
	{
		Iterator<PendingSlot> candidates = lost.iterator();
		while (candidates.hasNext())
		{
			PendingSlot candidate = candidates.next();
			if (candidate.potion == potion)
			{
				candidates.remove();
				return candidate;
			}
		}
		return null;
	}

	private static boolean holdsFinished(InventorySlot slot, Potion potion)
	{
		return slot.isFinished() && slot.getPotion() == potion;
	}

	private static final class PendingRelease
	{
		private final Station station;
		private final Potion potion;
		private final int expiresAt;

		private PendingRelease(Station station, Potion potion, int expiresAt)
		{
			this.station = station;
			this.potion = potion;
			this.expiresAt = expiresAt;
		}
	}

	private static final class PendingSlot
	{
		private final int slot;
		private final Potion potion;
		private final Station station;
		private final int expiresAt;

		private PendingSlot(int slot, Potion potion, Station station, int expiresAt)
		{
			this.slot = slot;
			this.potion = potion;
			this.station = station;
			this.expiresAt = expiresAt;
		}
	}
}
