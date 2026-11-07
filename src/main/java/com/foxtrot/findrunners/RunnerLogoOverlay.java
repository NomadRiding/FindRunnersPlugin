package com.foxtrot.findrunners;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class RunnerLogoOverlay extends Overlay
{
	private final PanelComponent panel = new PanelComponent();
	private boolean visible;

	public RunnerLogoOverlay()
	{
		setPriority(OverlayPriority.MED);
		setPosition(OverlayPosition.TOP_LEFT);
	}

	public void setLogo(BufferedImage logo, boolean show)
	{
		visible = show && logo != null;
		panel.getChildren().clear();
		if (visible)
		{
			panel.getChildren().add(new ImageComponent(logo));
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!visible)
		{
			return null;
		}
		return panel.render(graphics);
	}
}
