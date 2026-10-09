package com.mixologybatch;

import java.util.List;
import java.util.Set;

/**
 * Decides whether the inventory should be delivered to the conveyor or refilled,
 * based on how many current orders it can fill.
 */
final class DeliveryDecision
{
	enum Kind
	{
		/** Orders are unknown; fall back to the original batch behaviour. */
		UNKNOWN,
		/** Enough orders are fillable. */
		DELIVER,
		/** Too few orders are fillable; mix more potions first. */
		REFILL,
		/**
		 * No order can be filled and nothing can be mixed for them, so the only
		 * way to refresh the orders is a deposit that consumes the first potion.
		 */
		WASTE
	}

	private static final DeliveryDecision UNKNOWN = new DeliveryDecision(Kind.UNKNOWN, 0, 0, -1, null);

	private final Kind kind;
	private final int fillable;
	private final int required;
	private final int wastedSlot;
	private final Potion wastedPotion;

	private DeliveryDecision(Kind kind, int fillable, int required, int wastedSlot, Potion wastedPotion)
	{
		this.kind = kind;
		this.fillable = fillable;
		this.required = required;
		this.wastedSlot = wastedSlot;
		this.wastedPotion = wastedPotion;
	}

	static DeliveryDecision unknown()
	{
		return UNKNOWN;
	}

	/**
	 * The threshold is a target: orders for skipped potions that cannot be filled
	 * do not count towards it, so MMM, MMM, MML with a threshold of 2 delivers as
	 * soon as the MML order is fillable.
	 *
	 * @param canRefill whether a refill has room to mix at least one potion
	 */
	static DeliveryDecision decide(
		OrderMatch match,
		Set<Potion> skippedPotions,
		int threshold,
		boolean canRefill,
		List<InventorySlot> inventory)
	{
		List<PotionOrder> orders = match.getOrders();
		int eligible = 0;
		for (int index = 0; index < orders.size(); index++)
		{
			PotionOrder order = orders.get(index);
			if (order == null)
			{
				// Partial order data is treated as unknown rather than guessed at.
				return UNKNOWN;
			}
			if (match.isFillable(index) || !skippedPotions.contains(order.getPotion()))
			{
				eligible++;
			}
		}
		if (orders.isEmpty())
		{
			return UNKNOWN;
		}

		int fillable = match.getFillableCount();
		int required = Math.min(threshold, eligible);
		if (required > 0 && fillable >= required)
		{
			return new DeliveryDecision(Kind.DELIVER, fillable, required, -1, null);
		}
		if (required > 0 && canRefill)
		{
			return new DeliveryDecision(Kind.REFILL, fillable, required, -1, null);
		}

		for (int slot = 0; slot < inventory.size(); slot++)
		{
			if (inventory.get(slot).isPotion())
			{
				return new DeliveryDecision(Kind.WASTE, fillable, required, slot, inventory.get(slot).getPotion());
			}
		}
		// Nothing to deposit: refill if possible, otherwise nothing can be done yet.
		return new DeliveryDecision(canRefill ? Kind.REFILL : Kind.UNKNOWN, fillable, required, -1, null);
	}

	Kind getKind()
	{
		return kind;
	}

	int getFillable()
	{
		return fillable;
	}

	int getRequired()
	{
		return required;
	}

	int getWastedSlot()
	{
		return wastedSlot;
	}

	Potion getWastedPotion()
	{
		return wastedPotion;
	}
}
