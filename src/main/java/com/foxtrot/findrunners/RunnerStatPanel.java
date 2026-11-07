package com.foxtrot.findrunners;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public class RunnerStatPanel extends JPanel
{
	private final FindRunnersPlugin plugin;
	private final JPanel currentRun = new JPanel(new GridBagLayout());
	private final JPanel lastRun = new JPanel(new GridBagLayout());
	private final JPanel runnerStats = new JPanel(new GridBagLayout());
	private final JLabel curClient = new JLabel();
	private final JPanel curItems = new JPanel(new GridBagLayout());
	private final JLabel curPrice = new JLabel();
	private final JLabel curTotal = new JLabel();
	private final JLabel curDuration = new JLabel();
	private final JLabel lastClient = new JLabel();
	private final JPanel lastItems = new JPanel(new GridBagLayout());
	private final JLabel lastPrice = new JLabel();
	private final JLabel lastTotal = new JLabel();
	private final JLabel lastDuration = new JLabel();
	private final JLabel totalBones = new JLabel("", SwingConstants.RIGHT);
	private final JPanel boneBreakdown = new JPanel(new GridBagLayout());
	private final JLabel totalIncome = new JLabel("", SwingConstants.RIGHT);
	private final JLabel averageTick = new JLabel("", SwingConstants.RIGHT);
	private final JLabel averageAfk = new JLabel("", SwingConstants.RIGHT);
	private final JButton currentToggle = new JButton("Show current run");
	private final JButton lastToggle = new JButton("Show latest run");
	private final JButton statsToggle = new JButton("Hide runner stats");
	private boolean currentHidden = true;
	private boolean lastHidden = true;
	private boolean statsHidden;

	public RunnerStatPanel(FindRunnersPlugin plugin)
	{
		this.plugin = plugin;
		Border border = BorderFactory.createCompoundBorder(
			BorderFactory.createEmptyBorder(10, 0, 10, 0),
			new EmptyBorder(10, 5, 10, 5));
		curItems.setBorder(border);
		lastItems.setBorder(border);
		boneBreakdown.setBorder(border);

		buildRunPanel(currentRun, "Current run", curClient, curItems, curPrice, curTotal, curDuration);
		buildRunPanel(lastRun, "Latest run", lastClient, lastItems, lastPrice, lastTotal, lastDuration);
		buildTotals();

		currentRun.setVisible(false);
		lastRun.setVisible(false);
		currentToggle.addActionListener(e -> toggle(currentRun, currentToggle, true));
		lastToggle.addActionListener(e -> toggle(lastRun, lastToggle, false));
		statsToggle.addActionListener(e ->
		{
			statsHidden = !statsHidden;
			runnerStats.setVisible(!statsHidden);
			statsToggle.setText(statsHidden ? "Show runner stats" : "Hide runner stats");
		});

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 0;
		add(currentToggle, c);
		c.gridy++;
		add(currentRun, c);
		c.gridy++;
		add(lastToggle, c);
		c.gridy++;
		add(lastRun, c);
		c.gridy++;
		add(statsToggle, c);
		c.gridy++;
		add(runnerStats, c);
		refresh();
	}

	public void refresh()
	{
		RunnerStats stats = plugin.getStats();
		fillCurrent(stats);
		fillLast(stats);
		fillTotals(stats);
		revalidate();
		repaint();
	}

	private void fillCurrent(RunnerStats stats)
	{
		RunnerStats.RunnerData current = stats.currentRun;
		if (current == null)
		{
			currentRun.setVisible(false);
			currentHidden = true;
			currentToggle.setText("Show current run");
			currentToggle.setEnabled(false);
			return;
		}
		currentToggle.setEnabled(true);
		curClient.setText(current.client);
		curPrice.setText(FindRunnersPlugin.formatGp(current.price));
		curTotal.setText(FindRunnersPlugin.formatGp(current.totalPrice));
		long seconds = 0;
		try
		{
			seconds = LocalDateTime.parse(current.startTime).until(LocalDateTime.now(), ChronoUnit.SECONDS);
		}
		catch (Exception ignored)
		{
		}
		curDuration.setText(FindRunnersPlugin.formatTime(seconds));
		fillItems(curItems, current.items);
	}

	private void fillLast(RunnerStats stats)
	{
		RunnerStats.RunnerData last = stats.getLastRun();
		if (last == null)
		{
			lastRun.setVisible(false);
			lastHidden = true;
			lastToggle.setText("Show latest run");
			lastToggle.setEnabled(false);
			return;
		}
		lastToggle.setEnabled(true);
		lastClient.setText(last.client);
		lastPrice.setText(FindRunnersPlugin.formatGp(last.price));
		lastTotal.setText(FindRunnersPlugin.formatGp(last.totalPrice));
		lastDuration.setText(FindRunnersPlugin.formatTime(last.totalTime));
		fillItems(lastItems, last.items);
	}

	private void fillTotals(RunnerStats stats)
	{
		Map<String, Integer> bones = new HashMap<>();
		double tickSeconds = 0;
		double afkSeconds = 0;
		int tickRuns = 0;
		int afkRuns = 0;
		for (RunnerStats.RunnerData data : stats.runnerHistory.values())
		{
			int qty = data.boneCount();
			for (Map.Entry<String, Integer> entry : data.items.entrySet())
			{
				bones.put(entry.getKey(), bones.getOrDefault(entry.getKey(), 0) + entry.getValue());
			}
			if (qty <= 0)
			{
				continue;
			}
			double average = (double) data.totalTime / qty;
			if (data.type == RunType.tick)
			{
				tickSeconds += average;
				tickRuns++;
			}
			else if (data.type == RunType.afk)
			{
				afkSeconds += average;
				afkRuns++;
			}
		}
		fillItems(boneBreakdown, bones);
		totalBones.setText(FindRunnersPlugin.formatGp(stats.getTotalBones()));
		totalIncome.setText(FindRunnersPlugin.formatGp(stats.getTotalMade()));
		DecimalFormat format = new DecimalFormat("#.##");
		averageTick.setText(tickRuns == 0 ? "-" : format.format(tickSeconds / tickRuns) + " s/bone");
		averageAfk.setText(afkRuns == 0 ? "-" : format.format(afkSeconds / afkRuns) + " s/bone");
	}

	private void toggle(JPanel panel, JButton button, boolean current)
	{
		boolean hidden = current ? currentHidden : lastHidden;
		hidden = !hidden;
		panel.setVisible(!hidden);
		button.setText((hidden ? "Show " : "Hide ") + (current ? "current run" : "latest run"));
		if (current)
		{
			currentHidden = hidden;
		}
		else
		{
			lastHidden = hidden;
		}
	}

	private static void buildRunPanel(JPanel panel, String title, JLabel client, JPanel items, JLabel price, JLabel total, JLabel duration)
	{
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		addGrid(panel, header(title), 0, 0, 2);
		addGrid(panel, new JLabel("Client"), 0, 1, 1);
		addGrid(panel, client, 1, 1, 1);
		addGrid(panel, new JLabel("Bones"), 0, 2, 2);
		addGrid(panel, items, 0, 3, 2);
		addGrid(panel, new JLabel("Price per inv"), 0, 4, 1);
		addGrid(panel, price, 1, 4, 1);
		addGrid(panel, new JLabel("Total income"), 0, 5, 1);
		addGrid(panel, total, 1, 5, 1);
		addGrid(panel, duration, 0, 6, 2);
		addGrid(panel, new JSeparator(), 0, 7, 2);
	}

	private void buildTotals()
	{
		runnerStats.setBorder(new EmptyBorder(10, 10, 10, 10));
		addGrid(runnerStats, header("Runner stats"), 0, 0, 2);
		addGrid(runnerStats, new JLabel("Bones ran"), 0, 1, 1);
		addGrid(runnerStats, totalBones, 1, 1, 1);
		addGrid(runnerStats, boneBreakdown, 0, 2, 2);
		addGrid(runnerStats, new JLabel("Total income"), 0, 3, 1);
		addGrid(runnerStats, totalIncome, 1, 3, 1);
		addGrid(runnerStats, new JLabel("Average tick"), 0, 4, 1);
		addGrid(runnerStats, averageTick, 1, 4, 1);
		addGrid(runnerStats, new JLabel("Average AFK"), 0, 5, 1);
		addGrid(runnerStats, averageAfk, 1, 5, 1);
	}

	private static JLabel header(String text)
	{
		JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setFont(new Font("Serif", Font.BOLD, 16));
		label.setBorder(new EmptyBorder(10, 10, 10, 10));
		return label;
	}

	private static void fillItems(JPanel panel, Map<String, Integer> items)
	{
		panel.removeAll();
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 0;
		for (Map.Entry<String, Integer> entry : items.entrySet())
		{
			c.gridx = 0;
			panel.add(new JLabel(entry.getKey().replace(" bones", "")), c);
			c.gridx = 1;
			JLabel qty = new JLabel(FindRunnersPlugin.formatGp(entry.getValue()), SwingConstants.RIGHT);
			panel.add(qty, c);
			c.gridy++;
		}
	}

	private static void addGrid(JPanel panel, java.awt.Component component, int x, int y, int width)
	{
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = width;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(component, c);
	}
}
