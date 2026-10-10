/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.Configuration
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package me.Mixer.bluemapresidence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
import java.util.logging.Level;
import me.Mixer.bluemapresidence.Main;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class config {
    private final Main plugin;
    private FileConfiguration dataConfig = null;
    private File configFile = null;
    private final String configname = "config.yml";

    public config(Main plugin) {
        this.plugin = plugin;
        this.saveDefaultConfig();
    }

    public void reloadConfig() {
        if (this.configFile == null) {
            this.configFile = new File(this.plugin.getDataFolder(), "config.yml");
        }
        this.dataConfig = YamlConfiguration.loadConfiguration((File)this.configFile);
        InputStream defaultStream = this.plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(defaultStream));
            this.dataConfig.setDefaults((Configuration)defaultConfig);
        }
    }

    public FileConfiguration getConfig() {
        if (this.dataConfig == null) {
            this.reloadConfig();
        }
        return this.dataConfig;
    }

    public void saveConfig() {
        if (this.dataConfig == null || this.configFile == null) {
            return;
        }
        try {
            this.getConfig().save(this.configFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not save config to " + String.valueOf(this.configFile), e);
        }
    }

    public void saveDefaultConfig() {
        if (this.configFile == null) {
            this.configFile = new File(this.plugin.getDataFolder(), "config.yml");
        }
        if (!this.configFile.exists()) {
            this.plugin.saveResource("config.yml", false);
        }
    }

    public void testCompareConfig() {
        if (!this.getConfig().getBoolean("config.auto_update", true)) {
            return;
        }
        YamlConfiguration serverConf = YamlConfiguration.loadConfiguration((File)new File(this.plugin.getDataFolder(), "config.yml"));
        InputStream defaultStream = this.plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(defaultStream));
            for (String key : defaultConfig.getKeys(true)) {
                if (serverConf.contains(key)) continue;
                this.plugin.info("&cMissing value in config for " + key + ", automatically setting up");
                String value = defaultConfig.getString(key);
                if (Objects.equals(value, "true") || Objects.equals(value, "false")) {
                    this.getConfig().set(key, (Object)defaultConfig.getBoolean(key));
                } else if (config.isInteger(value)) {
                    this.getConfig().set(key, (Object)defaultConfig.getInt(key));
                } else if (config.isDouble(value)) {
                    this.getConfig().set(key, (Object)defaultConfig.getDouble(key));
                } else {
                    this.getConfig().set(key, (Object)value);
                }
                this.saveConfig();
            }
            for (String key : serverConf.getKeys(true)) {
                if (defaultConfig.contains(key)) continue;
                this.plugin.info("&cKey " + key + " is useless, you can remove it");
            }
        }
    }

    public static boolean isInteger(String Parameter) {
        try {
            Integer.parseInt(Parameter);
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isDouble(String Parameter) {
        try {
            Double.parseDouble(Parameter);
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }
}

