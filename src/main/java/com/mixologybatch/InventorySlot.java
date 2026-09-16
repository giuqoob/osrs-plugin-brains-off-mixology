package com.mixologybatch;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

final class InventorySlot
{
	private final boolean empty;
	private final int itemId;
	private final Potion potion;
	private final boolean finished;

	private InventorySlot(boolean empty, int itemId, Potion potion, boolean finished)
	{
		this.empty = empty;
		this.itemId = itemId;
		this.potion = potion;
		this.finished = finished;
	}

	static InventorySlot empty()
	{
		return new InventorySlot(true, -1, null, false);
	}

	static InventorySlot fromItemId(int itemId)
	{
		if (itemId <= 0)
		{
			return empty();
		}
		Potion potion = Potion.fromItemId(itemId);
		return new InventorySlot(false, itemId, potion, potion != null && potion.isFinishedItem(itemId));
	}

	static List<InventorySlot> fromContainer(ItemContainer container, int size)
	{
		List<InventorySlot> result = new ArrayList<>(size);
		Item[] items = container == null ? null : container.getItems();
		for (int slot = 0; slot < size; slot++)
		{
			if (items == null || slot >= items.length || items[slot] == null)
			{
				result.add(empty());
			}
			else
			{
				result.add(fromItemId(items[slot].getId()));
			}
		}
		return result;
	}

	boolean isEmpty()
	{
		return empty;
	}

	boolean isPotion()
	{
		return potion != null;
	}

	int getItemId()
	{
		return itemId;
	}

	Potion getPotion()
	{
		return potion;
	}

	boolean isFinished()
	{
		return finished;
	}
}
