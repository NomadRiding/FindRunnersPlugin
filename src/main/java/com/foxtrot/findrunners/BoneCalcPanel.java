package com.foxtrot.findrunners;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class BoneCalcPanel extends JPanel
{
	private final JTextField boneQty = new JTextField("0");
	private final JTextField price = new JTextField("0");
	private final JTextField totalCost = new JTextField("0");
	private boolean hidden = true;

	public BoneCalcPanel()
	{
		totalCost.setEditable(false);

		JButton reset = new JButton("Reset");
		reset.addActionListener(e ->
		{
			boneQty.setText("0");
			price.setText("0");
			totalCost.setText("0");
		});

		JButton calc = new JButton("Calculate");
		calc.addActionListener(e -> calculate());

		JPanel calcPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;

		JLabel header = new JLabel("Bone Calculator", SwingConstants.CENTER);
		header.setFont(new Font("Serif", Font.BOLD, 16));
		header.setBorder(new EmptyBorder(10, 10, 10, 10));
		calcPanel.add(header, c);

		c.gridy++;
		c.gridwidth = 1;
		calcPanel.add(new JLabel("Bone qty"), c);
		c.gridx = 1;
		calcPanel.add(new JLabel("Price per inv"), c);

		c.gridx = 0;
		c.gridy++;
		calcPanel.add(boneQty, c);
		c.gridx = 1;
		calcPanel.add(price, c);

		c.gridx = 0;
		c.gridy++;
		calcPanel.add(reset, c);
		c.gridx = 1;
		calcPanel.add(calc, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		calcPanel.add(new JLabel("Price for run"), c);
		c.gridy++;
		calcPanel.add(totalCost, c);
		calcPanel.setVisible(false);

		JButton spoiler = new JButton("Show bone calculator");
		spoiler.addActionListener(e ->
		{
			hidden = !hidden;
			calcPanel.setVisible(!hidden);
			spoiler.setText(hidden ? "Show bone calculator" : "Hide bone calculator");
		});

		setLayout(new BorderLayout());
		add(spoiler, BorderLayout.PAGE_START);
		add(new JSeparator(), BorderLayout.CENTER);
		add(calcPanel, BorderLayout.PAGE_END);
	}

	private void calculate()
	{
		try
		{
			int bones = FindRunnersPlugin.parseAmount(boneQty.getText());
			int each = FindRunnersPlugin.parseAmount(price.getText());
			boneQty.setText(Integer.toString(bones));
			price.setText(Integer.toString(each));
			long total = (long) Math.floor(bones / 26.0) * each;
			totalCost.setText(FindRunnersPlugin.formatGp(total));
		}
		catch (NumberFormatException ex)
		{
			totalCost.setText("Invalid");
		}
	}
}
