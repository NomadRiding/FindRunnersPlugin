package com.foxtrot.findrunners;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FindRunnersPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FindRunnersPlugin.class);
		RuneLite.main(args);
	}
}
