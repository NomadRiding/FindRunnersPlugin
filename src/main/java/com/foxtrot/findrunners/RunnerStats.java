package com.foxtrot.findrunners;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.Text;

@Slf4j
public class RunnerStats
{
	private transient Gson gson;
	private transient Filepath dataFile;

	public Map<String, RunnerData> runnerHistory = new HashMap<>();
	public RunnerData currentRun;
	public String lastRunKey;

	public void attach(Gson gson, Filepath dataFile)
	{
		this.gson = gson;
		this.dataFile = dataFile;
	}

	public void startRun(String client, int costPerTrip, RunType type)
	{
		if (currentRun != null)
		{
			endRun();
		}
		currentRun = new RunnerData(client, LocalDateTime.now(), costPerTrip, type);
	}

	public void addTrip(String client, String bones, int count)
	{
		if (currentRun == null || bones.isEmpty() || client.isEmpty() || count <= 0)
		{
			return;
		}
		if (currentRun.client.equalsIgnoreCase(Text.standardize(client)))
		{
			currentRun.addTrip(bones, count);
		}
	}

	public void endRun()
	{
		if (currentRun == null)
		{
			return;
		}
		currentRun.end();
		lastRunKey = currentRun.startTime;
		runnerHistory.put(lastRunKey, currentRun);
		currentRun = null;
	}

	public int getTotalBones()
	{
		int total = 0;
		for (RunnerData data : runnerHistory.values())
		{
			total += data.boneCount();
		}
		if (currentRun != null)
		{
			total += currentRun.boneCount();
		}
		return total;
	}

	public int getTotalMade()
	{
		int total = 0;
		for (RunnerData data : runnerHistory.values())
		{
			total += data.totalPrice;
		}
		if (currentRun != null)
		{
			total += currentRun.totalPrice;
		}
		return total;
	}

	@Nullable
	public RunnerData getLastRun()
	{
		if (lastRunKey == null)
		{
			return null;
		}
		return runnerHistory.get(lastRunKey);
	}

	public void save()
	{
		if (gson == null || dataFile == null)
		{
			return;
		}
		try
		{
			Filepath parent = dataFile.getParent();
			if (!parent.exists())
			{
				parent.createDirectories();
			}
			try (Writer writer = dataFile.openWriter())
			{
				gson.toJson(this, writer);
			}
		}
		catch (IOException e)
		{
			log.debug("Could not save run history", e);
		}
	}

	public static RunnerStats load(Gson gson, Filepath dataFile)
	{
		RunnerStats stats = new RunnerStats();
		stats.attach(gson, dataFile);
		if (!dataFile.exists())
		{
			return stats;
		}
		try (Reader reader = dataFile.openReader())
		{
			RunnerStats loaded = gson.fromJson(reader, RunnerStats.class);
			if (loaded == null)
			{
				return stats;
			}
			if (loaded.runnerHistory == null)
			{
				loaded.runnerHistory = new HashMap<>();
			}
			loaded.attach(gson, dataFile);
			return loaded;
		}
		catch (Exception e)
		{
			log.debug("Could not load run history", e);
			return stats;
		}
	}

	public static class RunnerData
	{
		public String client = "";
		public String startTime = "";
		public String endTime = "";
		public Map<String, Integer> items = new HashMap<>();
		public RunType type = RunType.none;
		public int price;
		public int totalPrice;
		public long totalTime;

		public RunnerData()
		{
		}

		public RunnerData(String client, LocalDateTime startTime, int price, RunType type)
		{
			this.client = client;
			this.startTime = startTime.toString();
			this.price = price;
			this.type = type;
		}

		public void addTrip(String item, int quantity)
		{
			totalPrice += price - (quantity * 5);
			items.put(item, items.getOrDefault(item, 0) + quantity);
		}

		public void end()
		{
			LocalDateTime now = LocalDateTime.now();
			totalTime = LocalDateTime.parse(startTime).until(now, ChronoUnit.SECONDS);
			endTime = now.toString();
		}

		public int boneCount()
		{
			int total = 0;
			for (int qty : items.values())
			{
				total += qty;
			}
			return total;
		}
	}
}
