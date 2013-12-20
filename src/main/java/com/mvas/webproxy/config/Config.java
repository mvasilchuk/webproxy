package com.mvas.webproxy.config;

import java.io.IOException;
import java.util.ArrayList;

public class Config implements IConfig {

    private static volatile IConfig configClass;
    private static volatile Config instance;

    private Config()
    {

    }

    public static Config getInstance(final IConfig configClass) throws IOException
    {
        Config.configClass = configClass;
        return getInstance();
    }

    public static Config getInstance() throws IOException {
        Config localInstance = instance;
        if (localInstance == null) {
            synchronized (Config.class) {
                localInstance = instance;
                if (localInstance == null) {
                    if(configClass == null)
                        throw new IllegalStateException("Config class not defined");
                    instance = localInstance = new Config();
                    configClass.init();

                }
            }
        }
        return localInstance;
    }

    @Override
    public synchronized void init() {
        if(configClass == null)
            throw new IllegalStateException("Configuration class not defined");
    }

    @Override
    public synchronized String get(String group, String name) {
        return configClass.get(group, name);
    }

    @Override
    public synchronized String set(String group, String name, Object value) {
        return configClass.set(group, name, value);
    }

    @Override
    public boolean has(String group, String name) {
        return configClass.has(group, name);
    }

    @Override
    public ArrayList<String> getGroups() {
        return configClass.getGroups();
    }


}
