package com.mixologybatch;

import com.google.inject.Provides;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Brains Off Mixology Helper",
	description = "Guides configurable low-attention Mastering Mixology inventories through ordered mixing and station batches",
	tags = {"mastering", "mixology", "herblore", "batch", "helper", "skilling", "varlamore"}
)
public class MixologyBatchPlugin extends Plugin
{
	private static final int LAB_REGION = 5521;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MixologyBatchConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Notifier notifier;

	@Inject
	private BatchPanelOverlay panelOverlay;

	@Inject
	private BatchSceneOverlay sceneOverlay;

	@Inject
	private BatchInventoryOverlay inventoryOverlay;

	@Inject
	private BatchQueueOverlay queueOverlay;

	private final BatchStateResolver resolver = new BatchStateResolver();
	private BatchPlan plan = BatchPlan.defaultPlan();
	private CyclePlan cyclePlan;
	private Guidance guidance = Guidance.outside();
	private EnumMap<Potion, Integer> currentPotionCounts = new EnumMap<>(Potion.class);
	private final Deque<Potion> previousQueuePotions = new ArrayDeque<>();
	private List<Potion> upcomingQueuePotions = Collections.emptyList();
	private List<Potion> plannedPotionQueue = Collections.emptyList();
	private Potion trackedPotion;
	private int trackedStep = 1;
	private MixPrediction mixPrediction;
	private Station partialProcessingStation;
	private int gameTickCounter;
	private BatchMode batchMode = BatchMode.REFILLING;
	private boolean inLab;
	private final ProcessedStationTracker processedStations = new ProcessedStationTracker();
	private List<PotionOrder> currentOrders = Collections.emptyList();
	private OrderMatch orderMatch = OrderMatch.none();
	private DeliveryDecision deliveryDecision = DeliveryDecision.unknown();
	private final QuickActionTracker quickActions = new QuickActionTracker();
	private final DigweedTracker digweed = new DigweedTracker();

	@Override
	protected void startUp()
	{
		overlayManager.add(panelOverlay);
		overlayManager.add(sceneOverlay);
		overlayManager.add(inventoryOverlay);
		overlayManager.add(queueOverlay);
		rebuildPlan();
		clientThread.invokeLater(this::updateState);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(panelOverlay);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(inventoryOverlay);
		overlayManager.remove(queueOverlay);
		deactivateLab();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		gameTickCounter++;
		readOrders();
		updateState();
		updateQuickActions();
		updateDigweed();
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		QuickAction action = QuickAction.fromSpotanim(event.getGraphicsObject().getId());
		if (!inLab || action == null || !config.highlightQuickAction())
		{
			return;
		}
		quickActions.open(
			action,
			client.getVarbitValue(action.getStation().getPotionVarbit()) != 0,
			client.getVarbitValue(action.getProgressVarbit()),
			client.getVarbitValue(action.getSkillshotVarbit()));
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Close a window as soon as it ends rather than on the next tick.
		if (quickActions.hasOpenWindow())
		{
			updateQuickActions();
		}
		for (DigweedSpot spot : DigweedSpot.values())
		{
			if (event.getVarbitId() == spot.getReadyVarbit())
			{
				updateDigweed();
				break;
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (inLab && event.getContainerId() == InventoryID.INV)
		{
			clientThread.invokeLater(this::updateState);
		}
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		boolean guardStations = config.guardWrongStations();
		boolean guardConveyor = config.guardConveyor()
			&& deliveryDecision.getKind() == DeliveryDecision.Kind.REFILL;
		if (!inLab || (!guardStations && !guardConveyor) || client.isMenuOpen())
		{
			return;
		}

		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		StationMenuGuard.MenuScan scan = new StationMenuGuard.MenuScan();
		ConveyorMenuGuard.MenuScan conveyorScan = new ConveyorMenuGuard.MenuScan();

		for (int i = 0; i < entries.length; i++)
		{
			MenuEntry entry = entries[i];
			scan.accept(
				i,
				entry.getIdentifier(),
				entry.getParam0(),
				entry.getParam1(),
				entry.getType(),
				entry.getOption());
			conveyorScan.accept(
				i,
				entry.getIdentifier(),
				entry.getParam0(),
				entry.getParam1(),
				entry.getType(),
				entry.getOption());
		}

		boolean changed = false;
		StationMenuGuard.MenuSwap swap = guardStations ? scan.select() : null;
		if (swap != null && !canUseStation(swap.getStation()))
		{
			swapEntries(entries, swap.getProcessingIndex(), swap.getCheckIndex());
			changed = true;
		}
		int[] conveyorSwap = guardConveyor ? conveyorScan.select() : null;
		if (conveyorSwap != null)
		{
			swapEntries(entries, conveyorSwap[0], conveyorSwap[1]);
			changed = true;
		}
		if (changed)
		{
			menu.setMenuEntries(entries);
		}
	}

	private static void swapEntries(MenuEntry[] entries, int first, int second)
	{
		MenuEntry swapped = entries[first];
		entries[first] = entries[second];
		entries[second] = swapped;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!inLab || "Examine".equalsIgnoreCase(event.getMenuOption()))
		{
			return;
		}

		Station clickedStation = StationMenuGuard.stationForObjectId(event.getId());
		if (batchMode.isRefilling()
			&& clickedStation != null
			&& clickedStation == partialProcessingStation
			&& event.getMenuAction() == MenuAction.GAME_OBJECT_FIRST_OPTION
			&& canUseStation(clickedStation))
		{
			// Treat choosing the available processing station before the inventory
			// is full as an explicit request to finish and deposit this partial batch.
			batchMode = BatchMode.FINISHING_PARTIAL;
			resetActionQueue();
			updateState();
			return;
		}

		Component lever = componentForLeverObject(event.getId());
		if (lever != null)
		{
			handleLeverClick(lever);
			return;
		}
		if (event.getId() == LabObject.MIXING_VESSEL.getObjectId()
			&& "Mix".equalsIgnoreCase(event.getMenuOption()))
		{
			handleMixClick();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				deactivateLab();
				break;
			default:
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.MM_OVERLAY)
		{
			clientThread.invokeLater(this::updateState);
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.MM_OVERLAY)
		{
			deactivateLab();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!MixologyBatchConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		rebuildPlan();
		clientThread.invokeLater(this::updateState);
	}

	private void rebuildPlan()
	{
		EnumMap<Potion, Integer> counts = new EnumMap<>(Potion.class);
		counts.put(Potion.MMM, config.mmmCount());
		counts.put(Potion.MMA, config.mmaCount());
		counts.put(Potion.MML, config.mmlCount());
		counts.put(Potion.AAA, config.aaaCount());
		counts.put(Potion.AAM, config.aamCount());
		counts.put(Potion.ALA, config.alaCount());
		counts.put(Potion.LLL, config.lllCount());
		counts.put(Potion.MLL, config.mllCount());
		counts.put(Potion.ALL, config.allCount());
		counts.put(Potion.MAL, config.malCount());
		plan = BatchPlan.create(counts, config.stationOrder());
		cyclePlan = null;
		resetActionQueue();
	}

	private void updateState()
	{
		boolean wasInLab = inLab;
		inLab = isPlayerInMixologyRoom();
		if (!inLab)
		{
			deactivateLab();
			return;
		}

		List<InventorySlot> inventory = readInventory();
		Potion vesselPotion = Potion.fromVarbit(client.getVarbitValue(VarbitID.MM_LAB_VESSEL_READY));
		int[] mixerSlots = {
			client.getVarbitValue(VarbitID.MM_LAB_MIXER_SLOT_0),
			client.getVarbitValue(VarbitID.MM_LAB_MIXER_SLOT_1),
			client.getVarbitValue(VarbitID.MM_LAB_MIXER_SLOT_2)
		};
		Map<Station, Potion> activeStations = new EnumMap<>(Station.class);
		for (Station station : Station.values())
		{
			Potion potion = Potion.fromVarbit(client.getVarbitValue(station.getPotionVarbit()));
			if (potion != null)
			{
				activeStations.put(station, potion);
			}
		}
		reconcileMixPrediction(inventory);
		if (config.useCurrentOrders())
		{
			processedStations.observe(inventory, activeStations, gameTickCounter);
		}

		List<InventorySlot> planningInventory = inventory;
		Potion planningVesselPotion = vesselPotion;
		int[] planningMixerSlots = mixerSlots;
		if (mixPrediction != null
			&& inventory.get(mixPrediction.getInventorySlot()).isEmpty())
		{
			planningInventory = new ArrayList<>(inventory);
			planningInventory.set(
				mixPrediction.getInventorySlot(),
				InventorySlot.fromItemId(mixPrediction.getPotion().getUnfinishedItemId()));
			planningVesselPotion = null;
			planningMixerSlots = new int[]{0, 0, 0};
		}

		Guidance previousGuidance = guidance;
		int inventoryPotionCount = countInventoryPotions(planningInventory);
		boolean mixingActivity = vesselPotion != null || BatchStateResolver.hasMixerContents(mixerSlots);
		if (!wasInLab || cyclePlan == null || !cyclePlan.belongsTo(plan))
		{
			cyclePlan = CyclePlan.create(refillPlan(), planningInventory);
			batchMode = cyclePlan.isValid()
				? BatchMode.initialize(
					inventoryPotionCount,
					activeStations.size(),
					cyclePlan.getPotionCapacity(),
					mixingActivity,
					BatchStateResolver.firstUnfinishedEntry(cyclePlan, inventory) != null)
				: BatchMode.PROCESSING;
		}

		if (batchMode == BatchMode.PROCESSING && mixingActivity && activeStations.isEmpty())
		{
			cyclePlan = CyclePlan.create(refillPlan(), planningInventory);
			batchMode = BatchMode.REFILLING;
		}
		else if (batchMode.isRefilling()
			&& cyclePlan != null
			&& !cyclePlan.containsPotionSlots(planningInventory))
		{
			cyclePlan = CyclePlan.create(refillPlan(), planningInventory);
		}
		if (cyclePlan != null && cyclePlan.isValid())
		{
			cyclePlan.observeInventory(planningInventory);
		}

		orderMatch = config.useCurrentOrders()
			? OrderMatch.match(currentOrders, inventory, processedStations(inventory))
			: OrderMatch.none();
		deliveryDecision = config.useCurrentOrders()
			? DeliveryDecision.decide(
				orderMatch,
				config.skipOrderPotions(),
				config.deliverThreshold(),
				BatchStateResolver.canRefill(refillPlan(), planningInventory),
				planningInventory)
			: DeliveryDecision.unknown();

		if (batchMode.isRefilling()
			&& cyclePlan != null
			&& cyclePlan.isValid()
			&& cyclePlan.getPotionCapacity() > 0
			&& inventoryPotionCount >= cyclePlan.getPotionCapacity()
			&& !mixingActivity)
		{
			batchMode = BatchMode.PROCESSING;
		}
		else if (activeStations.isEmpty()
			&& !mixingActivity
			&& rollingRefillDue(planningInventory))
		{
			cyclePlan = CyclePlan.create(refillPlan(), planningInventory);
			batchMode = BatchMode.REFILLING;
			resetActionQueue();
		}
		else if (!batchMode.isRefilling()
			&& inventoryPotionCount == 0
			&& activeStations.isEmpty())
		{
			cyclePlan = CyclePlan.create(refillPlan(), planningInventory);
			batchMode = BatchMode.REFILLING;
		}

		BatchEntry partialEntry = activeStations.isEmpty()
			? BatchStateResolver.firstUnfinishedEntry(cyclePlan, inventory)
			: null;
		partialProcessingStation = partialEntry == null ? null : partialEntry.getStation();

		currentPotionCounts = BatchStateResolver.potionCounts(planningInventory);
		if (planningVesselPotion != null)
		{
			currentPotionCounts.merge(planningVesselPotion, 1, Integer::sum);
		}
		for (Potion activePotion : activeStations.values())
		{
			currentPotionCounts.merge(activePotion, 1, Integer::sum);
		}
		plannedPotionQueue = buildPlannedPotionQueue(planningInventory, 4);

		guidance = resolver.resolve(
			activePlan(),
			cyclePlan,
			batchMode.isRefilling(),
			planningInventory,
			planningVesselPotion,
			planningMixerSlots,
			activeStations,
			previousGuidance);
		synchronizeActionQueue();
	}

	private void handleLeverClick(Component clicked)
	{
		if (!batchMode.isRefilling())
		{
			mixPrediction = null;
			cyclePlan = CyclePlan.create(refillPlan(), readInventory());
			batchMode = BatchMode.REFILLING;
			trackedPotion = null;
			trackedStep = 1;
			updateState();
		}
		if (trackedPotion == null || trackedStep < 1 || trackedStep > 3)
		{
			return;
		}

		MixQueueAction expected = MixQueueAction.forStep(trackedPotion, trackedStep);
		if (expected.getComponent() != clicked)
		{
			// Do not let a mistaken click advance or replace the intended sequence.
			refreshPotionQueue();
			return;
		}
		trackedStep++;
		refreshPotionQueue();
	}

	private void handleMixClick()
	{
		if (trackedPotion == null
			|| mixPrediction != null
			|| cyclePlan == null
			|| !cyclePlan.isValid())
		{
			return;
		}

		List<InventorySlot> inventory = readInventory();
		int inventorySlot = cyclePlan.firstEmptySlot(inventory);
		if (inventorySlot < 0)
		{
			return;
		}

		Potion predictedPotion = trackedPotion;
		mixPrediction = new MixPrediction(
			predictedPotion,
			inventorySlot,
			BatchStateResolver.potionCounts(inventory),
			gameTickCounter + 8);
		trackedPotion = null;
		trackedStep = 1;
		updateState();
	}

	private void reconcileMixPrediction(List<InventorySlot> inventory)
	{
		if (mixPrediction == null)
		{
			return;
		}

		Potion actualPotion = mixPrediction.findActualPotion(inventory);

		if (actualPotion != null)
		{
			addPreviousPotion(actualPotion);
			mixPrediction = null;
			trackedPotion = null;
			trackedStep = 1;
		}
		else if (mixPrediction.isExpired(gameTickCounter))
		{
			mixPrediction = null;
			trackedPotion = null;
			trackedStep = 1;
		}
	}

	private void synchronizeActionQueue()
	{
		if (guidance.getPhase() != Guidance.Phase.MIXING || guidance.getEntry() == null)
		{
			trackedPotion = null;
			trackedStep = 1;
			upcomingQueuePotions = Collections.emptyList();
			return;
		}

		Potion guidedPotion = guidance.getEntry().getPotion();
		if (trackedPotion != guidedPotion)
		{
			trackedPotion = guidedPotion;
			trackedStep = guidance.getStepNumber() >= 1 && guidance.getStepNumber() <= 4
				? guidance.getStepNumber() : 1;
		}
		else if (guidance.getStepNumber() > trackedStep && guidance.getStepNumber() <= 4)
		{
			trackedStep = guidance.getStepNumber();
		}
		refreshPotionQueue();
	}

	private void refreshPotionQueue()
	{
		if (trackedPotion == null || trackedStep < 1 || trackedStep > 4)
		{
			upcomingQueuePotions = Collections.emptyList();
			return;
		}

		List<Potion> potions = new ArrayList<>(4);
		potions.add(trackedPotion);
		int potionIndex = !plannedPotionQueue.isEmpty() && plannedPotionQueue.get(0) == trackedPotion ? 1 : 0;
		while (potions.size() < 4 && potionIndex < plannedPotionQueue.size())
		{
			potions.add(plannedPotionQueue.get(potionIndex++));
		}
		upcomingQueuePotions = Collections.unmodifiableList(potions);
	}

	private List<Potion> buildPlannedPotionQueue(List<InventorySlot> inventory, int maximum)
	{
		if (!batchMode.isRefilling() || cyclePlan == null || !cyclePlan.isValid())
		{
			return Collections.emptyList();
		}

		List<InventorySlot> projectedInventory = new ArrayList<>(inventory);
		List<Potion> potions = new ArrayList<>(maximum);
		while (potions.size() < maximum)
		{
			int nextSlot = cyclePlan.firstEmptySlot(projectedInventory);
			if (nextSlot < 0)
			{
				break;
			}
			Potion potion = BatchStateResolver.nextNeededPotion(
				activePlan(), cyclePlan, projectedInventory, nextSlot);
			if (potion == null)
			{
				break;
			}
			potions.add(potion);
			projectedInventory.set(nextSlot, InventorySlot.fromItemId(potion.getUnfinishedItemId()));
		}
		return Collections.unmodifiableList(potions);
	}

	private void addPreviousPotion(Potion potion)
	{
		previousQueuePotions.addLast(potion);
		while (previousQueuePotions.size() > 2)
		{
			previousQueuePotions.removeFirst();
		}
	}

	private void resetActionQueue()
	{
		previousQueuePotions.clear();
		upcomingQueuePotions = Collections.emptyList();
		plannedPotionQueue = Collections.emptyList();
		trackedPotion = null;
		trackedStep = 1;
		mixPrediction = null;
	}

	private static Component componentForLeverObject(int objectId)
	{
		for (Component component : Component.values())
		{
			if (component.getLever().getObjectId() == objectId)
			{
				return component;
			}
		}
		return null;
	}

	private boolean canUseStation(Station station)
	{
		boolean stationContainsPotion = Potion.fromVarbit(
			client.getVarbitValue(station.getPotionVarbit())) != null;
		return StationMenuGuard.canUseStation(
			guidance, station, stationContainsPotion, partialProcessingStation);
	}

	/**
	 * The plan a new cycle starts from: the configured stock plus, when enabled,
	 * potions for current orders that the stock cannot cover. Orders only change
	 * on deposit, so this is stable for the whole refill it starts.
	 */
	private BatchPlan refillPlan()
	{
		if (!config.useCurrentOrders() || !config.brewCurrentOrders())
		{
			return plan;
		}
		return plan.withOrderPotions(plan.ordersToBrew(currentOrders, config.skipOrderPotions()));
	}

	/**
	 * The plan the current cycle follows, including any order potions it was
	 * created with.
	 */
	private BatchPlan activePlan()
	{
		return cyclePlan == null ? plan : cyclePlan.getTarget();
	}

	/**
	 * With known orders, a finished inventory refills exactly when too few orders
	 * can be delivered. Otherwise the original rule applies: refill once only
	 * Mixalots and at most two other finished potions remain.
	 */
	private boolean rollingRefillDue(List<InventorySlot> inventory)
	{
		if (deliveryDecision.getKind() == DeliveryDecision.Kind.UNKNOWN)
		{
			return batchMode.allowsRollingRefill()
				&& BatchStateResolver.shouldStartRollingRefill(inventory);
		}
		return !batchMode.isRefilling()
			&& deliveryDecision.getKind() == DeliveryDecision.Kind.REFILL
			&& !BatchStateResolver.hasUnfinishedPotion(inventory);
	}

	private void updateDigweed()
	{
		if (!inLab)
		{
			digweed.reset();
			return;
		}
		Map<DigweedSpot, Boolean> readiness = new EnumMap<>(DigweedSpot.class);
		for (DigweedSpot spot : DigweedSpot.values())
		{
			readiness.put(spot, client.getVarbitValue(spot.getReadyVarbit()) == 1);
		}
		for (DigweedSpot spawned : digweed.observe(readiness))
		{
			notifier.notify(config.notifyDigweed(), "A Digweed has spawned in the " + spawned.getCornerName() + " corner.");
		}
	}

	private void updateQuickActions()
	{
		if (!inLab || !config.highlightQuickAction())
		{
			quickActions.reset();
			return;
		}
		for (QuickAction action : QuickAction.values())
		{
			quickActions.update(
				action,
				client.getVarbitValue(action.getStation().getPotionVarbit()) != 0,
				client.getVarbitValue(action.getProgressVarbit()),
				client.getVarbitValue(action.getSkillshotVarbit()));
		}
	}

	/**
	 * Orders are read only on game ticks: the game zeroes every order varbit
	 * mid-delivery before writing the new orders.
	 */
	private void readOrders()
	{
		if (!inLab || !config.useCurrentOrders())
		{
			currentOrders = Collections.emptyList();
			return;
		}

		List<PotionOrder> orders = new ArrayList<>(3);
		orders.add(PotionOrder.fromVarbits(
			client.getVarbitValue(VarbitID.MM_LAB_ORDER_1_TYPE),
			client.getVarbitValue(VarbitID.MM_LAB_ORDER_1_MODIFIER)));
		orders.add(PotionOrder.fromVarbits(
			client.getVarbitValue(VarbitID.MM_LAB_ORDER_2_TYPE),
			client.getVarbitValue(VarbitID.MM_LAB_ORDER_2_MODIFIER)));
		orders.add(PotionOrder.fromVarbits(
			client.getVarbitValue(VarbitID.MM_LAB_ORDER_3_TYPE),
			client.getVarbitValue(VarbitID.MM_LAB_ORDER_3_MODIFIER)));
		currentOrders = Collections.unmodifiableList(orders);
	}

	/**
	 * Observed processing stations, falling back to the station the cycle plan
	 * assigned to a slot when the processing was not observed.
	 */
	private Station[] processedStations(List<InventorySlot> inventory)
	{
		Station[] stations = new Station[inventory.size()];
		for (int slot = 0; slot < inventory.size(); slot++)
		{
			InventorySlot actual = inventory.get(slot);
			if (!actual.isFinished())
			{
				continue;
			}
			stations[slot] = processedStations.stationFor(slot);
			if (stations[slot] == null)
			{
				BatchEntry planned = getCycleEntry(slot, actual.getPotion());
				stations[slot] = planned == null ? null : planned.getStation();
			}
		}
		return stations;
	}

	private static int countInventoryPotions(List<InventorySlot> inventory)
	{
		int count = 0;
		for (InventorySlot slot : inventory)
		{
			if (slot.isPotion())
			{
				count++;
			}
		}
		return count;
	}

	private List<InventorySlot> readInventory()
	{
		List<InventorySlot> result = new ArrayList<>(BatchPlan.INVENTORY_SIZE);
		ItemContainer container = client.getItemContainer(InventoryID.INV);
		Item[] items = container == null ? null : container.getItems();
		for (int slot = 0; slot < BatchPlan.INVENTORY_SIZE; slot++)
		{
			if (items == null || slot >= items.length || items[slot] == null)
			{
				result.add(InventorySlot.empty());
			}
			else
			{
				result.add(InventorySlot.fromItemId(items[slot].getId()));
			}
		}
		return result;
	}

	private boolean isPlayerInMixologyRoom()
	{
		Player player = client.getLocalPlayer();
		Widget ordersOverlay = client.getWidget(InterfaceID.MM_OVERLAY, 0);
		return player != null
			&& player.getWorldLocation().getRegionID() == LAB_REGION
			&& player.getWorldLocation().getPlane() == 0
			&& ordersOverlay != null
			&& !ordersOverlay.isSelfHidden();
	}

	private void deactivateLab()
	{
		inLab = false;
		cyclePlan = null;
		partialProcessingStation = null;
		currentPotionCounts.clear();
		resetActionQueue();
		batchMode = BatchMode.REFILLING;
		guidance = Guidance.outside();
		processedStations.reset();
		currentOrders = Collections.emptyList();
		orderMatch = OrderMatch.none();
		deliveryDecision = DeliveryDecision.unknown();
		quickActions.reset();
		digweed.reset();
	}

	boolean isInLab()
	{
		return inLab;
	}

	Guidance getGuidance()
	{
		return guidance;
	}

	BatchPlan getPlan()
	{
		return activePlan();
	}

	/**
	 * Current orders that need brewing but don't fit after the configured stock.
	 */
	int getUnbrewableOrderCount()
	{
		return batchMode.isRefilling() ? activePlan().getDroppedOrderCount() : refillPlan().getDroppedOrderCount();
	}

	OrderMatch getOrderMatch()
	{
		return orderMatch;
	}

	DeliveryDecision getDeliveryDecision()
	{
		return deliveryDecision;
	}

	Set<DigweedSpot> getReadyDigweed()
	{
		return digweed.getReady();
	}

	boolean isQuickActionOpen(Station station)
	{
		return quickActions.isOpen(station);
	}

	BatchEntry getCycleEntry(int inventorySlot, Potion potion)
	{
		return cyclePlan == null || !cyclePlan.isValid()
			? null
			: cyclePlan.entryForSlot(inventorySlot, potion);
	}

	int getCurrentPotionCount(Potion potion)
	{
		return currentPotionCounts.getOrDefault(potion, 0);
	}

	int getCyclePotionCapacity()
	{
		return cyclePlan == null || !cyclePlan.isValid()
			? plan.size()
			: cyclePlan.getPotionCapacity();
	}

	List<Potion> getPreviousQueuePotions()
	{
		return Collections.unmodifiableList(new ArrayList<>(previousQueuePotions));
	}

	List<Potion> getUpcomingQueuePotions()
	{
		return upcomingQueuePotions;
	}

	Potion getNextQueuedPotion()
	{
		return upcomingQueuePotions.size() > 1 ? upcomingQueuePotions.get(1) : null;
	}

	@Provides
	MixologyBatchConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MixologyBatchConfig.class);
	}
}
