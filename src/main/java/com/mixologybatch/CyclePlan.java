package com.mixologybatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * Maps the configured logical station batches onto physical inventory slots.
 * Existing potions keep their place in the projected inventory and the lowest
 * available slots are reserved for the potions that will be mixed next.
 */
final class CyclePlan
{
	private final BatchPlan target;
	private final List<Integer> inventorySlots;
	private final Set<Integer> blockedSlots;
	private final Set<Integer> persistentBlockedSlots;
	private final Set<Integer> observedDigweedSlots;
	private final String error;

	private CyclePlan(
		BatchPlan target,
		List<Integer> inventorySlots,
		Set<Integer> blockedSlots,
		Set<Integer> persistentBlockedSlots,
		Set<Integer> observedDigweedSlots,
		String error)
	{
		this.target = target;
		this.inventorySlots = inventorySlots;
		this.blockedSlots = blockedSlots;
		this.persistentBlockedSlots = persistentBlockedSlots;
		this.observedDigweedSlots = observedDigweedSlots;
		this.error = error;
	}

	static CyclePlan create(BatchPlan target, List<InventorySlot> inventory)
	{
		if (!target.isValid())
		{
			return invalid(target, target.getError());
		}

		List<Integer> slots = new ArrayList<>(target.size());
		for (int slot = 0; slot < inventory.size(); slot++)
		{
			if (inventory.get(slot).isPotion())
			{
				slots.add(slot);
			}
		}
		if (slots.size() > target.size())
		{
			return invalid(target, "Inventory contains more potions than the configured batch.");
		}

		for (int slot = 0; slot < inventory.size() && slots.size() < target.size(); slot++)
		{
			if (inventory.get(slot).isEmpty())
			{
				slots.add(slot);
			}
		}
		for (int slot = 0; slot < inventory.size() && slots.size() < target.size(); slot++)
		{
			if (!slots.contains(slot))
			{
				slots.add(slot);
			}
		}
		if (slots.size() < target.size())
		{
			return invalid(target, "The configured batch does not fit in the inventory.");
		}

		Collections.sort(slots);
		Set<Integer> blocked = new HashSet<>();
		Set<Integer> digweed = new HashSet<>();
		for (int slot : slots)
		{
			InventorySlot actual = inventory.get(slot);
			if (!actual.isEmpty() && !actual.isPotion())
			{
				blocked.add(slot);
			}
			if (actual.getItemId() == ItemID.MM_LAB_SPECIAL_HERB)
			{
				digweed.add(slot);
			}
		}
		return new CyclePlan(
			target,
			Collections.unmodifiableList(slots),
			blocked,
			new HashSet<>(digweed),
			digweed,
			null);
	}

	private static CyclePlan invalid(BatchPlan target, String error)
	{
		return new CyclePlan(
			target,
			Collections.emptyList(),
			new HashSet<>(),
			new HashSet<>(),
			new HashSet<>(),
			error);
	}

	boolean isValid()
	{
		return error == null;
	}

	String getError()
	{
		return error;
	}

	/**
	 * Whether this cycle was built from the given configured stock plan. Order
	 * potions are fixed when the cycle is created, so later order changes do not
	 * rebuild it.
	 */
	boolean belongsTo(BatchPlan plan)
	{
		return target.getBase() == plan.getBase();
	}

	BatchPlan getTarget()
	{
		return target;
	}

	/**
	 * Ranks in processing order: each station's stock batch followed by that
	 * station's order potions, in station order.
	 */
	List<Integer> processingRanks()
	{
		List<Integer> ranks = new ArrayList<>(inventorySlots.size());
		for (int rank = 0; rank < inventorySlots.size(); rank++)
		{
			ranks.add(rank);
		}
		ranks.sort((left, right) ->
		{
			BatchEntry a = target.get(left);
			BatchEntry b = target.get(right);
			if (a.getStationOrdinal() != b.getStationOrdinal())
			{
				return Integer.compare(a.getStationOrdinal(), b.getStationOrdinal());
			}
			if (a.isOrderPotion() != b.isOrderPotion())
			{
				return a.isOrderPotion() ? 1 : -1;
			}
			return Integer.compare(left, right);
		});
		return ranks;
	}

	boolean containsPotionSlots(List<InventorySlot> inventory)
	{
		for (int slot = 0; slot < inventory.size(); slot++)
		{
			if (inventory.get(slot).isPotion() && !inventorySlots.contains(slot))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Non-potion items block only their current slots. A consumed Digweed keeps
	 * its old slot reserved so guidance cannot jump backwards, while moving the
	 * Digweed transfers that reservation to its new slot.
	 */
	void observeInventory(List<InventorySlot> inventory)
	{
		Set<Integer> currentDigweedSlots = new HashSet<>();
		for (int slot = 0; slot < inventory.size(); slot++)
		{
			InventorySlot actual = inventory.get(slot);
			if (actual.getItemId() == ItemID.MM_LAB_SPECIAL_HERB)
			{
				currentDigweedSlots.add(slot);
			}
		}

		Set<Integer> removedDigweedSlots = new HashSet<>(observedDigweedSlots);
		removedDigweedSlots.removeAll(currentDigweedSlots);
		Set<Integer> addedDigweedSlots = new HashSet<>(currentDigweedSlots);
		addedDigweedSlots.removeAll(observedDigweedSlots);
		int movedDigweedCount = Math.min(removedDigweedSlots.size(), addedDigweedSlots.size());
		for (int slot : removedDigweedSlots)
		{
			if (movedDigweedCount-- <= 0)
			{
				break;
			}
			persistentBlockedSlots.remove(slot);
		}
		for (int slot : currentDigweedSlots)
		{
			if (inventorySlots.contains(slot))
			{
				persistentBlockedSlots.add(slot);
			}
		}

		blockedSlots.clear();
		blockedSlots.addAll(persistentBlockedSlots);
		for (int slot : inventorySlots)
		{
			InventorySlot actual = inventory.get(slot);
			if (actual.isPotion())
			{
				persistentBlockedSlots.remove(slot);
				blockedSlots.remove(slot);
			}
			else if (!actual.isEmpty())
			{
				blockedSlots.add(slot);
			}
		}

		observedDigweedSlots.clear();
		observedDigweedSlots.addAll(currentDigweedSlots);
	}

	int getPotionCapacity()
	{
		return inventorySlots.size() - blockedSlots.size();
	}

	int firstEmptySlot(List<InventorySlot> inventory)
	{
		for (int slot : inventorySlots)
		{
			if (!blockedSlots.contains(slot) && inventory.get(slot).isEmpty())
			{
				return slot;
			}
		}
		return -1;
	}

	BatchEntry entryForSlot(int inventorySlot, Potion potion)
	{
		int rank = inventorySlots.indexOf(inventorySlot);
		return rank < 0 ? null : target.get(rank).remap(potion, inventorySlot);
	}

	BatchEntry entryAtRank(int rank, Potion potion)
	{
		return target.get(rank).remap(potion, inventorySlots.get(rank));
	}

	int size()
	{
		return inventorySlots.size();
	}

	int slotAtRank(int rank)
	{
		return inventorySlots.get(rank);
	}
}
