package com.mixologybatch;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class OrderFulfillment
{
	private OrderFulfillment()
	{
	}

	static Optional<Order> decodeOrder(int recipeValue, int modifierValue)
	{
		Potion potion = Potion.fromVarbit(recipeValue);
		Optional<Station> station = Station.fromOrderModifier(modifierValue);
		if (potion == null || station.isEmpty())
		{
			return Optional.empty();
		}

		return Optional.of(new Order(potion, station.get()));
	}

	static Status evaluate(Collection<FinishedPotion> inventory, List<Order> orders)
	{
		boolean unknownCandidate = false;
		for (FinishedPotion finishedPotion : inventory)
		{
			for (Order order : orders)
			{
				if (finishedPotion.getPotion() != order.getPotion())
				{
					continue;
				}

				Optional<Station> station = finishedPotion.getStation();
				if (station.isPresent() && station.get() == order.getStation())
				{
					return Status.READY;
				}

				if (station.isEmpty())
				{
					unknownCandidate = true;
				}
			}
		}

		return unknownCandidate ? Status.UNKNOWN : Status.NO_MATCH;
	}

	static final class Order
	{
		private final Potion potion;
		private final Station station;

		Order(Potion potion, Station station)
		{
			this.potion = Objects.requireNonNull(potion);
			this.station = Objects.requireNonNull(station);
		}

		Potion getPotion()
		{
			return potion;
		}

		Station getStation()
		{
			return station;
		}
	}

	enum Status
	{
		READY("Ready"),
		UNKNOWN("Unknown"),
		NO_MATCH("No match");

		private final String displayName;

		Status(String displayName)
		{
			this.displayName = displayName;
		}

		String getDisplayName()
		{
			return displayName;
		}
	}
}
