package com.mixologybatch;

final class BatchEntry
{
	private final Potion potion;
	private final Station station;
	private final int inventorySlot;
	private final int stationOrdinal;
	private final int stationPosition;
	private final int stationTotal;
	private final boolean orderPotion;

	BatchEntry(
		Potion potion,
		Station station,
		int inventorySlot,
		int stationOrdinal,
		int stationPosition,
		int stationTotal)
	{
		this(potion, station, inventorySlot, stationOrdinal, stationPosition, stationTotal, false);
	}

	BatchEntry(
		Potion potion,
		Station station,
		int inventorySlot,
		int stationOrdinal,
		int stationPosition,
		int stationTotal,
		boolean orderPotion)
	{
		this.potion = potion;
		this.station = station;
		this.inventorySlot = inventorySlot;
		this.stationOrdinal = stationOrdinal;
		this.stationPosition = stationPosition;
		this.stationTotal = stationTotal;
		this.orderPotion = orderPotion;
	}

	Potion getPotion()
	{
		return potion;
	}

	Station getStation()
	{
		return station;
	}

	int getInventorySlot()
	{
		return inventorySlot;
	}

	int getStationOrdinal()
	{
		return stationOrdinal;
	}

	int getStationPosition()
	{
		return stationPosition;
	}

	int getStationTotal()
	{
		return stationTotal;
	}

	/**
	 * Brewed for a current order rather than the configured stock. Order potions
	 * sit after all stock and are processed at the end of their station's batch.
	 */
	boolean isOrderPotion()
	{
		return orderPotion;
	}

	BatchEntry remap(Potion remappedPotion, int remappedInventorySlot)
	{
		return new BatchEntry(
			remappedPotion,
			station,
			remappedInventorySlot,
			stationOrdinal,
			stationPosition,
			stationTotal,
			orderPotion);
	}
}
