package com.foxtrot.findrunners;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.util.Text;

public class RunManagerPanel extends JPanel
{
	public final JTextField clientName = new JTextField();
	public final JComboBox<RunType> runType = new JComboBox<>();
	public final JTextField pricePerInv = new JTextField();
	public final JTextField pricePerRun = new JTextField();
	public final JTextField bonesToRun = new JTextField();
	public final JButton runButton = new JButton("Start run");

	private final FindRunnersPlugin plugin;
	private boolean hidden;

	public RunManagerPanel(FindRunnersPlugin plugin)
	{
		this.plugin = plugin;
		clientName.setHorizontalAlignment(JTextField.CENTER);
		pricePerInv.setHorizontalAlignment(JTextField.CENTER);
		pricePerRun.setHorizontalAlignment(JTextField.CENTER);
		bonesToRun.setHorizontalAlignment(JTextField.CENTER);

		pricePerRun.getDocument().addDocumentListener(clearOther(pricePerInv));
		pricePerInv.getDocument().addDocumentListener(clearOther(pricePerRun));

		runType.addItem(RunType.none);
		runType.addItem(RunType.afk);
		runType.addItem(RunType.tick);
		runType.addActionListener(e -> applySuggestedPrice());

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
		addRow(form, c, "Cost per inv", pricePerInv);
		addRow(form, c, "Cost per run", pricePerRun);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		form.add(runButton, c);

		JButton spoiler = new JButton("Hide run manager");
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
		pricePerInv.setText(Integer.toString(current.price));
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
			try
			{
				int qty = FindRunnersPlugin.parseAmount(bonesToRun.getText());
				int price;
				if (pricePerInv.getText().trim().isEmpty())
				{
					double trips = Math.ceil(qty / 26.0);
					if (trips <= 0)
					{
						return;
					}
					price = (int) Math.round(FindRunnersPlugin.parseAmount(pricePerRun.getText()) / trips);
				}
				else
				{
					price = FindRunnersPlugin.parseAmount(pricePerInv.getText());
				}

				String name = clientName.getText().trim();
				RunType type = (RunType) runType.getSelectedItem();
				if (!name.isEmpty() && price > 0 && qty > 0 && type != null && type != RunType.none)
				{
					stats.startRun(Text.standardize(name), price, type);
					plugin.persistStats();
					refreshButton();
				}
			}
			catch (NumberFormatException ignored)
			{
			}
			return;
		}

		stats.endRun();
		plugin.persistStats();
		refreshButton();
	}

	private void applySuggestedPrice()
	{
		RunType type = Objects.requireNonNull((RunType) runType.getSelectedItem());
		if (type == RunType.afk)
		{
			pricePerInv.setText("25k");
			pricePerInv.setForeground(Color.GREEN);
		}
		else if (type == RunType.tick)
		{
			pricePerInv.setText("15k");
			pricePerInv.setForeground(Color.GREEN);
		}
		else
		{
			pricePerInv.setForeground(Color.WHITE);
		}
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

	private static DocumentListener clearOther(JTextField other)
	{
		return new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				if (!other.getText().isEmpty())
				{
					other.setText("");
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
			}
		};
	}
}
