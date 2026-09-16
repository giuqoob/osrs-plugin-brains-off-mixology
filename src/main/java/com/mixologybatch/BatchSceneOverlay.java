package com.mixologybatch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

final class BatchSceneOverlay extends Overlay
{
	private static final Color VESSEL_COLOR = Color.WHITE;
	private static final Color COMPLETE_COLOR = new Color(0, 255, 90);
	private static final Color NEXT_RECIPE_COLOR = new Color(150, 150, 150);
	private static final Color UNKNOWN_COLOR = new Color(255, 190, 70);
	private static final Color NO_MATCH_COLOR = new Color(150, 150, 150);
	private static final int MARKER_OFFSET = -22;
	private static final int CURRENT_RECIPE_OFFSET = 0;
	private static final int NEXT_RECIPE_OFFSET = 20;
	private static final int SUMMARY_LINE_HEIGHT = 12;
	private static final int DEPOSIT_SUMMARY_OFFSET = 18;

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
		renderPotionSummary(graphics, guidance);
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
				color = config.stationColor();
				label = stationLabel(guidance.getEntry(), false);
				break;
			case WAIT_STATION:
				target = guidance.getEntry().getStation().getLabObject();
				color = config.stationColor();
				label = stationLabel(guidance.getEntry(), true);
				break;
			case DEPOSIT:
				target = LabObject.CONVEYOR;
				color = COMPLETE_COLOR;
				label = "BATCH READY";
				break;
			default:
				return null;
		}

		drawTarget(graphics, target, label, color);
		return null;
	}

	private void renderPotionSummary(Graphics2D graphics, Guidance guidance)
	{
		PotionSummaryDisplay display = config.potionSummaryDisplay();
		if (display == PotionSummaryDisplay.HIDDEN || !plugin.isInventoryAvailable())
		{
			return;
		}

		Map<Integer, FinishedPotion> finishedPotions = plugin.getFinishedPotions();
		boolean depositPhase = guidance.getAction() == Guidance.Action.DEPOSIT;
		if (finishedPotions.isEmpty() && !depositPhase)
		{
			return;
		}

		TileObject conveyor = objects.find(LabObject.CONVEYOR);
		if (conveyor == null)
		{
			return;
		}

		Font previousFont = graphics.getFont();
		Color previousColor = graphics.getColor();
		try
		{
			graphics.setFont(sceneHintFont(graphics));
			int lineHeight = Math.max(SUMMARY_LINE_HEIGHT, graphics.getFontMetrics().getHeight());
			int offset = depositPhase ? Math.max(DEPOSIT_SUMMARY_OFFSET, lineHeight) : 0;
			if (display.showsCount())
			{
				drawSummaryLine(graphics, conveyor, "Potions: " + finishedPotions.size(), Color.WHITE, offset);
				offset += lineHeight;
			}
			if (display.showsFulfillment())
			{
				Optional<OrderFulfillment.Status> status = plugin.getOrderFulfillment();
				if (status.isPresent())
				{
					drawSummaryLine(
						graphics,
						conveyor,
						"Order: " + status.get().getDisplayName(),
						statusColor(status.get()),
						offset);
				}
			}
		}
		finally
		{
			graphics.setFont(previousFont);
			graphics.setColor(previousColor);
		}
	}

	private Font sceneHintFont(Graphics2D graphics)
	{
		switch (config.sceneHintFont())
		{
			case RUNESCAPE:
				return FontManager.getRunescapeFont();
			case RUNESCAPE_SMALL:
				return FontManager.getRunescapeSmallFont();
			case RUNESCAPE_BOLD:
				return FontManager.getRunescapeBoldFont();
			default:
				return graphics.getFont().deriveFont(Font.BOLD, 16f);
		}
	}

	private void drawSummaryLine(Graphics2D graphics, TileObject object, String text, Color color, int verticalOffset)
	{
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

	private static Color statusColor(OrderFulfillment.Status status)
	{
		switch (status)
		{
			case READY:
				return COMPLETE_COLOR;
			case UNKNOWN:
				return UNKNOWN_COLOR;
			default:
				return NO_MATCH_COLOR;
		}
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
		Font previousFont = graphics.getFont();
		Color previousColor = graphics.getColor();
		try
		{
			graphics.setFont(sceneHintFont(graphics));
			drawSummaryLine(graphics, object, label, color, 0);
		}
		finally
		{
			graphics.setFont(previousFont);
			graphics.setColor(previousColor);
		}
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
		graphics.setFont(sceneHintFont(graphics));
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
