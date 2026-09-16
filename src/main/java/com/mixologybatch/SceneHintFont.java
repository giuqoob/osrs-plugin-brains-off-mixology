package com.mixologybatch;

public enum SceneHintFont
{
	DEFAULT("Default"),
	RUNESCAPE("RuneScape"),
	RUNESCAPE_SMALL("RuneScape Small"),
	RUNESCAPE_BOLD("RuneScape Bold");

	private final String displayName;

	SceneHintFont(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
