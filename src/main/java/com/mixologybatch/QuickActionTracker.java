package com.mixologybatch;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tracks open quick-action windows. A window opens on the station's spotanim
 * while it holds a potion, and closes once progress moves past the window, the
 * progress resets (missed click), the skillshot varbit changes (successful click)
 * or the station empties. The skillshot varbit may hold a stale value from an
 * earlier hit, so only a change from the opening value counts as a hit.
 */
final class QuickActionTracker
{
	private final Map<QuickAction, Window> windows = new EnumMap<>(QuickAction.class);

	void open(QuickAction action, boolean stationHasPotion, int progress, int skillshot)
	{
		if (stationHasPotion)
		{
			windows.put(action, new Window(progress, skillshot));
		}
	}

	void update(QuickAction action, boolean stationHasPotion, int progress, int skillshot)
	{
		Window window = windows.get(action);
		if (window == null)
		{
			return;
		}
		if (!stationHasPotion
			|| skillshot != window.skillshot
			|| progress < window.progress
			|| progress > window.progress + action.getWindow())
		{
			windows.remove(action);
		}
	}

	boolean isOpen(Station station)
	{
		QuickAction action = QuickAction.forStation(station);
		return action != null && windows.containsKey(action);
	}

	boolean hasOpenWindow()
	{
		return !windows.isEmpty();
	}

	void reset()
	{
		windows.clear();
	}

	private static final class Window
	{
		private final int progress;
		private final int skillshot;

		private Window(int progress, int skillshot)
		{
			this.progress = progress;
			this.skillshot = skillshot;
		}
	}
}
