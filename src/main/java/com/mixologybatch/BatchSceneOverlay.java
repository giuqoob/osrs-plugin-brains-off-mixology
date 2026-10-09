package com.mixologybatch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

final class BatchSceneOverlay extends Overlay
{
	private static final Color VESSEL_COLOR = Color.WHITE;
	private static final Color COMPLETE_COLOR = new Color(0, 255, 90);
	private static final Color WASTE_COLOR = new Color(255, 140, 0);
	private static final Color NEXT_RECIPE_COLOR = new Color(150, 150, 150);
	private static final int MARKER_OFFSET = -22;
	private static final int CURRENT_RECIPE_OFFSET = 0;
	private static final int NEXT_RECIPE_OFFSET = 20;
	// Lifts station labels clear of the game's progress bar above the machine.
	private static final int STATION_LABEL_OFFSET = -20;

	private final Client client;
	private final MixologyBatchPlugin plugin;
	private final MixologyBatchConfig config;
	private final LabObjectResolver objects;
	private final ModelOutlineRenderer outliner;

	@Inject
	private BatchSceneOverlay(
		Client client,
		MixologyBatchPlugin plugin,
		MixologyBatchConfig config,
		LabObjectResolver objects,
		ModelOutlineRenderer outliner)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.objects = objects;
		this.outliner = outliner;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInLab() || !config.showSceneGuidance())
		{
			return null;
		}

		Guidance guidance = plugin.getGuidance();
		renderQuickActions(guidance);
		renderDigweed(graphics);
		if (guidance.getPhase() == Guidance.Phase.MIXING)
		{
			renderMixingRecipe(
				graphics,
				guidance.getEntry().getPotion(),
				plugin.getNextQueuedPotion());
			return null;
		}
		renderPermanentLeverMarkers(graphics);

		LabObject target;
		Color color;
		String label;
		switch (guidance.getAction())
		{
			case USE_STATION:
				target = guidance.getEntry().getStation().getLabObject();
				color = stationColor(guidance.getEntry().getStation());
				label = stationLabel(guidance.getEntry(), false);
				break;
			case USE_ITEM_ON_STATION:
				target = guidance.getEntry().getStation().getLabObject();
				color = stationColor(guidance.getEntry().getStation());
				label = "USE SLOT " + (guidance.getEntry().getInventorySlot() + 1);
				break;
			case WAIT_STATION:
				target = guidance.getEntry().getStation().getLabObject();
				color = stationColor(guidance.getEntry().getStation());
				label = stationLabel(guidance.getEntry(), true);
				break;
			case DEPOSIT:
				DeliveryDecision decision = plugin.getDeliveryDecision();
				target = LabObject.CONVEYOR;
				switch (decision.getKind())
				{
					case REFILL:
						// Too few orders are fillable: the conveyor is deliberately not highlighted.
						return null;
					case WASTE:
						color = WASTE_COLOR;
						label = "WASTES " + decision.getWastedPotion().name();
						break;
					case DELIVER:
						color = COMPLETE_COLOR;
						label = "DELIVER " + decision.getFillable();
						break;
					default:
						color = COMPLETE_COLOR;
						label = "BATCH READY";
				}
				break;
			default:
				return null;
		}

		drawTarget(graphics, target, label, color);
		return null;
	}

	/**
	 * Outlines stations with an open quick-action window that are not already the
	 * guided target; the guided target switches its own outline colour instead.
	 */
	private void renderQuickActions(Guidance guidance)
	{
		for (Station station : Station.values())
		{
			if (!plugin.isQuickActionOpen(station) || isGuidedStation(guidance, station))
			{
				continue;
			}
			TileObject object = objects.find(station.getLabObject());
			if (object != null)
			{
				outliner.drawOutline(
					object,
					config.outlineWidth(),
					config.quickActionColor(),
					config.outlineFeather());
			}
		}
	}

	private void renderDigweed(Graphics2D graphics)
	{
		if (!config.highlightDigweed())
		{
			return;
		}
		for (DigweedSpot spot : plugin.getReadyDigweed())
		{
			drawTarget(graphics, spot.getLabObject(), "DIGWEED", config.digweedColor());
		}
	}

	private static boolean isGuidedStation(Guidance guidance, Station station)
	{
		return guidance.targetsStation()
			&& guidance.getEntry() != null
			&& guidance.getEntry().getStation() == station;
	}

	private Color stationColor(Station station)
	{
		return plugin.isQuickActionOpen(station) ? config.quickActionColor() : config.stationColor();
	}

	private void renderPermanentLeverMarkers(Graphics2D graphics)
	{
		for (Component component : Component.values())
		{
			TileObject object = objects.find(component.getLever());
			if (object != null)
			{
				drawLabel(
					graphics,
					object,
					Character.toString(component.getCode()),
					component.getColor(),
					MARKER_OFFSET);
			}
		}
	}

	private void renderMixingRecipe(Graphics2D graphics, Potion potion, Potion nextPotion)
	{
		for (Component component : Component.values())
		{
			TileObject object = objects.find(component.getLever());
			if (object == null)
			{
				continue;
			}
			drawLabel(
				graphics,
				object,
				Character.toString(component.getCode()),
				component.getColor(),
				MARKER_OFFSET);

			String currentLabel = leverLabel(potion, component);
			if (currentLabel != null)
			{
				outliner.drawOutline(
					object,
					config.outlineWidth(),
					component.getColor(),
					config.outlineFeather());
				drawLabel(
					graphics,
					object,
					currentLabel,
					component.getColor(),
					CURRENT_RECIPE_OFFSET);
			}

			String nextLabel = nextPotion == null ? null : leverLabel(nextPotion, component);
			if (nextLabel != null)
			{
				drawLabel(
					graphics,
					object,
					nextLabel,
					NEXT_RECIPE_COLOR,
					NEXT_RECIPE_OFFSET);
			}
		}
		drawTarget(graphics, LabObject.MIXING_VESSEL, "4", VESSEL_COLOR);
	}

	static String leverLabel(Potion potion, Component target)
	{
		StringBuilder label = new StringBuilder();
		Component[] recipe = potion.getRecipe();
		for (int index = 0; index < recipe.length; index++)
		{
			if (recipe[index] != target)
			{
				continue;
			}
			if (label.length() > 0)
			{
				label.append(" / ");
			}
			label.append(index + 1);
		}
		return label.length() == 0 ? null : label.toString();
	}

	private void drawTarget(Graphics2D graphics, LabObject target, String label, Color color)
	{
		TileObject object = objects.find(target);
		if (object == null)
		{
			return;
		}
		outliner.drawOutline(object, config.outlineWidth(), color, config.outlineFeather());
		drawLabel(graphics, object, label, color, isStation(target) ? STATION_LABEL_OFFSET : 0);
	}

	private static boolean isStation(LabObject object)
	{
		for (Station station : Station.values())
		{
			if (station.getLabObject() == object)
			{
				return true;
			}
		}
		return false;
	}

	private static String stationLabel(BatchEntry entry, boolean active)
	{
		return (entry.getStationPosition() + 1) + "/" + entry.getStationTotal()
			+ " " + (active ? "PROCESS" : entry.getStation().getActionName().toUpperCase());
	}

	private void drawLabel(
		Graphics2D graphics,
		TileObject object,
		String text,
		Color color,
		int verticalOffset)
	{
		graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 16f));
		Point location = Perspective.getCanvasTextLocation(client, graphics, object.getLocalLocation(), text, 120);
		if (location == null)
		{
			return;
		}
		int y = location.getY() + verticalOffset;
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, location.getX() + 1, y + 1);
		graphics.setColor(color);
		graphics.drawString(text, location.getX(), y);
	}
}
