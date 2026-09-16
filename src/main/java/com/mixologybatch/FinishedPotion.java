package com.mixologybatch;

import java.util.Objects;
import java.util.Optional;

final class FinishedPotion
{
	private final Potion potion;
	private final Station station;

	private FinishedPotion(Potion potion, Station station)
	{
		this.potion = Objects.requireNonNull(potion);
		this.station = station;
	}

	static FinishedPotion unknown(Potion potion)
	{
		return new FinishedPotion(potion, null);
	}

	static FinishedPotion known(Potion potion, Station station)
	{
		return new FinishedPotion(potion, Objects.requireNonNull(station));
	}

	Potion getPotion()
	{
		return potion;
	}

	Optional<Station> getStation()
	{
		return Optional.ofNullable(station);
	}
}
