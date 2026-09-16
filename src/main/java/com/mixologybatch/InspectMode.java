package com.mixologybatch;

public enum InspectMode
{
	OFF("Off"),
	UNKNOWN_FINISHED("Unknown"),
	ALL_FINISHED("All");

	private final String displayName;

	InspectMode(String displayName)
	{
		this.displayName = displayName;
	}

	boolean allows(FinishedPotion potion)
	{
		return this == ALL_FINISHED
			|| (this == UNKNOWN_FINISHED && potion.getStation().isEmpty());
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
