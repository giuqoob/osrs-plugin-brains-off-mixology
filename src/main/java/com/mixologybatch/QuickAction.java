package com.mixologybatch;

import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;

/**
 * Stations with a timed quick-action click. The Retort has none. The window is
 * how many progress steps after the spotanim the click still counts.
 */
enum QuickAction
{
	AGITATOR(
		Station.HOMOGENISE,
		SpotanimID.VFX_MACHINERY_ALCHEMY01_AGITATOR01,
		VarbitID.MM_AGITATOR_PROGRESS,
		VarbitID.MM_LAB_HIT_SKILLSHOT_AGITATOR,
		2),
	ALEMBIC(
		Station.CRYSTALLISE,
		SpotanimID.VFX_MACHINERY_ALCHEMY01_ALEMBIC01,
		VarbitID.MM_ALEMBIC_PROGRESS,
		VarbitID.MM_LAB_HIT_SKILLSHOT_ALEMBIC,
		1);

	private final Station station;
	private final int spotanimId;
	private final int progressVarbit;
	private final int skillshotVarbit;
	private final int window;

	QuickAction(Station station, int spotanimId, int progressVarbit, int skillshotVarbit, int window)
	{
		this.station = station;
		this.spotanimId = spotanimId;
		this.progressVarbit = progressVarbit;
		this.skillshotVarbit = skillshotVarbit;
		this.window = window;
	}

	Station getStation()
	{
		return station;
	}

	int getProgressVarbit()
	{
		return progressVarbit;
	}

	int getSkillshotVarbit()
	{
		return skillshotVarbit;
	}

	int getWindow()
	{
		return window;
	}

	static QuickAction fromSpotanim(int spotanimId)
	{
		for (QuickAction action : values())
		{
			if (action.spotanimId == spotanimId)
			{
				return action;
			}
		}
		return null;
	}

	static QuickAction forStation(Station station)
	{
		for (QuickAction action : values())
		{
			if (action.station == station)
			{
				return action;
			}
		}
		return null;
	}
}
