package com.mvas.webproxy.config;


import java.util.ArrayList;

public interface IConfig {
    void init();
    public String get(final String group, final String name);
    public String set(final String group, final String name, final Object value);
    public boolean has(final String group, final String name);
    public ArrayList<String> getGroups();
}
