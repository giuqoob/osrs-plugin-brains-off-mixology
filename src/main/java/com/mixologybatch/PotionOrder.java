package com.mixologybatch;

/**
 * One of the three current conveyor orders: a potion type that must be
 * processed at a specific station.
 */
final class PotionOrder
{
	private final Potion potion;
	private final Station station;

	PotionOrder(Potion potion, Station station)
	{
		this.potion = potion;
		this.station = station;
	}

	/**
	 * Decodes the MM_LAB_ORDER_n_TYPE / _MODIFIER varbit pair. Returns null
	 * when either value is unset, as the game briefly zeroes both on delivery.
	 */
	static PotionOrder fromVarbits(int typeValue, int modifierValue)
	{
		Potion potion = Potion.fromVarbit(typeValue);
		Station station = Station.fromOrderModifier(modifierValue);
		return potion == null || station == null ? null : new PotionOrder(potion, station);
	}

	Potion getPotion()
	{
		return potion;
	}

	Station getStation()
	{
		return station;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof PotionOrder))
		{
			return false;
		}
		PotionOrder order = (PotionOrder) other;
		return potion == order.potion && station == order.station;
	}

	@Override
	public int hashCode()
	{
		return potion.hashCode() * 31 + station.hashCode();
	}
}
