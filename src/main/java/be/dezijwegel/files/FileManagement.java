package be.dezijwegel.files;

import be.dezijwegel.bettersleeping.BetterSleeping;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class FileManagement {

    private FileConfiguration configuration;
    private File file;
    private FileType type;
    private BetterSleeping plugin;

    public enum FileType {
        CONFIG,
        LANG
    }
    
    /**
     * @param type of file
     * @param plugin
     */
    public FileManagement(FileType type, BetterSleeping plugin) {
        this.plugin = plugin;
                
        switch (type) {
            case CONFIG:
                this.file = new File(plugin.getDataFolder(), "config.yml");
                this.configuration = YamlConfiguration.loadConfiguration(file);
                this.type = type;
                break;
            case LANG:
                this.file = new File(plugin.getDataFolder(), "lang.yml");
                this.configuration = YamlConfiguration.loadConfiguration(file);
                this.type = type;
                break; 
        }

    }

    /**
     * Get an Object from a file
     * @param path
     * @return Object
     */
    public Object get(String path) {
        return configuration.get(path);
    }

    /**
     * Get a String from a file
     * @param path
     * @return String
     */
    public String getString(String path) {
        String string = configuration.getString(path);
        if (string != null)
        {
            if (string.contains("&")) string = string.replaceAll("&", "§");
        }
        return string;
    }

    /**
     * Get an integer from a file
     * @param path
     * @return int
     */
    public int getInt(String path)
    {
        return configuration.getInt(path);
    }
    
    /**
     * Get a long from a file
     * @param path
     * @return 
     */
    public long getLong(String path)
    {
        return configuration.getLong(path);
    }

    /**
     * Get a boolean from a file
     * @param path
     * @return
     */
    public boolean getBoolean(String path) {
        return configuration.getBoolean(path);
    }

    /**
     * Get a String array from the file
     * @param path
     * @return
     */
    public List<String> getStringArray(String path) {
        return configuration.getStringList(path);
    }
    
    /**
     * Check if the file contains a specific path
     * @param path
     * @return 
     */
    public boolean contains(String path)
    {
        return configuration.contains(path);
    }
    
    /**
     * This method will force the file back to its default
     * This is helpful when new options are added and comments are needed
     */
    public void forceDefaultConfig()
    {
       switch (type) {
            case CONFIG:
                plugin.saveResource("config.yml", true);
                break;
            case LANG:
                plugin.saveResource("lang.yml", true);
                break;
        }
    }
    
    /**
     * Reload the config file
     */
    public void reloadFile() {
        if (configuration == null) {
            switch (type) {
                case CONFIG:
                    file = new File(plugin.getDataFolder(), "config.yml");
                    break;
                case LANG:
                    file = new File(plugin.getDataFolder(), "lang.yml");
                    break;
            }
        }
        configuration = YamlConfiguration.loadConfiguration(file);

        // Look for defaults in the jar
        Reader defConfigStream = null;
        try {
            switch (type) {
                case CONFIG:
                    defConfigStream = new InputStreamReader(plugin.getResource("config.yml"), "UTF8");
                    break;
                case LANG:
                    defConfigStream = new InputStreamReader(plugin.getResource("lang.yml"), "UTF8");
                    break;
            }
        } catch (UnsupportedEncodingException ex) {}
        
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            configuration.setDefaults(defConfig);
        }
    }

    public void saveDefaultConfig() {
        if (file == null) {
            switch (type) {
                case CONFIG:
                    file = new File(plugin.getDataFolder(), "config.yml");
                    break;
                case LANG:
                    file = new File(plugin.getDataFolder(), "lang.yml");
                    break;
            }
        }
        if (!file.exists()) {
            switch (type) {
                case CONFIG:
                    plugin.saveResource("config.yml", false);
                    break;
                case LANG:
                    plugin.saveResource("lang.yml", false);
                    break;
            }
        }
    }
}
