package com.mixologybatch;

public enum PotionSummaryDisplay
{
	HIDDEN("Hidden"),
	FINISHED_COUNT("Count"),
	ORDER_FULFILLMENT("Fulfillment"),
	BOTH("Both");

	private final String displayName;

	PotionSummaryDisplay(String displayName)
	{
		this.displayName = displayName;
	}

	boolean showsCount()
	{
		return this == FINISHED_COUNT || this == BOTH;
	}

	boolean showsFulfillment()
	{
		return this == ORDER_FULFILLMENT || this == BOTH;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
