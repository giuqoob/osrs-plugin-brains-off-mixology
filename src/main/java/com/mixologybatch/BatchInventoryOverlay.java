package com.mixologybatch;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

final class BatchInventoryOverlay extends WidgetItemOverlay
{
	private static final Color FIRST_BATCH = new Color(80, 220, 255);
	private static final Color SECOND_BATCH = new Color(255, 210, 70);
	private static final Color THIRD_BATCH = new Color(255, 110, 230);
	private static final Color UNKNOWN_DOT = new Color(155, 155, 155);
	private static final Color UNKNOWN_DOT_OUTLINE = new Color(35, 35, 35, 180);

	private final Client client;
	private final MixologyBatchPlugin plugin;
	private final MixologyBatchConfig config;
	private final TooltipManager tooltipManager;

	@Inject
	private BatchInventoryOverlay(
		Client client,
		MixologyBatchPlugin plugin,
		MixologyBatchConfig config,
		TooltipManager tooltipManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.tooltipManager = tooltipManager;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!plugin.isInLab())
		{
			return;
		}
		Potion potion = Potion.fromItemId(itemId);
		if (potion == null)
		{
			return;
		}
		int slot = widgetItem.getWidget().getIndex();
		Rectangle bounds = widgetItem.getCanvasBounds();
		FinishedPotion finishedPotion = plugin.getFinishedPotions().get(slot);
		if (finishedPotion != null
			&& finishedPotion.getPotion() == potion
			&& potion.isFinishedItem(itemId))
		{
			renderProcessingAids(graphics, bounds, finishedPotion);
		}

		if (!config.showInventoryBatches())
		{
			return;
		}

		BatchEntry entry = plugin.getCycleEntry(slot, potion);
		if (entry == null)
		{
			return;
		}

		String text = "#" + (entry.getStationOrdinal() + 1);
		graphics.setFont(FontManager.getRunescapeSmallFont());
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, bounds.x + 3, bounds.y + 12);
		graphics.setColor(batchColor(entry.getStationOrdinal()));
		graphics.drawString(text, bounds.x + 2, bounds.y + 11);

		Guidance guidance = plugin.getGuidance();
		if (guidance.getPhase() == Guidance.Phase.PROCESSING
			&& guidance.getEntry() != null
			&& guidance.getEntry().getInventorySlot() == slot)
		{
			graphics.setColor(config.stationColor());
			graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
			graphics.drawRect(bounds.x + 1, bounds.y + 1, bounds.width - 3, bounds.height - 3);
		}
	}

	private void renderProcessingAids(Graphics2D graphics, Rectangle bounds, FinishedPotion potion)
	{
		if (config.markUnknownProcessingType() && !potion.getStation().isPresent())
		{
			int x = bounds.x + bounds.width - 6;
			int y = bounds.y + 3;
			graphics.setColor(UNKNOWN_DOT_OUTLINE);
			graphics.drawOval(x - 1, y - 1, 4, 4);
			graphics.setColor(UNKNOWN_DOT);
			graphics.fillOval(x, y, 3, 3);
		}

		if (!config.showProcessingDetailsOnHover()
			|| client.isDraggingWidget()
			|| client.isMenuOpen())
		{
			return;
		}

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		if (!bounds.contains(mouse.getX(), mouse.getY()))
		{
			return;
		}

		String text = potion.getStation()
			.map(station -> station.getProcessingName() + " (" + station.getObjectName() + ")")
			.orElse("Unknown - Inspect to identify");
		tooltipManager.add(new Tooltip(text));
	}

	private static Color batchColor(int stationOrdinal)
	{
			switch (stationOrdinal)
		{
			case 0:
				return FIRST_BATCH;
			case 1:
				return SECOND_BATCH;
			default:
				return THIRD_BATCH;
		}
	}
}
