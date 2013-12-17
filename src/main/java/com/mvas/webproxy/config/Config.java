package com.mvas.webproxy.config;

import java.util.ArrayList;

public class Config implements IConfig {

    private static volatile IConfig configClass;
    private static volatile Config instance;
    private static boolean initialized = false;

    private Config()
    {

    }

    public static Config getInstance(final IConfig configClass)
    {
        Config.configClass = configClass;
        return getInstance();
    }

    public static Config getInstance() {
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
            throw new IllegalStateException();
        initialized = true;
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
