package com.mvas.webproxy.config;

import org.ini4j.Ini;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;


public class IniConfig implements IConfig {
    private Logger logger = LoggerFactory.getLogger(IniConfig.class);

    public static final String CONFIG_FILE_NAME = ".webproxy.ini";

    static Ini iniFile;

    @Override
    public synchronized void init() {
        try {
            String fileName = System.getProperty("user.home") + "/" + CONFIG_FILE_NAME;

            File configFile = new File(fileName);

            boolean valid = true;
            if(!configFile.exists())
                valid = configFile.createNewFile();

            if(!valid)
            {
                logger.error("Cannot create configuration file");
                iniFile = new Ini();
                return;
            }

            iniFile = new Ini(new File(fileName));
            logger.debug("Using configuration from " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized String get(String group, String name) {
        //logger.debug("iniFile: " + iniFile);
        String result = iniFile.get(group, name);
        logger.info("ini get {group:" + group + ", name:" + name + "} -> " + result);
        return result;
    }

    @Override
    public synchronized String set(String group, String name, Object value) {
        logger.info("ini set {group:" + group + ", name:" + name + ", value:" + value + "}");
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
