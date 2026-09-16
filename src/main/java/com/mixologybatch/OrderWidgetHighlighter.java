package com.mixologybatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

final class OrderWidgetHighlighter
{
	private static final int BUILD_POTION_ORDERS_SCRIPT = 7063;
	private static final int FULFILLABLE_COLOR = 0x00ff00;
	private static final int ORDER_COUNT = 3;

	private final Client client;
	private final ClientThread clientThread;
	private final MixologyBatchConfig config;
	private final PotionOrderMonitor monitor;
	private final List<OwnedHighlight> highlights = new ArrayList<>(ORDER_COUNT);

	private Map<Integer, FinishedPotion> lastFinishedPotions = Collections.emptyMap();
	private List<OrderFulfillment.Order> lastOrders = Collections.emptyList();

	@Inject
	OrderWidgetHighlighter(
		Client client,
		ClientThread clientThread,
		MixologyBatchConfig config,
		PotionOrderMonitor monitor)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.monitor = monitor;
	}

	@Subscribe(priority = -1)
	public void onGameTick(GameTick event)
	{
		refresh(false);
	}

	@Subscribe(priority = -1)
	public void onClientTick(ClientTick event)
	{
		refresh(false);
	}

	@Subscribe(priority = -1)
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == BUILD_POTION_ORDERS_SCRIPT)
		{
			refresh(true);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!MixologyBatchConfig.GROUP.equals(event.getGroup())
			|| !"highlightFulfillableOrders".equals(event.getKey()))
		{
			return;
		}

		if (config.highlightFulfillableOrders())
		{
			clientThread.invokeLater(() -> refresh(true));
		}
		else
		{
			clear();
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.MM_OVERLAY)
		{
			clear();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN
			|| state == GameState.HOPPING
			|| state == GameState.CONNECTION_LOST)
		{
			clear();
		}
	}

	void deactivate()
	{
		clear();
	}

	private void refresh(boolean force)
	{
		Map<Integer, FinishedPotion> finishedPotions = monitor.getFinishedPotions();
		List<OrderFulfillment.Order> orders = monitor.getCurrentOrders();
		if (!force && finishedPotions == lastFinishedPotions && orders == lastOrders)
		{
			return;
		}

		clear();
		lastFinishedPotions = finishedPotions;
		lastOrders = orders;
		if (!config.highlightFulfillableOrders()
			|| !monitor.isInventoryAvailable()
			|| orders.size() != ORDER_COUNT)
		{
			return;
		}

		List<Widget> orderTexts = findOrderTexts();
		if (orderTexts.size() != ORDER_COUNT)
		{
			return;
		}

		for (int index = 0; index < ORDER_COUNT; index++)
		{
			if (canFulfill(orders.get(index), finishedPotions.values()))
			{
				highlight(orderTexts.get(index));
			}
		}
	}

	private List<Widget> findOrderTexts()
	{
		Widget parent = client.getWidget(InterfaceID.MmOverlay.CONTENT);
		if (parent == null || parent.isSelfHidden() || parent.getChildren() == null)
		{
			return Collections.emptyList();
		}

		List<Widget> layout = new ArrayList<>();
		for (Widget child : parent.getChildren())
		{
			if (child != null && (child.getType() == WidgetType.GRAPHIC || child.getType() == WidgetType.TEXT))
			{
				layout.add(child);
			}
		}

		if (layout.size() < 1 + ORDER_COUNT * 2 || layout.get(0).getType() != WidgetType.TEXT)
		{
			return Collections.emptyList();
		}

		List<Widget> orderTexts = new ArrayList<>(ORDER_COUNT);
		for (int index = 0; index < ORDER_COUNT; index++)
		{
			Widget graphic = layout.get(index * 2 + 1);
			Widget text = layout.get(index * 2 + 2);
			if (graphic.getType() != WidgetType.GRAPHIC
				|| text.getType() != WidgetType.TEXT
				|| text.getText() == null)
			{
				return Collections.emptyList();
			}
			orderTexts.add(text);
		}
		return orderTexts;
	}

	private static boolean canFulfill(
		OrderFulfillment.Order order,
		Iterable<FinishedPotion> finishedPotions)
	{
		for (FinishedPotion finishedPotion : finishedPotions)
		{
			Optional<Station> station = finishedPotion.getStation();
			if (finishedPotion.getPotion() == order.getPotion()
				&& station.isPresent()
				&& station.get() == order.getStation())
			{
				return true;
			}
		}
		return false;
	}

	private void highlight(Widget widget)
	{
		int originalColor = widget.getTextColor();
		if (originalColor == FULFILLABLE_COLOR)
		{
			return;
		}

		highlights.add(new OwnedHighlight(widget, originalColor));
		widget.setTextColor(FULFILLABLE_COLOR);
	}

	private void clear()
	{
		for (OwnedHighlight highlight : highlights)
		{
			highlight.restore();
		}
		highlights.clear();
		lastFinishedPotions = Collections.emptyMap();
		lastOrders = Collections.emptyList();
	}

	private static final class OwnedHighlight
	{
		private final Widget widget;
		private final int originalColor;

		private OwnedHighlight(Widget widget, int originalColor)
		{
			this.widget = widget;
			this.originalColor = originalColor;
		}

		private void restore()
		{
			if (widget.getTextColor() == FULFILLABLE_COLOR)
			{
				widget.setTextColor(originalColor);
			}
		}
	}
}
