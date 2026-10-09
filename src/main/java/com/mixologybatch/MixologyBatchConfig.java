package com.mixologybatch;

import java.awt.Color;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;

@ConfigGroup(MixologyBatchConfig.GROUP)
public interface MixologyBatchConfig extends Config
{
	String GROUP = "mixologybatchhelper";

	@ConfigSection(
		name = "Batch potions",
		description = "Number of copies to distribute evenly across the three processing stations",
		position = 10
	)
	String POTIONS = "potions";

	@ConfigSection(
		name = "Display",
		description = "Overlay and highlight settings",
		position = 20
	)
	String DISPLAY = "display";

	@ConfigSection(
		name = "Orders",
		description = "Awareness of the three current conveyor orders",
		position = 30
	)
	String ORDERS = "orders";

	@ConfigItem(
		keyName = "stationOrder",
		name = "Station order",
		description = "Order of the three contiguous station batches. Concentrate last is safest for spam-clicking.",
		position = 0
	)
	default StationOrder stationOrder()
	{
		return StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "mmmCount", name = "MMM", description = "Mammoth-might mix copies", position = 0)
	default int mmmCount()
	{
		return 0;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "mmaCount", name = "MMA", description = "Mystic mana amalgam copies", position = 1)
	default int mmaCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "mmlCount", name = "MML", description = "Marley's moonlight copies", position = 2)
	default int mmlCount()
	{
		return 4;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "aaaCount", name = "AAA", description = "Alco-augmentator copies", position = 3)
	default int aaaCount()
	{
		return 0;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "aamCount", name = "AAM", description = "Azure aura mix copies", position = 4)
	default int aamCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "alaCount", name = "ALA", description = "Aqualux amalgam copies", position = 5)
	default int alaCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "lllCount", name = "LLL", description = "Liplack liquor copies", position = 6)
	default int lllCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "mllCount", name = "MLL", description = "Megalite liquid copies", position = 7)
	default int mllCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "allCount", name = "ALL", description = "Anti-leech lotion copies", position = 8)
	default int allCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "malCount", name = "MAL", description = "Mixalot copies", position = 9)
	default int malCount()
	{
		return 6;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "showPanel",
		name = "Show instruction panel",
		description = "Show the current action and live per-recipe inventory totals",
		position = 0
	)
	default boolean showPanel()
	{
		return true;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "showSceneGuidance",
		name = "Show scene guidance",
		description = "Show every numbered recipe object while mixing, then the current station or conveyor",
		position = 1
	)
	default boolean showSceneGuidance()
	{
		return true;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "showInventoryBatches",
		name = "Number inventory batches",
		description = "Mark each planned potion slot with its station batch number",
		position = 2
	)
	default boolean showInventoryBatches()
	{
		return true;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "showActionQueue",
		name = "Show potion queue",
		description = "Show the previous two, current, and next three potions with their full recipes",
		position = 3
	)
	default boolean showActionQueue()
	{
		return true;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "guardWrongStations",
		name = "Guard wrong stations",
		description = "Swap the station's existing Check option into left-click position while it is unavailable",
		position = 4
	)
	default boolean guardWrongStations()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		section = DISPLAY,
		keyName = "stationColor",
		name = "Station highlight",
		description = "Colour used for station outlines and the active inventory slot",
		position = 5
	)
	default Color stationColor()
	{
		return new Color(255, 0, 255, 255);
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "highlightQuickAction",
		name = "Highlight quick action",
		description = "Outline the Agitator or Alembic while its quick-action click window is open",
		position = 6
	)
	default boolean highlightQuickAction()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		section = DISPLAY,
		keyName = "quickActionColor",
		name = "Quick action color",
		description = "Colour used for the quick-action station outline",
		position = 7
	)
	default Color quickActionColor()
	{
		return Color.GREEN;
	}

	@Range(min = 1, max = 8)
	@ConfigItem(
		section = DISPLAY,
		keyName = "outlineWidth",
		name = "Outline width",
		description = "Width of scene-object outlines",
		position = 8
	)
	default int outlineWidth()
	{
		return 3;
	}

	@Range(min = 0, max = 4)
	@ConfigItem(
		section = DISPLAY,
		keyName = "outlineFeather",
		name = "Outline feather",
		description = "Softness of scene-object outlines",
		position = 9
	)
	default int outlineFeather()
	{
		return 1;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "highlightDigweed",
		name = "Highlight Digweed",
		description = "Outline a Digweed in the lab corner where it has spawned and show its corner in the panel",
		position = 10
	)
	default boolean highlightDigweed()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		section = DISPLAY,
		keyName = "digweedColor",
		name = "Digweed color",
		description = "Colour used for the Digweed outline",
		position = 11
	)
	default Color digweedColor()
	{
		return new Color(255, 220, 60);
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "notifyDigweed",
		name = "Notify Digweed",
		description = "Send a RuneLite notification when a Digweed spawns",
		position = 12
	)
	default Notification notifyDigweed()
	{
		return Notification.ON;
	}

	@ConfigItem(
		section = ORDERS,
		keyName = "useCurrentOrders",
		name = "Use current orders",
		description = "Read the three conveyor orders and show which are ready to deliver. When off, the plugin ignores orders entirely",
		position = 0
	)
	default boolean useCurrentOrders()
	{
		return true;
	}

	@Range(min = 1, max = 3)
	@ConfigItem(
		section = ORDERS,
		keyName = "deliverThreshold",
		name = "Deliver threshold",
		description = "Orders that must be fillable before delivering; otherwise refill first. Orders for skipped potions that you can't fill don't count, so the target drops when only fewer orders are possible",
		position = 1
	)
	default int deliverThreshold()
	{
		return 1;
	}

	@ConfigItem(
		section = ORDERS,
		keyName = "skipOrderPotions",
		name = "Skip order potions",
		description = "Potions never brewed for orders. Their orders are left to be refreshed by a normal deposit",
		position = 2
	)
	default Set<Potion> skipOrderPotions()
	{
		return EnumSet.of(Potion.MMM, Potion.AAA);
	}

	@ConfigItem(
		section = ORDERS,
		keyName = "brewCurrentOrders",
		name = "Brew current orders",
		description = "When a refill starts, mix potions for current orders the stock can't cover. They go after the stock and are used on their station at the end of its batch",
		position = 3
	)
	default boolean brewCurrentOrders()
	{
		return true;
	}

	@ConfigItem(
		section = ORDERS,
		keyName = "guardConveyor",
		name = "Guard conveyor",
		description = "Swap the conveyor's Examine option into left-click while a refill is due, so a stray click doesn't deposit. The deposit stays on right-click",
		position = 4
	)
	default boolean guardConveyor()
	{
		return false;
	}
}
