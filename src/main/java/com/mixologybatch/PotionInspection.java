package com.mixologybatch;

import java.util.Locale;
import java.util.Optional;

final class PotionInspection
{
	private static final String DESCRIPTION_PREFIX = "it's a vial of ";

	private PotionInspection()
	{
	}

	static Optional<FinishedPotion> parse(String text)
	{
		if (text == null)
		{
			return Optional.empty();
		}

		String normalized = text.replaceAll("(?i)<br\\s*/?>", " ")
			.replaceAll("<[^>]*>", "")
			.replaceAll("\\s+", " ")
			.trim()
			.toLowerCase(Locale.ROOT);
		if (!normalized.startsWith(DESCRIPTION_PREFIX))
		{
			return Optional.empty();
		}

		String description = normalized.substring(DESCRIPTION_PREFIX.length());
		for (Station station : Station.values())
		{
			for (String modifier : modifierNames(station))
			{
				for (Potion potion : Potion.values())
				{
					String potionDescription = modifier + " "
						+ potion.getDisplayName().toLowerCase(Locale.ROOT) + ".";
					if (description.startsWith(potionDescription))
					{
						return Optional.of(FinishedPotion.known(potion, station));
					}
				}
			}
		}

		return Optional.empty();
	}

	private static String[] modifierNames(Station station)
	{
		switch (station)
		{
			case CRYSTALLISE:
				return new String[]{"crystalised", "crystallised"};
			case HOMOGENISE:
				return new String[]{"homogenous", "homogenised"};
			case CONCENTRATE:
				return new String[]{"concentrated"};
			default:
				throw new IllegalArgumentException("Unknown station: " + station);
		}
	}
}
