package com.mixologybatch;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

final class BatchInventoryOverlay extends WidgetItemOverlay
{
	private static final Color FIRST_BATCH = new Color(80, 220, 255);
	private static final Color SECOND_BATCH = new Color(255, 210, 70);
	private static final Color THIRD_BATCH = new Color(255, 110, 230);
	private static final Color ORDER_POTION = new Color(70, 255, 120);

	private final MixologyBatchPlugin plugin;
	private final MixologyBatchConfig config;

	@Inject
	private BatchInventoryOverlay(MixologyBatchPlugin plugin, MixologyBatchConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!plugin.isInLab() || !config.showInventoryBatches())
		{
			return;
		}
		Potion potion = Potion.fromItemId(itemId);
		if (potion == null)
		{
			return;
		}
		int slot = widgetItem.getWidget().getIndex();
		BatchEntry entry = plugin.getCycleEntry(slot, potion);
		if (entry == null)
		{
			return;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();
		String text = "#" + (entry.getStationOrdinal() + 1) + (entry.isOrderPotion() ? " ORD" : "");
		graphics.setFont(FontManager.getRunescapeSmallFont());
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, bounds.x + 3, bounds.y + 12);
		graphics.setColor(entry.isOrderPotion() ? ORDER_POTION : batchColor(entry.getStationOrdinal()));
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
