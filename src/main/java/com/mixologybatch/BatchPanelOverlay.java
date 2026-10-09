package com.mixologybatch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
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
		if (config.highlightDigweed())
		{
			for (DigweedSpot spot : plugin.getReadyDigweed())
			{
				addLine("Digweed ready", spot.getCornerName(), config.digweedColor());
			}
		}
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
				renderComplete();
				break;
			default:
		}
		renderOrders();
		renderBatchInventory();
		return super.render(graphics);
	}

	private void renderComplete()
	{
		DeliveryDecision decision = plugin.getDeliveryDecision();
		switch (decision.getKind())
		{
			case DELIVER:
				addLine("Deliver", decision.getFillable() + " order" + (decision.getFillable() == 1 ? "" : "s") + " ready", COMPLETE);
				break;
			case REFILL:
				addLine("Refill", decision.getFillable() + "/" + decision.getRequired() + " orders ready", EXTRA);
				break;
			case WASTE:
				addLine("No order ready", "Deposit wastes", ERROR);
				addLine(
					"Slot " + (decision.getWastedSlot() + 1),
					decision.getWastedPotion().name(),
					ERROR);
				break;
			default:
				addLine("Complete", "Deposit / reset", COMPLETE);
				addLine("Potions", plugin.getPlan().size() + "/" + plugin.getPlan().size(), COMPLETE);
		}
	}

	private void renderOrders()
	{
		OrderMatch match = plugin.getOrderMatch();
		if (!config.useCurrentOrders() || match.getOrders().isEmpty())
		{
			return;
		}

		panelComponent.getChildren().add(TitleComponent.builder().text("Orders").build());
		DeliveryDecision decision = plugin.getDeliveryDecision();
		if (decision.getKind() != DeliveryDecision.Kind.UNKNOWN)
		{
			addLine(
				"Ready",
				decision.getFillable() + " (need " + decision.getRequired() + ")",
				decision.getKind() == DeliveryDecision.Kind.DELIVER ? COMPLETE : EXTRA);
		}
		int unbrewable = plugin.getUnbrewableOrderCount();
		if (unbrewable > 0)
		{
			addLine("No room", unbrewable + " order potion" + (unbrewable == 1 ? "" : "s") + " not brewed", ERROR);
		}
		for (int index = 0; index < match.getOrders().size(); index++)
		{
			PotionOrder order = match.getOrders().get(index);
			if (order == null)
			{
				addLine("?", "Unknown", EMPTY);
				continue;
			}
			boolean fillable = match.isFillable(index);
			addLine(
				order.getPotion().name() + "  " + order.getStation().getObjectName(),
				fillable ? "Ready"
					: config.skipOrderPotions().contains(order.getPotion()) ? "Skipped" : "Missing",
				fillable ? COMPLETE : EMPTY);
		}
	}

	private void renderMixing(Guidance guidance)
	{
		BatchEntry entry = guidance.getEntry();
		addLine("Mix potion", (entry.getInventorySlot() + 1) + "/" + plugin.getPlan().size(), Color.WHITE);
		addLine(entry.getPotion().name(), entry.getPotion().getDisplayName(), Color.WHITE);
		addLine("Recipe", entry.getPotion().getRecipeSequence(), Color.WHITE);
		addLine("Later", "#" + (entry.getStationOrdinal() + 1) + " " + entry.getStation().getObjectName(), config.stationColor());
		if (entry.isOrderPotion())
		{
			addLine("For order", entry.getPotion().name() + "  " + entry.getStation().getObjectName(), COMPLETE);
		}
	}

	private void renderProcessing(Guidance guidance)
	{
		BatchEntry entry = guidance.getEntry();
		addLine("Station batch", (entry.getStationOrdinal() + 1) + "/3", config.stationColor());
		addLine(entry.getStation().getObjectName(), entry.getStation().getActionName(), config.stationColor());
		addLine("Potion", (entry.getStationPosition() + 1) + "/" + entry.getStationTotal() + "  " + entry.getPotion().name(), Color.WHITE);
		switch (guidance.getAction())
		{
			case WAIT_STATION:
				addLine("NEXT", "Processing", config.stationColor());
				break;
			case USE_ITEM_ON_STATION:
				addLine("NEXT", "Use slot " + (entry.getInventorySlot() + 1) + " on " + entry.getStation().getObjectName(), config.stationColor());
				break;
			default:
				addLine("NEXT", "Use station", config.stationColor());
		}
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
