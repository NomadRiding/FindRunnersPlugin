package com.foxtrot.findrunners;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.FriendsChatRank;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.FriendsChatSync;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.Text;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.inject.Provider;

@Slf4j
@PluginDescriptor(
	name = "Find Runners",
	description = "Friends chat tools for finding runners and tracking bone runs",
	tags = {"runner", "bones", "friends-chat", "trade"},
	internalName = "find-runners"
)
public class FindRunnersPlugin extends Plugin
{
	static final String SET_CLIENT = "Set as Client";
	static final String REMOVE_CLIENT = "Remove as Client";
	private static final String TRADING_WITH = "Trading with: ";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private FindRunnersConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Provider<MenuManager> menuManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private Gson gson;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private FriendsChatOverlay chatOverlay;

	@Inject
	private RunnerNotificationOverlay notificationOverlay;

	@Inject
	private RunnerLogoOverlay logoOverlay;

	private NavigationButton navButton;
	private FindRunnersPanel panel;
	private RunnerStats stats = new RunnerStats();
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> notifyTask;
	private BufferedImage defaultLogo;
	private volatile boolean running;
	private volatile boolean statsReady;
	private boolean repeatingTrade;
	private boolean runner;
	private boolean host;

	@Override
	protected void startUp() throws Exception
	{
		running = true;
		statsReady = false;
		executor = Executors.newSingleThreadScheduledExecutor(runnable ->
		{
			Thread thread = new Thread(runnable, "find-runners");
			thread.setDaemon(true);
			return thread;
		});

		defaultLogo = createIcon(64);
		panel = new FindRunnersPanel(this);
		navButton = NavigationButton.builder()
			.tooltip("Find Runners")
			.icon(createIcon(16))
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		menuManager.get().addPlayerMenuItem(SET_CLIENT);
		menuManager.get().addPlayerMenuItem(REMOVE_CLIENT);
		overlayManager.add(chatOverlay);
		overlayManager.add(logoOverlay);
		applyLogo(defaultLogo);

		Filepath dataFile = getPluginDirectory().joinSegment("rundata.json");
		executor.execute(() ->
		{
			RunnerStats loaded = RunnerStats.load(gson, dataFile);
			if (!running)
			{
				return;
			}
			clientThread.invoke(() ->
			{
				stats = loaded;
				statsReady = true;
				SwingUtilities.invokeLater(panel::refresh);
			});
		});

		executor.scheduleAtFixedRate(() ->
		{
			if (!running || stats.currentRun == null || panel == null)
			{
				return;
			}
			SwingUtilities.invokeLater(panel.statPanel::refresh);
		}, 5, 5, TimeUnit.SECONDS);

		refreshRole();
	}

	@Override
	protected void shutDown()
	{
		running = false;
		statsReady = false;
		if (notifyTask != null)
		{
			notifyTask.cancel(false);
			notifyTask = null;
		}
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
		menuManager.get().removePlayerMenuItem(SET_CLIENT);
		menuManager.get().removePlayerMenuItem(REMOVE_CLIENT);
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
		overlayManager.remove(chatOverlay);
		overlayManager.remove(logoOverlay);
		overlayManager.remove(notificationOverlay);
		notificationOverlay.clear();
		runner = false;
		host = false;
	}

	ItemManager getItemManager()
	{
		return itemManager;
	}

	ClientThread getClientThread()
	{
		return clientThread;
	}

	public String priceText(RunType type)
	{
		String value = type == RunType.tick ? config.tickPrice() : config.afkPrice();
		if (value == null || value.trim().isEmpty())
		{
			return type == RunType.tick ? "15k" : "25k";
		}
		return value.trim();
	}

	public int pricePerInventory(RunType type)
	{
		return parseAmount(priceText(type));
	}

	public String insuranceText()
	{
		String value = config.insurance();
		return value == null ? "" : value.trim();
	}

	public void saveRunPrice(String key, String value)
	{
		configManager.setConfiguration("findrunners", key, value);
	}

	public RunnerStats getStats()
	{
		return stats;
	}

	public boolean isReady()
	{
		return statsReady;
	}

	public void persistStats()
	{
		if (executor == null || !statsReady)
		{
			return;
		}
		executor.execute(stats::save);
	}

	@Provides
	FindRunnersConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FindRunnersConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("findrunners"))
		{
			return;
		}
		refreshLogo();
		if (FindRunnersConfig.INSURANCE_KEY.equals(event.getKey())
			|| FindRunnersConfig.AFK_PRICE_KEY.equals(event.getKey())
			|| FindRunnersConfig.TICK_PRICE_KEY.equals(event.getKey()))
		{
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.reloadRates();
				}
			});
		}
		Widget chat = client.getWidget(InterfaceID.Chatbox.CHATDISPLAY);
		if (chat != null)
		{
			chatOverlay.rebuild(chat.getWidth());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			refreshRole();
		}
	}

	@Subscribe
	public void onFriendsChatSync(FriendsChatSync event)
	{
		refreshRole();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = event.getMenuOption();
		if (!SET_CLIENT.equals(option) && !REMOVE_CLIENT.equals(option))
		{
			return;
		}
		String target = playerName(event.getMenuTarget());
		if (SET_CLIENT.equals(option))
		{
			configManager.setConfiguration("findrunners", "clientName", target);
			SwingUtilities.invokeLater(() -> panel.runManager.clientName.setText(target));
		}
		else if (target.equalsIgnoreCase(config.clientName()))
		{
			configManager.setConfiguration("findrunners", "clientName", "");
			SwingUtilities.invokeLater(() -> panel.runManager.clientName.setText(""));
			if (stats.currentRun != null && stats.currentRun.client.equalsIgnoreCase(Text.standardize(target)))
			{
				stats.endRun();
				persistStats();
				SwingUtilities.invokeLater(panel::refresh);
			}
		}
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		if (!config.offerAllEnabled() || client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
		{
			return;
		}

		MenuEntry[] entries = client.getMenuEntries();
		for (MenuEntry entry : entries)
		{
			String option = Text.removeTags(entry.getOption()).toLowerCase();
			if (!option.equals("offer-all"))
			{
				continue;
			}
			String target = Text.removeTags(entry.getTarget()).toLowerCase();
			for (MenuEntry other : entries)
			{
				String otherOption = Text.removeTags(other.getOption()).toLowerCase();
				String otherTarget = Text.removeTags(other.getTarget()).toLowerCase();
				if (otherOption.equals("offer") && otherTarget.equals(target))
				{
					entry.setForceLeftClick(true);
					break;
				}
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage message)
	{
		if (message.getType() == ChatMessageType.FRIENDSCHAT && inConfiguredChat())
		{
			handleFriendsChat(message);
		}
		if (message.getMessage() != null && message.getMessage().equalsIgnoreCase("accepted trade."))
		{
			recordAcceptedTrade();
		}
		if (config.filterTradeEnabled() && message.getType() == ChatMessageType.TRADE)
		{
			removeTradeLine(message);
		}
		if (config.spamTrade() && message.getType() == ChatMessageType.TRADEREQ && !repeatingTrade)
		{
			repeatingTrade = true;
			try
			{
				for (int i = 0; i < 7; i++)
				{
					client.addChatMessage(message.getType(), message.getName(), message.getMessage(), message.getSender());
				}
			}
			finally
			{
				repeatingTrade = false;
			}
		}
	}

	private void handleFriendsChat(ChatMessage message)
	{
		String text = message.getMessage() == null ? "" : message.getMessage();
		Widget chat = client.getWidget(InterfaceID.Chatbox.CHATDISPLAY);
		int width = chat == null ? 300 : chat.getWidth();
		chatOverlay.addLine(Text.removeTags(message.getName()), Text.removeTags(text), width);

		String lower = text.toLowerCase();
		if (lower.startsWith("!bones "))
		{
			replyBones(message, lower.substring("!bones ".length()).trim());
		}
		if (text.trim().equalsIgnoreCase("@runner") && config.enablePing() && runner)
		{
			FriendsChatMember sender = member(message.getName());
			if (sender != null && sender.getRank() != null && sender.getRank() != FriendsChatRank.UNRANKED)
			{
				flash("Runner requested");
			}
		}
	}

	private void replyBones(ChatMessage message, String args)
	{
		String[] parts = args.split("\\s+");
		if (parts.length < 2)
		{
			reply(message, "Use !bones afk 1000 or !bones tick 1000");
			return;
		}
		RunType runType;
		if (parts[0].equals("afk"))
		{
			runType = RunType.afk;
		}
		else if (parts[0].equals("tick"))
		{
			runType = RunType.tick;
		}
		else
		{
			reply(message, "Unknown run type. Use afk or tick.");
			return;
		}
		int rate;
		try
		{
			rate = pricePerInventory(runType);
		}
		catch (NumberFormatException ex)
		{
			reply(message, "Set a " + parts[0] + " price above the bone calculator.");
			return;
		}
		if (rate <= 0)
		{
			reply(message, "Set a " + parts[0] + " price above the bone calculator.");
			return;
		}
		int qty;
		try
		{
			qty = parseAmount(parts[1]);
		}
		catch (NumberFormatException ex)
		{
			reply(message, "Bone amount must be a number.");
			return;
		}
		if (qty <= 0)
		{
			reply(message, "Bone amount must be greater than 0.");
			return;
		}
		int runs = BoneCalcPanel.countInventoryRuns(qty);
		long price = (long) runs * rate;
		reply(message, parts[0] + " running " + new DecimalFormat("#,###").format(qty) + " bones would be " + formatGp(price) + " gp");
	}

	private void recordAcceptedTrade()
	{
		if (!config.enableStats() || !statsReady || stats.currentRun == null)
		{
			return;
		}
		String expected = Text.standardize(config.clientName());
		String partner = tradingPartner();
		if (expected.isEmpty() || partner == null || !partner.equalsIgnoreCase(expected))
		{
			return;
		}
		ItemContainer offer = client.getItemContainer(InventoryID.TRADE);
		if (offer == null)
		{
			return;
		}
		for (Item item : offer.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			String name = itemManager.getItemComposition(item.getId()).getName();
			if (name != null && name.toLowerCase().contains("bones"))
			{
				stats.addTrip(partner, name, item.getQuantity());
			}
		}
		persistStats();
		SwingUtilities.invokeLater(panel::refresh);
	}

	private String tradingPartner()
	{
		Widget title = client.getWidget(InterfaceID.Trademain.TITLE);
		if (title == null || title.getText() == null || !title.getText().startsWith(TRADING_WITH))
		{
			return null;
		}
		return Text.standardize(Text.removeTags(title.getText().substring(TRADING_WITH.length())));
	}

	private void removeTradeLine(ChatMessage message)
	{
		ChatLineBuffer buffer = client.getChatLineMap().get(message.getType().getType());
		if (buffer != null && message.getMessageNode() != null)
		{
			buffer.removeMessageNode(message.getMessageNode());
		}
	}

	private void refreshRole()
	{
		runner = false;
		host = false;
		if (!inConfiguredChat())
		{
			logoOverlay.setLogo(defaultLogo, false);
			return;
		}
		Player local = client.getLocalPlayer();
		FriendsChatMember member = local == null ? null : member(local.getName());
		if (member != null && member.getRank() != null && member.getRank() != FriendsChatRank.UNRANKED)
		{
			if (member.getRank() == FriendsChatRank.FRIEND)
			{
				host = true;
			}
			else
			{
				runner = true;
			}
		}
		logoOverlay.setLogo(defaultLogo, runner || host);
		refreshLogo();
	}

	private void refreshLogo()
	{
		String url = config.logoUrl() == null ? "" : config.logoUrl().trim();
		if (!url.startsWith("http://") && !url.startsWith("https://"))
		{
			applyLogo(defaultLogo);
			return;
		}
		Request request = new Request.Builder().url(url).build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Logo download failed", e);
				clientThread.invoke(() -> applyLogo(defaultLogo));
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					BufferedImage image = null;
					if (response.isSuccessful() && body != null)
					{
						image = javax.imageio.ImageIO.read(body.byteStream());
					}
					BufferedImage chosen = image == null ? defaultLogo : image;
					clientThread.invoke(() -> applyLogo(chosen));
				}
			}
		});
	}

	private void applyLogo(BufferedImage image)
	{
		if (!running)
		{
			return;
		}
		BufferedImage scaled = scale(image, config.logoScale());
		logoOverlay.setLogo(scaled, runner || host);
	}

	private void flash(String text)
	{
		if (notifyTask != null)
		{
			notifyTask.cancel(false);
		}
		notificationOverlay.show(text);
		overlayManager.add(notificationOverlay);
		final int[] remaining = {30};
		notifyTask = executor.scheduleAtFixedRate(() ->
		{
			remaining[0]--;
			clientThread.invoke(() ->
			{
				if (!running || remaining[0] <= 0)
				{
					if (notifyTask != null)
					{
						notifyTask.cancel(false);
					}
					overlayManager.remove(notificationOverlay);
					notificationOverlay.clear();
					return;
				}
				notificationOverlay.flash((remaining[0] & 1) == 0);
			});
		}, 1, 1, TimeUnit.SECONDS);
	}

	private boolean inConfiguredChat()
	{
		if (config.ccName() == null || config.ccName().trim().isEmpty())
		{
			return false;
		}
		FriendsChatManager manager = client.getFriendsChatManager();
		if (manager == null || manager.getOwner() == null)
		{
			return false;
		}
		return Text.standardize(manager.getOwner()).equalsIgnoreCase(Text.standardize(config.ccName()));
	}

	private FriendsChatMember member(String name)
	{
		FriendsChatManager manager = client.getFriendsChatManager();
		if (manager == null || name == null)
		{
			return null;
		}
		return manager.findByName(Text.removeTags(name));
	}

	private void reply(ChatMessage message, String text)
	{
		client.addChatMessage(message.getType(), "", text, null);
	}

	private static String playerName(String target)
	{
		String cleaned = Text.removeTags(target == null ? "" : target).trim();
		int level = cleaned.toLowerCase().indexOf("(level");
		if (level >= 0)
		{
			cleaned = cleaned.substring(0, level).trim();
		}
		return cleaned;
	}

	public static int parseAmount(String raw)
	{
		String value = raw.replace(",", "").replace(" ", "").trim().toLowerCase();
		if (value.isEmpty())
		{
			return 0;
		}
		double multiplier = 1;
		char suffix = value.charAt(value.length() - 1);
		if (suffix == 'k' || suffix == 'm' || suffix == 'b')
		{
			value = value.substring(0, value.length() - 1);
			if (suffix == 'k')
			{
				multiplier = 1_000;
			}
			else if (suffix == 'm')
			{
				multiplier = 1_000_000;
			}
			else
			{
				multiplier = 1_000_000_000d;
			}
		}
		return (int) Math.round(Double.parseDouble(value) * multiplier);
	}

	public static String formatGp(long number)
	{
		long absolute = Math.abs(number);
		if (absolute >= 1_000_000_000L)
		{
			return String.format("%.2fB", number / 1_000_000_000d);
		}
		if (absolute >= 1_000_000L)
		{
			return String.format("%.2fM", number / 1_000_000d);
		}
		if (absolute >= 10_000L)
		{
			return String.format("%.1fK", number / 1_000d);
		}
		return new DecimalFormat("#,###").format(number);
	}

	public static String formatTime(long seconds)
	{
		if (seconds < 0)
		{
			seconds = 0;
		}
		long hour = seconds / 3600;
		long minute = (seconds / 60) % 60;
		long second = seconds % 60;
		return String.format("%02d:%02d:%02d", hour, minute, second);
	}

	private static BufferedImage scale(BufferedImage image, int scale)
	{
		int safeScale = Math.max(1, Math.min(scale, 20));
		int size = Math.max(16, Math.round(60f * safeScale / 10f));
		Image scaled = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
		BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = output.createGraphics();
		graphics.drawImage(scaled, 0, 0, null);
		graphics.dispose();
		return output;
	}

	private static BufferedImage createIcon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(196, 149, 74));
		graphics.fillRoundRect(0, 0, size - 1, size - 1, size / 4, size / 4);
		graphics.setColor(new Color(70, 44, 24));
		graphics.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, size / 2)));
		graphics.drawString("R", size / 5, size * 2 / 3);
		graphics.dispose();
		return image;
	}
}
