package com.mvas.webproxy.config;

import org.ini4j.Ini;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class IniConfig implements IConfig {
    private Logger logger = LoggerFactory.getLogger(IniConfig.class);

    Ini iniFile;
    Properties properties = new Properties();
    String configFileName;

    public IniConfig()
    {
        this(new HashMap<String, String>());
    }

    public IniConfig(HashMap<String, String> args)
    {
        InputStream configStream = null;
        try {
            configStream = getClass().getResourceAsStream("/config.properties");
            properties.load(configStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(configStream != null)
                try {
                    configStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        if(args.containsKey("config"))
            configFileName = args.get("config");
        else
            configFileName = properties.getProperty("config").replace("~",System.getProperty("user.home"));
    }

    @Override
    public synchronized void init() throws IOException
    {
        File configFile = new File(configFileName);

        boolean valid = true;
        if(!configFile.exists())
        {
            File parent = configFile.getParentFile();
            if(!parent.canWrite())
                throw new IOException("Cannot create file "  + configFileName );
            valid = configFile.createNewFile();
        }


        if(!valid)
        {
            throw new IOException("Cannot create configuration file "  + configFileName);
        }

        iniFile = new Ini(configFile);
        logger.info("Using configuration from " + configFileName);
    }

    @Override
    public synchronized String get(String group, String name) {
        //logger.debug("iniFile: " + iniFile);
        String result = iniFile.get(group, name);
        logger.debug("ini get {group:" + group + ", name:" + name + "} -> " + result);
        return result;
    }

    @Override
    public synchronized String set(String group, String name, Object value) {
        logger.debug("ini set {group:" + group + ", name:" + name + ", value:" + value + "}");
        String result = iniFile.put(group, name, value);
        try {
            iniFile.store();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public boolean has(String group, String name) {
        return iniFile.containsKey(name);
    }

    @Override
    public ArrayList<String> getGroups() {
        ArrayList<String> result = new ArrayList<>();
        for(Map.Entry<String, Profile.Section> entry: iniFile.entrySet())
        {
            result.add(entry.getKey());
        }
        return result;
    }
}
