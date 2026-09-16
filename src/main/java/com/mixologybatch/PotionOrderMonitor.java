package com.mixologybatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.VarbitComposition;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

@Singleton
final class PotionOrderMonitor
{
	private static final int INVENTORY_DRAG_SCRIPT = 6013;
	private static final int[] ORDER_RECIPE_VARBITS = {
		VarbitID.MM_LAB_ORDER_1_TYPE,
		VarbitID.MM_LAB_ORDER_2_TYPE,
		VarbitID.MM_LAB_ORDER_3_TYPE
	};
	private static final int[] ORDER_MODIFIER_VARBITS = {
		VarbitID.MM_LAB_ORDER_1_MODIFIER,
		VarbitID.MM_LAB_ORDER_2_MODIFIER,
		VarbitID.MM_LAB_ORDER_3_MODIFIER
	};

	private final Client client;
	private final ClientThread clientThread;
	private final FinishedPotionTracker tracker = new FinishedPotionTracker();

	private boolean active;
	private boolean inventoryAvailable;
	private boolean inspectionPending;
	private Runnable inventoryChanged;
	private String inspectionDialogText;
	private Map<Integer, FinishedPotion> finishedPotions = Collections.emptyMap();
	private List<OrderFulfillment.Order> currentOrders = Collections.emptyList();
	private Optional<OrderFulfillment.Status> orderFulfillment = Optional.empty();

	@Inject
	PotionOrderMonitor(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	void activate(Runnable inventoryChanged)
	{
		this.inventoryChanged = Objects.requireNonNull(inventoryChanged);
		active = true;
		refreshInventoryAvailability();
	}

	void deactivate()
	{
		active = false;
		inventoryChanged = null;
		suspendInventory();
	}

	void finishTick()
	{
		if (!active || !inventoryAvailable)
		{
			return;
		}

		tracker.finishTick();
		currentOrders = readOrders();
		publish();
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!active)
		{
			return;
		}

		refreshInventoryAvailability();
		checkInspectionDialog();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (!active || event.getContainerId() != InventoryID.INV)
		{
			return;
		}

		ItemContainer container = event.getItemContainer();
		if (container == null)
		{
			suspendInventory();
		}
		else if (inventoryAvailable)
		{
			tracker.observeInventory(InventorySlot.fromContainer(container, BatchPlan.INVENTORY_SIZE));
		}
		else
		{
			initialize(container);
		}

		if (inventoryChanged != null)
		{
			clientThread.invokeLater(inventoryChanged);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (!active)
		{
			return;
		}

		for (Station station : Station.values())
		{
			if (isVarbitChangeFor(event, station.getPotionVarbit()))
			{
				observeStations();
				break;
			}
		}

		if (isOrderVarbitChange(event))
		{
			currentOrders = Collections.emptyList();
			orderFulfillment = Optional.empty();
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (!active || !inventoryAvailable || event.getScriptId() != INVENTORY_DRAG_SCRIPT
			|| event.getScriptEvent() == null)
		{
			return;
		}

		Widget source = event.getScriptEvent().getSource();
		Widget target = event.getScriptEvent().getTarget();
		if (!isInventoryItemWidget(source) || !isInventoryItemWidget(target))
		{
			return;
		}

		cancelInspection();
		tracker.observeSwap(source.getIndex(), target.getIndex());
	}

	@Subscribe(priority = 1)
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!active)
		{
			return;
		}

		if (!beginInspection(event))
		{
			cancelInspection();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (active && inspectionPending && event.getGroupId() == InterfaceID.OBJECTBOX)
		{
			clientThread.invokeLater(this::checkInspectionDialog);
		}
	}

	private void refreshInventoryAvailability()
	{
		ItemContainer container = client.getItemContainer(InventoryID.INV);
		if (container == null)
		{
			if (inventoryAvailable)
			{
				suspendInventory();
			}
		}
		else if (!inventoryAvailable)
		{
			initialize(container);
		}
	}

	private void initialize(ItemContainer container)
	{
		tracker.initialize(
			InventorySlot.fromContainer(container, BatchPlan.INVENTORY_SIZE),
			readStations());
		inventoryAvailable = true;
		finishedPotions = tracker.snapshot();
		currentOrders = Collections.emptyList();
		orderFulfillment = Optional.empty();
	}

	private Map<Station, Potion> readStations()
	{
		Map<Station, Potion> stations = new EnumMap<>(Station.class);
		for (Station station : Station.values())
		{
			Potion potion = Potion.fromVarbit(client.getVarbitValue(station.getPotionVarbit()));
			if (potion != null)
			{
				stations.put(station, potion);
			}
		}
		return stations;
	}

	private void observeStations()
	{
		if (!inventoryAvailable)
		{
			return;
		}

		Map<Station, Potion> stations = readStations();
		for (Station station : Station.values())
		{
			tracker.observeStation(station, stations.get(station));
		}
	}

	private List<OrderFulfillment.Order> readOrders()
	{
		List<OrderFulfillment.Order> orders = new ArrayList<>(ORDER_RECIPE_VARBITS.length);
		for (int index = 0; index < ORDER_RECIPE_VARBITS.length; index++)
		{
			OrderFulfillment.decodeOrder(
				client.getVarbitValue(ORDER_RECIPE_VARBITS[index]),
				client.getVarbitValue(ORDER_MODIFIER_VARBITS[index]))
				.ifPresent(orders::add);
		}
		return orders.size() == ORDER_RECIPE_VARBITS.length
			? Collections.unmodifiableList(orders)
			: Collections.emptyList();
	}

	private void publish()
	{
		finishedPotions = tracker.snapshot();
		orderFulfillment = currentOrders.size() == ORDER_RECIPE_VARBITS.length
			? Optional.of(OrderFulfillment.evaluate(finishedPotions.values(), currentOrders))
			: Optional.empty();
	}

	private boolean beginInspection(MenuOptionClicked event)
	{
		if (!inventoryAvailable || !"Inspect".equalsIgnoreCase(event.getMenuOption()))
		{
			return false;
		}

		Widget inventory = event.getWidget();
		int slot = event.getParam0();
		Potion potion = Potion.fromItemId(event.getItemId());
		FinishedPotion tracked = finishedPotions.get(slot);
		if (inventory == null
			|| inventory.getId() != InterfaceID.Inventory.ITEMS
			|| potion == null
			|| !potion.isFinishedItem(event.getItemId())
			|| tracked == null
			|| tracked.getPotion() != potion)
		{
			return false;
		}

		cancelInspection();
		Widget dialog = client.getWidget(InterfaceID.Objectbox.TEXT);
		inspectionDialogText = dialog == null ? null : dialog.getText();
		tracker.beginInspection(slot);
		inspectionPending = true;
		return true;
	}

	private void checkInspectionDialog()
	{
		if (!active || !inventoryAvailable || !inspectionPending)
		{
			return;
		}

		Widget dialog = client.getWidget(InterfaceID.Objectbox.TEXT);
		if (dialog == null || Objects.equals(inspectionDialogText, dialog.getText()))
		{
			return;
		}

		tracker.resolveInspection(dialog.getText());
		inspectionPending = false;
		inspectionDialogText = null;
		publish();
	}

	private void cancelInspection()
	{
		tracker.cancelInspection();
		inspectionPending = false;
		inspectionDialogText = null;
	}

	private void suspendInventory()
	{
		tracker.clear();
		inventoryAvailable = false;
		finishedPotions = Collections.emptyMap();
		currentOrders = Collections.emptyList();
		orderFulfillment = Optional.empty();
		inspectionPending = false;
		inspectionDialogText = null;
	}

	private static boolean isInventoryItemWidget(Widget widget)
	{
		return widget != null && widget.getParentId() == InterfaceID.Inventory.ITEMS;
	}

	private boolean isOrderVarbitChange(VarbitChanged event)
	{
		for (int index = 0; index < ORDER_RECIPE_VARBITS.length; index++)
		{
			if (isVarbitChangeFor(event, ORDER_RECIPE_VARBITS[index])
				|| isVarbitChangeFor(event, ORDER_MODIFIER_VARBITS[index]))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isVarbitChangeFor(VarbitChanged event, int varbitId)
	{
		if (event.getVarbitId() != -1)
		{
			return event.getVarbitId() == varbitId;
		}

		VarbitComposition varbit = client.getVarbit(varbitId);
		return varbit != null && event.getVarpId() == varbit.getIndex();
	}

	boolean isInventoryAvailable()
	{
		return inventoryAvailable;
	}

	Map<Integer, FinishedPotion> getFinishedPotions()
	{
		return finishedPotions;
	}

	List<OrderFulfillment.Order> getCurrentOrders()
	{
		return currentOrders;
	}

	Optional<OrderFulfillment.Status> getOrderFulfillment()
	{
		return orderFulfillment;
	}
}
