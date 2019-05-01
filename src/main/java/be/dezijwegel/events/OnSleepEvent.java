package be.dezijwegel.events;

import be.dezijwegel.bettersleeping.BetterSleeping;
import be.dezijwegel.bettersleeping.Reloadable;
import be.dezijwegel.files.FileManagement;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dieter Nuytemans
 */
public class OnSleepEvent implements Listener, Reloadable {

    private static final int NIGHT = 12516;

    private BetterSleeping plugin;
    private Random randomGenerator = new Random();
    ;

    private HashMap<String, BukkitTask> sleepingPlayerTasks = new HashMap<>();
    private AtomicInteger playersSleeping = new AtomicInteger(0);

    private FileManagement configFile;
    private FileManagement langFile;

    // Config options
    private int percentNeeded;
    private long sleepDelay;
    private String overworldName;
    private boolean randomizedMessages;

    // Language strings
    private String prefix;
    private String player_name;
    private List<String> randomMessages;
    private String enough_sleeping;
    private String amount_left_plural;
    private String amount_left_single;

    public OnSleepEvent(FileManagement configFile, FileManagement langFile, BetterSleeping plugin) {
        this.plugin = plugin;
        this.configFile = configFile;
        this.langFile = langFile;
        reload();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerEnterBed(PlayerBedEnterEvent e) {
        Player sleepingPlayer = e.getPlayer();
        String sleepingPlayerName = sleepingPlayer.getDisplayName();

        // Need to check if player is actually sleeping first or else can get triggered on "Monsters nearby" etc.
        if (e.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            playersSleeping.getAndIncrement();
            sleepingPlayerTasks.put(sleepingPlayerName,
                    Bukkit.getServer().getScheduler().runTaskLater(
                            plugin, () -> sleepCheck(sleepingPlayer), sleepDelay)
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeaveBed(PlayerBedLeaveEvent e) {

        // Stops the count going below zero after morning and everyone leaves bed.
        if (playersSleeping.getAndDecrement() <= 0) {
            playersSleeping.set(0);
        }

        String playerName = e.getPlayer().getDisplayName();

        BukkitTask sleepingPlayerTask = sleepingPlayerTasks.get(playerName);
        if (sleepingPlayerTask != null) sleepingPlayerTask.cancel();
        sleepingPlayerTasks.remove(playerName);
    }

    private boolean isNightOrStorming() {
        World world = Bukkit.getWorld(overworldName);
        return world.isThundering() || world.getTime() >= NIGHT;
    }

    private void sleepCheck(Player sleepingPlayer) {
        int numNeeded = playersNeeded();
        String sleepingPlayerName = sleepingPlayer.getName();

        if (isNightOrStorming()
                && sleepingPlayerTasks.containsKey(sleepingPlayerName)
                && !sleepingPlayerTasks.get(sleepingPlayerName).isCancelled()) {

            String sleepMessage = randomizedMessages ? getRandomizedSleepmessage(sleepingPlayer)
                    : player_name.replaceAll("<player>", sleepingPlayerName);

            if (playersSleeping.get() >= numNeeded) {
                for (BukkitTask sleepTask : sleepingPlayerTasks.values()) {
                    sleepTask.cancel();
                }
                sleepingPlayerTasks.clear();
                playersSleeping.set(0);

                for (World world : Bukkit.getWorlds()) {
                    world.setStorm(false);
                    world.setTime(0);
                }
            } else {
                int numLeft = numNeeded - playersSleeping.get();
                String remainingMessage = "";

                if (numLeft > 1) {
                    remainingMessage += amount_left_plural.replaceAll("<amount>", Integer.toString(numLeft));
                } else if (numLeft > 0) {
                    remainingMessage = amount_left_single.replaceAll("<amount>", Integer.toString(numLeft));
                }

                sleepMessage = sleepMessage + " " + remainingMessage;

                if (!sleepingPlayerTasks.containsKey(sleepingPlayerName)
                        || sleepingPlayerTasks.get(sleepingPlayerName).isCancelled()) {
                    return;
                }
            }

            sendMessageToPlayers(sleepMessage);
        }

        sleepingPlayerTasks.remove(sleepingPlayerName);
    }

    /**
     * Calculate the amount of players needed according to the settings and current online players in the overworld only
     *
     * @return float
     */
    private int playersNeeded() {
        int numOnline;

        try {
            numOnline = Bukkit.getWorld(overworldName).getPlayers().size();
        } catch (NullPointerException e) {
            // If the world wasn't found then just get online player total
            numOnline = Bukkit.getOnlinePlayers().size();
        }

        float needed = (percentNeeded * numOnline / 100.0f);
        return (int) Math.floor(needed);
    }

    private String getRandomizedSleepmessage(Player sleepingPlayer) {
        String randomMessage = randomMessages.get(randomGenerator.nextInt(randomMessages.size())).replaceAll("<player>", sleepingPlayer.getName());

        if (randomMessage.contains("<otherplayer>")) {
            List<Player> players = Bukkit.getWorld(overworldName).getPlayers();
            String randomPlayer;

            players.remove(sleepingPlayer);

            if (players.size() > 0)
                randomPlayer = players.get(randomGenerator.nextInt(players.size())).getName();
            else
                randomPlayer = "themselves";

            return randomMessage.replaceAll("<otherplayer>", randomPlayer);
        }

        return randomMessage;
    }

    private void sendMessageToPlayers(String message) {
        Bukkit.broadcastMessage(prefix + message);
    }

    /**
     * Reload all config settings from the confg files into this object
     */
    @Override
    public void reload() {
        if (configFile.contains("sleep_delay"))
            sleepDelay = configFile.getLong("sleep_delay");
        else {
            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
            console.sendMessage("[BetterSleeping]" + ChatColor.GREEN + "New configuration option(s) found!");
            console.sendMessage("[BetterSleeping]" + ChatColor.RED + "Resetting the config file..");
            sleepDelay = 40;
            configFile.forceDefaultConfig();
        }

        if (configFile.contains("percentage_needed")) {
            percentNeeded = configFile.getInt("percentage_needed");

            if (percentNeeded > 100) percentNeeded = 100;
            else if (percentNeeded < 1) percentNeeded = 1;
        } else percentNeeded = 30;

        if (configFile.contains("overworld_name")) {
            overworldName = configFile.getString("overworld_name");
        } else {
            overworldName = "world";
        }

        if (configFile.contains("randomize_sleep_messages")) {
            randomizedMessages = configFile.getBoolean("randomize_sleep_messages");
        } else {
            randomizedMessages = false;
        }

        if (langFile.contains("prefix"))
            prefix = langFile.getString("prefix");
        else prefix = "§6[BetterSleeping] §3";

        if (langFile.contains("enough_sleeping"))
            enough_sleeping = langFile.getString("enough_sleeping");
        else enough_sleeping = "Enough people are sleeping now!";

        if (langFile.contains("player_name"))
            player_name = langFile.getString("player_name");
        else player_name = "<player> went to sleep.";

        if (langFile.contains("random_sleep_messages")) {
            randomMessages = langFile.getStringArray("random_sleep_messages");
        } else {
            randomMessages = new ArrayList<>();
            randomMessages.add("<player> went to sleep.");
        }

        if (langFile.contains("amount_left_plural"))
            amount_left_plural = langFile.getString("amount_left_plural");
        else amount_left_plural = "There are <amount> more people needed to skip the night/storm!";

        if (langFile.contains("amount_left_single"))
            amount_left_single = langFile.getString("amount_left_single");
        else amount_left_single = "Only <amount> more person needs to sleep to skip the night/storm!";
    }
}
