package com.foxtrot.findrunners;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

public class FindRunnersPanel extends PluginPanel
{
	public final BoneCalcPanel calculator;
	public final RunManagerPanel runManager;
	public final RunnerStatPanel statPanel;

	public FindRunnersPanel(FindRunnersPlugin plugin)
	{
		super(false);
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setLayout(new BorderLayout());

		JPanel column = new JPanel();
		column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));
		column.setOpaque(false);

		calculator = new BoneCalcPanel(plugin);
		runManager = new RunManagerPanel(plugin);
		statPanel = new RunnerStatPanel(plugin);
		calculator.setBorder(new EmptyBorder(8, 8, 8, 8));
		runManager.setBorder(new EmptyBorder(8, 8, 8, 8));
		statPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

		column.add(calculator);
		column.add(runManager);
		column.add(statPanel);
		add(column, BorderLayout.NORTH);
	}

	public void refresh()
	{
		runManager.refreshButton();
		statPanel.refresh();
	}

	public void reloadRates()
	{
		calculator.reloadRates();
	}
}
