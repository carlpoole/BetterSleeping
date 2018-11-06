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

/**
 * @author Dieter Nuytemans
 */
public class OnSleepEvent implements Listener, Reloadable {

    private BetterSleeping plugin;

    private int percentNeeded;
    private int playersSleeping;
    private long sleepDelay;
    private FileManagement configFile;
    private FileManagement langFile;

    private String prefix;
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
        playersSleeping++;
        int numNeeded = playersNeeded();

        if (playersSleeping >= numNeeded) {
            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (playersSleeping >= numNeeded) {
                    for (World world : Bukkit.getWorlds()) {
                        world.setStorm(false);
                        world.setTime(1000);
                    }

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage(prefix + enough_sleeping);
                    }
                }
            }, sleepDelay);

        } else {
            int numLeft = numNeeded - playersSleeping;
            String msg = "";

            if (numLeft > 1) {
                msg = amount_left_plural.replaceAll("<amount>", Integer.toString(numLeft));
            } else if (numLeft > 0) {
                msg = amount_left_single.replaceAll("<amount>", Integer.toString(numLeft));
            }

            if (!msg.isEmpty()) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(prefix + msg);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLeaveBed(PlayerBedLeaveEvent e) {
        playersSleeping--;
    }

    /**
     * Calculate the amount of players needed according to the settings and current online players
     *
     * @return float
     */
    private int playersNeeded() {
        // Todo: this needs to only reflect overworld players
        int numOnline = Bukkit.getOnlinePlayers().size();
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

        if (langFile.contains("prefix"))
            prefix = langFile.getString("prefix");
        else prefix = "§6[BetterSleeping] §3";

        if (langFile.contains("enough_sleeping"))
            enough_sleeping = langFile.getString("enough_sleeping");
        else enough_sleeping = "Enough people are sleeping now!";

        if (langFile.contains("amount_left_plural"))
            amount_left_plural = langFile.getString("amount_left_plural");
        else amount_left_plural = "There are <amount> more people needed to skip the night/storm!";

        if (langFile.contains("amount_left_single"))
            amount_left_single = langFile.getString("amount_left_single");
        else amount_left_single = "Only <amount> more person needs to sleep to skip the night/storm!";
    }
}
