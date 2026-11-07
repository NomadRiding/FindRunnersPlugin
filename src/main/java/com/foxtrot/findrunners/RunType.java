package com.foxtrot.findrunners;

public enum RunType
{
	none,
	afk,
	tick;

	@Override
	public String toString()
	{
		if (this == afk)
		{
			return "AFK";
		}
		if (this == tick)
		{
			return "Tick";
		}
		return "None";
	}
}
