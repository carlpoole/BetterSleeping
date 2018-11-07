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
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dieter Nuytemans
 */
public class OnSleepEvent implements Listener, Reloadable {

    private static final int NIGHT = 12516;

    private BetterSleeping plugin;

    private HashMap<String, BukkitTask> sleepingPlayerTasks = new HashMap<>();

    private int percentNeeded;
    private AtomicInteger playersSleeping = new AtomicInteger(0);
    private long sleepDelay;
    private String overworldName;
    private FileManagement configFile;
    private FileManagement langFile;

    private String prefix;
    private String player_name;
    private String enough_sleeping;
    private String amount_left_plural;
    private String amount_left_single;

    public OnSleepEvent(FileManagement configFile, FileManagement langFile, BetterSleeping plugin) {
        this.plugin = plugin;
        this.configFile = configFile;
        this.langFile = langFile;
        reload();
    }

    @EventHandler
    public void onPlayerEnterBed(PlayerBedEnterEvent e) {
        playersSleeping.getAndIncrement();
        Player sleepingPlayer = e.getPlayer();
        sleepingPlayerTasks.put(sleepingPlayer.getDisplayName(),
                Bukkit.getServer().getScheduler().runTaskLater(
                        plugin, () -> sleepCheck(sleepingPlayer.getDisplayName()), sleepDelay)
        );
    }

    @EventHandler
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

    private boolean isNight() {
        return Bukkit.getWorld(overworldName).getTime() >= NIGHT;
    }

    private void sleepCheck(String playerName) {
        int numNeeded = playersNeeded();

        if (isNight()
                && sleepingPlayerTasks.containsKey(playerName)
                && !sleepingPlayerTasks.get(playerName).isCancelled()) {

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

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(prefix + enough_sleeping);
                }
            } else {
                int numLeft = numNeeded - playersSleeping.get();
                String msg = "";

                if (numLeft > 1) {
                    msg += amount_left_plural.replaceAll("<amount>", Integer.toString(numLeft));
                } else if (numLeft > 0) {
                    msg = amount_left_single.replaceAll("<amount>", Integer.toString(numLeft));
                }

                if (!msg.isEmpty()) {
                    msg = player_name.replaceAll("<player>", playerName) + " " + msg;

                    if (isNight() && sleepingPlayerTasks.containsKey(playerName) && !sleepingPlayerTasks.get(playerName).isCancelled()) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage(prefix + msg);
                        }
                    }
                }
            }
        }

        sleepingPlayerTasks.remove(playerName);
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
        return (int) Math.ceil(needed);
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

        if (langFile.contains("prefix"))
            prefix = langFile.getString("prefix");
        else prefix = "§6[BetterSleeping] §3";

        if (langFile.contains("enough_sleeping"))
            enough_sleeping = langFile.getString("enough_sleeping");
        else enough_sleeping = "Enough people are sleeping now!";

        if (langFile.contains("player_name"))
            player_name = langFile.getString("player_name");
        else player_name = "<player> went to sleep.";

        if (langFile.contains("amount_left_plural"))
            amount_left_plural = langFile.getString("amount_left_plural");
        else amount_left_plural = "There are <amount> more people needed to skip the night/storm!";

        if (langFile.contains("amount_left_single"))
            amount_left_single = langFile.getString("amount_left_single");
        else amount_left_single = "Only <amount> more person needs to sleep to skip the night/storm!";
    }
}
