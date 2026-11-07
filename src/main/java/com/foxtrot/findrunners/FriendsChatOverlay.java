package com.foxtrot.findrunners;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.FriendsChatRank;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class FriendsChatOverlay extends Overlay
{
	private static final int MAX_LINES = 50;

	private final FindRunnersConfig config;
	private final Client client;
	private final Deque<ChatLine> lines = new ArrayDeque<>();
	private final PanelComponent panel = new PanelComponent();

	@Inject
	public FriendsChatOverlay(Client client, FindRunnersConfig config)
	{
		this.client = client;
		this.config = config;
		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	public void addLine(String name, String message, int width)
	{
		lines.addLast(new ChatLine(name, message));
		while (lines.size() > MAX_LINES)
		{
			lines.removeFirst();
		}
		rebuild(width);
	}

	public void rebuild(int width)
	{
		Player self = client.getLocalPlayer();
		if (self == null || self.getName() == null)
		{
			return;
		}
		panel.setPreferredSize(new Dimension(Math.max(width, 120), 0));
		panel.getChildren().clear();

		List<ChatLine> shown = new ArrayList<>();
		ChatLine[] stored = lines.toArray(new ChatLine[0]);
		for (int i = stored.length - 1; i >= 0 && shown.size() < config.ccLines(); i--)
		{
			ChatLine line = stored[i];
			if (config.clientFilterEnabled() && !config.clientName().isEmpty())
			{
				boolean clientMatch = config.clientName().equalsIgnoreCase(line.name);
				boolean selfMatch = line.name.equalsIgnoreCase(self.getName());
				if (!clientMatch && !selfMatch)
				{
					continue;
				}
			}
			shown.add(line);
		}
		for (int i = shown.size() - 1; i >= 0; i--)
		{
			ChatLine line = shown.get(i);
			panel.getChildren().add(LineComponent.builder()
				.left(line.name + ": " + line.message)
				.leftColor(colorFor(line.name, self.getName()))
				.build());
		}
	}

	private Color colorFor(String name, String selfName)
	{
		Color color = config.ccColor();
		FriendsChatManager manager = client.getFriendsChatManager();
		if (manager != null)
		{
			FriendsChatMember member = manager.findByName(name);
			if (member != null && member.getRank() != null && member.getRank() != FriendsChatRank.UNRANKED)
			{
				color = member.getRank() == FriendsChatRank.FRIEND ? config.ccHostColor() : config.ccRankColor();
			}
		}
		if (name.equalsIgnoreCase(selfName))
		{
			color = config.ccSelfColor();
		}
		if (!config.clientName().isEmpty() && name.equalsIgnoreCase(config.clientName()))
		{
			color = config.ccClientColor();
		}
		return color;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.splitCcEnabled() || panel.getChildren().isEmpty())
		{
			return null;
		}
		return panel.render(graphics);
	}

	private static final class ChatLine
	{
		private final String name;
		private final String message;

		private ChatLine(String name, String message)
		{
			this.name = name;
			this.message = message;
		}
	}
}
