package com.mixologybatch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Optional;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

final class BatchPanelOverlay extends OverlayPanel
{
	private static final Color ERROR = new Color(255, 90, 90);
	private static final Color COMPLETE = new Color(70, 255, 120);
	private static final Color EMPTY = new Color(150, 150, 150);
	private static final Color EXTRA = new Color(255, 175, 70);
	private static final Color UNKNOWN = new Color(255, 190, 70);

	private final MixologyBatchPlugin plugin;
	private final MixologyBatchConfig config;

	@Inject
	private BatchPanelOverlay(MixologyBatchPlugin plugin, MixologyBatchConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		panelComponent.setPreferredSize(new Dimension(270, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInLab() || !config.showPanel())
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder().text("Brains Off Mixology Helper").build());
		Guidance guidance = plugin.getGuidance();
		switch (guidance.getPhase())
		{
			case INVALID:
				addLine("Paused", guidance.getMessage(), ERROR);
				break;
			case EMPTY:
				addLine("No batch", guidance.getMessage(), ERROR);
				break;
			case MIXING:
				renderMixing(guidance);
				break;
			case PROCESSING:
				renderProcessing(guidance);
				break;
			case COMPLETE:
				addLine("Complete", "Deposit / reset", COMPLETE);
				addLine("Planned batch", Integer.toString(plugin.getPlan().size()), COMPLETE);
				break;
			default:
		}
		renderPotionSummary();
		renderBatchInventory();
		return super.render(graphics);
	}

	private void renderPotionSummary()
	{
		PotionSummaryDisplay display = config.potionSummaryDisplay();
		if (display == PotionSummaryDisplay.HIDDEN || !plugin.isInventoryAvailable())
		{
			return;
		}

		if (display.showsCount())
		{
			addLine("Potions", Integer.toString(plugin.getFinishedPotions().size()), Color.WHITE);
		}
		if (display.showsFulfillment())
		{
			Optional<OrderFulfillment.Status> status = plugin.getOrderFulfillment();
			if (status.isPresent())
			{
				addLine("Order", status.get().getDisplayName(), statusColor(status.get()));
			}
		}
	}

	private static Color statusColor(OrderFulfillment.Status status)
	{
		switch (status)
		{
			case READY:
				return COMPLETE;
			case UNKNOWN:
				return UNKNOWN;
			default:
				return EMPTY;
		}
	}

	private void renderMixing(Guidance guidance)
	{
		BatchEntry entry = guidance.getEntry();
		addLine("Mix potion", (entry.getInventorySlot() + 1) + "/" + plugin.getPlan().size(), Color.WHITE);
		addLine(entry.getPotion().name(), entry.getPotion().getDisplayName(), Color.WHITE);
		addLine("Recipe", entry.getPotion().getRecipeSequence(), Color.WHITE);
		addLine("Later", "#" + (entry.getStationOrdinal() + 1) + " " + entry.getStation().getObjectName(), config.stationColor());
	}

	private void renderProcessing(Guidance guidance)
	{
		BatchEntry entry = guidance.getEntry();
		addLine("Station batch", (entry.getStationOrdinal() + 1) + "/3", config.stationColor());
		addLine(entry.getStation().getObjectName(), entry.getStation().getActionName(), config.stationColor());
		addLine("Potion", (entry.getStationPosition() + 1) + "/" + entry.getStationTotal() + "  " + entry.getPotion().name(), Color.WHITE);
		addLine("NEXT", guidance.getAction() == Guidance.Action.WAIT_STATION ? "Processing" : "Use station", config.stationColor());
	}

	private void renderBatchInventory()
	{
		BatchPlan plan = plugin.getPlan();
		if (!plan.isValid() || plan.size() == 0)
		{
			return;
		}

		panelComponent.getChildren().add(TitleComponent.builder().text("Batch inventory").build());
		int capacity = plugin.getCyclePotionCapacity();
		if (capacity < plan.size())
		{
			addLine(capacity + "/" + plan.size(), "potion slots available", EXTRA);
		}
		for (Potion potion : Potion.values())
		{
			int configured = plan.getConfiguredCount(potion);
			int current = plugin.getCurrentPotionCount(potion);
			if (configured == 0 && current == 0)
			{
				continue;
			}
			if (configured == 0)
			{
				addLine(Integer.toString(current), potion.name() + " extra", EXTRA);
				continue;
			}
			Color color = current == 0
				? EMPTY
				: current > configured ? EXTRA
				: current == configured ? COMPLETE : Color.WHITE;
			addLine(current + "/" + configured, potion.name() + " left", color);
		}
	}

	private void addLine(String left, String right, Color rightColor)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(left)
			.right(right)
			.rightColor(rightColor)
			.build());
	}
}
