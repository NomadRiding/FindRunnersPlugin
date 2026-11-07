package com.foxtrot.findrunners;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.util.Text;

public class RunManagerPanel extends JPanel
{
	public final JTextField clientName = new JTextField();
	public final JComboBox<RunType> runType = new JComboBox<>();
	public final JTextField bonesToRun = new JTextField();
	public final JButton runButton = new JButton("Start run");

	private final FindRunnersPlugin plugin;
	private boolean hidden = true;

	public RunManagerPanel(FindRunnersPlugin plugin)
	{
		this.plugin = plugin;
		clientName.setHorizontalAlignment(JTextField.CENTER);
		bonesToRun.setHorizontalAlignment(JTextField.CENTER);

		runType.addItem(RunType.none);
		runType.addItem(RunType.afk);
		runType.addItem(RunType.tick);

		runButton.addActionListener(e -> onRunClicked());

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;

		JLabel header = new JLabel("Run Manager", SwingConstants.CENTER);
		header.setFont(new Font("Serif", Font.BOLD, 16));
		header.setBorder(new EmptyBorder(10, 10, 10, 10));
		form.add(header, c);

		addRow(form, c, "Client", clientName);
		addRow(form, c, "Run type", runType);
		addRow(form, c, "Bones per run", bonesToRun);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		c.insets = new Insets(12, 0, 0, 0);
		form.add(runButton, c);

		form.setVisible(false);

		JButton spoiler = new JButton("Show run manager");
		spoiler.addActionListener(e ->
		{
			hidden = !hidden;
			form.setVisible(!hidden);
			spoiler.setText(hidden ? "Show run manager" : "Hide run manager");
		});

		JPanel top = new JPanel(new BorderLayout());
		top.add(spoiler, BorderLayout.CENTER);

		setLayout(new BorderLayout());
		add(top, BorderLayout.PAGE_START);
		add(new JSeparator(), BorderLayout.CENTER);
		add(form, BorderLayout.PAGE_END);
		refreshButton();
	}

	public void refreshButton()
	{
		RunnerStats.RunnerData current = plugin.getStats().currentRun;
		if (current == null)
		{
			runButton.setText("Start run");
			return;
		}
		runButton.setText("End run");
		clientName.setText(current.client);
	}

	private void onRunClicked()
	{
		RunnerStats stats = plugin.getStats();
		if (runButton.getText().equalsIgnoreCase("start run"))
		{
			if (!plugin.isReady())
			{
				return;
			}
			String name = clientName.getText().trim();
			RunType type = (RunType) runType.getSelectedItem();
			if (name.isEmpty() || type == null || type == RunType.none)
			{
				return;
			}
			int price;
			try
			{
				price = plugin.pricePerInventory(type);
			}
			catch (NumberFormatException ex)
			{
				return;
			}
			if (price <= 0)
			{
				return;
			}
			stats.startRun(Text.standardize(name), price, type);
			plugin.persistStats();
			refreshButton();
			return;
		}

		stats.endRun();
		plugin.persistStats();
		refreshButton();
	}

	private void addRow(JPanel form, GridBagConstraints c, String label, java.awt.Component field)
	{
		c.gridy++;
		c.gridwidth = 1;
		c.gridx = 0;
		form.add(new JLabel(label), c);
		c.gridx = 1;
		form.add(field, c);
	}
}
