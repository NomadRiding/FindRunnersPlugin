package com.foxtrot.findrunners;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;

public class BoneCalcPanel extends JPanel {
	private static final int INVENTORY_SIZE = 26;

	private final FindRunnersPlugin plugin;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final JTextField afkPrice = new JTextField();
	private final JTextField tickPrice = new JTextField();
	private final JTextField insurance = new JTextField();
	private final JComboBox<RunType> runType = new JComboBox<>(new RunType[] { RunType.afk, RunType.tick });
	private final JComboBox<BoneChoice> boneChoice = new JComboBox<>(BoneChoice.values());
	private final JLabel requestedLabel = new JLabel("Requested bone");
	private final JTextField requestedBone = new JTextField();
	private final JTextField pricePerBone = new JTextField("-");
	private final JTextField boneQty = new JTextField("0");
	private final JTextField pricePerRun = new JTextField("0");
	private final JTextField inventoryRuns = new JTextField("0");
	private final JTextField bonesValue = new JTextField("-");
	private final JPanel splitPanel = new JPanel();
	private long gePricePerBone;
	private boolean hidden = true;

	public BoneCalcPanel(FindRunnersPlugin plugin) {
		this.plugin = plugin;
		this.itemManager = plugin.getItemManager();
		this.clientThread = plugin.getClientThread();
		pricePerBone.setEditable(false);
		pricePerRun.setEditable(false);
		inventoryRuns.setEditable(false);
		bonesValue.setEditable(false);
		bonesValue.setToolTipText("Price per bone: -");
		requestedBone.setToolTipText("Type the full item name, such as Lava dragon bones");
		insurance.setToolTipText("Example: 20m");
		afkPrice.setToolTipText("Price per inventory for an AFK run. Example: 25k");
		tickPrice.setToolTipText("Price per inventory for a tick run. Example: 15k");
		runType.setToolTipText("Uses the AFK or tick price set above");
		insurance.setText(plugin.insuranceText());
		afkPrice.setText(plugin.priceText(RunType.afk));
		tickPrice.setText(plugin.priceText(RunType.tick));
		insurance.addActionListener(e -> commitInsurance());
		insurance.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				commitInsurance();
			}
		});
		afkPrice.addActionListener(e -> commitRate(afkPrice, RunType.afk, FindRunnersConfig.AFK_PRICE_KEY));
		tickPrice.addActionListener(e -> commitRate(tickPrice, RunType.tick, FindRunnersConfig.TICK_PRICE_KEY));
		afkPrice.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				commitRate(afkPrice, RunType.afk, FindRunnersConfig.AFK_PRICE_KEY);
			}
		});
		tickPrice.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				commitRate(tickPrice, RunType.tick, FindRunnersConfig.TICK_PRICE_KEY);
			}
		});

		JButton reset = new JButton("Reset");
		reset.addActionListener(e -> reset());
		JButton calc = new JButton("Calculate");
		calc.addActionListener(e -> lookupGe());
		boneQty.addActionListener(e -> lookupGe());
		boneQty.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if ("0".equals(boneQty.getText().trim())) {
					boneQty.setText("");
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (boneQty.getText().trim().isEmpty()) {
					boneQty.setText("0");
				}
			}
		});
		runType.addActionListener(e -> refreshTotals());
		requestedBone.addActionListener(e -> lookupGe());

		boneChoice.addActionListener(e -> {
			boolean other = boneChoice.getSelectedItem() == BoneChoice.OTHER;
			requestedLabel.setVisible(other);
			requestedBone.setVisible(other);
			if (other) {
				gePricePerBone = 0;
				setPricePerBoneText("-");
				refreshTotals();
			} else {
				lookupGe();
			}
			revalidate();
			repaint();
			if (other) {
				SwingUtilities.invokeLater(requestedBone::requestFocusInWindow);
			}
		});

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

		addRow(calcPanel, c, "Run type", runType);
		addRow(calcPanel, c, "Bone", boneChoice);
		addRow(calcPanel, c, requestedLabel, requestedBone);
		requestedLabel.setVisible(false);
		requestedBone.setVisible(false);
		addRow(calcPanel, c, "Bone qty", boneQty);
		// addRow(calcPanel, c, "Price per bone", pricePerBone);
		addRow(calcPanel, c, "Total Run Cost", pricePerRun);
		addRow(calcPanel, c, "Inventory runs", inventoryRuns);
		// addRow(calcPanel, c, "Bones val", bonesValue);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		JLabel splitHeader = new JLabel("Split");
		splitHeader.setFont(new Font("Serif", Font.BOLD, 14));
		splitHeader.setBorder(new EmptyBorder(8, 0, 4, 0));
		calcPanel.add(splitHeader, c);
		c.gridy++;
		splitPanel.setLayout(new BoxLayout(splitPanel, BoxLayout.Y_AXIS));
		splitPanel.setOpaque(false);
		splitPanel.setBorder(new EmptyBorder(0, 0, 16, 0));
		calcPanel.add(splitPanel, c);

		c.gridy++;
		c.gridwidth = 1;
		c.gridx = 0;
		calcPanel.add(reset, c);
		c.gridx = 1;
		calcPanel.add(calc, c);
		calcPanel.setVisible(false);

		JButton spoiler = new JButton("Show bone calculator");
		spoiler.addActionListener(e -> {
			hidden = !hidden;
			calcPanel.setVisible(!hidden);
			spoiler.setText(hidden ? "Show bone calculator" : "Hide bone calculator");
		});

		JPanel rates = new JPanel(new GridBagLayout());
		rates.setOpaque(false);
		rates.setBorder(new EmptyBorder(0, 0, 12, 0));
		GridBagConstraints ratesConstraints = new GridBagConstraints();
		ratesConstraints.fill = GridBagConstraints.HORIZONTAL;
		ratesConstraints.weightx = 1;
		ratesConstraints.gridx = 0;
		ratesConstraints.gridy = -1;
		ratesConstraints.gridwidth = 2;
		addRow(rates, ratesConstraints, "Insurance", insurance);
		addRow(rates, ratesConstraints, "Tick price", tickPrice);
		addRow(rates, ratesConstraints, "AFK price", afkPrice);

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(rates, BorderLayout.NORTH);
		top.add(spoiler, BorderLayout.SOUTH);

		setLayout(new BorderLayout());
		add(top, BorderLayout.PAGE_START);
		add(new JSeparator(), BorderLayout.CENTER);
		add(calcPanel, BorderLayout.PAGE_END);
		lookupGe();
	}

	static int countInventoryRuns(int bones) {
		if (bones <= 0) {
			return 0;
		}
		return (bones + INVENTORY_SIZE - 1) / INVENTORY_SIZE;
	}

	private void reset() {
		boneQty.setText("0");
		pricePerRun.setText("0");
		inventoryRuns.setText("0");
		bonesValue.setText("-");
		splitPanel.removeAll();
		lookupGe();
	}

	private void lookupGe() {
		BoneChoice choice = (BoneChoice) boneChoice.getSelectedItem();
		if (choice == null) {
			return;
		}
		String typed = requestedBone.getText();
		clientThread.invokeLater(() -> {
			long eachBone = choice == BoneChoice.OTHER
					? findTypedBonePrice(typed)
					: itemManager.getItemPrice(choice.itemId);
			SwingUtilities.invokeLater(() -> showGePrice(choice, typed, eachBone));
		});
	}

	private void showGePrice(BoneChoice choice, String typed, long eachBone) {
		if (boneChoice.getSelectedItem() != choice) {
			return;
		}
		if (choice == BoneChoice.OTHER && !requestedBone.getText().equals(typed)) {
			return;
		}
		gePricePerBone = Math.max(0, eachBone);
		if (gePricePerBone <= 0) {
			setPricePerBoneText(choice == BoneChoice.OTHER ? "Not found" : "No price");
		} else {
			setPricePerBoneText(String.format("%,d", gePricePerBone));
		}
		refreshTotals();
	}

	private void refreshTotals() {
		int qty;
		int perInv;
		long cover;
		try {
			qty = FindRunnersPlugin.parseAmount(boneQty.getText());
			perInv = plugin.pricePerInventory((RunType) runType.getSelectedItem());
			cover = insurance.getText().trim().isEmpty() ? 0 : FindRunnersPlugin.parseAmount(insurance.getText());
		} catch (NumberFormatException ex) {
			pricePerRun.setText("Invalid");
			inventoryRuns.setText("Invalid");
			bonesValue.setText("Invalid");
			showSplitMessage("Use numbers such as 8000 or 10m.");
			return;
		}

		int runs = countInventoryRuns(qty);
		inventoryRuns.setText(Integer.toString(runs));
		pricePerRun.setText(FindRunnersPlugin.formatGp((long) perInv * runs));
		bonesValue.setText(gePricePerBone <= 0 || qty <= 0
				? "-"
				: FindRunnersPlugin.formatGp(gePricePerBone * qty));
		rebuildSplit(qty, perInv, cover, gePricePerBone);
	}

	private void rebuildSplit(int qty, int priceEachInv, long cover, long gePrice) {
		splitPanel.removeAll();
		if (qty <= 0) {
			showSplitMessage("Set a bone quantity.");
			return;
		}
		if (gePrice <= 0) {
			showSplitMessage("GE price is not loaded yet.");
			return;
		}
		if (cover <= 0) {
			showSplitMessage("Set an insurance amount.");
			return;
		}
		int maxBones = maxBonesPerTrade(cover, gePrice, priceEachInv);
		if (maxBones <= 0) {
			showSplitMessage("One inventory costs more than the insurance.");
			return;
		}

		int remaining = qty;
		int trade = 1;
		int groupStart = 0;
		int groupTake = 0;
		long groupPayment = 0;
		boolean firstGroup = true;
		while (remaining > 0 && trade <= 30) {
			int take = Math.min(remaining, maxBones);
			int runs = countInventoryRuns(take);
			long payment = (long) runs * Math.max(priceEachInv, 0);
			if (groupStart == 0) {
				groupStart = trade;
				groupTake = take;
				groupPayment = payment;
			} else if (take != groupTake || payment != groupPayment) {
				addTradeGroup(groupStart, trade - 1, groupTake, groupPayment, gePrice, firstGroup);
				firstGroup = false;
				groupStart = trade;
				groupTake = take;
				groupPayment = payment;
			}
			remaining -= take;
			trade++;
		}
		if (groupStart > 0) {
			addTradeGroup(groupStart, trade - 1, groupTake, groupPayment, gePrice, firstGroup);
		}
		splitPanel.revalidate();
		splitPanel.repaint();
	}

	private static int maxBonesPerTrade(long cover, long gePrice, int priceEachInv) {
		long charge = Math.max(priceEachInv, 0);
		long fullInventory = (long) INVENTORY_SIZE * gePrice + charge;
		if (fullInventory <= cover) {
			long inventories = cover / fullInventory;
			long bones = inventories * INVENTORY_SIZE;
			return (int) Math.min(Integer.MAX_VALUE, bones);
		}
		long roomForBones = cover - charge;
		if (roomForBones < gePrice) {
			return 0;
		}
		return (int) Math.min(INVENTORY_SIZE - 1L, roomForBones / gePrice);
	}

	private void addTradeGroup(int start, int end, int take, long payment, long gePrice, boolean first) {
		String title = start == end ? "Trade " + start : "Trade " + start + " - " + end;
		JLabel heading = new JLabel(title);
		heading.setFont(heading.getFont().deriveFont(Font.BOLD));
		heading.setBorder(new EmptyBorder(first ? 4 : 12, 0, 2, 0));
		splitPanel.add(heading);
		long boneValue = (long) take * gePrice;
		JLabel qtyLabel = new JLabel("Bone qty: " + String.format("%,d", take));
		qtyLabel.setToolTipText(
				String.format("Bone value: %,d + payment %,d = %,d", boneValue, payment, boneValue + payment));
		splitPanel.add(qtyLabel);
		splitPanel.add(new JLabel("Payment: " + formatPayment(payment)));
	}

	private static String formatPayment(long gp) {
		long absolute = Math.abs(gp);
		if (absolute >= 1_000_000L) {
			return String.format("%.2f", gp / 1_000_000d).replace('.', ',') + " M";
		}
		if (absolute >= 1_000L) {
			return String.format("%.2f", gp / 1_000d).replace('.', ',') + " K";
		}
		return String.format("%,d", gp);
	}

	private void showSplitMessage(String message) {
		splitPanel.removeAll();
		splitPanel.add(new JLabel(message));
		splitPanel.revalidate();
		splitPanel.repaint();
	}

	private long findTypedBonePrice(String typedName) {
		String query = typedName == null ? "" : typedName.trim();
		if (query.isEmpty() || itemManager == null) {
			return 0;
		}
		List<ItemPrice> results = itemManager.search(query);
		if (results == null || results.isEmpty()) {
			return 0;
		}
		for (ItemPrice result : results) {
			if (result.getName() != null && result.getName().equalsIgnoreCase(query)) {
				return itemManager.getItemPrice(result.getId());
			}
		}
		if (results.size() == 1 && results.get(0).getId() > 0) {
			return itemManager.getItemPrice(results.get(0).getId());
		}
		return 0;
	}

	public void reloadRates() {
		String cover = plugin.insuranceText();
		String afk = plugin.priceText(RunType.afk);
		String tick = plugin.priceText(RunType.tick);
		if (!insurance.getText().trim().equals(cover)) {
			insurance.setText(cover);
		}
		if (!afkPrice.getText().trim().equals(afk)) {
			afkPrice.setText(afk);
		}
		if (!tickPrice.getText().trim().equals(tick)) {
			tickPrice.setText(tick);
		}
		refreshTotals();
	}

	private void commitInsurance() {
		String text = insurance.getText().trim();
		if (!text.isEmpty()) {
			try {
				if (FindRunnersPlugin.parseAmount(text) < 0) {
					insurance.setText(plugin.insuranceText());
					return;
				}
			} catch (NumberFormatException ex) {
				insurance.setText(plugin.insuranceText());
				return;
			}
		}
		if (!text.equals(plugin.insuranceText())) {
			plugin.saveRunPrice(FindRunnersConfig.INSURANCE_KEY, text);
		}
		refreshTotals();
	}

	private void commitRate(JTextField field, RunType type, String key) {
		String text = field.getText().trim();
		try {
			int amount = text.isEmpty() ? -1 : FindRunnersPlugin.parseAmount(text);
			if (amount < 0) {
				field.setText(plugin.priceText(type));
				return;
			}
			if (!text.equals(plugin.priceText(type))) {
				plugin.saveRunPrice(key, text);
			}
			refreshTotals();
		} catch (NumberFormatException ex) {
			field.setText(plugin.priceText(type));
		}
	}

	private void setPricePerBoneText(String text) {
		pricePerBone.setText(text);
		bonesValue.setToolTipText("Price per bone: " + text);
	}

	private static void addRow(JPanel panel, GridBagConstraints c, String label, java.awt.Component field) {
		JLabel name = new JLabel(label);
		name.setToolTipText(label);
		addRow(panel, c, name, field);
	}

	private static void addRow(JPanel panel, GridBagConstraints c, java.awt.Component label, java.awt.Component field) {
		if (label instanceof JLabel) {
			JLabel name = (JLabel) label;
			if (name.getToolTipText() == null && name.getText() != null && !name.getText().isEmpty()) {
				name.setToolTipText(name.getText());
			}
		}
		c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(3, 0, 3, 12);
		if (label instanceof JLabel) {
			JLabel name = (JLabel) label;
			name.setMinimumSize(name.getPreferredSize());
		}
		panel.add(label, c);

		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(3, 0, 3, 0);
		panel.add(field, c);

		c.insets = new Insets(0, 0, 0, 0);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
	}

	private enum BoneChoice {
		DRAGON("Dragon bones", ItemID.DRAGON_BONES),
		SUPERIOR("Superior dragon bones", ItemID.DRAGON_BONES_SUPERIOR),
		OTHER("Other", -1);

		private final String label;
		private final int itemId;

		BoneChoice(String label, int itemId) {
			this.label = label;
			this.itemId = itemId;
		}

		@Override
		public String toString() {
			return label;
		}
	}
}
