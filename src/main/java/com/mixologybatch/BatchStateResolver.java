package com.mixologybatch;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class BatchStateResolver
{
	Guidance resolve(
		BatchPlan plan,
		CyclePlan cycle,
		boolean refilling,
		List<InventorySlot> inventory,
		Potion vesselPotion,
		int[] mixerSlots,
		Map<Station, Potion> activeStations,
		Guidance previousGuidance)
	{
		if (!plan.isValid())
		{
			return Guidance.invalid(plan.getError());
		}
		if (plan.size() == 0)
		{
			return Guidance.empty();
		}
		if (cycle == null || !cycle.isValid())
		{
			return Guidance.invalid(cycle == null ? "Start a new batch." : cycle.getError());
		}
		if (!cycle.containsPotionSlots(inventory))
		{
			return Guidance.invalid("A potion moved outside the projected batch; pull a lever to start a new cycle.");
		}
		if (activeStations.size() > 1)
		{
			return Guidance.invalid("More than one processing station contains a potion.");
		}

		if (refilling)
		{
			return resolveMixing(plan, cycle, inventory, vesselPotion, mixerSlots, activeStations);
		}
		return resolveProcessing(cycle, inventory, activeStations, previousGuidance);
	}

	private Guidance resolveMixing(
		BatchPlan plan,
		CyclePlan cycle,
		List<InventorySlot> inventory,
		Potion vesselPotion,
		int[] mixerSlots,
		Map<Station, Potion> activeStations)
	{
		if (!activeStations.isEmpty())
		{
			return Guidance.invalid("Collect the station potion before starting a new mixing cycle.");
		}

		int inventoryPotionCount = countPotions(inventory);
		int potionCapacity = cycle.getPotionCapacity();
		if (potionCapacity == 0)
		{
			return Guidance.invalid("Clear at least one inventory slot to start mixing.");
		}
		if (inventoryPotionCount >= potionCapacity)
		{
			return resolveProcessing(cycle, inventory, activeStations, Guidance.outside());
		}

		int nextSlot = cycle.firstEmptySlot(inventory);
		if (nextSlot < 0)
		{
			return Guidance.invalid("Clear an inventory slot reserved for the next potion.");
		}

		Potion expected = nextNeededPotion(plan, cycle, inventory, nextSlot);
		if (expected == null)
		{
			return Guidance.invalid("The configured recipe totals are already present; process or adjust the batch.");
		}

		if (vesselPotion != null)
		{
			BatchEntry entry = cycle.entryForSlot(nextSlot, vesselPotion);
			return Guidance.mixing(entry, MixStep.resolve(vesselPotion, mixerSlots, true));
		}

		BatchEntry entry = cycle.entryForSlot(nextSlot, expected);
		MixStep step = MixStep.resolve(expected, mixerSlots, false);
		if (step.getKind() == MixStep.Kind.INVALID)
		{
			// A mistaken lever must not replace the intended recipe with an error.
			// Keep the correct sequence visible and reconcile the actual potion once mixed.
			step = MixStep.resolve(expected, new int[]{0, 0, 0}, false);
		}
		return Guidance.mixing(entry, step);
	}

	private Guidance resolveProcessing(
		CyclePlan cycle,
		List<InventorySlot> inventory,
		Map<Station, Potion> activeStations,
		Guidance previousGuidance)
	{
		if (!activeStations.isEmpty())
		{
			Map.Entry<Station, Potion> active = activeStations.entrySet().iterator().next();
			BatchEntry previousEntry = previousGuidance == null ? null : previousGuidance.getEntry();
			if (previousEntry != null && previousGuidance.targetsStation())
			{
				if (previousEntry.getStation() != active.getKey()
					|| previousEntry.getPotion() != active.getValue())
				{
					return Guidance.invalid(
						"Wrong station: use " + previousEntry.getStation().getObjectName()
							+ " for " + previousEntry.getPotion().name() + ".");
				}
				return Guidance.processing(previousEntry, true);
			}

			BatchEntry recovered = recoverActiveEntry(cycle, inventory, active.getKey(), active.getValue());
			return Guidance.processing(recovered, true);
		}

		BatchEntry unfinished = firstUnfinishedEntry(cycle, inventory);
		if (unfinished != null)
		{
			// A station takes the first unfinished potion in the inventory, so an
			// order potion behind other unfinished potions must be used on it directly.
			return unfinished.isOrderPotion() && hasUnfinishedBefore(inventory, unfinished.getInventorySlot())
				? Guidance.useItemOnStation(unfinished)
				: Guidance.processing(unfinished, false);
		}
		return Guidance.complete();
	}

	private static boolean hasUnfinishedBefore(List<InventorySlot> inventory, int slot)
	{
		for (int index = 0; index < slot; index++)
		{
			InventorySlot actual = inventory.get(index);
			if (actual.isPotion() && !actual.isFinished())
			{
				return true;
			}
		}
		return false;
	}

	static BatchEntry firstUnfinishedEntry(CyclePlan cycle, List<InventorySlot> inventory)
	{
		if (cycle == null || !cycle.isValid())
		{
			return null;
		}
		for (int rank : cycle.processingRanks())
		{
			int slot = cycle.slotAtRank(rank);
			InventorySlot actual = inventory.get(slot);
			if (actual.isPotion() && !actual.isFinished())
			{
				return cycle.entryAtRank(rank, actual.getPotion());
			}
		}
		return null;
	}

	private static BatchEntry recoverActiveEntry(
		CyclePlan cycle,
		List<InventorySlot> inventory,
		Station station,
		Potion potion)
	{
		for (int rank = 0; rank < cycle.size(); rank++)
		{
			BatchEntry candidate = cycle.entryAtRank(rank, potion);
			if (candidate.getStation() == station && inventory.get(candidate.getInventorySlot()).isEmpty())
			{
				return candidate;
			}
		}
		for (int rank = 0; rank < cycle.size(); rank++)
		{
			BatchEntry candidate = cycle.entryAtRank(rank, potion);
			if (candidate.getStation() == station)
			{
				return candidate;
			}
		}
		return cycle.entryAtRank(0, potion);
	}

	static Potion nextNeededPotion(
		BatchPlan plan,
		CyclePlan cycle,
		List<InventorySlot> inventory,
		int nextSlot)
	{
		BatchEntry nextEntry = cycle.entryForSlot(nextSlot, plan.get(0).getPotion());
		if (nextEntry == null)
		{
			return null;
		}
		Station station = nextEntry.getStation();
		EnumMap<Potion, Integer> present = new EnumMap<>(Potion.class);
		for (int rank = 0; rank < cycle.size(); rank++)
		{
			int inventorySlot = cycle.slotAtRank(rank);
			InventorySlot actual = inventory.get(inventorySlot);
			if (!actual.isPotion())
			{
				continue;
			}
			BatchEntry assigned = cycle.entryAtRank(rank, actual.getPotion());
			if (assigned.getStation() == station)
			{
				present.merge(actual.getPotion(), 1, Integer::sum);
			}
		}
		for (BatchEntry entry : plan.getEntries())
		{
			if (entry.getStation() != station)
			{
				continue;
			}
			Potion potion = entry.getPotion();
			if (present.getOrDefault(potion, 0) < plan.getConfiguredCount(station, potion))
			{
				return potion;
			}
		}
		return null;
	}

	private static int countPotions(List<InventorySlot> inventory)
	{
		int count = 0;
		for (InventorySlot slot : inventory)
		{
			if (slot.isPotion())
			{
				count++;
			}
		}
		return count;
	}

	static EnumMap<Potion, Integer> potionCounts(List<InventorySlot> inventory)
	{
		EnumMap<Potion, Integer> counts = new EnumMap<>(Potion.class);
		for (InventorySlot slot : inventory)
		{
			if (slot.isPotion())
			{
				counts.merge(slot.getPotion(), 1, Integer::sum);
			}
		}
		return counts;
	}

	/**
	 * A delivered batch can roll into the next refill once only two non-MAL
	 * potions remain. MAL is deliberately ignored because retained Mixalots
	 * should carry into the next configured batch.
	 */
	static boolean shouldStartRollingRefill(List<InventorySlot> inventory)
	{
		int nonMixalotCount = 0;
		for (InventorySlot slot : inventory)
		{
			if (!slot.isPotion())
			{
				continue;
			}
			if (!slot.isFinished())
			{
				return false;
			}
			if (slot.getPotion() != Potion.MAL && ++nonMixalotCount > 2)
			{
				return false;
			}
		}
		return true;
	}

	static boolean hasUnfinishedPotion(List<InventorySlot> inventory)
	{
		for (InventorySlot slot : inventory)
		{
			if (slot.isPotion() && !slot.isFinished())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether a refill started from this inventory would have at least one potion
	 * to mix: there is a free planned slot and the configured totals are not met.
	 */
	static boolean canRefill(BatchPlan plan, List<InventorySlot> inventory)
	{
		if (!plan.isValid() || plan.size() == 0)
		{
			return false;
		}
		CyclePlan candidate = CyclePlan.create(plan, inventory);
		if (!candidate.isValid())
		{
			return false;
		}
		candidate.observeInventory(inventory);
		int nextSlot = candidate.firstEmptySlot(inventory);
		return countPotions(inventory) < candidate.getPotionCapacity()
			&& nextSlot >= 0
			&& nextNeededPotion(plan, candidate, inventory, nextSlot) != null;
	}

	static boolean hasMixerContents(int[] mixerSlots)
	{
		for (int slot : mixerSlots)
		{
			if (slot != 0)
			{
				return true;
			}
		}
		return false;
	}

	static Map<Station, Potion> noActiveStations()
	{
		return new EnumMap<>(Station.class);
	}
}
