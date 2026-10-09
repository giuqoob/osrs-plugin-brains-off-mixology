package com.mixologybatch;

import net.runelite.api.MenuAction;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public class ConveyorMenuGuardTest
{
	private static final int CONVEYOR = LabObject.CONVEYOR.getObjectId();

	@Test
	public void pairsTheDepositWithTheSameConveyorsExamine()
	{
		ConveyorMenuGuard.MenuScan scan = new ConveyorMenuGuard.MenuScan();
		scan.accept(0, 0, 0, 0, MenuAction.CANCEL, "Cancel");
		scan.accept(1, CONVEYOR, 50, 60, MenuAction.EXAMINE_OBJECT, "Examine");
		scan.accept(2, 0, 0, 0, MenuAction.WALK, "Walk here");
		scan.accept(3, CONVEYOR, 50, 60, MenuAction.GAME_OBJECT_FIRST_OPTION, "Fulfil-order");

		assertArrayEquals(new int[]{3, 1}, scan.select());
	}

	@Test
	public void neverPairsEntriesFromDifferentConveyorTiles()
	{
		ConveyorMenuGuard.MenuScan scan = new ConveyorMenuGuard.MenuScan();
		scan.accept(1, CONVEYOR, 50, 61, MenuAction.EXAMINE_OBJECT, "Examine");
		scan.accept(3, CONVEYOR, 50, 60, MenuAction.GAME_OBJECT_FIRST_OPTION, "Fulfil-order");

		assertNull(scan.select());
	}

	@Test
	public void ignoresOtherObjectsAndMenusWithoutADeposit()
	{
		ConveyorMenuGuard.MenuScan scan = new ConveyorMenuGuard.MenuScan();
		scan.accept(1, LabObject.ALEMBIC.getObjectId(), 50, 60, MenuAction.EXAMINE_OBJECT, "Examine");
		scan.accept(2, LabObject.ALEMBIC.getObjectId(), 50, 60, MenuAction.GAME_OBJECT_FIRST_OPTION, "Crystallise");
		scan.accept(3, CONVEYOR, 50, 60, MenuAction.EXAMINE_OBJECT, "Examine");

		assertNull(scan.select());
	}
}
