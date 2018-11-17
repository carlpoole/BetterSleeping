/*
 * Geen license header toegevoegd
 * Dieter Nuytemans
 */
package be.dezijwegel.events;

import be.dezijwegel.bettersleeping.BetterSleeping;
import be.dezijwegel.files.FileManagement;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;

/**
 *
 * @author Dieter Nuytemans
 * OnSleepEventLocal will consider each world seperate from eachother
 */
public class OnSleepEventLocal extends OnSleepEvent{
    
    private BetterSleeping plugin;
    
    private HashMap<String, Integer> playersSleeping;
    
    public OnSleepEventLocal(FileManagement configFile, FileManagement langFile, BetterSleeping plugin) {
        super(configFile, langFile);
        
        this.plugin = plugin;
        
        playersSleeping = new HashMap<>();
        for (World world : Bukkit.getWorlds())
        {
            playersSleeping.put(world.getName(), 0);
        }
    }
    
    @EventHandler
    public void onPlayerEnterBed(PlayerBedEnterEvent e)
    {
        World worldObj = e.getPlayer().getWorld();
        String world = worldObj.getName();
        
        playersSleeping.put(world, playersSleeping.get(world) + 1);
        
        float numNeeded = playersNeeded(world);
        
        int numSleeping = playersSleeping.get(world);
        
        if (numSleeping >= numNeeded)
        {
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (p.getWorld().getName().equals(world))
                    p.sendMessage(prefix + enough_sleeping);
            }
            
            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (numSleeping >= numNeeded) {
                    worldObj.setStorm(false);
                    worldObj.setTime(1000);
                    for (Player p : Bukkit.getOnlinePlayers())
                    {
                        if (p.getWorld().getName().equals(world))
                            p.sendMessage(prefix + good_morning);
                    }
                }
                
                
            }, sleepDelay);
        } else {
            float numLeft = numNeeded - numSleeping;
            if (numLeft > 0 ) {
                
                String msg = amount_left.replaceAll("<amount>", Integer.toString((int) Math.round(numLeft)));
                
                for (Player p : Bukkit.getOnlinePlayers())
                {
                    if (p.getWorld().getName().equals(world))
                    p.sendMessage(prefix + msg);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerLeaveBed(PlayerBedLeaveEvent e)
    {
        String world = e.getPlayer().getWorld().getName();
        
        playersSleeping.put(world, playersSleeping.get(world) - 1);
    }
    
    /**
     * Calculate the amount of players needed to sleep in a specific world
     * @param world
     * @return float
     */
    public float playersNeeded(String world)
    {
        int numInWorld = 0;
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (p.getWorld().getName().equals(world)) numInWorld++;
        }
        
        return (playersNeeded * numInWorld / 100.0f);
    }
    
}