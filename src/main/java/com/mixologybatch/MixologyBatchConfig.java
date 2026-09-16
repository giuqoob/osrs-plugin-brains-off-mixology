package com.mixologybatch;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
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

	@ConfigItem(
		section = DISPLAY,
		keyName = "leftClickInspect",
		name = "Left-click Inspect",
		description = "Choose which finished potions promote their existing Inspect action to left-click",
		position = 5
	)
	default InspectMode leftClickInspect()
	{
		return InspectMode.OFF;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "highlightFulfillableOrders",
		name = "Highlight fulfillable orders",
		description = "Colour order names green when a matching finished potion is ready to deliver",
		position = 6
	)
	default boolean highlightFulfillableOrders()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		section = DISPLAY,
		keyName = "stationColor",
		name = "Station highlight",
		description = "Colour used for station outlines and the active inventory slot",
		position = 7
	)
	default Color stationColor()
	{
		return new Color(255, 0, 255, 255);
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
		keyName = "sceneHintFont",
		name = "Scene font",
		description = "Font for all scene hints, including lever letters and recipe numbers",
		position = 20
	)
	default SceneHintFont sceneHintFont()
	{
		return SceneHintFont.DEFAULT;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "potionSummaryDisplay",
		name = "Potion summary",
		description = "Show the finished-potion count, fulfillable order status, both, or neither at the conveyor and in the panel",
		position = 21
	)
	default PotionSummaryDisplay potionSummaryDisplay()
	{
		return PotionSummaryDisplay.BOTH;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "markUnknownProcessingType",
		name = "Mark unknown processing type",
		description = "Mark finished potions whose processing type has not been identified",
		position = 22
	)
	default boolean markUnknownProcessingType()
	{
		return false;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "showProcessingDetailsOnHover",
		name = "Show processing details on hover",
		description = "Show a finished potion's processing type while hovering over it",
		position = 23
	)
	default boolean showProcessingDetailsOnHover()
	{
		return false;
	}
}
