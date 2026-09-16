package com.mixologybatch;

import java.util.Optional;
import net.runelite.api.gameval.VarbitID;

enum Station
{
	CRYSTALLISE("Alembic", "Crystallise", "Crystallised", "CRY", LabObject.ALEMBIC, VarbitID.MM_LAB_ALEMBIC_POTION),
	HOMOGENISE("Agitator", "Homogenise", "Homogenised", "HOM", LabObject.AGITATOR, VarbitID.MM_LAB_AGITATOR_POTION),
	CONCENTRATE("Retort", "Concentrate", "Concentrated", "CON", LabObject.RETORT, VarbitID.MM_LAB_RETORT_POTION);

	private final String objectName;
	private final String actionName;
	private final String processingName;
	private final String shortName;
	private final LabObject labObject;
	private final int potionVarbit;

	Station(String objectName, String actionName, String processingName, String shortName, LabObject labObject, int potionVarbit)
	{
		this.objectName = objectName;
		this.actionName = actionName;
		this.processingName = processingName;
		this.shortName = shortName;
		this.labObject = labObject;
		this.potionVarbit = potionVarbit;
	}

	String getObjectName()
	{
		return objectName;
	}

	String getActionName()
	{
		return actionName;
	}

	String getProcessingName()
	{
		return processingName;
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

	static Optional<Station> fromOrderModifier(int value)
	{
		switch (value)
		{
			case 1:
				return Optional.of(HOMOGENISE);
			case 2:
				return Optional.of(CONCENTRATE);
			case 3:
				return Optional.of(CRYSTALLISE);
			default:
				return Optional.empty();
		}
	}
}
