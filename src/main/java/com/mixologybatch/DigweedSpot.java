package com.mixologybatch;

import net.runelite.api.gameval.VarbitID;

/**
 * The four lab corners where a Digweed can spawn. Each MM_HERB_READY varbit is
 * 1 while its corner's Digweed is ready to pick.
 */
enum DigweedSpot
{
	NORTH_EAST("north-east", LabObject.DIGWEED_NORTH_EAST, VarbitID.MM_HERB_READY_1),
	SOUTH_EAST("south-east", LabObject.DIGWEED_SOUTH_EAST, VarbitID.MM_HERB_READY_2),
	SOUTH_WEST("south-west", LabObject.DIGWEED_SOUTH_WEST, VarbitID.MM_HERB_READY_3),
	NORTH_WEST("north-west", LabObject.DIGWEED_NORTH_WEST, VarbitID.MM_HERB_READY_4);

	private final String cornerName;
	private final LabObject labObject;
	private final int readyVarbit;

	DigweedSpot(String cornerName, LabObject labObject, int readyVarbit)
	{
		this.cornerName = cornerName;
		this.labObject = labObject;
		this.readyVarbit = readyVarbit;
	}

	String getCornerName()
	{
		return cornerName;
	}

	LabObject getLabObject()
	{
		return labObject;
	}

	int getReadyVarbit()
	{
		return readyVarbit;
	}
}
