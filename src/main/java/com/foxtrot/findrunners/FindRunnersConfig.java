package com.foxtrot.findrunners;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("findrunners")
public interface FindRunnersConfig extends Config
{
	@ConfigSection(
		name = "General",
		description = "Trade chat and screenshots",
		position = 0
	)
	String generalSection = "general";

	@ConfigItem(
		position = 1,
		keyName = "filterTrade",
		name = "Filter trade messages",
		description = "Remove trade chat lines other than trade requests.",
		section = generalSection
	)
	default boolean filterTradeEnabled()
	{
		return true;
	}

	@ConfigItem(
		position = 2,
		keyName = "spamTrade",
		name = "Repeat trade requests",
		description = "Copy an incoming trade request in your trade chat so it is easier to see.",
		section = generalSection
	)
	default boolean spamTrade()
	{
		return true;
	}

	@ConfigSection(
		name = "Friends chat",
		description = "Friends chat overlay and colors",
		position = 100
	)
	String chatSection = "chat";

	@ConfigItem(
		position = 101,
		keyName = "ccName",
		name = "Friends chat owner",
		description = "Owner of the friends chat this plugin should listen to.",
		section = chatSection
	)
	default String ccName()
	{
		return "";
	}

	@ConfigItem(
		position = 102,
		keyName = "splitCC",
		name = "Split friends chat",
		description = "Show recent friends chat lines above the chat box.",
		section = chatSection
	)
	default boolean splitCcEnabled()
	{
		return true;
	}

	@ConfigItem(
		position = 103,
		keyName = "ccLines",
		name = "Friends chat lines",
		description = "How many friends chat lines to show in the overlay.",
		section = chatSection
	)
	default int ccLines()
	{
		return 5;
	}

	@ConfigItem(
		position = 104,
		keyName = "ccColor",
		name = "Chat color",
		description = "Color for unranked friends chat messages.",
		section = chatSection
	)
	default Color ccColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		position = 105,
		keyName = "ccRColor",
		name = "Ranked color",
		description = "Color for ranked members.",
		section = chatSection
	)
	default Color ccRankColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		position = 106,
		keyName = "ccSColor",
		name = "Your color",
		description = "Color for your own messages.",
		section = chatSection
	)
	default Color ccSelfColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		position = 107,
		keyName = "ccClientColor",
		name = "Client color",
		description = "Color for the player set as your client.",
		section = chatSection
	)
	default Color ccClientColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
		position = 108,
		keyName = "ccHostColor",
		name = "Host color",
		description = "Color for friends chat members with the Friend rank.",
		section = chatSection
	)
	default Color ccHostColor()
	{
		return Color.RED;
	}

	@ConfigSection(
		name = "Runner",
		description = "Runner tools",
		position = 300
	)
	String runnerSection = "runner";

	@ConfigItem(
		position = 301,
		keyName = "offerAll",
		name = "Offer all",
		description = "Make Offer-all the left-click option while trading.",
		section = runnerSection
	)
	default boolean offerAllEnabled()
	{
		return true;
	}

	@ConfigItem(
		position = 302,
		keyName = "clientFilter",
		name = "Client filter",
		description = "In the friends chat overlay, show only you and your client.",
		section = runnerSection
	)
	default boolean clientFilterEnabled()
	{
		return false;
	}

	@ConfigItem(
		position = 303,
		keyName = "clientName",
		name = "Client name",
		description = "Player marked as your current client.",
		section = runnerSection
	)
	default String clientName()
	{
		return "";
	}

	@ConfigItem(
		position = 304,
		keyName = "enableStats",
		name = "Track trades",
		description = "Record bone trades with your client in the side panel.",
		section = runnerSection
	)
	default boolean enableStats()
	{
		return true;
	}

	@ConfigItem(
		position = 305,
		keyName = "enablePing",
		name = "Runner ping",
		description = "Flash the screen when a ranked member says @runner.",
		section = runnerSection
	)
	default boolean enablePing()
	{
		return true;
	}

	@ConfigItem(
		position = 306,
		keyName = "logoScale",
		name = "Logo scale",
		description = "Logo size. 10 is the normal size.",
		section = runnerSection
	)
	default int logoScale()
	{
		return 10;
	}

	@ConfigSection(
		name = "Connection",
		description = "Optional remote logo",
		position = 500
	)
	String connectionSection = "connection";

	@ConfigItem(
		position = 501,
		keyName = "logoUrl",
		name = "Logo URL",
		description = "Optional web address of a PNG logo. Leave blank to use the built-in icon.",
		section = connectionSection,
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default String logoUrl()
	{
		return "";
	}
}
