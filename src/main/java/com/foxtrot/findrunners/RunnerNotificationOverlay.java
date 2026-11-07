package com.foxtrot.findrunners;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class RunnerNotificationOverlay extends Overlay
{
	private final PanelComponent panel = new PanelComponent();
	private String text = "";
	private boolean lit;

	public RunnerNotificationOverlay()
	{
		setPriority(OverlayPriority.HIGHEST);
		setPosition(OverlayPosition.TOP_CENTER);
	}

	public void show(String message)
	{
		text = message;
		flash(true);
	}

	public void flash(boolean on)
	{
		lit = on;
		panel.getChildren().clear();
		panel.setPreferredSize(new Dimension(280, 48));
		panel.getChildren().add(TitleComponent.builder()
			.text(text)
			.color(lit ? Color.WHITE : Color.RED)
			.build());
	}

	public void clear()
	{
		text = "";
		panel.getChildren().clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (text.isEmpty())
		{
			return null;
		}
		return panel.render(graphics);
	}
}
