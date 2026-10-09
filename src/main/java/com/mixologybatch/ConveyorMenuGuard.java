package com.mixologybatch;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.MenuAction;

/**
 * Finds the conveyor's deposit entry and its own Examine entry so the plugin
 * can swap them while a deposit is unwanted. Entries are only reordered, never
 * removed or added, so the deposit stays reachable via right-click.
 */
final class ConveyorMenuGuard
{
	private ConveyorMenuGuard()
	{
	}

	/**
	 * Groups conveyor entries by their exact scene object, as with stations, so
	 * an Examine entry is never paired with another object's deposit entry.
	 */
	static final class MenuScan
	{
		private final List<Target> targets = new ArrayList<>();

		void accept(int index, int objectId, int sceneX, int sceneY, MenuAction type, String option)
		{
			if (objectId != LabObject.CONVEYOR.getObjectId())
			{
				return;
			}
			Target target = findOrCreate(sceneX, sceneY);
			if (type == MenuAction.GAME_OBJECT_FIRST_OPTION)
			{
				target.depositIndex = index;
			}
			else if (type == MenuAction.EXAMINE_OBJECT || "Examine".equalsIgnoreCase(option))
			{
				target.examineIndex = index;
			}
		}

		/**
		 * @return {deposit index, examine index} for the conveyor whose deposit entry
		 *     is highest in the menu, or null when there is nothing to swap
		 */
		int[] select()
		{
			Target selected = null;
			for (Target target : targets)
			{
				if (target.depositIndex >= 0
					&& (selected == null || target.depositIndex > selected.depositIndex))
				{
					selected = target;
				}
			}
			if (selected == null || selected.examineIndex < 0)
			{
				return null;
			}
			return new int[]{selected.depositIndex, selected.examineIndex};
		}

		private Target findOrCreate(int sceneX, int sceneY)
		{
			for (Target target : targets)
			{
				if (target.sceneX == sceneX && target.sceneY == sceneY)
				{
					return target;
				}
			}
			Target target = new Target(sceneX, sceneY);
			targets.add(target);
			return target;
		}
	}

	private static final class Target
	{
		private final int sceneX;
		private final int sceneY;
		private int depositIndex = -1;
		private int examineIndex = -1;

		private Target(int sceneX, int sceneY)
		{
			this.sceneX = sceneX;
			this.sceneY = sceneY;
		}
	}
}
