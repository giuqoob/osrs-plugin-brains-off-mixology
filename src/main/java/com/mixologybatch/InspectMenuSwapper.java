package com.mixologybatch;

import java.util.Map;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

final class InspectMenuSwapper
{
	private InspectMenuSwapper()
	{
	}

	static boolean promoteInspect(
		MenuEntry[] entries,
		InspectMode mode,
		Map<Integer, FinishedPotion> inventory)
	{
		if (mode == InspectMode.OFF || entries.length == 0)
		{
			return false;
		}

		int defaultIndex = entries.length - 1;
		MenuEntry defaultEntry = entries[defaultIndex];
		Widget inventoryWidget = defaultEntry.getWidget();
		int slot = defaultEntry.getParam0();
		int itemId = defaultEntry.getItemId();
		FinishedPotion finishedPotion = inventory.get(slot);
		if (inventoryWidget == null
			|| inventoryWidget.getId() != InterfaceID.Inventory.ITEMS
			|| finishedPotion == null
			|| finishedPotion.getPotion().getFinishedItemId() != itemId
			|| !mode.allows(finishedPotion))
		{
			return false;
		}

		for (int i = 0; i <= defaultIndex; i++)
		{
			MenuEntry entry = entries[i];
			Widget entryWidget = entry.getWidget();
			if ("Inspect".equalsIgnoreCase(entry.getOption())
				&& entryWidget != null
				&& entryWidget.getId() == inventoryWidget.getId()
				&& entry.getParam0() == slot
				&& entry.getItemId() == itemId)
			{
				boolean changed = i != defaultIndex;
				if (changed)
				{
					entries[i] = defaultEntry;
					entries[defaultIndex] = entry;
				}
				if (entry.getType() == MenuAction.CC_OP_LOW_PRIORITY)
				{
					entry.setType(MenuAction.CC_OP);
					changed = true;
				}
				return changed;
			}
		}
		return false;
	}
}
