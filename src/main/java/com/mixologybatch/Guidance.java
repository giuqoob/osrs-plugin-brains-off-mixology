package com.mixologybatch;

final class Guidance
{
	enum Phase
	{
		OUTSIDE,
		INVALID,
		EMPTY,
		MIXING,
		PROCESSING,
		COMPLETE
	}

	enum Action
	{
		NONE,
		PULL_LEVER,
		MIX_VESSEL,
		USE_STATION,
		/**
		 * Use a specific inventory potion on the station, because left-clicking
		 * the station would take an earlier unfinished potion instead.
		 */
		USE_ITEM_ON_STATION,
		WAIT_STATION,
		DEPOSIT
	}

	private final Phase phase;
	private final Action action;
	private final BatchEntry entry;
	private final Component component;
	private final int stepNumber;
	private final String message;

	private Guidance(
		Phase phase,
		Action action,
		BatchEntry entry,
		Component component,
		int stepNumber,
		String message)
	{
		this.phase = phase;
		this.action = action;
		this.entry = entry;
		this.component = component;
		this.stepNumber = stepNumber;
		this.message = message;
	}

	static Guidance outside()
	{
		return new Guidance(Phase.OUTSIDE, Action.NONE, null, null, 0, null);
	}

	static Guidance invalid(String message)
	{
		return new Guidance(Phase.INVALID, Action.NONE, null, null, 0, message);
	}

	static Guidance empty()
	{
		return new Guidance(Phase.EMPTY, Action.NONE, null, null, 0, "Configure at least one potion.");
	}

	static Guidance mixing(BatchEntry entry, MixStep step)
	{
		if (step.getKind() == MixStep.Kind.INVALID)
		{
			return invalid(step.getError());
		}
		Action action = step.getKind() == MixStep.Kind.PULL_LEVER
			? Action.PULL_LEVER : Action.MIX_VESSEL;
		return new Guidance(Phase.MIXING, action, entry, step.getComponent(), step.getNumber(), null);
	}

	static Guidance processing(BatchEntry entry, boolean active)
	{
		return new Guidance(
			Phase.PROCESSING,
			active ? Action.WAIT_STATION : Action.USE_STATION,
			entry,
			null,
			0,
			null);
	}

	static Guidance useItemOnStation(BatchEntry entry)
	{
		return new Guidance(Phase.PROCESSING, Action.USE_ITEM_ON_STATION, entry, null, 0, null);
	}

	/**
	 * Whether this guidance points at a station the player should use now or is
	 * already using.
	 */
	boolean targetsStation()
	{
		return action == Action.USE_STATION
			|| action == Action.USE_ITEM_ON_STATION
			|| action == Action.WAIT_STATION;
	}

	static Guidance complete()
	{
		return new Guidance(Phase.COMPLETE, Action.DEPOSIT, null, null, 0, null);
	}

	Phase getPhase()
	{
		return phase;
	}

	Action getAction()
	{
		return action;
	}

	BatchEntry getEntry()
	{
		return entry;
	}

	Component getComponent()
	{
		return component;
	}

	int getStepNumber()
	{
		return stepNumber;
	}

	String getMessage()
	{
		return message;
	}
}

