package com.mixologybatch;

import com.google.inject.Provides;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
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
	private EventBus eventBus;

	@Inject
	private MixologyBatchConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BatchPanelOverlay panelOverlay;

	@Inject
	private BatchSceneOverlay sceneOverlay;

	@Inject
	private BatchInventoryOverlay inventoryOverlay;

	@Inject
	private BatchQueueOverlay queueOverlay;

	@Inject
	private PotionOrderMonitor potionOrderMonitor;

	@Inject
	private OrderWidgetHighlighter orderWidgetHighlighter;

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

	@Override
	protected void startUp()
	{
		eventBus.register(potionOrderMonitor);
		eventBus.register(orderWidgetHighlighter);
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
		orderWidgetHighlighter.deactivate();
		eventBus.unregister(orderWidgetHighlighter);
		eventBus.unregister(potionOrderMonitor);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		gameTickCounter++;
		updateState();
		potionOrderMonitor.finishTick();
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		if (!inLab || client.isMenuOpen())
		{
			return;
		}

		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		boolean changed = false;
		if (!client.isWidgetSelected()
			&& !client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			changed = InspectMenuSwapper.promoteInspect(
				entries, config.leftClickInspect(), getFinishedPotions());
		}
		if (config.guardWrongStations())
		{
			changed = guardWrongStation(entries) || changed;
		}
		if (changed)
		{
			menu.setMenuEntries(entries);
		}
	}

	private boolean guardWrongStation(MenuEntry[] entries)
	{
		StationMenuGuard.MenuScan scan = new StationMenuGuard.MenuScan();

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
		}

		StationMenuGuard.MenuSwap swap = scan.select();
		if (swap == null || canUseStation(swap.getStation()))
		{
			return false;
		}

		MenuEntry processingEntry = entries[swap.getProcessingIndex()];
		entries[swap.getProcessingIndex()] = entries[swap.getCheckIndex()];
		entries[swap.getCheckIndex()] = processingEntry;
		return true;
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
			case CONNECTION_LOST:
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
		if (!wasInLab)
		{
			potionOrderMonitor.activate(this::updateState);
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
			cyclePlan = CyclePlan.create(plan, planningInventory);
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
			cyclePlan = CyclePlan.create(plan, planningInventory);
			batchMode = BatchMode.REFILLING;
		}
		else if (batchMode.isRefilling()
			&& cyclePlan != null
			&& !cyclePlan.containsPotionSlots(planningInventory))
		{
			cyclePlan = CyclePlan.create(plan, planningInventory);
		}
		if (cyclePlan != null && cyclePlan.isValid())
		{
			cyclePlan.observeInventory(planningInventory);
		}

		if (batchMode.isRefilling()
			&& cyclePlan != null
			&& cyclePlan.isValid()
			&& cyclePlan.getPotionCapacity() > 0
			&& inventoryPotionCount >= cyclePlan.getPotionCapacity()
			&& !mixingActivity)
		{
			batchMode = BatchMode.PROCESSING;
		}
		else if (batchMode.allowsRollingRefill()
			&& activeStations.isEmpty()
			&& !mixingActivity
			&& BatchStateResolver.shouldStartRollingRefill(planningInventory))
		{
			cyclePlan = CyclePlan.create(plan, planningInventory);
			batchMode = BatchMode.REFILLING;
			resetActionQueue();
		}
		else if (!batchMode.isRefilling()
			&& inventoryPotionCount == 0
			&& activeStations.isEmpty())
		{
			cyclePlan = CyclePlan.create(plan, planningInventory);
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
			plan,
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
			cyclePlan = CyclePlan.create(plan, readInventory());
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
				plan, cyclePlan, projectedInventory, nextSlot);
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
		return InventorySlot.fromContainer(
			client.getItemContainer(InventoryID.INV), BatchPlan.INVENTORY_SIZE);
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
		potionOrderMonitor.deactivate();
		cyclePlan = null;
		partialProcessingStation = null;
		currentPotionCounts.clear();
		resetActionQueue();
		batchMode = BatchMode.REFILLING;
		guidance = Guidance.outside();
	}

	boolean isInLab()
	{
		return inLab;
	}

	boolean isInventoryAvailable()
	{
		return potionOrderMonitor.isInventoryAvailable();
	}

	Map<Integer, FinishedPotion> getFinishedPotions()
	{
		return potionOrderMonitor.getFinishedPotions();
	}

	List<OrderFulfillment.Order> getCurrentOrders()
	{
		return potionOrderMonitor.getCurrentOrders();
	}

	Optional<OrderFulfillment.Status> getOrderFulfillment()
	{
		return potionOrderMonitor.getOrderFulfillment();
	}

	Guidance getGuidance()
	{
		return guidance;
	}

	BatchPlan getPlan()
	{
		return plan;
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
