package com.mixologybatch;

import net.runelite.api.gameval.VarbitID;

enum Station
{
	CRYSTALLISE("Alembic", "Crystallise", "CRY", LabObject.ALEMBIC, VarbitID.MM_LAB_ALEMBIC_POTION, 3),
	HOMOGENISE("Agitator", "Homogenise", "HOM", LabObject.AGITATOR, VarbitID.MM_LAB_AGITATOR_POTION, 1),
	CONCENTRATE("Retort", "Concentrate", "CON", LabObject.RETORT, VarbitID.MM_LAB_RETORT_POTION, 2);

	private final String objectName;
	private final String actionName;
	private final String shortName;
	private final LabObject labObject;
	private final int potionVarbit;
	private final int orderModifier;

	Station(
		String objectName,
		String actionName,
		String shortName,
		LabObject labObject,
		int potionVarbit,
		int orderModifier)
	{
		this.objectName = objectName;
		this.actionName = actionName;
		this.shortName = shortName;
		this.labObject = labObject;
		this.potionVarbit = potionVarbit;
		this.orderModifier = orderModifier;
	}

	String getObjectName()
	{
		return objectName;
	}

	String getActionName()
	{
		return actionName;
	}

	String getShortName()
	{
		return shortName;
	}

	LabObject getLabObject()
	{
		return labObject;
	}

	int getPotionVarbit()
	{
		return potionVarbit;
	}

	/**
	 * Order modifier varbits use 1 = Homogenous, 2 = Concentrated and
	 * 3 = Crystalised, which deliberately differs from this enum's order.
	 */
	static Station fromOrderModifier(int value)
	{
		for (Station station : values())
		{
			if (station.orderModifier == value)
			{
				return station;
			}
		}
		return null;
	}
}

